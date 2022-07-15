package com.seibel.lod.core.api.external.items.interfaces.override;

import com.seibel.lod.core.api.external.items.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.core.api.external.items.enums.worldGeneration.EDhApiWorldGenThreadMode;
import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiChunkWrapper;
import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiLevelWrapper;

/**
 * @author James Seibel
 * @version 2022-7-14
 */
public interface IDhApiWorldGenerator extends IDhApiOverrideable
{
	/** Returns where chunk generation requests can be generated. */
	EDhApiWorldGenThreadMode getThreadingMode();
	
	IDhApiChunkWrapper generateChunk(int chunkPosX, int chunkPosZ, IDhApiLevelWrapper serverLevelWrapper, EDhApiWorldGenerationStep maxStepToGenerate);
	
}
