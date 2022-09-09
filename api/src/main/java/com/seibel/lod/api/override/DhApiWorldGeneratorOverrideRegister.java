package com.seibel.lod.api.override;

import com.seibel.lod.api.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.items.objects.DhApiResult;
import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.worldGenerator.ICoreDhApiWorldGenerator;
import com.seibel.lod.core.handlers.dependencyInjection.WorldGeneratorInjector;

/**
 * Handles adding world generator overrides.
 *
 * @author James Seibel
 * @version 2022-9-8
 */
public class DhApiWorldGeneratorOverrideRegister
{
	/**
	 * Registers the given world generator. <Br> <Br>
	 *
	 * This registers a backup world generator for all levels and will be overridden if there
	 * is a world generator for the specific level. <Br>
	 * If another world generator has already been registered, DhApiResult will return
	 * the name of the previously registered generator and success = false.
	 */
	public static DhApiResult registerWorldGeneratorOverride(ICoreDhApiWorldGenerator worldGenerator)
	{
		try
		{
			WorldGeneratorInjector.INSTANCE.bind(worldGenerator);
			return DhApiResult.createSuccess();
		}
		catch (Exception e)
		{
			return DhApiResult.createFail(e.getMessage());
		}
	}
	
	/**
	 * Registers the given world generator for the given level. <Br> <Br>
	 *
	 * Only one world generator can be registered for a specific level at a given time. <Br>
	 * If another world generator has already been registered, DhApiResult will return
	 * the name of the previously registered generator and success = false.
	 */
	public static DhApiResult registerWorldGeneratorOverride(IDhApiLevelWrapper levelWrapper, ICoreDhApiWorldGenerator worldGenerator)
	{
		try
		{
//			WorldGeneratorInjector.INSTANCE.bind(levelWrapper, worldGenerator);
			return DhApiResult.createSuccess();
		}
		catch (Exception e)
		{
			return DhApiResult.createFail(e.getMessage());
		}
	}
	
	
}
