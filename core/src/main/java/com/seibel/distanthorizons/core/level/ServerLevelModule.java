package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.core.config.AppliedConfigState;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.file.fullDatafile.GeneratedFullDataFileHandler;
import com.seibel.distanthorizons.core.file.structure.AbstractSaveStructure;
import com.seibel.distanthorizons.core.generation.BatchGenerator;
import com.seibel.distanthorizons.core.generation.WorldGenerationQueue;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhBlockPos2D;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import com.seibel.distanthorizons.coreapi.DependencyInjection.WorldGeneratorInjector;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class ServerLevelModule
{
	private static class WorldGenState
	{
		public final WorldGenerationQueue worldGenerationQueue;
		WorldGenState(IDhServerLevel level)
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
			this.worldGenerationQueue = new WorldGenerationQueue(worldGenerator);
		}
		
		CompletableFuture<Void> closeAsync(boolean doInterrupt)
		{
			return this.worldGenerationQueue.startClosing(true, doInterrupt)
					.exceptionally(ex ->
							{
								LOGGER.error("Error closing generation queue", ex);
								return null;
							}
					).thenRun(this.worldGenerationQueue::close)
					.exceptionally(ex ->
					{
						LOGGER.error("Error closing world gen", ex);
						return null;
					});
		}
		
		public void tick(DhBlockPos2D targetPosForGeneration)
		{
			worldGenerationQueue.runCurrentGenTasksUntilBusy(targetPosForGeneration);
		}
		
	}
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	public final IServerLevelWrapper levelWrapper;
	public final IDhServerLevel parent;
	public final AbstractSaveStructure saveStructure;
	public final GeneratedFullDataFileHandler dataFileHandler;
	public final AppliedConfigState<Boolean> worldGeneratorEnabledConfig;
	private final AtomicReference<WorldGenState> worldGenStateRef = new AtomicReference<>();
	
	public ServerLevelModule(IDhServerLevel parent, IServerLevelWrapper levelWrapper, AbstractSaveStructure saveStructure)
	{
		this.parent = parent;
		this.levelWrapper = levelWrapper;
		this.saveStructure = saveStructure;
		this.dataFileHandler = new GeneratedFullDataFileHandler(parent, saveStructure);
		this.worldGeneratorEnabledConfig = new AppliedConfigState<>(Config.Client.Advanced.WorldGenerator.enableDistantGeneration);
	}
	
	//==============//
	// tick methods //
	//==============//
	
	
	public void startWorldGen()
	{
		// create the new world generator
		WorldGenState newWgs = new WorldGenState(parent);
		if (!this.worldGenStateRef.compareAndSet(null, newWgs))
		{
			LOGGER.warn("Failed to start world gen due to concurrency");
			newWgs.closeAsync(false);
		}
		dataFileHandler.addWorldGenCompleteListener(parent);
		dataFileHandler.setGenerationQueue(newWgs.worldGenerationQueue);
	}
	
	public void stopWorldGen()
	{
		WorldGenState worldGenState = this.worldGenStateRef.get();
		if (worldGenState == null)
		{
			LOGGER.warn("Attempted to stop world gen when it was not running");
			return;
		}
		
		// shut down the world generator
		while (!this.worldGenStateRef.compareAndSet(worldGenState, null))
		{
			worldGenState = this.worldGenStateRef.get();
			if (worldGenState == null)
			{
				return;
			}
		}
		dataFileHandler.clearGenerationQueue();
		worldGenState.closeAsync(true).join(); //TODO: Make it async.
		dataFileHandler.removeWorldGenCompleteListener(parent);
	}
	
	public boolean isWorldGenRunning()
	{
		return this.worldGenStateRef.get() != null;
	}
	
	public void worldGenTick(DhBlockPos2D targetPosForGeneration)
	{
		WorldGenState worldGenState = this.worldGenStateRef.get();
		if (worldGenState != null)
		{
			// queue new world generation requests
			worldGenState.tick(targetPosForGeneration);
		}
	}
	
	//===============//
	// data handling //
	//===============//
	public void close()
	{
		// shutdown the world-gen
		WorldGenState worldGenState = this.worldGenStateRef.get();
		if (worldGenState != null)
		{
			while (!this.worldGenStateRef.compareAndSet(worldGenState, null))
			{
				worldGenState = this.worldGenStateRef.get();
				if (worldGenState == null)
				{
					break;
				}
			}
			
			if (worldGenState != null)
			{
				worldGenState.closeAsync(true).join(); //TODO: Make it async.
			}
		}
		dataFileHandler.close();
	}
	
	
	
	
	//=======================//
	// misc helper functions //
	//=======================//
	
	public void dumpRamUsage()
	{
		//TODO
	}
	
}
