package com.seibel.lod.core.level;

import com.seibel.lod.core.config.AppliedConfigState;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.datatype.full.FullDataSource;
import com.seibel.lod.core.datatype.transform.ChunkToLodBuilder;
import com.seibel.lod.core.file.datafile.IDataSourceProvider;
import com.seibel.lod.core.generation.IWorldGenerator;
import com.seibel.lod.core.generation.WorldGenerationQueue;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.file.datafile.GeneratedDataFileHandler;
import com.seibel.lod.core.util.FileScanUtil;
import com.seibel.lod.core.file.renderfile.RenderFileHandler;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.render.RenderBufferHandler;
import com.seibel.lod.core.file.structure.LocalSaveStructure;
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
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** The level used on a singleplayer world */
public class DhClientServerLevel implements IDhClientLevel, IDhServerLevel
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	public final LocalSaveStructure save;
	public final GeneratedDataFileHandler dataFileHandler;
	public final ChunkToLodBuilder chunkToLodBuilder;
	public final IServerLevelWrapper serverLevel;
	private final AppliedConfigState<Boolean> generatorEnabled;
	public F3Screen.NestedMessage f3Msg;
	public final AtomicReference<RenderState> renderState = new AtomicReference<>();
	public final AtomicReference<WorldGenState> worldGenState = new AtomicReference<>();
	
	
	
	public DhClientServerLevel(LocalSaveStructure save, IServerLevelWrapper level)
	{
		this.serverLevel = level;
		this.save = save;
		save.getDataFolder(level).mkdirs();
		save.getRenderCacheFolder(level).mkdirs();
		this.dataFileHandler = new GeneratedDataFileHandler(this, save.getDataFolder(level));
		FileScanUtil.scanFile(save, this.serverLevel, this.dataFileHandler, null);
		LOGGER.info("Started DHLevel for {} with saves at {}", level, save);
		this.f3Msg = new F3Screen.NestedMessage(this::f3Log);
		this.chunkToLodBuilder = new ChunkToLodBuilder();
		this.generatorEnabled = new AppliedConfigState<>(Config.Client.WorldGenerator.enableDistantGeneration);
	}
	
	
	
	/** Returns what should be displayed in Minecraft's F3 debug menu */
	private String[] f3Log()
	{
		RenderState rs = this.renderState.get();
		if (rs == null)
		{
			return new String[] { LodUtil.formatLog("level @ {}: Inactive", this.serverLevel.getDimensionType().getDimensionName()) };
		}
		else
		{
			return new String[] {
					LodUtil.formatLog("level @ {}: Active", this.serverLevel.getDimensionType().getDimensionName())
			};
		}
	}
	
	@Override
	public void clientTick()
	{
		RenderState rs = this.renderState.get();
		if (rs == null)
			return;
		
		if (rs.tree.viewDistance != Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH)
		{
			if (!this.renderState.compareAndSet(rs, null))
				return; //If we fail, we'll just wait for the next tick
			
			IClientLevelWrapper levelWrapper = rs.clientLevel;
			rs.close().join(); //TODO: Make it async.
			rs = new RenderState(levelWrapper);
			if (!this.renderState.compareAndSet(null, rs))
			{
				//FIXME: How to handle this?
				LOGGER.warn("Failed to set render state due to concurrency after changing view distance");
				rs.close();
				return;
			}
		}
		
		rs.tree.tick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
		rs.renderBufferHandler.update();
	}
	
	private void saveWrites(ChunkSizedData data)
	{
		RenderState rs = this.renderState.get();
		DhLodPos pos = data.getBBoxLodPos().convertUpwardsTo(FullDataSource.SECTION_SIZE_OFFSET);
		if (rs != null)
		{
			rs.renderFileHandler.write(new DhSectionPos(pos.detailLevel, pos.x, pos.z), data);
		}
		else
		{
			this.dataFileHandler.write(new DhSectionPos(pos.detailLevel, pos.x, pos.z), data);
		}
	}
	
	@Override
	public void serverTick() { this.chunkToLodBuilder.tick(); }
	
	public void startRenderer(IClientLevelWrapper clientLevel)
	{
		LOGGER.info("Starting renderer for {}", this);
		RenderState rs = new RenderState(clientLevel);
		if (!this.renderState.compareAndSet(null, rs))
		{
			LOGGER.warn("Failed to start renderer due to concurrency");
			rs.close();
		}
		else
		{
			this.generatorEnabled.pollNewValue();
			if (this.generatorEnabled.get() && this.worldGenState.get() == null)
			{
				WorldGenState wgs = new WorldGenState();
				if (!this.worldGenState.compareAndSet(null, wgs))
				{
					LOGGER.warn("Failed to start world gen due to concurrency");
					wgs.close(false);
				}
			}
			
		}
	}
	
	@Override
	public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler)
	{
		RenderState rs = this.renderState.get();
		if (rs == null)
		{
			LOGGER.error("Tried to call render() on {} when renderer has not been started!", this);
			return;
		}
		rs.renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
	}
	
	public void stopRenderer()
	{
		LOGGER.info("Stopping renderer for {}", this);
		RenderState rs = this.renderState.get();
		if (rs == null)
		{
			LOGGER.warn("Tried to stop renderer for {} when it was not started!", this);
			return;
		}
		
		while (!this.renderState.compareAndSet(rs, null))
		{
			rs = this.renderState.get();
			if (rs == null)
				return;
		}
		
		rs.close().join(); //TODO: Make it async.
		WorldGenState wgs = this.worldGenState.get();
		if (wgs != null)
		{
			while (!this.worldGenState.compareAndSet(wgs, null))
			{
				wgs = this.worldGenState.get();
				if (wgs == null)
					return;
			}
			wgs.close(true).join(); //TODO: Make it async.
		}
	}
	
	@Override //FIXME
	public int computeBaseColor(DhBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper block)
	{
		IClientLevelWrapper clientLevel = this.getClientLevelWrapper();
		if (clientLevel == null)
		{
			return 0;
		}
		else
		{
			return clientLevel.computeBaseColor(pos, biome, block);
		}
	}
	
	@Override
	public IClientLevelWrapper getClientLevelWrapper()
	{
		RenderState rs = this.renderState.get();
		return rs == null ? null : rs.clientLevel;
	}
	
	@Override
	public ILevelWrapper getLevelWrapper() { return this.serverLevel; }
	
	@Override
	public void updateChunk(IChunkWrapper chunk)
	{
		CompletableFuture<ChunkSizedData> future = this.chunkToLodBuilder.tryGenerateData(chunk);
		if (future != null)
		{
			future.thenAccept(this::saveWrites);
		}
	}
	
	@Override
	public void dumpRamUsage()
	{
		//TODO
	}
	
	@Override
	public int getMinY() { return this.serverLevel.getMinHeight(); }
	
	@Override
	public CompletableFuture<Void> save()
	{
		RenderState rs = this.renderState.get();
		if (rs != null)
		{
			return rs.renderFileHandler.flushAndSave().thenCombine(this.dataFileHandler.flushAndSave(), (a, b) -> null);
		}
		else
		{
			return this.dataFileHandler.flushAndSave();
		}
	}
	
	@Override
	public void close()
	{
		RenderState rs = this.renderState.get();
		if (rs != null)
		{
			while (!this.renderState.compareAndSet(rs, null))
			{
				rs = this.renderState.get();
				if (rs == null)
					return;
			}
			rs.close().join(); //TODO: Make it async.
		}
		
		WorldGenState wgs = this.worldGenState.get();
		if (wgs != null)
		{
			while (!this.worldGenState.compareAndSet(wgs, null))
			{
				wgs = this.worldGenState.get();
				if (wgs == null)
					return;
			}
			wgs.close(true).join(); //TODO: Make it async.
		}
		
		LOGGER.info("Closed {}", this);
	}
	
	
	@Override
	public void doWorldGen()
	{
		WorldGenState wgs = this.worldGenState.get();
		if (this.generatorEnabled.pollNewValue())
		{
			boolean shouldDoWorldGen = this.generatorEnabled.get() && this.renderState.get() != null;
			if (shouldDoWorldGen && wgs == null)
			{
				WorldGenState newWgs = new WorldGenState();
				if (!this.worldGenState.compareAndSet(null, newWgs))
				{
					LOGGER.warn("Failed to start world gen due to concurrency");
					newWgs.close(false);
				}
			}
			else if (!shouldDoWorldGen && wgs != null)
			{
				while (!this.worldGenState.compareAndSet(wgs, null))
				{
					wgs = this.worldGenState.get();
					if (wgs == null)
						return;
				}
				wgs.close(true).join(); //TODO: Make it async.
			}
		}
		
		if (wgs != null)
		{
			wgs.chunkGenerator.preGeneratorTaskStart();
			wgs.worldGenerationQueue.pollAndStartClosest(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
		}
	}
	
	@Override
	public IServerLevelWrapper getServerLevelWrapper() { return this.serverLevel; }
	
	@Override
	public IDataSourceProvider getFileHandler() { return this.dataFileHandler; }
	
	
	
	
	//================//
	// helper classes //
	//================//
	
	private class RenderState
	{
		final IClientLevelWrapper clientLevel;
		final LodQuadTree tree;
		final RenderFileHandler renderFileHandler;
		final RenderBufferHandler renderBufferHandler; //TODO: Should this be owned by renderer?
		final LodRenderer renderer;
		
		RenderState(IClientLevelWrapper clientLevel)
		{
			this.clientLevel = clientLevel;
			this.renderFileHandler = new RenderFileHandler(dataFileHandler, DhClientServerLevel.this, save.getRenderCacheFolder(serverLevel));
			this.tree = new LodQuadTree(DhClientServerLevel.this, Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * 16,
					MC_CLIENT.getPlayerBlockPos().x, MC_CLIENT.getPlayerBlockPos().z, this.renderFileHandler);
			this.renderBufferHandler = new RenderBufferHandler(tree);
			FileScanUtil.scanFile(save, serverLevel, null, this.renderFileHandler);
			this.renderer = new LodRenderer(this.renderBufferHandler);
		}
		
		CompletableFuture<Void> close()
		{
			this.renderer.close();
			this.renderBufferHandler.close();
			this.tree.close();
			return this.renderFileHandler.flushAndSave();
		}
	}
	
	private class WorldGenState
	{
		public final IWorldGenerator chunkGenerator;
		public final WorldGenerationQueue worldGenerationQueue;
		
		WorldGenState()
		{
			this.chunkGenerator = new BatchGenerator(DhClientServerLevel.this);
			this.worldGenerationQueue = new WorldGenerationQueue(this.chunkGenerator);
			dataFileHandler.setGenerationQueue(this.worldGenerationQueue);
		}
		
		CompletableFuture<Void> close(boolean doInterrupt)
		{
			dataFileHandler.popGenerationQueue();
			return this.worldGenerationQueue.startClosing(true, doInterrupt)
											.exceptionally(ex ->
									   {
										   LOGGER.error("Error closing generation queue", ex);
										   return null;
									   }).thenRun(this.chunkGenerator::close)
											.exceptionally(ex ->
									   {
										   LOGGER.error("Error closing world gen", ex);
										   return null;
									   });
		}
	}
	
}
