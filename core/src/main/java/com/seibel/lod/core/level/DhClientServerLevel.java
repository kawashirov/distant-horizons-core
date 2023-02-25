package com.seibel.lod.core.level;

import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.lod.core.DependencyInjection.WorldGeneratorInjector;
import com.seibel.lod.core.config.AppliedConfigState;
import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.FullDataSource;
import com.seibel.lod.core.dataObjects.transformers.ChunkToLodBuilder;
import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.generation.BatchGenerator;
import com.seibel.lod.core.generation.WorldGenerationQueue;
import com.seibel.lod.core.level.states.ClientRenderState;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.file.fullDatafile.GeneratedFullDataFileHandler;
import com.seibel.lod.core.util.FileScanUtil;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.file.structure.LocalSaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.logging.f3.F3Screen;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.util.math.Mat4f;
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
	
	public final LocalSaveStructure saveStructure;
	public final GeneratedFullDataFileHandler dataFileHandler;
	public final ChunkToLodBuilder chunkToLodBuilder;
	public final IServerLevelWrapper serverLevel;
	private final AppliedConfigState<Boolean> generatorEnabled;
	public F3Screen.NestedMessage f3Message;
	
	private final AtomicReference<ClientRenderState> ClientRenderStateRef = new AtomicReference<>();
	private final AtomicReference<WorldGenState> worldGenStateRef = new AtomicReference<>();
	
	
	
	public DhClientServerLevel(LocalSaveStructure saveStructure, IServerLevelWrapper level)
	{
		this.serverLevel = level;
		
		this.saveStructure = saveStructure;
		saveStructure.getDataFolder(level).mkdirs();
		saveStructure.getRenderCacheFolder(level).mkdirs();
		
		this.dataFileHandler = new GeneratedFullDataFileHandler(this, saveStructure.getDataFolder(level));
		FileScanUtil.scanFiles(saveStructure, this.serverLevel, this.dataFileHandler, null);
		
		LOGGER.info("Started DHLevel for "+level+" with saves at "+saveStructure);
		this.f3Message = new F3Screen.NestedMessage(this::f3Log);
		this.chunkToLodBuilder = new ChunkToLodBuilder();
		this.generatorEnabled = new AppliedConfigState<>(Config.Client.WorldGenerator.enableDistantGeneration);
	}
	
	
	
	/** Returns what should be displayed in Minecraft's F3 debug menu */
	private String[] f3Log()
	{
		ClientRenderState rs = this.ClientRenderStateRef.get();
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
		ClientRenderState clientRenderState = this.ClientRenderStateRef.get();
		if (clientRenderState == null)
		{
			return;
		}
		
		if (clientRenderState.quadtree.blockRenderDistance != Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH)
		{
			if (!this.ClientRenderStateRef.compareAndSet(clientRenderState, null))
			{
				return; //If we fail, we'll just wait for the next tick
			}
			
			IClientLevelWrapper levelWrapper = clientRenderState.clientLevel;
			clientRenderState.closeAsync().join(); //TODO: Make it async.
			clientRenderState = new ClientRenderState(this, levelWrapper);
			if (!this.ClientRenderStateRef.compareAndSet(null, clientRenderState))
			{
				//FIXME: How to handle this?
				LOGGER.warn("Failed to set render state due to concurrency after changing view distance");
				clientRenderState.closeAsync();
				return;
			}
		}
		
		clientRenderState.quadtree.tick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
		clientRenderState.renderer.bufferHandler.update();
	}
	
	@Override
	public void serverTick() { this.chunkToLodBuilder.tick(); }
	
	public void startRenderer(IClientLevelWrapper clientLevel)
	{
		LOGGER.info("Starting renderer for "+this);
		ClientRenderState ClientRenderState = new ClientRenderState(this, clientLevel);
		if (!this.ClientRenderStateRef.compareAndSet(null, ClientRenderState))
		{
			LOGGER.warn("Failed to start renderer due to concurrency");
			ClientRenderState.closeAsync();
		}
		else
		{
			this.generatorEnabled.pollNewValue();
			if (this.generatorEnabled.get() && this.worldGenStateRef.get() == null)
			{
				WorldGenState worldGenState = new WorldGenState(this);
				if (!this.worldGenStateRef.compareAndSet(null, worldGenState))
				{
					LOGGER.warn("Failed to start world gen due to concurrency");
					worldGenState.closeAsync(false);
				}
			}
			
		}
	}
	
	@Override
	public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler)
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState == null)
		{
			LOGGER.error("Tried to call render() on "+this+" when renderer has not been started!");
			return;
		}
		ClientRenderState.renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
	}
	
	public void stopRenderer()
	{
		LOGGER.info("Stopping renderer for "+this);
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState == null)
		{
			LOGGER.warn("Tried to stop renderer for "+this+" when it was not started!");
			return;
		}
		
		// stop the render state
		while (!this.ClientRenderStateRef.compareAndSet(ClientRenderState, null)) // TODO why is there a while loop here?
		{
			ClientRenderState = this.ClientRenderStateRef.get();
			if (ClientRenderState == null)
			{
				return;
			}
		}
		ClientRenderState.closeAsync().join(); //TODO: Make it async.
		
		// stop the world generator
		WorldGenState worldGenState = this.worldGenStateRef.get();
		if (worldGenState != null)
		{
			while (!this.worldGenStateRef.compareAndSet(worldGenState, null))
			{
				worldGenState = this.worldGenStateRef.get();
				if (worldGenState == null)
				{
					return;
				}
			}
			worldGenState.closeAsync(true).join(); //TODO: Make it async.
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
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		return ClientRenderState == null ? null : ClientRenderState.clientLevel;
	}
	
	@Override
	public ILevelWrapper getLevelWrapper() { return this.serverLevel; }
	
	@Override
	public void updateChunkAsync(IChunkWrapper chunk)
	{
		CompletableFuture<ChunkSizedFullDataSource> future = this.chunkToLodBuilder.tryGenerateData(chunk);
		if (future != null)
		{
			future.thenAccept(this::saveWrites);
		}
	}
	private void saveWrites(ChunkSizedFullDataSource data)
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		DhLodPos pos = data.getBBoxLodPos().convertToDetailLevel(FullDataSource.SECTION_SIZE_OFFSET);
		if (ClientRenderState != null)
		{
			ClientRenderState.renderSourceFileHandler.write(new DhSectionPos(pos.detailLevel, pos.x, pos.z), data);
		}
		else
		{
			this.dataFileHandler.write(new DhSectionPos(pos.detailLevel, pos.x, pos.z), data);
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
	public CompletableFuture<Void> saveAsync()
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState != null)
		{
			return ClientRenderState.renderSourceFileHandler.flushAndSave().thenCombine(this.dataFileHandler.flushAndSave(), (voidA, voidB) -> null);
		}
		else
		{
			return this.dataFileHandler.flushAndSave();
		}
	}
	
	@Override
	public void close()
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState != null)
		{
			while (!this.ClientRenderStateRef.compareAndSet(ClientRenderState, null))
			{
				ClientRenderState = this.ClientRenderStateRef.get();
				if (ClientRenderState == null)
				{
					return;
				}
			}
			ClientRenderState.closeAsync().join(); //TODO: Make this async.
		}
		
		WorldGenState wgs = this.worldGenStateRef.get();
		if (wgs != null)
		{
			while (!this.worldGenStateRef.compareAndSet(wgs, null))
			{
				wgs = this.worldGenStateRef.get();
				if (wgs == null)
				{
					return;
				}
			}
			wgs.closeAsync(true).join(); //TODO: Make it async.
		}
		
		LOGGER.info("Closed "+this);
	}
	
	
	@Override
	public void doWorldGen()
	{
		WorldGenState wgs = this.worldGenStateRef.get();
		
		// if the world generator config changes, add/remove the world generator
		if (this.generatorEnabled.pollNewValue())
		{
			boolean shouldDoWorldGen = this.generatorEnabled.get() && this.ClientRenderStateRef.get() != null;
			if (shouldDoWorldGen && wgs == null)
			{
				// create the new world generator
				WorldGenState newWgs = new WorldGenState(this);
				if (!this.worldGenStateRef.compareAndSet(null, newWgs))
				{
					LOGGER.warn("Failed to start world gen due to concurrency");
					newWgs.closeAsync(false);
				}
			}
			else if (!shouldDoWorldGen && wgs != null)
			{
				// shut down the world generator
				while (!this.worldGenStateRef.compareAndSet(wgs, null))
				{
					wgs = this.worldGenStateRef.get();
					if (wgs == null)
					{
						return;
					}
				}
				wgs.closeAsync(true).join(); //TODO: Make it async.
			}
		}
		
		
		if (wgs != null)
		{
			// queue new world generation requests
			wgs.chunkGenerator.preGeneratorTaskStart();
			wgs.worldGenerationQueue.runCurrentGenTasksUntilBusy(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
		}
	}
	
	@Override
	public IServerLevelWrapper getServerLevelWrapper() { return this.serverLevel; }
	
	@Override
	public IFullDataSourceProvider getFileHandler() { return this.dataFileHandler; }
	
	@Override
	public void clearRenderDataCache()
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState != null && ClientRenderState.quadtree != null)
		{
			ClientRenderState.quadtree.clearRenderDataCache();
		}
	}
	
	
	
	
	//================//
	// helper classes //
	//================//
	
	private class WorldGenState
	{
		public final IDhApiWorldGenerator chunkGenerator;
		public final WorldGenerationQueue worldGenerationQueue;
		
		
		
		WorldGenState(IDhLevel level)
		{
			IDhApiWorldGenerator worldGenerator = WorldGeneratorInjector.INSTANCE.get(level.getLevelWrapper());
			if (worldGenerator == null)
			{
				// no override generator is bound, use the Core world generator
				worldGenerator = new BatchGenerator(level);
				// binding the core generator won't prevent other mods from binding their own generators 
				// since core world generator's should have the lowest override priority
				WorldGeneratorInjector.INSTANCE.bind(level.getLevelWrapper(), worldGenerator);
			}
			this.chunkGenerator = worldGenerator;
			
			this.worldGenerationQueue = new WorldGenerationQueue(this.chunkGenerator);
			DhClientServerLevel.this.dataFileHandler.setGenerationQueue(this.worldGenerationQueue);
		}
		
		
		
		CompletableFuture<Void> closeAsync(boolean doInterrupt)
		{
			DhClientServerLevel.this.dataFileHandler.clearGenerationQueue();
			return this.worldGenerationQueue.startClosing(true, doInterrupt)
					.exceptionally(ex ->
					{
						LOGGER.error("Error closing generation queue", ex);
						return null;
					}
					).thenRun(this.chunkGenerator::close)
					.exceptionally(ex ->
					{
						LOGGER.error("Error closing world gen", ex);
						return null;
					});
		}
	}
	
}
