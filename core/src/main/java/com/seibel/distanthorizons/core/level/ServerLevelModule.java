package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.core.config.AppliedConfigState;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.file.fullDatafile.GeneratedFullDataFileHandler;
import com.seibel.distanthorizons.core.file.structure.AbstractSaveStructure;
import com.seibel.distanthorizons.core.generation.BatchGenerator;
import com.seibel.distanthorizons.core.generation.WorldGenerationQueue;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import com.seibel.distanthorizons.coreapi.DependencyInjection.WorldGeneratorInjector;
import org.apache.logging.log4j.Logger;

public class ServerLevelModule
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	public final IDhServerLevel parentServerLevel;
	public final AbstractSaveStructure saveStructure;
	public final GeneratedFullDataFileHandler dataFileHandler;
	public final AppliedConfigState<Boolean> worldGeneratorEnabledConfig;
	
	public final WorldGenModule worldGenModule;
	
	
	
	public ServerLevelModule(IDhServerLevel parentServerLevel, AbstractSaveStructure saveStructure)
	{
		this.parentServerLevel = parentServerLevel;
		this.saveStructure = saveStructure;
		this.dataFileHandler = new GeneratedFullDataFileHandler(parentServerLevel, saveStructure);
		this.worldGeneratorEnabledConfig = new AppliedConfigState<>(Config.Client.Advanced.WorldGenerator.enableDistantGeneration);
		this.worldGenModule = new WorldGenModule(this.dataFileHandler, this.parentServerLevel);
	}
	
	
	
	public void close()
	{
		// shutdown the world-gen
		this.worldGenModule.close();
		this.dataFileHandler.close();
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	public static class WorldGenState extends WorldGenModule.AbstractWorldGenState
	{
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
		
	}
	
}
