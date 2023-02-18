package com.seibel.lod.core.level;

import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.file.renderfile.RenderSourceFileHandler;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.util.FileScanUtil;
import com.seibel.lod.core.file.fullDatafile.RemoteFullDataFileHandler;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.render.RenderBufferHandler;
import com.seibel.lod.core.file.structure.ClientOnlySaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.util.math.Mat4f;
import com.seibel.lod.core.render.renderer.LodRenderer;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class DhClientLevel implements IDhClientLevel
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	public final ClientOnlySaveStructure save;
	public final RemoteFullDataFileHandler dataFileHandler;
	public final RenderSourceFileHandler renderSourceFileHandler;
	public final RenderBufferHandler renderBufferHandler; //TODO: Should this be owned by renderer?
	public final IClientLevelWrapper level;
	public LodRenderer renderer = null;
	public LodQuadTree tree;
	
	
	
	public DhClientLevel(ClientOnlySaveStructure save, IClientLevelWrapper level)
	{
		this.save = save;
		save.getDataFolder(level).mkdirs();
		save.getRenderCacheFolder(level).mkdirs();
		this.dataFileHandler = new RemoteFullDataFileHandler(this, save.getDataFolder(level));
		this.renderSourceFileHandler = new RenderSourceFileHandler(this.dataFileHandler, this, save.getRenderCacheFolder(level));
		this.tree = new LodQuadTree(this, Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * 16,
				MC_CLIENT.getPlayerBlockPos().x, MC_CLIENT.getPlayerBlockPos().z, this.renderSourceFileHandler);
		this.renderBufferHandler = new RenderBufferHandler(this.tree);
		this.level = level;
		FileScanUtil.scanFiles(save, level, this.dataFileHandler, this.renderSourceFileHandler);
		LOGGER.info("Started DHLevel for {} with saves at {}", level, save);
	}
	
	
	
	@Override
	public void dumpRamUsage()
	{
		//TODO
	}
	
	@Override
	public void clientTick()
	{
		this.tree.tick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
		this.renderBufferHandler.update();
	}
	
	@Override
	public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler)
	{
		if (this.renderer == null)
		{
			this.renderer = new LodRenderer(this.renderBufferHandler);
		}
		this.renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
	}
	
	@Override
	public int computeBaseColor(DhBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper block) { return 0; /* TODO */ }
	
	@Override
	public IClientLevelWrapper getClientLevelWrapper() { return this.level; }
	
	@Override
	public ILevelWrapper getLevelWrapper() { return this.level; }
	
	@Override
	public IFullDataSourceProvider getFileHandler() { return this.dataFileHandler; }
	
	@Override 
	public void clearRenderDataCache()
	{
		if (this.tree != null)
		{
			this.tree.clearRenderDataCache();
		}
	}
	
	@Override
	public void updateChunkAsync(IChunkWrapper chunk)
	{
		//TODO
	}
	
	@Override
	public int getMinY() { return this.level.getMinHeight(); }
	
	@Override
	public CompletableFuture<Void> saveAsync() { return this.renderSourceFileHandler.flushAndSave(); }
	
	@Override
	public void close()
	{
		this.renderSourceFileHandler.close();
		LOGGER.info("Closed DHLevel for {}", this.level);
	}
	
}
