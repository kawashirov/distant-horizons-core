package com.seibel.lod.core.api.external.coreImplementations.interfaces.override.worldGenerator;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.ICoreDhApiOverrideable;
import com.seibel.lod.core.api.external.coreImplementations.interfaces.wrappers.world.ICoreDhApiLevelWrapper;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenThreadMode;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenerationStep;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

/**
 * @author James Seibel
 * @version 2022-9-8
 */
public interface ICoreDhApiWorldGenerator extends ICoreDhApiOverrideable
{
	/** Returns which thread chunk generation requests can be created on. */
	EWorldGenThreadMode getCoreThreadingMode();
	
	IChunkWrapper generateCoreChunk(int chunkPosX, int chunkPosZ, ICoreDhApiLevelWrapper serverLevelWrapper, EWorldGenerationStep maxStepToGenerate);
	
}
