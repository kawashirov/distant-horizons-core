package testItems.worldGeneratorInjection.objects;

import com.seibel.lod.api.items.enums.worldGeneration.EDhApiWorldGenThreadMode;
import com.seibel.lod.api.items.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.api.items.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.lod.api.items.interfaces.world.IDhApiChunkWrapper;
import com.seibel.lod.api.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenThreadMode;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenerationStep;
import com.seibel.lod.core.dependencyInjection.OverrideInjector;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

/**
 * Dummy test implementation object for world generator injection unit tests.
 *
 * @author James Seibel
 * @version 2022-8-9
 */
public class WorldGeneratorTestPrimary implements IDhApiWorldGenerator
{
	public static int PRIORITY = OverrideInjector.DEFAULT_NON_CORE_OVERRIDE_PRIORITY + 5;
	public static EDhApiWorldGenThreadMode THREAD_MODE = EDhApiWorldGenThreadMode.MULTI_THREADED;
	

	//==============//
	// IOverridable //
	//==============//

	@Override
	public int getPriority() { return PRIORITY; }



	//======================//
	// IDhApiWorldGenerator //
	//======================//

	@Override
	public EDhApiWorldGenThreadMode getCoreThreadingMode() { return THREAD_MODE; }
	
	@Override 
	public IDhApiChunkWrapper generateCoreChunk(int chunkPosX, int chunkPosZ, IDhApiLevelWrapper serverLevelWrapper, EDhApiWorldGenerationStep maxStepToGenerate)
	{
		return null;
	}
	
}
