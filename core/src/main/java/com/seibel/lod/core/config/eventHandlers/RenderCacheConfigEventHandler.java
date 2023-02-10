package com.seibel.lod.core.config.eventHandlers;

import com.seibel.lod.api.DhApiMain;
import com.seibel.lod.api.enums.config.EVerticalQuality;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.config.types.ConfigEntry;

/**
 * Listens to the config and will automatically
 * clear the current render cache if certain settings are changed. <br> <br>
 * 
 * Note: if additional settings should clear the render cache, add those to this listener, don't create a new listener
 * 
 * @author James Seibel
 * @version 2023-2-9
 */
public class RenderCacheConfigEventHandler implements ConfigEntry.Listener
{
	public static RenderCacheConfigEventHandler INSTANCE = new RenderCacheConfigEventHandler();
	
	private EVerticalQuality previousVerticalQualitySetting = null;
	
	
	
	/** private since we only ever need one handler at a time */
	private RenderCacheConfigEventHandler() {  }
	
	
	
	@Override 
	public void onModify()
	{
		// check if the vertical quality changed
		EVerticalQuality newVerticalQuality = Config.Client.Graphics.Quality.verticalQuality.get();
		if (this.previousVerticalQualitySetting != newVerticalQuality)
		{
			// TODO add a cancelable delay between the method being fired and any data getting cleared,
			//      this would be to prevent clearing the same data 5 times in rapid succession 
			//      when the user is switching through settings in the config UI
			 
			if (DhApiMain.Delayed.renderProxy.clearRenderDataCache().success)
			{
				this.previousVerticalQualitySetting = newVerticalQuality;
			}
		}
		
	}
	
	@Override
	public void onUiModify() { /* do nothing, we only care about modified config values */ }
	
}
