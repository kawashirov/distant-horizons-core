package com.seibel.lod.api.methods.config;

import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.core.interfaces.config.IConfigEntry;
import com.seibel.lod.core.interfaces.config.IConverter;
import com.seibel.lod.core.interfaces.config.converters.DefaultConverter;

/**
 * A wrapper used to interface with Distant Horizon's Config.
 *
 * When using this object you need to explicitly define the generic types,
 * otherwise Intellij won't do any type checking and the wrong types can be used. <br>
 * For example a method returning IDhApiConfig<Integer> when the config should be a Boolean.
 *
 * @param <apiType>
 * @author James Seibel
 * @version 2022-6-30
 */
public class DhApiConfig<coreType, apiType> implements IDhApiConfig<apiType>
{
	private final IConfigEntry<coreType> configEntry;
	
	private final IConverter<coreType, apiType> configConverter;
	
	
	/**
	 * This constructor should only be called internally. <br>
	 * There is no reason for API users to create this object. <br><br>
	 *
	 * Uses the default object converter, this requires coreType and apiType to be the same.
	 */
	@SuppressWarnings("unchecked") // DefaultConverter's cast is safe
	public DhApiConfig(IConfigEntry<coreType> newConfigEntry)
	{
		this.configEntry = newConfigEntry;
		this.configConverter = (IConverter<coreType, apiType>) new DefaultConverter<coreType>();
	}
	
	/**
	 * This constructor should only be called internally. <br>
	 * There is no reason for API users to create this object. <br><br>
	 */
	public DhApiConfig(IConfigEntry<coreType> newConfigEntry, IConverter<coreType, apiType> newConverter)
	{
		this.configEntry = newConfigEntry;
		this.configConverter = newConverter;
	}
	
	
	public apiType getValue() { return this.configConverter.convertToApiType(this.configEntry.get()); }
	public apiType getTrueValue() { return this.configConverter.convertToApiType(this.configEntry.getTrueValue()); }
	public apiType getApiValue() { return this.configConverter.convertToApiType(this.configEntry.getApiValue()); }
	
	public boolean setValue(apiType newValue)
	{
		if (this.configEntry.getAllowApiOverride())
		{
			this.configEntry.setApiValue(this.configConverter.convertToCoreType(newValue));
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean getCanBeOverrodeByApi() { return this.configEntry.getAllowApiOverride(); }
	
	public apiType getDefaultValue() { return this.configConverter.convertToApiType(configEntry.getDefaultValue()); }
	public apiType getMaxValue() { return this.configConverter.convertToApiType(this.configEntry.getMax()); }
	public apiType getMinValue() { return this.configConverter.convertToApiType(this.configEntry.getMin()); }
	
}
