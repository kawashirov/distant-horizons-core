package com.seibel.lod.core.api.external.override.interfaces;

import com.seibel.lod.core.api.external.override.enums.EDhApiWorldGenerationStep;
import com.seibel.lod.core.api.external.shared.enums.EDhApiWorldGenThreadMode;
import com.seibel.lod.core.api.external.shared.interfaces.IDhApiChunkWrapper;
import com.seibel.lod.core.api.external.shared.interfaces.IDhApiLevelWrapper;

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
