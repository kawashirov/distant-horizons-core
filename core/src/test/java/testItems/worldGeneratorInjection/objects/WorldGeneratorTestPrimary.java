package testItems.worldGeneratorInjection.objects;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.worldGenerator.ICoreDhApiWorldGenerator;
import com.seibel.lod.core.api.external.coreImplementations.interfaces.wrappers.world.ICoreDhApiLevelWrapper;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenThreadMode;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenerationStep;
import com.seibel.lod.core.handlers.dependencyInjection.OverrideInjector;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

/**
 * Dummy test implementation object for world generator injection unit tests.
 *
 * @author James Seibel
 * @version 2022-8-9
 */
public class WorldGeneratorTestPrimary implements ICoreDhApiWorldGenerator
{
	public static int PRIORITY = OverrideInjector.DEFAULT_NON_CORE_OVERRIDE_PRIORITY + 5;
	public static EWorldGenThreadMode THREAD_MODE = EWorldGenThreadMode.MULTI_THREADED;
	

	//==============//
	// IOverridable //
	//==============//

	@Override
	public int getPriority() { return PRIORITY; }



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
