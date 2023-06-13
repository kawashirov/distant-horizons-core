package com.seibel.lod.core.config.eventHandlers.presets;

import com.seibel.lod.core.config.ConfigEntryWithPresetOptions;
import com.seibel.lod.core.config.listeners.IConfigListener;
import com.seibel.lod.coreapi.interfaces.config.IConfigEntry;
import com.seibel.lod.coreapi.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class AbstractPresetConfigEventHandler<TPresetEnum extends Enum<?>> implements IConfigListener
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	
	protected final ArrayList<ConfigEntryWithPresetOptions<TPresetEnum, ?>> configList = new ArrayList<>();
	
	protected boolean changingPreset = false;
	
	
	
	/** 
	 * Set the UI only config based on what is set in the file. <br> 
	 * This should only be called once. 
	 */
	public void setUiOnlyConfigValues()
	{
		TPresetEnum currentQualitySetting = this.getCurrentQualityPreset();
		this.getPresetConfigEntry().set(currentQualitySetting);
	}
	
	
	//===========//
	// listeners //
	//===========//
	
	@Override 
	public void onConfigValueSet()
	{
		TPresetEnum qualityPreset = this.getPresetConfigEntry().get();
		
		// if the quick value is custom, nothing needs to be changed
		if (qualityPreset == this.getCustomPresetEnum())
		{
			return;
		}
		
		
		LOGGER.debug("changing preset to: " + qualityPreset);
		this.changingPreset = true;
		
		for (ConfigEntryWithPresetOptions<TPresetEnum, ?> configEntry : this.configList)
		{
			configEntry.updateConfigEntry(qualityPreset);
		}
		
		this.changingPreset = false;
		LOGGER.debug("preset active: "+qualityPreset);
		
	}
	
	@Override
	public void onUiModify() { /* do nothing, we only care about modified config values */ }
	
	/**
	 * listen for changed graphics settings and set the
	 * quick quality to "custom" if anything was changed
	 */
	public void onConfigValueChanged()
	{
		if (this.changingPreset)
		{
			// if a preset is currently being applied, ignore all changes
			return;
		}
		
		
		TPresetEnum newPreset = this.getCurrentQualityPreset();
		TPresetEnum currentPreset = this.getPresetConfigEntry().get();
		
		if (newPreset != currentPreset)
		{
			this.getPresetConfigEntry().set(this.getCustomPresetEnum());
		}
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	/** @return what {@link TPresetEnum} is currently viable based on the {@link AbstractPresetConfigEventHandler#configList}. */
	public TPresetEnum getCurrentQualityPreset()
	{
		// get all quick options
		HashSet<TPresetEnum> possiblePresetSet = new HashSet<>(this.getPresetEnumList());
		
		
		// remove any quick options that aren't possible with the currently selected options
		for (ConfigEntryWithPresetOptions<TPresetEnum, ?> configEntry : this.configList)
		{
			HashSet<TPresetEnum> optionPresetSet = configEntry.getPossibleQualitiesFromCurrentOptionValue();
			possiblePresetSet.retainAll(optionPresetSet);
		}
		
		
		
		ArrayList<TPresetEnum> possiblePrestList = new ArrayList<>(possiblePresetSet);
		if (possiblePrestList.size() > 1)
		{
			// we shouldn't have multiple options, but just in case
			LOGGER.warn("Multiple potential preset options ["+StringUtil.join(", ", possiblePrestList)+"], defaulting to the first one.");
		}
		
		if (possiblePrestList.size() == 0)
		{
			// if no options are valid, return "CUSTOM"
			possiblePrestList.add(this.getCustomPresetEnum());
		}
		
		return possiblePrestList.get(0);
	}
	
	
	
	//==================//
	// abstract methods //
	//==================//
	
	protected abstract IConfigEntry<TPresetEnum> getPresetConfigEntry();
	
	protected abstract List<TPresetEnum> getPresetEnumList();
	protected abstract TPresetEnum getCustomPresetEnum();
	
	
}
