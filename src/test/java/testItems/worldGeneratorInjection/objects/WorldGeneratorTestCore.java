package testItems.worldGeneratorInjection.objects;

import com.seibel.lod.core.api.external.items.enums.override.EDhApiOverridePriority;
import com.seibel.lod.core.api.external.items.enums.worldGeneration.EDhApiWorldGenThreadMode;
import com.seibel.lod.core.api.external.items.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.core.api.external.items.interfaces.override.IDhApiWorldGenerator;
import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiChunkWrapper;
import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiLevelWrapper;

/**
 * Dummy test implementation object for world generator injection unit tests.
 *
 * @author James Seibel
 * @version 2022-7-26
 */
public class WorldGeneratorTestCore implements IDhApiWorldGenerator
{
	public static EDhApiWorldGenThreadMode THREAD_MODE = EDhApiWorldGenThreadMode.SERVER_THREAD;
	
	
	//==============//
	// IOverridable //
	//==============//
	
	@Override
	public EDhApiOverridePriority getOverrideType() { return EDhApiOverridePriority.CORE; }
	
	
	
	//======================//
	// IDhApiWorldGenerator //
	//======================//
	
	@Override
	public EDhApiWorldGenThreadMode getThreadingMode() { return THREAD_MODE; }
	
	@Override
	public IDhApiChunkWrapper generateChunk(int chunkPosX, int chunkPosZ, IDhApiLevelWrapper serverLevelWrapper, EDhApiWorldGenerationStep maxStepToGenerate)
	{
		// not necessary for testing
		return null;
	}
}
