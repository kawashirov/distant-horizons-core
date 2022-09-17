package com.seibel.lod.api.methods.override;

import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGeneratorOverrideRegister;
import com.seibel.lod.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.objects.DhApiResult;
import com.seibel.lod.core.DependencyInjection.WorldGeneratorInjector;

/**
 * Handles adding world generator overrides.
 *
 * @author James Seibel
 * @version 2022-9-16
 */
public class DhApiWorldGeneratorOverrideRegister implements IDhApiWorldGeneratorOverrideRegister
{
	public static DhApiWorldGeneratorOverrideRegister INSTANCE = new DhApiWorldGeneratorOverrideRegister();
	
	private DhApiWorldGeneratorOverrideRegister() {  }
	
	
	
	@Override
	public DhApiResult registerWorldGeneratorOverride(IDhApiWorldGenerator worldGenerator)
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
	
	@Override
	public DhApiResult registerWorldGeneratorOverride(IDhApiLevelWrapper levelWrapper, IDhApiWorldGenerator worldGenerator)
	{
		try
		{
			WorldGeneratorInjector.INSTANCE.bind(levelWrapper, worldGenerator);
			return DhApiResult.createSuccess();
		}
		catch (Exception e)
		{
			return DhApiResult.createFail(e.getMessage());
		}
	}
	
}
