package com.seibel.lod.core.level;

import com.seibel.lod.core.generation.GenerationQueue;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.io.datafile.GeneratedDataFileHandler;
import com.seibel.lod.core.util.FileScanUtil;
import com.seibel.lod.core.io.renderfile.RenderFileHandler;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.render.RenderBufferHandler;
import com.seibel.lod.core.io.structure.LocalSaveStructure;
import com.seibel.lod.core.generation.BatchGenerator;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.logging.f3.F3Screen;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.util.math.Mat4f;
import com.seibel.lod.core.render.renderer.LodRenderer;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class DhClientServerLevel implements IClientLevel, IServerLevel {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
    public final LocalSaveStructure save;
    public final GeneratedDataFileHandler dataFileHandler;
    public volatile GenerationQueue generationQueue = null;
    public RenderFileHandler renderFileHandler = null;
    public RenderBufferHandler renderBufferHandler = null; //TODO: Should this be owned by renderer?
    public final IServerLevelWrapper serverLevel;
    public IClientLevelWrapper clientLevel;
    public LodRenderer renderer = null;
    public LodQuadTree tree = null;
    public volatile BatchGenerator worldGenerator = null;
    private final ReentrantLock renderStateLifecycleLock = new ReentrantLock();

    public F3Screen.NestedMessage f3Msg;

    public DhClientServerLevel(LocalSaveStructure save, IServerLevelWrapper level) {
        this.serverLevel = level;
        this.save = save;
        save.getDataFolder(level).mkdirs();
        save.getRenderCacheFolder(level).mkdirs();
        dataFileHandler = new GeneratedDataFileHandler(this, save.getDataFolder(level));
        FileScanUtil.scanFile(save, serverLevel, dataFileHandler, null);
        LOGGER.info("Started DHLevel for {} with saves at {}", level, save);
        f3Msg = new F3Screen.NestedMessage(this::f3Log);
    }

    private String[] f3Log() {
        if (clientLevel == null) {
            return new String[]{LodUtil.formatLog("level @ {}: Inactive", serverLevel.getDimensionType().getDimensionName())};
        } else {
            return new String[]{
                    LodUtil.formatLog("level @ {}: Active", serverLevel.getDimensionType().getDimensionName())
            };
        }
    }

    @Override
    public void clientTick() {
        //LOGGER.info("Client tick for {}", level);
        if (clientLevel == null) return;
        if (tree.viewDistance != Config.Client.Graphics.Quality.lodChunkRenderDistance.get()*16) {
            IClientLevelWrapper temp = clientLevel;
            renderStateLifecycleLock.lock();
            try {
            stopRenderer();
            startRenderer(temp);
            } finally {
                renderStateLifecycleLock.unlock();
            }
            return;
        }
        tree.tick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
        renderBufferHandler.update();
    }

    @Override
    public void serverTick() {
        //TODO Update network packet and stuff or state or etc..
    }

    public void startRenderer(IClientLevelWrapper clientLevel) {
        LOGGER.info("Starting renderer for {}", this);
        renderStateLifecycleLock.lock();
        try {
            if (renderBufferHandler != null || this.clientLevel != null) {
                LOGGER.warn("Tried to call startRenderer() on {} when renderer is already setup!", this);
                return;
            }
            this.clientLevel = clientLevel;
            // TODO: Make a registry for generators for modding support.
            worldGenerator = new BatchGenerator(this);
            generationQueue = new GenerationQueue(worldGenerator);
            dataFileHandler.setGenerationQueue(generationQueue);
            renderFileHandler = new RenderFileHandler(dataFileHandler, this, save.getRenderCacheFolder(serverLevel));
            tree = new LodQuadTree(this, Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * 16,
                    MC_CLIENT.getPlayerBlockPos().x, MC_CLIENT.getPlayerBlockPos().z, renderFileHandler);
            renderBufferHandler = new RenderBufferHandler(tree);
            FileScanUtil.scanFile(save, serverLevel, null, renderFileHandler);
        } finally {
            renderStateLifecycleLock.unlock();
        }
    }

    @Override
    public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler) {
        if (!renderStateLifecycleLock.tryLock()) return;
        try {
            if (renderBufferHandler == null) {
                LOGGER.error("Tried to call render() on {} when renderer has not been started!", this);
                return;
            }
            if (renderer == null) {
                renderer = new LodRenderer(this);
            }
            renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
        } finally {
            renderStateLifecycleLock.unlock();
        }
    }

    public void stopRenderer() {
        LOGGER.info("Stopping renderer for {}", this);
        renderStateLifecycleLock.lock();
        try {
            if (renderBufferHandler == null) {
                LOGGER.warn("Tried to call stopRenderer() on {} when renderer is already closed!", this);
                return;
            }
            tree.close();
            tree = null;
            dataFileHandler.popGenerationQueue();
            final BatchGenerator f_worldGen = worldGenerator;
            CompletableFuture<Void> closer = generationQueue.startClosing(true, true)
                    .exceptionally(ex -> {
                        LOGGER.error("Error closing generation queue", ex);
                        return null;
                    }).thenRun(f_worldGen::close)
                    .exceptionally(ex -> {
                        LOGGER.error("Error closing world gen", ex);
                        return null;
                    });
            generationQueue = null;
            worldGenerator = null;
            renderBufferHandler.close();
            renderBufferHandler = null;
            renderFileHandler.flushAndSave(); //Ignore the completion feature so that this action is async
            renderFileHandler.close();
            renderFileHandler = null;
            closer.join(); // TODO: Could this cause deadlocks? we are blocking in main thread.
            clientLevel = null;
        } finally {
            renderStateLifecycleLock.unlock();
        }
    }

    @Override
    public RenderBufferHandler getRenderBufferHandler() {
        return renderBufferHandler;
    }

    @Override
    public int computeBaseColor(DhBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper block) {
        return clientLevel.computeBaseColor(pos, biome, block);
    }

    @Override
    public IClientLevelWrapper getClientLevelWrapper() {
        return clientLevel;
    }
    
    @Override
    public ILevelWrapper getLevelWrapper()
    {
        return this.serverLevel;
    }
    
    @Override
    public void dumpRamUsage() {
        //TODO
    }

    @Override
    public int getMinY() {
        return serverLevel.getMinHeight();
    }

    @Override
    public CompletableFuture<Void> save() {

        if (renderFileHandler != null) {
            return renderFileHandler.flushAndSave().thenCompose(v -> dataFileHandler.flushAndSave());
        } else {
            return dataFileHandler.flushAndSave();
        }
    }
    @Override
    public void close() {
        renderStateLifecycleLock.lock();
        try {
            if (generationQueue != null) generationQueue.close();
            if (worldGenerator != null) worldGenerator.close();
            if (renderer != null) renderer.close();
            if (tree != null) tree.close();
            if (renderBufferHandler != null) renderBufferHandler.close();
            if (renderFileHandler != null) renderFileHandler.close();
            dataFileHandler.close();
        } finally {
            renderStateLifecycleLock.unlock();
        }
        LOGGER.info("Closed {}", this);
    }


    @Override
    public void doWorldGen() {
        final BatchGenerator f_worldGen = worldGenerator;
        if (f_worldGen != null) {
            f_worldGen.update();
            final GenerationQueue f_genQueue = generationQueue;
            if (f_genQueue != null)
                f_genQueue.pollAndStartClosest(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
        }
    }

    @Override
    public IServerLevelWrapper getServerLevelWrapper() {
        return serverLevel;
    }
}
