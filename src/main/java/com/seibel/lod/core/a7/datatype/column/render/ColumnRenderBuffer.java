package com.seibel.lod.core.a7.datatype.column.render;

import com.seibel.lod.core.a7.datatype.column.ColumnRenderSource;
import com.seibel.lod.core.a7.datatype.column.accessor.ColumnArrayView;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.render.a7LodRenderer;
import com.seibel.lod.core.a7.util.UncheckedInterruptedException;
import com.seibel.lod.core.a7.render.RenderBuffer;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.CubicLodTemplate;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.LodQuadBuilder;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.enums.config.EGpuUploadMethod;
import com.seibel.lod.core.enums.rendering.EDebugMode;
import com.seibel.lod.core.enums.rendering.EGLProxyContext;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.objects.GLVertexBuffer;
import com.seibel.lod.core.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.*;

import static com.seibel.lod.core.render.GLProxy.GL_LOGGER;


public class ColumnRenderBuffer extends RenderBuffer {
    //TODO: Make the pool use configurable number of threads
    public static final ExecutorService BUFFER_BUILDERS = LodUtil.makeThreadPool(4, "BufferBuilder");
    public static final ExecutorService BUFFER_UPLOADER = LodUtil.makeSingleThreadPool("ColumnBufferUploader");
    public static final int MAX_CONCURRENT_CALL = 8;

    public static final ConfigBasedLogger EVENT_LOGGER = new ConfigBasedLogger(LogManager.getLogger(),
            () -> Config.Client.Advanced.Debugging.DebugSwitch.logRendererBufferEvent.get());
    private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    private static final long MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS = 1_000_000;
    GLVertexBuffer[] vbos;
    public final DHBlockPos pos;

    public ColumnRenderBuffer(DHBlockPos pos) {
        this.pos = pos;
        vbos = new GLVertexBuffer[0];
    }


    private void _uploadBuffersDirect(LodQuadBuilder builder, EGpuUploadMethod method) throws InterruptedException {
        resize(builder.getCurrentNeededVertexBufferCount());
        long remainingNS = 0;
        long BPerNS = Config.Client.Advanced.Buffers.gpuUploadPerMegabyteInMilliseconds.get();

        int i = 0;
        Iterator<ByteBuffer> iter = builder.makeVertexBuffers();
        while (iter.hasNext()) {
            if (i >= vbos.length) {
                throw new RuntimeException("Too many vertex buffers!!");
            }
            ByteBuffer bb = iter.next();
            GLVertexBuffer vbo = getOrMakeVbo(i++, method.useBufferStorage);
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

    private void _uploadBuffersMapped(LodQuadBuilder builder, EGpuUploadMethod method)
    {
        resize(builder.getCurrentNeededVertexBufferCount());
        for (int i=0; i<vbos.length; i++) {
            if (vbos[i]==null) vbos[i] = new GLVertexBuffer(method.useBufferStorage);
        }
        LodQuadBuilder.BufferFiller func = builder.makeBufferFiller(method);
        int i = 0;
        while (i < vbos.length && func.fill(vbos[i++])) {}
    }

    private GLVertexBuffer getOrMakeVbo(int iIndex, boolean useBuffStorage) {
        if (vbos[iIndex] == null) {
            vbos[iIndex] = new GLVertexBuffer(useBuffStorage);
        }
        return vbos[iIndex];
    }

    private void resize(int size) {
        if (vbos.length != size) {
            GLVertexBuffer[] newVbos = new GLVertexBuffer[size];
            if (vbos.length > size) {
                for (int i=size; i<vbos.length; i++) {
                    if (vbos[i]!=null) vbos[i].close();
                    vbos[i] = null;
                }
            }
            for (int i=0; i<newVbos.length && i<vbos.length; i++) {
                newVbos[i] = vbos[i];
                vbos[i] = null;
            }
            for (GLVertexBuffer b : vbos) {
                if (b != null) throw new RuntimeException("LEAKING VBO!");
            }
            vbos = newVbos;
        }
    }

    public void uploadBuffer(LodQuadBuilder builder, EGpuUploadMethod method) throws InterruptedException {
        if (method.useEarlyMapping) {
            _uploadBuffersMapped(builder, method);
        } else {
            _uploadBuffersDirect(builder, method);
        }
    }

    @Override
    public boolean render(a7LodRenderer renderContext) {
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

    public static CompletableFuture<ColumnRenderBuffer[]> build(IClientLevel clientLevel, Reference<ColumnRenderBuffer> usedBufferSlotOpaque, Reference<ColumnRenderBuffer> usedBufferSlotTransparent, ColumnRenderSource data, ColumnRenderSource[] adjData) {
        if (isBusy()) return null;
        //LOGGER.info("RenderRegion startBuild @ {}", data.sectionPos);
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        EVENT_LOGGER.trace("RenderRegion start QuadBuild @ {}", data.sectionPos);
                        int skyLightCullingBelow = Config.Client.Graphics.AdvancedGraphics.caveCullingHeight.get();
                        // FIXME: Clamp also to the max world height.
                        skyLightCullingBelow = Math.max(skyLightCullingBelow, clientLevel.getMinY());
                        LodQuadBuilder builderOpaque = new LodQuadBuilder(true,
                                (short) (skyLightCullingBelow - clientLevel.getMinY()));

                        LodQuadBuilder builderTransparent = new LodQuadBuilder(true,
                                (short) (skyLightCullingBelow - clientLevel.getMinY()));

                        makeLodRenderData(builderOpaque, builderTransparent, data, adjData);
                        if (builderOpaque.getCurrentQuadsCount() > 0) {
                            //LOGGER.info("her");
                        }
                        EVENT_LOGGER.trace("RenderRegion end QuadBuild @ {}", data.sectionPos);
                        LodQuadBuilder[] builders = new LodQuadBuilder[2];
                        builders[0] = builderOpaque;
                        builders[1] = builderOpaque;

                        return builders;
                    } catch (UncheckedInterruptedException e) {
                        throw e;
                    }
                    catch (Throwable e3) {
                        LOGGER.error("\"LodNodeBufferBuilder\" was unable to build quads: ", e3);
                        throw e3;
                    }
                }, BUFFER_BUILDERS)
                .thenApplyAsync((builders) -> {
                    try {
                        EVENT_LOGGER.trace("RenderRegion start Upload @ {}", data.sectionPos);
                        GLProxy glProxy = GLProxy.getInstance();
                        EGpuUploadMethod method = GLProxy.getInstance().getGpuUploadMethod();
                        EGLProxyContext oldContext = glProxy.getGlContext();
                        glProxy.setGlContext(EGLProxyContext.LOD_BUILDER);
                        ColumnRenderBuffer buffersSlotOpaque = usedBufferSlotOpaque.swap(null);
                        ColumnRenderBuffer buffersSlotTransparent = usedBufferSlotTransparent.swap(null);

                        if (buffersSlotOpaque == null)
                            buffersSlotOpaque = new ColumnRenderBuffer(
                                new DHBlockPos(data.sectionPos.getCorner().getCorner(), clientLevel.getMinY())
                            );
                        if (buffersSlotTransparent == null)
                            buffersSlotTransparent = new ColumnRenderBuffer(
                                    new DHBlockPos(data.sectionPos.getCorner().getCorner(), clientLevel.getMinY())
                            );
                        try {
                            buffersSlotOpaque.uploadBuffer(builders[0], method);
                            buffersSlotTransparent.uploadBuffer(builders[1], method);
                            EVENT_LOGGER.trace("RenderRegion end Upload @ {}", data.sectionPos);
                            ColumnRenderBuffer[] buffers = new ColumnRenderBuffer[2];
                            buffers[0] = buffersSlotOpaque;
                            buffers[1] = buffersSlotTransparent;
                            return buffers;
                        } catch (Exception e) {
                            buffersSlotOpaque.close();
                            buffersSlotTransparent.close();
                            throw e;
                        } finally {
                            glProxy.setGlContext(oldContext);
                        }
                    } catch (InterruptedException e) {
                        throw UncheckedInterruptedException.convert(e);
                    } catch (Throwable e3) {
                        LOGGER.error("\"LodNodeBufferBuilder\" was unable to upload buffer: ", e3);
                        throw e3;
                    }
                }, BUFFER_UPLOADER).handle((v, e) -> {
                    //LOGGER.info("RenderRegion endBuild @ {}", data.sectionPos);
                    if (e != null) {
                        if (!usedBufferSlotOpaque.isEmpty()) {
                            ColumnRenderBuffer buffersSlot = usedBufferSlotOpaque.swap(null);
                            buffersSlot.close();
                        }
                        if (!usedBufferSlotTransparent.isEmpty()) {
                            ColumnRenderBuffer buffersSlot = usedBufferSlotTransparent.swap(null);
                            buffersSlot.close();
                        }
                        return null;
                    } else {
                        return v;
                    }
                });
    }



    private static void makeLodRenderData(LodQuadBuilder quadBuilderOpaque, LodQuadBuilder quadBuilderTransparent, ColumnRenderSource region, ColumnRenderSource[] adjRegions) {

        // Variable initialization
        EDebugMode debugMode = Config.Client.Advanced.Debugging.debugMode.get();

        byte detailLevel = region.getDataDetail();
        for (int x = 0; x < ColumnRenderSource.SECTION_SIZE; x++) {
            for (int z = 0; z < ColumnRenderSource.SECTION_SIZE; z++) {
                UncheckedInterruptedException.throwIfInterrupted();

                ColumnArrayView posData = region.getVerticalDataView(x, z);
                if (posData.size() == 0 || !DataPointUtil.doesItExist(posData.get(0))
                        || DataPointUtil.isVoid(posData.get(0)))
                    continue;

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
                            adjData[lodDirection.ordinal() - 2][0] = adjRegion.getVerticalDataView(xAdj, zAdj);
                        } else {
                            adjData[lodDirection.ordinal() - 2] = new ColumnArrayView[2];
                            adjData[lodDirection.ordinal() - 2][0] = adjRegion.getVerticalDataView(xAdj, zAdj);
                            adjData[lodDirection.ordinal() - 2][1] =  adjRegion.getVerticalDataView(
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
                    if (DataPointUtil.isVoid(data) || !DataPointUtil.doesItExist(data))
                        break;

                    long adjDataTop = i - 1 >= 0 ? posData.get(i - 1) : DataPointUtil.EMPTY_DATA;
                    long adjDataBot = i + 1 < posData.size() ? posData.get(i + 1) : DataPointUtil.EMPTY_DATA;


                    // We send the call to create the vertices
                    if(DataPointUtil.getAlpha(data) == 255)
                    {
                        CubicLodTemplate.addLodToBuffer(data, adjDataTop, adjDataBot, adjData, detailLevel,
                                x, z, quadBuilderOpaque, debugMode);
                    }else
                    {
                        CubicLodTemplate.addLodToBuffer(data, adjDataTop, adjDataBot, adjData, detailLevel,
                                x, z, quadBuilderTransparent, debugMode);
                    }
                }
            }
        }
        quadBuilderOpaque.mergeQuads();
        quadBuilderTransparent.mergeQuads();
    }
}
