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
 * @version 2022-12-5
 */
public class WorldGeneratorTestSecondary extends TestWorldGenerator
{
	public static int PRIORITY = OverrideInjector.DEFAULT_NON_CORE_OVERRIDE_PRIORITY;
	public static EDhApiWorldGenThreadMode THREAD_MODE = EDhApiWorldGenThreadMode.SERVER_THREAD;
	
	
	@Override
	public int getPriority() { return PRIORITY; }
	
	@Override
	public EDhApiWorldGenThreadMode getThreadingMode() { return THREAD_MODE; }
	
}
