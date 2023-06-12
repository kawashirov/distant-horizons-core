package com.seibel.lod.core.config.eventHandlers;

import com.seibel.lod.api.DhApiMain;
import com.seibel.lod.api.enums.config.EHorizontalResolution;
import com.seibel.lod.api.enums.config.EVerticalQuality;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.config.listeners.ConfigChangeListener;
import com.seibel.lod.core.config.listeners.IConfigListener;
import com.seibel.lod.core.util.DetailDistanceUtil;

public class ResetConfigEventHandler
{
	public static ResetConfigEventHandler INSTANCE = new ResetConfigEventHandler();
	public final ConfigChangeListener<Boolean> configChangeListener;
	
	
	
	/** private since we only ever need one handler at a time */
	private ResetConfigEventHandler() 
	{
		this.configChangeListener = new ConfigChangeListener<>(Config.Client.ResetConfirmation.resetAllSettings, (resetSettings) -> { doStuff(resetSettings); });
		
	}
	
	private void doStuff(boolean resetSettings)
	{
		if (!resetSettings)
		{
			return;
		}
		
		
		Config.Client.ResetConfirmation.resetAllSettings.set(false);
	}
	
}
