package testItems.worldGeneratorInjection.objects;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.worldGenerator.ICoreDhApiWorldGenerator;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenThreadMode;
import com.seibel.lod.core.handlers.dependencyInjection.OverrideInjector;

/**
 * Dummy test implementation object for world generator injection unit tests.
 *
 * @author James Seibel
 * @version 2022-7-26
 */
//public class WorldGeneratorTestPrimary implements ICoreDhApiWorldGenerator
//{
//	public static EWorldGenThreadMode THREAD_MODE = EWorldGenThreadMode.MULTI_THREADED;
//
//
//	//==============//
//	// IOverridable //
//	//==============//
//
//	@Override
//	public int getOverridePriority() { return OverrideInjector.DEFAULT_NON_CORE_OVERRIDE_PRIORITY; }
//
//
//
//	//======================//
//	// IDhApiWorldGenerator //
//	//======================//
//
//	@Override
//	public EDhApiWorldGenThreadMode getThreadingMode() { return THREAD_MODE; }
//
//	@Override
//	public IDhApiChunkWrapper generateChunk(int chunkPosX, int chunkPosZ, IDhApiLevelWrapper serverLevelWrapper, EDhApiWorldGenerationStep maxStepToGenerate)
//	{
//		// not necessary for testing
//		return null;
//	}
//}
