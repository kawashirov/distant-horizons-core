package com.seibel.lod.core.api.external.methods.override;

import com.seibel.lod.core.api.external.items.interfaces.override.IDhApiWorldGenerator;
import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.core.api.external.items.objects.DhApiResult;

/**
 * Handles adding/removing world generator overrides.
 *
 * @author James Seibel
 * @version 2022-7-15
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
	public static DhApiResult registerWorldGeneratorOverride(IDhApiWorldGenerator worldGenerator)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Registers the given world generator for the given level. <Br> <Br>
	 *
	 * Only one world generator can be registered for a specific level at a given time. <Br>
	 * If another world generator has already been registered, DhApiResult will return
	 * the name of the previously registered generator and success = false.
	 */
	public static DhApiResult registerWorldGeneratorOverride(IDhApiLevelWrapper levelWrapper, IDhApiWorldGenerator worldGenerator)
	{
		throw new UnsupportedOperationException();
	}
	
	
}
