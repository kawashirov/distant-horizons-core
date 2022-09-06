package com.seibel.lod.api.items.interfaces.override;

import com.seibel.lod.core.api.external.items.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.core.api.external.items.enums.worldGeneration.EDhApiWorldGenThreadMode;
import com.seibel.lod.api.items.interfaces.world.IDhApiChunkWrapper;
import com.seibel.lod.api.items.interfaces.world.IDhApiLevelWrapper;

/**
 * @author James Seibel
 * @version 2022-7-26
 */
public interface IDhApiWorldGenerator extends IDhApiOverrideable
{
	/** Returns which thread chunk generation requests can be created on. */
	EDhApiWorldGenThreadMode getThreadingMode();
	
	IDhApiChunkWrapper generateChunk(int chunkPosX, int chunkPosZ, IDhApiLevelWrapper serverLevelWrapper, EDhApiWorldGenerationStep maxStepToGenerate);
	
}
