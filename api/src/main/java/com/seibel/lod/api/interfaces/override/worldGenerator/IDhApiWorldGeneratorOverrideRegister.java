package com.seibel.lod.api.interfaces.override.worldGenerator;

import com.seibel.lod.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.objects.DhApiResult;

/**
 * Handles adding world generator overrides.
 *
 * @author James Seibel
 * @version 2022-12-10
 */
public interface IDhApiWorldGeneratorOverrideRegister
{
	/**
	 * Registers the given world generator for the given level. <Br> <Br>
	 *
	 * Only one world generator can be registered for a specific level at a given time. <Br>
	 * If another world generator has already been registered, DhApiResult will return
	 * the name of the previously registered generator and success = false.
	 */
	DhApiResult<Void> registerWorldGeneratorOverride(IDhApiLevelWrapper levelWrapper, IDhApiWorldGenerator worldGenerator);
	
}
