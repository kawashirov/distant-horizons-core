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
import com.seibel.lod.coreapi.interfaces.config.IConfigEntry;
import com.seibel.lod.coreapi.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RenderQualityPresetConfigEventHandler extends AbstractPresetConfigEventHandler<EQualityPreset>
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
			this.put(EQualityPreset.EXTREME, EVerticalQuality.EXTREME);
		}});
	private final ConfigEntryWithPresetOptions<EQualityPreset, EHorizontalQuality> horizontalQuality = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.Graphics.Quality.horizontalQuality,
		new HashMap<EQualityPreset, EHorizontalQuality>()
		{{
			this.put(EQualityPreset.MINIMUM, EHorizontalQuality.LOWEST);
			this.put(EQualityPreset.LOW, EHorizontalQuality.LOW);
			this.put(EQualityPreset.MEDIUM, EHorizontalQuality.MEDIUM);
			this.put(EQualityPreset.HIGH, EHorizontalQuality.HIGH);
			this.put(EQualityPreset.EXTREME, EHorizontalQuality.EXTREME);
		}});
	private final ConfigEntryWithPresetOptions<EQualityPreset, ETransparency> transparency = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.Graphics.Quality.transparency, 
		new HashMap<EQualityPreset, ETransparency>()
		{{
			this.put(EQualityPreset.MINIMUM, ETransparency.DISABLED);
			this.put(EQualityPreset.LOW, ETransparency.FAKE);
			this.put(EQualityPreset.MEDIUM, ETransparency.COMPLETE);
			this.put(EQualityPreset.HIGH, ETransparency.COMPLETE);
			this.put(EQualityPreset.EXTREME, ETransparency.COMPLETE);
		}});
	
	
	
	//==============//
	// constructors //
	//==============//
	
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
			new ConfigChangeListener<>(config.configEntry, (val) -> { this.onConfigValueChanged(); });
		}
	}
	
	
	
	//==============//
	// enum getters //
	//==============//
	
	@Override
	protected IConfigEntry<EQualityPreset> getPresetConfigEntry() { return Config.Client.qualityPresetSetting; }
	
	@Override 
	protected List<EQualityPreset> getPresetEnumList() { return Arrays.asList(EQualityPreset.values()); }
	@Override 
	protected EQualityPreset getCustomPresetEnum() { return EQualityPreset.CUSTOM; }
	
}
