package com.seibel.lod.core.config.eventHandlers;

import com.seibel.lod.api.enums.config.EHorizontalQuality;
import com.seibel.lod.api.enums.config.EHorizontalResolution;
import com.seibel.lod.api.enums.config.EVerticalQuality;
import com.seibel.lod.api.enums.config.quickOptions.EQualityPreset;
import com.seibel.lod.api.enums.rendering.ETransparency;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.config.ConfigEntryWithPresetOptions;
import com.seibel.lod.core.config.listeners.ConfigChangeListener;
import com.seibel.lod.core.config.listeners.IConfigListener;
import com.seibel.lod.coreapi.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Listens to the config and will automatically
 * clear the current render cache if certain settings are changed. 
 */
public class RenderQualityPresetConfigEventHandler implements IConfigListener
{
	public static final RenderQualityPresetConfigEventHandler INSTANCE = new RenderQualityPresetConfigEventHandler();
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	
	private final ConfigEntryWithPresetOptions<EQualityPreset, EHorizontalResolution> drawResolution = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.Graphics.Quality.drawResolution, 
		new HashMap<EQualityPreset, EHorizontalResolution>()
		{{
			this.put(EQualityPreset.MINIMUM, EHorizontalResolution.TWO_BLOCKS);
			this.put(EQualityPreset.LOW, EHorizontalResolution.BLOCK);
			this.put(EQualityPreset.MEDIUM, EHorizontalResolution.BLOCK);
			this.put(EQualityPreset.HIGH, EHorizontalResolution.BLOCK);
			this.put(EQualityPreset.EXTREME, EHorizontalResolution.BLOCK);
		}});
	private final ConfigEntryWithPresetOptions<EQualityPreset, EVerticalQuality> verticalQuality = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.Graphics.Quality.verticalQuality,
		new HashMap<EQualityPreset, EVerticalQuality>()
		{{
			this.put(EQualityPreset.MINIMUM, EVerticalQuality.HEIGHT_MAP);
			this.put(EQualityPreset.LOW, EVerticalQuality.LOW);
			this.put(EQualityPreset.MEDIUM, EVerticalQuality.MEDIUM);
			this.put(EQualityPreset.HIGH, EVerticalQuality.HIGH);
			this.put(EQualityPreset.EXTREME, EVerticalQuality.HIGH);
		}});
	
	
	private final ConfigEntryWithPresetOptions<EQualityPreset, EHorizontalQuality> horizontalQuality = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.Graphics.Quality.horizontalQuality,
		new HashMap<EQualityPreset, EHorizontalQuality>()
		{{
			this.put(EQualityPreset.MINIMUM, EHorizontalQuality.MEDIUM); // TODO Lowest and Low have a bug, making "chunk" the highest detail level
			this.put(EQualityPreset.LOW, EHorizontalQuality.MEDIUM);		//      I should look into why this is instead of just removing the option
			this.put(EQualityPreset.MEDIUM, EHorizontalQuality.MEDIUM);
			this.put(EQualityPreset.HIGH, EHorizontalQuality.HIGH);
			this.put(EQualityPreset.EXTREME, EHorizontalQuality.HIGH);
		}});
//	// TODO merge horizontal scale and quality (merge the numbers into quality)
//	private final ConfigEntryWithPresetOptions<EQualityPreset, Integer> horizontalScale = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.Graphics.Quality.horizontalScale,
//		new HashMap<EQualityPreset, Integer>()
//		{{
//			this.put(EQualityPreset.MINIMUM, 4);
//			this.put(EQualityPreset.LOW, 8);
//			this.put(EQualityPreset.MEDIUM, 12);
//			this.put(EQualityPreset.HIGH, 24);
//			this.put(EQualityPreset.EXTREME, 64);
//		}});
	
	
	private final ConfigEntryWithPresetOptions<EQualityPreset, ETransparency> transparency = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.Graphics.Quality.transparency, 
		new HashMap<EQualityPreset, ETransparency>()
		{{
			this.put(EQualityPreset.MINIMUM, ETransparency.DISABLED);
			this.put(EQualityPreset.LOW, ETransparency.FAKE);
			this.put(EQualityPreset.MEDIUM, ETransparency.COMPLETE);
			this.put(EQualityPreset.HIGH, ETransparency.COMPLETE);
			this.put(EQualityPreset.EXTREME, ETransparency.COMPLETE);
		}});
	
	
	private final ArrayList<ConfigEntryWithPresetOptions<EQualityPreset, ?>> configList = new ArrayList<>();
	
	/** True if a preset is currently being applied */
	private boolean changingPreset = false;
	
	
	
	/** private since we only ever need one handler at a time */
	private RenderQualityPresetConfigEventHandler() 
	{
		// add each config used by this preset
		this.configList.add(this.drawResolution);
		this.configList.add(this.verticalQuality);
		this.configList.add(this.horizontalQuality);
		this.configList.add(this.transparency);
		
		
		for (ConfigEntryWithPresetOptions<EQualityPreset, ?> config : this.configList)
		{
			// ignore try-using, the listener should only ever be added once and should never be removed
			new ConfigChangeListener<>(config.configEntry, (val) -> { this.onGraphicsConfigValueChanged(); });
		}
	}
	
	
	
	//===========//
	// listeners //
	//===========//
	
	@Override 
	public void onConfigValueSet()
	{
		EQualityPreset qualityPreset = Config.Client.qualityPresetSetting.get();
		if (qualityPreset == null)
		{
			// the value will be null when the config menu is first opened,
			// set the value to what it should be.

			EQualityPreset currentQualitySetting = this.getCurrentQualityPreset();
			Config.Client.qualityPresetSetting.set(currentQualitySetting);
			return;
		}
		
		// if the quick value is custom, nothing needs to be changed
		if (qualityPreset == EQualityPreset.CUSTOM)
		{
			return;
		}
		
		
		LOGGER.debug("changing quality preset to: " + qualityPreset);
		this.changingPreset = true;
		
		for (ConfigEntryWithPresetOptions<EQualityPreset, ?> configEntry : this.configList)
		{
			configEntry.updateConfigEntry(qualityPreset);
		}
		
		this.changingPreset = false;
		LOGGER.debug("quality preset active: "+qualityPreset);
		
	}
	
	@Override
	public void onUiModify() { /* do nothing, we only care about modified config values */ }
	
	/**
	 * listen for changed graphics settings and set the
	 * quick quality to "custom" if anything was changed
	 */
	public void onGraphicsConfigValueChanged()
	{
		if (this.changingPreset)
		{
			// if a preset is currently being applied, ignore all changes
			return;
		}
		
		
		EQualityPreset newPreset = this.getCurrentQualityPreset();
		EQualityPreset currentPreset = Config.Client.qualityPresetSetting.get();
		
		if (newPreset != currentPreset)
		{
			Config.Client.qualityPresetSetting.set(EQualityPreset.CUSTOM);
		}
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	/** @return what {@link EQualityPreset} is currently viable based on the {@link RenderQualityPresetConfigEventHandler#configList}. */
	public EQualityPreset getCurrentQualityPreset()
	{
		// get all quick options
		HashSet<EQualityPreset> possiblePresetSet = new HashSet<>(Arrays.asList(EQualityPreset.values()));
		
		
		// remove any quick options that aren't possible with the currently selected options
		for (ConfigEntryWithPresetOptions<EQualityPreset, ?> configEntry : this.configList)
		{
			HashSet<EQualityPreset> optionPresetSet = configEntry.getPossibleQualitiesFromCurrentOptionValue();
			possiblePresetSet.retainAll(optionPresetSet);
		}
		
		
		
		ArrayList<EQualityPreset> possiblePrestList = new ArrayList<>(possiblePresetSet);
		if (possiblePrestList.size() > 1)
		{
			// we shouldn't have multiple options, but just in case
			LOGGER.warn("Multiple potential graphics options ["+StringUtil.join(", ", possiblePrestList)+"], defaulting to the first one.");
		}
		
		if (possiblePrestList.size() == 0)
		{
			// if no options are valid, return "CUSTOM"
			possiblePrestList.add(EQualityPreset.CUSTOM);
		}
		
		return possiblePrestList.get(0);
	}
	
	
}
