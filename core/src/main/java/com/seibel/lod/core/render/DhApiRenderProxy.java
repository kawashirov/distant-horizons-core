package com.seibel.lod.core.render;

import com.seibel.lod.api.interfaces.render.IDhApiRenderProxy;
import com.seibel.lod.api.objects.DhApiResult;
import com.seibel.lod.core.api.internal.SharedApi;
import com.seibel.lod.core.level.IDhLevel;

/**
 * Used to interact with Distant Horizons' rendering systems.
 * 
 * @author James Seibel
 * @version 2023-2-8
 */
public class DhApiRenderProxy implements IDhApiRenderProxy
{
	public static DhApiRenderProxy INSTANCE = new DhApiRenderProxy();
	
	
	
	private DhApiRenderProxy() {  }
	
	
	
	public DhApiResult<Boolean> clearRenderDataCache()
	{
		// make sure this is a valid time to run the method
		if (SharedApi.currentWorld == null)
		{
			return DhApiResult.createFail("No world loaded");			
		}
		
		
		// clear the render caches for each level
		Iterable<? extends IDhLevel> loadedLevels = SharedApi.currentWorld.getAllLoadedLevels();
		for (IDhLevel level : loadedLevels)
		{
			if (level != null)
			{
				level.clearRenderDataCache();
			}
		}
		
		return DhApiResult.createSuccess();
	}
	
	
}
