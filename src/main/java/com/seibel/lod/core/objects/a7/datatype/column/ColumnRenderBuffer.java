package com.seibel.lod.core.objects.a7.datatype.column;

import com.seibel.lod.core.Config;
import com.seibel.lod.core.api.internal.ClientApi;
import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.CubicLodTemplate;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.LodQuadBuilder;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.enums.rendering.GLProxyContext;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.a7.UncheckedInterruptedException;
import com.seibel.lod.core.objects.a7.render.RenderBuffer;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.render.LodRenderer;
import com.seibel.lod.core.render.objects.GLVertexBuffer;
import com.seibel.lod.core.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL32;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.seibel.lod.core.render.GLProxy.GL_LOGGER;


public class ColumnRenderBuffer extends RenderBuffer {
    //TODO: Make the pool use configurable number of threads
    public static final ExecutorService BUFFER_BUILDERS =
            Executors.newCachedThreadPool(new LodThreadFactory("ColumnBufferBuilders", 5));
    public static final ExecutorService BUFFER_UPLOADER = LodUtil.makeSingleThreadPool("ColumnBufferUploader");

    public static final ConfigBasedLogger EVENT_LOGGER = new ConfigBasedLogger(LogManager.getLogger(LodRenderer.class),
            () -> Config.Client.Advanced.Debugging.DebugSwitch.logRendererBufferEvent.get());
    private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    private static final long MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS = 1_000_000;
    GLVertexBuffer[] vbos;

    public ColumnRenderBuffer() {
        vbos = new GLVertexBuffer[0];
    }


    private void _uploadBuffersDirect(LodQuadBuilder builder, GpuUploadMethod method) throws InterruptedException {
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
                vbo.uploadBuffer(bb, size/LodUtil.LOD_VERTEX_FORMAT.getByteSize(), method, LodBufferBuilderFactory.FULL_SIZED_BUFFER);
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

    private void _uploadBuffersMapped(LodQuadBuilder builder, GpuUploadMethod method)
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

    public void uploadBuffer(LodQuadBuilder builder, GpuUploadMethod method) throws InterruptedException {
        if (method.useEarlyMapping) {
            _uploadBuffersMapped(builder, method);
        } else {
            _uploadBuffersDirect(builder, method);
        }
    }

    @Override
    public boolean render(LodRenderProgram shaderProgram) {
        boolean hasRendered = false;
        for (GLVertexBuffer vbo : vbos) {
            if (vbo == null) continue;
            if (vbo.getVertexCount() == 0) continue;
            hasRendered = true;
            vbo.bind();
            shaderProgram.bindVertexBuffer(vbo.getId());
            if (LodRenderer.ENABLE_IBO) {
                GL32.glDrawElements(GL32.GL_TRIANGLES, (vbo.getVertexCount()/4)*6, ClientApi.renderer.quadIBO.getType(), 0);
            } else {
                GL32.glDrawArrays(GL32.GL_TRIANGLES, 0, vbo.getVertexCount());
            }
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
            if (b.getSize() == LodBufferBuilderFactory.FULL_SIZED_BUFFER) {
                statsMap.incStat("FullsizedVBOs");
            }
            if (b.getSize() == 0) GL_LOGGER.warn("VBO with size 0");
            statsMap.incBytesStat("TotalUsage", b.getSize());
        }
    }

    @Override
    public void close() {
        GLProxy.getInstance().recordOpenGlCall(() -> {
            for (GLVertexBuffer b : vbos) {
                if (b == null) continue;
                b.destroy(false);
            }
        });
    }


    public static CompletableFuture<ColumnRenderBuffer> build(ColumnRenderBuffer usedBuffer, ColumnDatatype data, ColumnDatatype[] adjData) {
        EVENT_LOGGER.trace("RenderRegion startBuild @ {}", data.sectionPos);
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        EVENT_LOGGER.trace("RenderRegion start QuadBuild @ {}", data.sectionPos);
                        int skyLightCullingBelow = Config.Client.Graphics.AdvancedGraphics.caveCullingHeight.get();
                        // FIXME: Clamp also to the max world height.
                        skyLightCullingBelow = Math.max(skyLightCullingBelow, LodBuilder.MIN_WORLD_HEIGHT);
                        LodQuadBuilder builder = new LodQuadBuilder(true, skyLightCullingBelow);
                        makeLodRenderData(builder, data, adjData);
                        EVENT_LOGGER.trace("RenderRegion end QuadBuild @ {}", data.sectionPos);
                        return builder;
                    } catch (UncheckedInterruptedException e) {
                        throw e;
                    }
                    catch (Throwable e3) {
                        EVENT_LOGGER.error("\"LodNodeBufferBuilder\" was unable to build quads: ", e3);
                        throw e3;
                    }
                }, BUFFER_BUILDERS)
                .thenApplyAsync((builder) -> {
                    try {
                        EVENT_LOGGER.trace("RenderRegion start Upload @ {}", data.sectionPos);
                        GLProxy glProxy = GLProxy.getInstance();
                        GpuUploadMethod method = GLProxy.getInstance().getGpuUploadMethod();
                        GLProxyContext oldContext = glProxy.getGlContext();
                        glProxy.setGlContext(GLProxyContext.LOD_BUILDER);
                        ColumnRenderBuffer buffer = usedBuffer!=null ? usedBuffer : new ColumnRenderBuffer();
                        try {
                            buffer.uploadBuffer(builder, method);
                            EVENT_LOGGER.trace("RenderRegion end Upload @ {}", data.sectionPos);
                            return buffer;
                        } catch (Exception e) {
                            buffer.close();
                            throw e;
                        } finally {
                            glProxy.setGlContext(oldContext);
                        }
                    } catch (InterruptedException e) {
                        throw UncheckedInterruptedException.convert(e);
                    } catch (Throwable e3) {
                        EVENT_LOGGER.error("\"LodNodeBufferBuilder\" was unable to upload buffer: ", e3);
                        throw e3;
                    }
                }, BUFFER_UPLOADER).handle((v, e) -> {
                    if (e != null) {
                        usedBuffer.close();
                        return null;
                    } else {
                        return v;
                    }
                });
    }



    private static void makeLodRenderData(LodQuadBuilder quadBuilder, ColumnDatatype region, ColumnDatatype[] adjRegions) {

        // Variable initialization
        DebugMode debugMode = Config.Client.Advanced.Debugging.debugMode.get();

        byte detailLevel = region.getDataDetail();
        int dataSize = 1 << detailLevel;
        for (int x = 0; x < dataSize; x++) {
            for (int z = 0; z < dataSize; z++) {
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
                for (LodDirection lodDirection : LodDirection.ADJ_DIRECTIONS) {
                    try {
                        int xAdj = x + lodDirection.getNormal().x;
                        int zAdj = z + lodDirection.getNormal().z;
                        boolean isCrossRegionBoundary = (xAdj < 0 || xAdj >= dataSize) ||
                                (zAdj < 0 || zAdj >= dataSize);
                        ColumnDatatype adjRegion;
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
                                if (xAdj < 0) xAdj += dataSize;
                                if (zAdj < 0) zAdj += dataSize;
                                if (xAdj >= dataSize) xAdj -= dataSize;
                                if (zAdj >= dataSize) zAdj -= dataSize;
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
                                    xAdj + (lodDirection.getAxis()==LodDirection.Axis.X ? 0 : 1),
                                    zAdj + (lodDirection.getAxis()==LodDirection.Axis.Z ? 0 : 1));
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
                    CubicLodTemplate.addLodToBuffer(data, adjDataTop, adjDataBot, adjData, detailLevel,
                            x, z, quadBuilder, debugMode);
                }
            }
        }
        quadBuilder.mergeQuads();
    }
}
