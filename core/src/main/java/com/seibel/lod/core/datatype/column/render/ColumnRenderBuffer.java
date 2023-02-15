package com.seibel.lod.core.datatype.column.render;

import com.seibel.lod.core.datatype.column.ColumnRenderSource;
import com.seibel.lod.core.datatype.column.accessor.ColumnArrayView;
import com.seibel.lod.core.datatype.column.accessor.ColumnFormat;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.render.renderer.LodRenderer;
import com.seibel.lod.core.util.objects.UncheckedInterruptedException;
import com.seibel.lod.core.render.AbstractRenderBuffer;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.api.enums.config.EGpuUploadMethod;
import com.seibel.lod.api.enums.rendering.EDebugMode;
import com.seibel.lod.api.enums.rendering.EGLProxyContext;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.render.glObject.GLProxy;
import com.seibel.lod.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.lod.core.util.*;
import com.seibel.lod.core.util.objects.Reference;
import com.seibel.lod.core.util.objects.StatsMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.*;

import static com.seibel.lod.core.render.glObject.GLProxy.GL_LOGGER;


public class ColumnRenderBuffer extends AbstractRenderBuffer
{
    //TODO: Make the pool change thread count after the config value is changed
    public static final ExecutorService BUFFER_BUILDERS = LodUtil.makeThreadPool(Config.Client.Advanced.Threading.numberOfBufferBuilderThreads.get(), "BufferBuilder");
    public static final ExecutorService BUFFER_UPLOADER = LodUtil.makeSingleThreadPool("ColumnBufferUploader");
    public static final int MAX_CONCURRENT_CALL = 8;

    public static final ConfigBasedLogger EVENT_LOGGER = new ConfigBasedLogger(LogManager.getLogger(),
            () -> Config.Client.Advanced.Debugging.DebugSwitch.logRendererBufferEvent.get());
    private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    private static final long MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS = 1_000_000;
    GLVertexBuffer[] vbos;
    GLVertexBuffer[] vbosTransparent;
    public final DhBlockPos pos;
	
	
	
    public ColumnRenderBuffer(DhBlockPos pos) {
        this.pos = pos;
        vbos = new GLVertexBuffer[0];
        vbosTransparent = new GLVertexBuffer[0];
    }

	
	
    private static void _doUploadBuffersDirect(GLVertexBuffer[] vbos, Iterator<ByteBuffer> iter, EGpuUploadMethod method) throws InterruptedException {
        long remainingNS = 0;
        long BPerNS = Config.Client.Advanced.Buffers.gpuUploadPerMegabyteInMilliseconds.get();
        int i = 0;
        while (iter.hasNext()) {
            if (i >= vbos.length) {
                throw new RuntimeException("Too many vertex buffers!!");
            }
            ByteBuffer bb = iter.next();
            GLVertexBuffer vbo = getOrMake(vbos, i++, method.useBufferStorage);
            int size = bb.limit() - bb.position();
            try {
                vbo.bind();
                vbo.uploadBuffer(bb, size/LodUtil.LOD_VERTEX_FORMAT.getByteSize(), method, FULL_SIZED_BUFFER);
            } catch (Exception e) {
                vbos[i-1] = null;
                vbo.close();
                LOGGER.error("Failed to upload buffer: ", e);
            }
            if (BPerNS<=0) continue;
            // upload buffers over an extended period of time
            // to hopefully prevent stuttering.
            remainingNS += size * BPerNS;
            if (remainingNS >= TimeUnit.NANOSECONDS.convert(1000 / 60, TimeUnit.MILLISECONDS)) {
                if (remainingNS > MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS)
                    remainingNS = MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS;
                Thread.sleep(remainingNS / 1000000, (int) (remainingNS % 1000000));
                remainingNS = 0;
            }
        }
        if (i < vbos.length) {
            throw new RuntimeException("Too few vertex buffers!!");
        }
    }


    private void _uploadBuffersDirect(LodQuadBuilder builder, EGpuUploadMethod method) throws InterruptedException {
        vbos = resize(vbos, builder.getCurrentNeededOpaqueVertexBufferCount());
        _doUploadBuffersDirect(vbos, builder.makeOpaqueVertexBuffers(), method);
        vbosTransparent = resize(vbosTransparent, builder.getCurrentNeededTransparentVertexBufferCount());
        _doUploadBuffersDirect(vbosTransparent, builder.makeTransparentVertexBuffers(), method);
    }

    private void _uploadBuffersMapped(LodQuadBuilder builder, EGpuUploadMethod method)
    {
        vbos = resize(vbos, builder.getCurrentNeededOpaqueVertexBufferCount());
        for (int i=0; i<vbos.length; i++) {
            if (vbos[i]==null) vbos[i] = new GLVertexBuffer(method.useBufferStorage);
        }
        LodQuadBuilder.BufferFiller func = builder.makeOpaqueBufferFiller(method);
        {
            int i = 0;
            while (i < vbos.length && func.fill(vbos[i++])) {
            }
        }
        vbosTransparent = resize(vbosTransparent, builder.getCurrentNeededTransparentVertexBufferCount());
        for (int i=0; i<vbosTransparent.length; i++) {
            if (vbosTransparent[i]==null) vbosTransparent[i] = new GLVertexBuffer(method.useBufferStorage);
        }
        func = builder.makeTransparentBufferFiller(method);
        {
            int i = 0;
            while (i < vbosTransparent.length && func.fill(vbosTransparent[i++])) {
            }
        }
    }

    private static GLVertexBuffer[] resize(GLVertexBuffer[] vbos, int newSize) {
        if (vbos.length == newSize) return vbos;
        GLVertexBuffer[] newVbos = new GLVertexBuffer[newSize];
        System.arraycopy(vbos, 0, newVbos, 0, Math.min(vbos.length, newSize));
        if (newSize < vbos.length) {
            for (int i = newSize; i < vbos.length; i++) {
                if (vbos[i] != null) {
                    vbos[i].close();
                }
            }
        }
        return newVbos;
    }

    private static GLVertexBuffer getOrMake(GLVertexBuffer[] vbos, int iIndex, boolean useBuffStorage) {
        if (vbos[iIndex] == null) {
            vbos[iIndex] = new GLVertexBuffer(useBuffStorage);
        }
        return vbos[iIndex];
    }

    public void uploadBuffer(LodQuadBuilder builder, EGpuUploadMethod method) throws InterruptedException {
        if (method.useEarlyMapping) {
            _uploadBuffersMapped(builder, method);
        } else {
            _uploadBuffersDirect(builder, method);
        }
    }

    @Override
    public boolean renderOpaque(LodRenderer renderContext) {
        boolean hasRendered = false;
        renderContext.setupOffset(pos);
        for (GLVertexBuffer vbo : vbos) {
            if (vbo == null) continue;
            if (vbo.getVertexCount() == 0) continue;
            hasRendered = true;
            renderContext.drawVbo(vbo);
            //LodRenderer.tickLogger.info("Vertex buffer: {}", vbo);
        }
        return hasRendered;
    }

    @Override
    public boolean renderTransparent(LodRenderer renderContext) {
        boolean hasRendered = false;
        renderContext.setupOffset(pos);
        for (GLVertexBuffer vbo : vbosTransparent) {
            if (vbo == null) continue;
            if (vbo.getVertexCount() == 0) continue;
            hasRendered = true;
            renderContext.drawVbo(vbo);
            //LodRenderer.tickLogger.info("Vertex buffer: {}", vbo);
        }
        return hasRendered;
    }

    @Override
    public void debugDumpStats(StatsMap statsMap) {
        statsMap.incStat("RenderBuffers");
        statsMap.incStat("SimpleRenderBuffers");
        for (GLVertexBuffer b : vbos) {
            if (b == null) continue;
            statsMap.incStat("VBOs");
            if (b.getSize() == FULL_SIZED_BUFFER) {
                statsMap.incStat("FullsizedVBOs");
            }
            if (b.getSize() == 0) GL_LOGGER.warn("VBO with size 0");
            statsMap.incBytesStat("TotalUsage", b.getSize());
        }
    }

    private boolean closed = false;
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        GLProxy.getInstance().recordOpenGlCall(() -> {
            for (GLVertexBuffer b : vbos) {
                if (b == null) continue;
                b.destroy(false);
            }
            for (GLVertexBuffer b : vbosTransparent) {
                if (b == null) continue;
                b.destroy(false);
            }
        });
    }

    private static long getCurrentJobsCount() {
        long jobs = ((ThreadPoolExecutor) BUFFER_BUILDERS).getQueue().stream().filter(t -> !((Future<?>) t).isDone()).count();
        jobs += ((ThreadPoolExecutor) BUFFER_UPLOADER).getQueue().stream().filter(t -> !((Future<?>) t).isDone()).count();
        return jobs;
    }

    public static boolean isBusy() {
        return getCurrentJobsCount() > MAX_CONCURRENT_CALL;
    }
	
	public static CompletableFuture<ColumnRenderBuffer> build(IDhClientLevel clientLevel, Reference<ColumnRenderBuffer> usedBufferSlot, ColumnRenderSource data, ColumnRenderSource[] adjData)
	{
		if (isBusy())
		{
			return null;
		}
		
		//LOGGER.info("RenderRegion startBuild @ {}", data.sectionPos);
		return CompletableFuture.supplyAsync(() -> 
			{
				try
				{
					boolean enableTransparency = Config.Client.Graphics.Quality.transparency.get().tranparencyEnabled;
					
					EVENT_LOGGER.trace("RenderRegion start QuadBuild @ {}", data.sectionPos);
					boolean enableSkyLightCulling = Config.Client.Graphics.AdvancedGraphics.enableCaveCulling.get();
					
					int skyLightCullingBelow = Config.Client.Graphics.AdvancedGraphics.caveCullingHeight.get();
					// FIXME: Clamp also to the max world height.
					skyLightCullingBelow = Math.max(skyLightCullingBelow, clientLevel.getMinY());
					
					LodQuadBuilder builder = new LodQuadBuilder(enableSkyLightCulling,
							(short) (skyLightCullingBelow - clientLevel.getMinY()), enableTransparency);
					
					makeLodRenderData(builder, data, adjData);
					EVENT_LOGGER.trace("RenderRegion end QuadBuild @ {}", data.sectionPos);
					return builder;
				}
				catch (UncheckedInterruptedException e)
				{
					throw e;
				}
				catch (Throwable e3)
				{
					LOGGER.error("\"LodNodeBufferBuilder\" was unable to build quads: ", e3);
					throw e3;
				}
				
			}, BUFFER_BUILDERS)
			.thenApplyAsync((quadBuilder) -> 
			{
				try
				{
					EVENT_LOGGER.trace("RenderRegion start Upload @ {}", data.sectionPos);
					GLProxy glProxy = GLProxy.getInstance();
					EGpuUploadMethod method = GLProxy.getInstance().getGpuUploadMethod();
					EGLProxyContext oldContext = glProxy.getGlContext();
					glProxy.setGlContext(EGLProxyContext.LOD_BUILDER);
					ColumnRenderBuffer buffer = usedBufferSlot.swap(null);
					
					if (buffer == null)
						buffer = new ColumnRenderBuffer(
								new DhBlockPos(data.sectionPos.getCorner().getCornerBlockPos(), clientLevel.getMinY())
						);
					try
					{
						buffer.uploadBuffer(quadBuilder, method);
						EVENT_LOGGER.trace("RenderRegion end Upload @ {}", data.sectionPos);
						return buffer;
					}
					catch (Exception e)
					{
						buffer.close();
						throw e;
					}
					finally
					{
						glProxy.setGlContext(oldContext);
					}
				}
				catch (InterruptedException e)
				{
					throw UncheckedInterruptedException.convert(e);
				}
				catch (Throwable e3)
				{
					LOGGER.error("\"LodNodeBufferBuilder\" was unable to upload buffer: ", e3);
					throw e3;
				}
			}, BUFFER_UPLOADER).handle((v, e) -> {
				//LOGGER.info("RenderRegion endBuild @ {}", data.sectionPos);
				if (e != null)
				{
					ColumnRenderBuffer buffer;
					if (!usedBufferSlot.isEmpty())
					{
						buffer = usedBufferSlot.swap(null);
						buffer.close();
					}
					return null;
				}
				else
				{
					return v;
				}
			});
	}



    private static void makeLodRenderData(LodQuadBuilder quadBuilder, ColumnRenderSource region, ColumnRenderSource[] adjRegions) {

        // Variable initialization
        EDebugMode debugMode = Config.Client.Advanced.Debugging.debugMode.get();

        byte detailLevel = region.getDataDetail();
        for (int x = 0; x < ColumnRenderSource.SECTION_SIZE; x++) {
            for (int z = 0; z < ColumnRenderSource.SECTION_SIZE; z++) {
                UncheckedInterruptedException.throwIfInterrupted();

                ColumnArrayView posData = region.getVerticalDataPointView(x, z);
                if (posData.size() == 0 || !ColumnFormat.doesDataPointExist(posData.get(0))
                        || ColumnFormat.isVoid(posData.get(0)))
                    continue;
                ColumnRenderSource.DebugSourceFlag debugSourceFlag = region.debugGetFlag(x, z);

                ColumnArrayView[][] adjData = new ColumnArrayView[4][];
                // We extract the adj data in the four cardinal direction

                // we first reset the adjShadeDisabled. This is used to disable the shade on the
                // border when we have transparent block like water or glass
                // to avoid having a "darker border" underground
                // Arrays.fill(adjShadeDisabled, false);

                // We check every adj block in each direction

                // If the adj block is rendered in the same region and with same detail
                // and is positioned in a place that is not going to be rendered by vanilla game
                // then we can set this position as adj
                // We avoid cases where the adjPosition is in player chunk while the position is
                // not
                // to always have a wall underwater
                for (ELodDirection lodDirection : ELodDirection.ADJ_DIRECTIONS) {
                    try {
                        int xAdj = x + lodDirection.getNormal().x;
                        int zAdj = z + lodDirection.getNormal().z;
                        boolean isCrossRegionBoundary = (xAdj < 0 || xAdj >= ColumnRenderSource.SECTION_SIZE) ||
                                (zAdj < 0 || zAdj >= ColumnRenderSource.SECTION_SIZE);
                        ColumnRenderSource adjRegion;
                        byte adjDetail;

                        //we check if the detail of the adjPos is equal to the correct one (region border fix)
                        //or if the detail is wrong by 1 value (region+circle border fix)
                        if (isCrossRegionBoundary) {
                            //we compute at which detail that position should be rendered
                            adjRegion = adjRegions[lodDirection.ordinal()-2];
                            if(adjRegion == null) continue;
                            adjDetail = adjRegion.getDataDetail();
                            if (adjDetail != detailLevel) {
                                //TODO: Implement this
                            } else {
                                if (xAdj < 0) xAdj += ColumnRenderSource.SECTION_SIZE;
                                if (zAdj < 0) zAdj += ColumnRenderSource.SECTION_SIZE;
                                if (xAdj >= ColumnRenderSource.SECTION_SIZE) xAdj -= ColumnRenderSource.SECTION_SIZE;
                                if (zAdj >= ColumnRenderSource.SECTION_SIZE) zAdj -= ColumnRenderSource.SECTION_SIZE;
                            }
                        } else {
                            adjRegion = region;
                            adjDetail = detailLevel;
                        }

                        if (adjDetail < detailLevel-1 || adjDetail > detailLevel+1) {
                            continue;
                        }

                        if (adjDetail == detailLevel || adjDetail > detailLevel) {
                            adjData[lodDirection.ordinal() - 2] = new ColumnArrayView[1];
                            adjData[lodDirection.ordinal() - 2][0] = adjRegion.getVerticalDataPointView(xAdj, zAdj);
                        } else {
                            adjData[lodDirection.ordinal() - 2] = new ColumnArrayView[2];
                            adjData[lodDirection.ordinal() - 2][0] = adjRegion.getVerticalDataPointView(xAdj, zAdj);
                            adjData[lodDirection.ordinal() - 2][1] =  adjRegion.getVerticalDataPointView(
                                    xAdj + (lodDirection.getAxis()== ELodDirection.Axis.X ? 0 : 1),
                                    zAdj + (lodDirection.getAxis()== ELodDirection.Axis.Z ? 0 : 1));
                        }
                    } catch (RuntimeException e) {
                        EVENT_LOGGER.warn("Failed to get adj data for [{}:{},{}] at [{}]", detailLevel, x, z, lodDirection);
                        EVENT_LOGGER.warn("Detail exception: ", e);
                    }
                }

                // We render every vertical lod present in this position
                // We only stop when we find a block that is void or non-existing block
                for (int i = 0; i < posData.size(); i++) {
                    long data = posData.get(i);
                    // If the data is not renderable (Void or non-existing) we stop since there is
                    // no data left in this position
                    if (ColumnFormat.isVoid(data) || !ColumnFormat.doesDataPointExist(data))
                        break;

                    long adjDataTop = i - 1 >= 0 ? posData.get(i - 1) : ColumnFormat.EMPTY_DATA;
                    long adjDataBot = i + 1 < posData.size() ? posData.get(i + 1) : ColumnFormat.EMPTY_DATA;

                    CubicLodTemplate.addLodToBuffer(data, adjDataTop, adjDataBot, adjData, detailLevel,
                            x, z, quadBuilder, debugMode, debugSourceFlag);
                }
            }
        }
        quadBuilder.mergeQuads();
    }
}
