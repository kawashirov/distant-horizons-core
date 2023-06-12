package com.seibel.lod.core.config.eventHandlers.presets;

import com.seibel.lod.api.enums.config.quickOptions.EThreadPreset;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.config.ConfigEntryWithPresetOptions;
import com.seibel.lod.core.config.listeners.ConfigChangeListener;
import com.seibel.lod.coreapi.interfaces.config.IConfigEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ThreadPresetConfigEventHandler extends AbstractPresetConfigEventHandler<EThreadPreset>
{
	public static final ThreadPresetConfigEventHandler INSTANCE = new ThreadPresetConfigEventHandler();
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	
	private final ConfigEntryWithPresetOptions<EThreadPreset, Integer> worldGen = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.MultiThreading.numberOfWorldGenerationThreads, 
		new HashMap<EThreadPreset, Integer>()
		{{
			this.put(EThreadPreset.MINIMAL_IMPACT, 1);
			this.put(EThreadPreset.LOW_IMPACT, getThreadCountByPercent(0.1));
			this.put(EThreadPreset.BALANCED, getThreadCountByPercent(0.2));
			this.put(EThreadPreset.AGGRESSIVE, getThreadCountByPercent(0.4));
			this.put(EThreadPreset.I_PAID_FOR_THE_WHOLE_CPU, getThreadCountByPercent(1.0));
		}});
	private final ConfigEntryWithPresetOptions<EThreadPreset, Integer> bufferBuilders = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.MultiThreading.numberOfBufferBuilderThreads, 
		new HashMap<EThreadPreset, Integer>()
		{{
			this.put(EThreadPreset.MINIMAL_IMPACT, 1);
			this.put(EThreadPreset.LOW_IMPACT, getThreadCountByPercent(0.1));
			this.put(EThreadPreset.BALANCED, getThreadCountByPercent(0.2));
			this.put(EThreadPreset.AGGRESSIVE, getThreadCountByPercent(0.4));
			this.put(EThreadPreset.I_PAID_FOR_THE_WHOLE_CPU, getThreadCountByPercent(1.0));
		}});
	private final ConfigEntryWithPresetOptions<EThreadPreset, Integer> fileHandlers = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.MultiThreading.numberOfFileHandlerThreads, 
		new HashMap<EThreadPreset, Integer>()
		{{
			this.put(EThreadPreset.MINIMAL_IMPACT, 1);
			this.put(EThreadPreset.LOW_IMPACT, getThreadCountByPercent(0.1));
			this.put(EThreadPreset.BALANCED, getThreadCountByPercent(0.2));
			this.put(EThreadPreset.AGGRESSIVE, getThreadCountByPercent(0.2));
			this.put(EThreadPreset.I_PAID_FOR_THE_WHOLE_CPU, getThreadCountByPercent(1.0));
		}});
	private final ConfigEntryWithPresetOptions<EThreadPreset, Integer> dataConverters = new ConfigEntryWithPresetOptions<>(Config.Client.Advanced.MultiThreading.numberOfDataConverterThreads, 
		new HashMap<EThreadPreset, Integer>()
		{{
			this.put(EThreadPreset.MINIMAL_IMPACT, 1);
			this.put(EThreadPreset.LOW_IMPACT, getThreadCountByPercent(0.1));
			this.put(EThreadPreset.BALANCED, getThreadCountByPercent(0.2));
			this.put(EThreadPreset.AGGRESSIVE, getThreadCountByPercent(0.2));
			this.put(EThreadPreset.I_PAID_FOR_THE_WHOLE_CPU, getThreadCountByPercent(1.0));
		}});
	
	
	
	//==============//
	// constructors //
	//==============//
	
	/** private since we only ever need one handler at a time */
	private ThreadPresetConfigEventHandler() 
	{
		// add each config used by this preset
		this.configList.add(this.worldGen);
		this.configList.add(this.bufferBuilders);
		this.configList.add(this.fileHandlers);
		this.configList.add(this.dataConverters);
		
		
		for (ConfigEntryWithPresetOptions<EThreadPreset, ?> config : this.configList)
		{
			// ignore try-using, the listener should only ever be added once and should never be removed
			new ConfigChangeListener<>(config.configEntry, (val) -> { this.onConfigValueChanged(); });
		}
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	/**
	 * Pre-computed values for your convenience: <br>
	 * Format: percent: 4coreCpu-8coreCpu-16coreCpu <br><br>
	 * <code>
	 * 0.1: 1-1-2	<br>
	 * 0.2: 1-2-4	<br>
	 * 0.4: 2-4-7	<br>
	 * 0.6: 3-5-10	<br>
	 * 0.8: 4-7-13	<br>
	 * 1.0: 4-8-16	<br>
	 * </code>
	 */
	private static int getThreadCountByPercent(double percent) throws IllegalArgumentException
	{
		if (percent <= 0 || percent > 1)
		{
			throw new IllegalArgumentException("percent must be greater than 0 and less than or equal to 1.");
		}
		
		// this is logical processor count, not physical CPU cores
		int totalProcessorCount = Runtime.getRuntime().availableProcessors();
		int coreCount = (int) Math.ceil(totalProcessorCount * percent);
		return Math.max(1, coreCount);
	}
	
	
	
	//==============//
	// enum getters //
	//==============//
	
	@Override
	protected IConfigEntry<EThreadPreset> getPresetConfigEntry() { return Config.Client.threadPresetSetting; }
	
	@Override 
	protected List<EThreadPreset> getPresetEnumList() { return Arrays.asList(EThreadPreset.values()); }
	@Override 
	protected EThreadPreset getCustomPresetEnum() { return EThreadPreset.CUSTOM; }
	
}
