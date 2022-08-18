package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.a7.generation.GenerationQueue;
import com.seibel.lod.core.a7.generation.IGenerator;
import com.seibel.lod.core.a7.render.LodQuadTree;
import com.seibel.lod.core.a7.save.io.file.GeneratedDataFileHandler;
import com.seibel.lod.core.a7.util.FileScanner;
import com.seibel.lod.core.a7.save.io.file.DataFileHandler;
import com.seibel.lod.core.a7.save.io.render.RenderFileHandler;
import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.a7.render.RenderBufferHandler;
import com.seibel.lod.core.a7.save.structure.LocalSaveStructure;
import com.seibel.lod.core.builders.worldGeneration.BatchGenerator;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.a7.render.a7LodRenderer;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class DhClientServerLevel implements IClientLevel, IServerLevel {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
    public final LocalSaveStructure save;
    public final DataFileHandler dataFileHandler;
    public GenerationQueue generationQueue = null;
    public RenderFileHandler renderFileHandler = null;
    public RenderBufferHandler renderBufferHandler = null; //TODO: Should this be owned by renderer?
    public final IServerLevelWrapper serverLevel;
    public IClientLevelWrapper clientLevel;
    public a7LodRenderer renderer = null;
    public LodQuadTree tree = null;
    public BatchGenerator worldGenerator = null;

    public DhClientServerLevel(LocalSaveStructure save, IServerLevelWrapper level) {
        this.serverLevel = level;
        this.save = save;
        save.getDataFolder(level).mkdirs();
        save.getRenderCacheFolder(level).mkdirs();
        generationQueue = new GenerationQueue();
        dataFileHandler = new GeneratedDataFileHandler(this, save.getDataFolder(level), generationQueue);
        FileScanner.scanFile(save, serverLevel, dataFileHandler, null);
        LOGGER.info("Started DHLevel for {} with saves at {}", level, save);
    }

    @Override
    public void clientTick() {
        //LOGGER.info("Client tick for {}", level);
        if (tree != null) tree.tick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
        if (renderBufferHandler != null) renderBufferHandler.update();
    }

    @Override
    public void serverTick() {
        //TODO Update network packet and stuff or state or etc..
    }

    public void startRenderer(IClientLevelWrapper clientLevel) {
        LOGGER.info("Starting renderer for {}", this);
        if (renderBufferHandler != null || this.clientLevel != null) {
            LOGGER.warn("Tried to call startRenderer() on {} when renderer is already setup!", this);
            return;
        }
        this.clientLevel = clientLevel;
        // TODO: Make a registry for generators for modding support.
        worldGenerator = new BatchGenerator(this);
        generationQueue.setGenerator(worldGenerator);
        renderFileHandler = new RenderFileHandler(dataFileHandler, this, save.getRenderCacheFolder(serverLevel));
        tree = new LodQuadTree(this, Config.Client.Graphics.Quality.lodChunkRenderDistance.get()*16,
                MC_CLIENT.getPlayerBlockPos().x, MC_CLIENT.getPlayerBlockPos().z, renderFileHandler);
        renderBufferHandler = new RenderBufferHandler(tree);
        FileScanner.scanFile(save, serverLevel, null, renderFileHandler);
    }

    @Override
    public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler) {
        if (renderBufferHandler == null) {
            LOGGER.error("Tried to call render() on {} when renderer has not been started!", this);
            return;
        }
        if (renderer == null) {
            renderer = new a7LodRenderer(this);
        }
        renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
    }

    public void stopRenderer() {
        LOGGER.info("Stopping renderer for {}", this);
        if (renderBufferHandler == null) {
            LOGGER.warn("Tried to call stopRenderer() on {} when renderer is already closed!", this);
            return;
        }
        tree.close();
        tree = null;
        generationQueue.removeGenerator();
        renderBufferHandler.close();
        renderBufferHandler = null;
        renderFileHandler.flushAndSave(); //Ignore the completion feature so that this action is async
        renderFileHandler.close();
        renderFileHandler = null;
        try {
            worldGenerator.close();
        } catch (Exception e) {
            LOGGER.error("Error closing world generator", e);
        }
        worldGenerator = null;
    }

    @Override
    public RenderBufferHandler getRenderBufferHandler() {
        return renderBufferHandler;
    }

    @Override
    public int computeBaseColor(DHBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper block) {
        return clientLevel.computeBaseColor(pos, biome, block);
    }

    @Override
    public IClientLevelWrapper getClientLevelWrapper() {
        return clientLevel;
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
        return renderFileHandler == null ? dataFileHandler.flushAndSave() : renderFileHandler.flushAndSave();
        //Note: saving renderFileHandler will also save the dataFileHandler.
    }
    @Override
    public void close() {
        if (worldGenerator != null) worldGenerator.close();
        if (renderer != null) renderer.close();
        if (tree != null) tree.close();
        if (renderBufferHandler != null) renderBufferHandler.close();
        if (renderFileHandler != null) renderFileHandler.close();
        dataFileHandler.close();
        LOGGER.info("Closed {}", this);
    }


    @Override
    public void doWorldGen() {
        if (worldGenerator != null) {
            worldGenerator.update();
            if (generationQueue != null)
                generationQueue.pollAndStartClosest(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
        }
    }

    @Override
    public IServerLevelWrapper getServerLevelWrapper() {
        return serverLevel;
    }
}
