package com.seibel.lod.api.interfaces.override;

import com.seibel.lod.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.lod.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.enums.worldGeneration.EDhApiWorldGenThreadMode;
import com.seibel.lod.api.interfaces.world.IDhApiChunkWrapper;

/**
 * @author James Seibel
 * @version 2022-9-8
 */
public abstract class AbstractDhApiWorldGenerator implements IDhApiOverrideable
{	
	/** Returns which thread chunk generation requests can be created on. */
	public abstract EDhApiWorldGenThreadMode getThreadingMode();
	
	public EDhApiWorldGenThreadMode getCoreThreadingMode()
	{
		return this.getThreadingMode();
	}
	
	public abstract IDhApiChunkWrapper generateChunk(int chunkPosX, int chunkPosZ, IDhApiLevelWrapper serverLevelWrapper, EDhApiDistantGeneratorMode maxStepToGenerate);
	
	public final IDhApiChunkWrapper generateCoreChunk(int chunkPosX, int chunkPosZ, IDhApiLevelWrapper serverLevelWrapper, EDhApiDistantGeneratorMode maxStepToGenerate)
	{
		// TODO probably need to change the return type
		return null; //generateChunk(chunkPosX, chunkPosZ, null, generationStepEnumConverter.convertToApiType(maxStepToGenerate));
	}
	
	
}
