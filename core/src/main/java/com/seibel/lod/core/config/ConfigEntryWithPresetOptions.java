package com.seibel.lod.core.config;

import com.seibel.lod.core.config.types.ConfigEntry;

import java.util.HashMap;
import java.util.HashSet;

public class ConfigEntryWithPresetOptions<TQuickEnum, TConfig>
{
	public final ConfigEntry<TConfig> configEntry;
	
	private final HashMap<TQuickEnum, TConfig> configOptionByQualityOption;
	
	
	
	public ConfigEntryWithPresetOptions(ConfigEntry<TConfig> configEntry, HashMap<TQuickEnum, TConfig> configOptionByQualityOption)
	{
		this.configEntry = configEntry;
		this.configOptionByQualityOption = configOptionByQualityOption;
	}
	
	
	
	public void updateConfigEntry(TQuickEnum quickQuality)
	{
		TConfig newValue = this.configOptionByQualityOption.get(quickQuality);
		this.configEntry.set(newValue);
	}
	
	public HashSet<TQuickEnum> getPossibleQualitiesFromCurrentOptionValue()
	{
		TConfig inputOptionValue = this.configEntry.get();
		HashSet<TQuickEnum> possibleQualities = new HashSet<>();
		
		for (TQuickEnum key : this.configOptionByQualityOption.keySet())
		{
			TConfig optionValue = this.configOptionByQualityOption.get(key);
			if (optionValue.equals(inputOptionValue))
			{
				possibleQualities.add(key);
			}
		}
		
		return possibleQualities;
	}
	
}
