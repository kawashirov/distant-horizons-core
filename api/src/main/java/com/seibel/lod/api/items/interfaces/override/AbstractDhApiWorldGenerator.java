package com.seibel.lod.api.items.interfaces.override;

import com.seibel.lod.api.items.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.api.items.enums.worldGeneration.EDhApiWorldGenThreadMode;
import com.seibel.lod.api.items.interfaces.world.IDhApiChunkWrapper;
import com.seibel.lod.api.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.worldGenerator.ICoreDhApiWorldGenerator;
import com.seibel.lod.core.api.external.coreImplementations.interfaces.wrappers.world.ICoreDhApiLevelWrapper;
import com.seibel.lod.core.api.external.coreImplementations.objects.converters.GenericEnumConverter;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenThreadMode;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenerationStep;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

/**
 * @author James Seibel
 * @version 2022-9-8
 */
public abstract class AbstractDhApiWorldGenerator implements ICoreDhApiWorldGenerator, IDhApiOverrideable
{
	private final GenericEnumConverter<EWorldGenThreadMode, EDhApiWorldGenThreadMode> threadModeEnumConverter = new GenericEnumConverter<>(EWorldGenThreadMode.class, EDhApiWorldGenThreadMode.class);
	private final GenericEnumConverter<EWorldGenerationStep, EDhApiWorldGenerationStep> generationStepEnumConverter = new GenericEnumConverter<>(EWorldGenerationStep.class, EDhApiWorldGenerationStep.class);
	
	
	/** Returns which thread chunk generation requests can be created on. */
	public abstract EDhApiWorldGenThreadMode getThreadingMode();
	
	@Override
	public EWorldGenThreadMode getCoreThreadingMode()
	{
		return threadModeEnumConverter.convertToCoreType(this.getThreadingMode());
	}
	
	public abstract IDhApiChunkWrapper generateChunk(int chunkPosX, int chunkPosZ, IDhApiLevelWrapper serverLevelWrapper, EDhApiWorldGenerationStep maxStepToGenerate);
	
	@Override
	public final IChunkWrapper generateCoreChunk(int chunkPosX, int chunkPosZ, ICoreDhApiLevelWrapper serverLevelWrapper, EWorldGenerationStep maxStepToGenerate)
	{
		// TODO probably need to change the return type
		return null; //generateChunk(chunkPosX, chunkPosZ, null, generationStepEnumConverter.convertToApiType(maxStepToGenerate));
	}
	
	
}
