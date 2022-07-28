package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.a7.generation.GenerationQueue;
import com.seibel.lod.core.a7.generation.IGenerator;
import com.seibel.lod.core.a7.render.LodQuadTree;
import com.seibel.lod.core.a7.util.FileScanner;
import com.seibel.lod.core.a7.save.io.file.LocalDataFileHandler;
import com.seibel.lod.core.a7.save.io.render.RenderFileHandler;
import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.a7.render.RenderBufferHandler;
import com.seibel.lod.core.a7.save.structure.LocalSaveStructure;
import com.seibel.lod.core.builders.worldGeneration.BatchGenerator;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.a7.render.a7LodRenderer;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class DhClientServerLevel implements IClientLevel, IServerLevel {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
    public final LocalSaveStructure save;
    public final LocalDataFileHandler dataFileHandler;
    public GenerationQueue generationQueue = null;
    public RenderFileHandler renderFileHandler = null;
    public RenderBufferHandler renderBufferHandler = null; //TODO: Should this be owned by renderer?
    public final ILevelWrapper level;
    public a7LodRenderer renderer = null;
    public LodQuadTree tree = null;
    public IGenerator worldGenerator = null;

    public DhClientServerLevel(LocalSaveStructure save, ILevelWrapper level) {
        this.level = level;
        this.save = save;
        save.getDataFolder(level).mkdirs();
        save.getRenderCacheFolder(level).mkdirs();
        dataFileHandler = new LocalDataFileHandler(this, save.getDataFolder(level));
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
    public void startRenderer() {
        LOGGER.info("Starting renderer for {}", level);
        if (renderBufferHandler != null) {
            LOGGER.warn("Tried to call startRenderer() on the clientServerLevel {} when renderer is already setup!", level);
            return;
        }

        // FIXME: This A need B and B need A messes needs to be reworked!
        renderFileHandler = new RenderFileHandler(dataFileHandler, this, save.getRenderCacheFolder(level));
        final RenderFileHandler f_renderFileHandler = renderFileHandler;
        generationQueue = new GenerationQueue(f_renderFileHandler::write);
        renderFileHandler.setPlaceHolderQueue(generationQueue);

        tree = new LodQuadTree(this, Config.Client.Graphics.Quality.lodChunkRenderDistance.get()*16,
                MC_CLIENT.getPlayerBlockPos().x, MC_CLIENT.getPlayerBlockPos().z, renderFileHandler);
        renderBufferHandler = new RenderBufferHandler(tree);
        FileScanner.scanFile(save, level, dataFileHandler, renderFileHandler);
    }

    @Override
    public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler) {
        if (renderBufferHandler == null) {
            LOGGER.error("Tried to call render() on the clientServerLevel {} when renderer has not been started!", level);
            return;
        }
        if (renderer == null) {
            renderer = new a7LodRenderer(this);
        }
        renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
    }

    public void stopRenderer() {
        LOGGER.info("Stopping renderer for {}", level);
        if (renderBufferHandler == null) {
            LOGGER.warn("Tried to call stopRenderer() on the clientServerLevel {} when renderer is already closed!", level);
            return;
        }
        renderBufferHandler.close();
        renderBufferHandler = null;
        tree = null; //TODO Close the tree
        generationQueue = null;
        renderFileHandler.flushAndSave(); //Ignore the completion feature so that this action is async
        renderFileHandler.close();
        renderFileHandler = null;
    }

    @Override
    public RenderBufferHandler getRenderBufferHandler() {
        return renderBufferHandler;
    }

    @Override
    public int computeBaseColor(IBiomeWrapper biome, IBlockStateWrapper block) {
        return 0; //TODO
    }

    @Override
    public void dumpRamUsage() {
        //TODO
    }

    @Override
    public int getMinY() {
        return level.getMinHeight();
    }

    @Override
    public CompletableFuture<Void> save() {
        return renderFileHandler == null ? dataFileHandler.flushAndSave() : renderFileHandler.flushAndSave();
        //Note: saving renderFileHandler will also save the dataFileHandler.
    }

    private BatchGenerator batchGenerator = null;
    @Override
    public void close() {
        if (batchGenerator != null) batchGenerator.close();
        if (renderer != null) renderer.close();
        if (renderBufferHandler != null) renderBufferHandler.close();
        if (renderFileHandler != null) renderFileHandler.close();
        dataFileHandler.close();
        LOGGER.info("Closed DHLevel for {}", level);
    }


    @Override
    public void doWorldGen() {
        if (worldGenerator == null) {
            // TODO: Make a registry for generators for modding support.
            batchGenerator = new BatchGenerator(this);
            worldGenerator = batchGenerator;
        } else {
            batchGenerator.update();
            if (generationQueue != null)
                generationQueue.doGeneration(batchGenerator);
        }
    }

    @Override
    public ILevelWrapper getLevelWrapper() {
        return level;
    }
}
