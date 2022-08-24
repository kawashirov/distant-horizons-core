package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.a7.render.LodQuadTree;
import com.seibel.lod.core.a7.util.FileScanner;
import com.seibel.lod.core.a7.save.io.file.RemoteDataFileHandler;
import com.seibel.lod.core.a7.save.io.render.RenderFileHandler;
import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.a7.render.RenderBufferHandler;
import com.seibel.lod.core.a7.save.structure.ClientOnlySaveStructure;
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
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class DhClientLevel implements IClientLevel {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
    public final ClientOnlySaveStructure save;
    public final RemoteDataFileHandler dataFileHandler;
    public final RenderFileHandler renderFileHandler;
    public final RenderBufferHandler renderBufferHandler; //TODO: Should this be owned by renderer?
    public final IClientLevelWrapper level;
    public a7LodRenderer renderer = null;
    public LodQuadTree tree;

    public DhClientLevel(ClientOnlySaveStructure save, IClientLevelWrapper level) {
        this.save = save;
        save.getDataFolder(level).mkdirs();
        save.getRenderCacheFolder(level).mkdirs();
        dataFileHandler = new RemoteDataFileHandler(this, save.getDataFolder(level));
        renderFileHandler = new RenderFileHandler(dataFileHandler, this, save.getRenderCacheFolder(level));
        tree = new LodQuadTree(this, Config.Client.Graphics.Quality.lodChunkRenderDistance.get()*16,
                MC_CLIENT.getPlayerBlockPos().x, MC_CLIENT.getPlayerBlockPos().z, renderFileHandler);
        renderBufferHandler = new RenderBufferHandler(tree);
        this.level = level;
        FileScanner.scanFile(save, level, dataFileHandler, renderFileHandler);
        LOGGER.info("Started DHLevel for {} with saves at {}", level, save);
    }

    @Override
    public void dumpRamUsage() {
        //TODO
    }

    @Override
    public void clientTick() {
        tree.tick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
        renderBufferHandler.update();
    }

    @Override
    public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler) {
        if (renderer == null) {
            renderer = new a7LodRenderer(this);
        }
        renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
    }

    @Override
    public RenderBufferHandler getRenderBufferHandler() {
        return renderBufferHandler;
    }

    @Override
    public int computeBaseColor(DHBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper block) {
        return 0; //TODO
    }

    @Override
    public IClientLevelWrapper getClientLevelWrapper() {
        return level;
    }
    
    @Override
    public ILevelWrapper getLevelWrapper()
    {
        return this.level;
    }
    
    @Override
    public int getMinY() {
        return level.getMinHeight();
    }

    @Override
    public CompletableFuture<Void> save() {
        return renderFileHandler.flushAndSave();
    }

    @Override
    public void close() {
        renderFileHandler.close();
        LOGGER.info("Closed DHLevel for {}", level);
    }
    
}
