package com.seibel.lod.api.items.interfaces.override.worldGenerator;

import com.seibel.lod.api.items.enums.worldGeneration.EDhApiWorldGenThreadMode;
import com.seibel.lod.api.items.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.api.items.interfaces.override.IDhApiOverrideable;
import com.seibel.lod.api.items.interfaces.world.IDhApiChunkWrapper;
import com.seibel.lod.api.items.interfaces.world.IDhApiLevelWrapper;

/**
 * @author James Seibel
 * @version 2022-9-8
 */
public interface IDhApiWorldGenerator extends IDhApiOverrideable
{
	/** Returns which thread chunk generation requests can be created on. */
	EDhApiWorldGenThreadMode getCoreThreadingMode();
	
	IDhApiChunkWrapper generateCoreChunk(int chunkPosX, int chunkPosZ, IDhApiLevelWrapper serverLevelWrapper, EDhApiWorldGenerationStep maxStepToGenerate);
	
}
