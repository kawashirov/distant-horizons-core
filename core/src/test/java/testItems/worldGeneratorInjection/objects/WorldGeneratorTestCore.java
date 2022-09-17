package testItems.worldGeneratorInjection.objects;

import com.seibel.lod.api.enums.worldGeneration.EDhApiWorldGenThreadMode;
import com.seibel.lod.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.lod.api.interfaces.world.IDhApiChunkWrapper;
import com.seibel.lod.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.core.DependencyInjection.OverrideInjector;

/**
 * Dummy test implementation object for world generator injection unit tests.
 *
 * @author James Seibel
 * @version 2022-7-26
 */
public class WorldGeneratorTestCore implements IDhApiWorldGenerator
{
	public static EDhApiWorldGenThreadMode THREAD_MODE = EDhApiWorldGenThreadMode.SINGLE_THREADED;
	
	
	//==============//
	// IOverridable //
	//==============//
	
	@Override
	public int getPriority() { return OverrideInjector.CORE_PRIORITY; }
	
	
	
	//======================//
	// IDhApiWorldGenerator //
	//======================//
	
	
	/** Returns which thread chunk generation requests can be created on. */
	@Override 
	public EDhApiWorldGenThreadMode getCoreThreadingMode()
	{
		return THREAD_MODE;
	}
	
	@Override 
	public IDhApiChunkWrapper generateCoreChunk(int chunkPosX, int chunkPosZ, IDhApiLevelWrapper serverLevelWrapper, EDhApiWorldGenerationStep maxStepToGenerate)
	{
		return null;
	}
	
	
}
