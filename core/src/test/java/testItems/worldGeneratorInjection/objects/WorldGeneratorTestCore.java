package testItems.worldGeneratorInjection.objects;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.worldGenerator.ICoreDhApiWorldGenerator;
import com.seibel.lod.core.api.external.coreImplementations.interfaces.wrappers.world.ICoreDhApiLevelWrapper;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenThreadMode;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenerationStep;
import com.seibel.lod.core.dependencyInjection.OverrideInjector;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

/**
 * Dummy test implementation object for world generator injection unit tests.
 *
 * @author James Seibel
 * @version 2022-7-26
 */
public class WorldGeneratorTestCore implements ICoreDhApiWorldGenerator
{
	public static EWorldGenThreadMode THREAD_MODE = EWorldGenThreadMode.SINGLE_THREADED;
	
	
	//==============//
	// IOverridable //
	//==============//
	
	@Override
	public int getPriority() { return OverrideInjector.CORE_PRIORITY; }
	
	
	
	//======================//
	// IDhApiWorldGenerator //
	//======================//
	
	@Override
	public EWorldGenThreadMode getCoreThreadingMode() { return THREAD_MODE; }
	
	@Override
	public IChunkWrapper generateCoreChunk(int chunkPosX, int chunkPosZ, ICoreDhApiLevelWrapper serverLevelWrapper, EWorldGenerationStep maxStepToGenerate)
	{
		// not necessary for testing
		return null;
	}
	
}
