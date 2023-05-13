package com.seibel.lod.core.config.eventHandlers;

import com.seibel.lod.api.DhApiMain;
import com.seibel.lod.api.enums.config.EHorizontalResolution;
import com.seibel.lod.api.enums.config.EVerticalQuality;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.config.types.ConfigEntry;
import com.seibel.lod.core.util.DetailDistanceUtil;

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
	
	// previous values used to check if a watched setting was actually modified
	private EVerticalQuality previousVerticalQualitySetting = null;
	private EHorizontalResolution previousHorizontalResolution = null;
	
	
	
	/** private since we only ever need one handler at a time */
	private RenderCacheConfigEventHandler() {  }
	
	
	
	@Override 
	public void onModify()
	{		
		// confirm a setting was actually changed
		boolean refreshRenderData = false;
		
		
		EVerticalQuality newVerticalQuality = Config.Client.Graphics.Quality.verticalQuality.get();
		if (this.previousVerticalQualitySetting != newVerticalQuality)
		{
			this.previousVerticalQualitySetting = newVerticalQuality;
			refreshRenderData = true;
		}
		
		EHorizontalResolution newHorizontalResolution = Config.Client.Graphics.Quality.drawResolution.get();
		if (this.previousHorizontalResolution != newHorizontalResolution)
		{
			this.previousHorizontalResolution = newHorizontalResolution;
			refreshRenderData = true;
		}
		
		
		
		if (refreshRenderData)
		{
			// TODO add a timeout to prevent rapidly changing settings causing the render data thrashing.
			DetailDistanceUtil.minDetail = newHorizontalResolution.detailLevel;
			DhApiMain.Delayed.renderProxy.clearRenderDataCache();
		}
		
	}
	
	@Override
	public void onUiModify() { /* do nothing, we only care about modified config values */ }
	
}
