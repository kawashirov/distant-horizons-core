package com.seibel.lod.core.api.external.override;

import com.seibel.lod.core.api.external.override.interfaces.IDhApiWorldGenerator;
import com.seibel.lod.core.api.external.shared.interfaces.IDhApiLevelWrapper;
import com.seibel.lod.core.api.external.shared.objects.DhApiResult;

/**
 * Handles adding/removing world generator overrides.
 *
 * @author James Seibel
 * @version 2022-7-14
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
	public static DhApiResult RegisterWorldGeneratorOverride(IDhApiWorldGenerator worldGenerator)
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
	public static DhApiResult RegisterWorldGeneratorOverride(IDhApiLevelWrapper levelWrapper, IDhApiWorldGenerator worldGenerator)
	{
		throw new UnsupportedOperationException();
	}
	
	
	
	/**
	 * Removes the given world generator for the given level if it has been registered. <br>
	 * If the world generator wasn't registered, the result will return success = false.
	 */
	public static DhApiResult UnRegisterWorldGeneratorOverride(IDhApiLevelWrapper levelWrapper, IDhApiWorldGenerator worldGenerator)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Removes the registered world generator for the given level if one has been registered. <br>
	 * If no world generator was registered, the result will return success = false.
	 */
	public static DhApiResult UnRegisterWorldGeneratorOverride(IDhApiLevelWrapper levelWrapper)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Removes the registered world generator for each level it was registered. <br>
	 * If this world generator wasn't registered for any level, the result will return success = false.
	 */
	public static DhApiResult UnRegisterWorldGeneratorOverride(IDhApiWorldGenerator worldGenerator)
	{
		throw new UnsupportedOperationException();
	}
	
	
}
