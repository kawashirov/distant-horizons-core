package com.seibel.lod.core.api.external.coreImplementations.objects.converters;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.config.IConverter;

/**
 * This assumes the two enums contain the same values.
 *
 * @author James Seibel
 * @version 2022-6-30
 */
public class GenericEnumConverter<coreEnum extends Enum<coreEnum>, apiEnum extends Enum<apiEnum>> implements IConverter<coreEnum, apiEnum>
{
	private final Class<coreEnum> apiCoreType;
	private final Class<apiEnum> apiEnumClass;
	
	
	public GenericEnumConverter(Class<coreEnum> newCoreEnumClass, Class<apiEnum> newApiEnumClass)
	{
		this.apiCoreType = newCoreEnumClass;
		this.apiEnumClass = newApiEnumClass;
	}
	
	
	@Override
	public coreEnum convertToCoreType(apiEnum apiObject)
	{
		return parseEnum(apiObject.name(), this.apiCoreType);
	}
	
	@Override
	public apiEnum convertToApiType(coreEnum coreObject)
	{
		return parseEnum(coreObject.name(), this.apiEnumClass);
	}
	
	
	/**
	 * Since this does require string conversions it isn't the fastest option,
	 * however it works and should be fast enough for most use cases. <br>
	 * If speed becomes an issue this can always be replaced with either individual
	 * converters for each enum (using a switch statement) or a more exotic solution. <br> <br>
	 *
	 * Original source: https://stackoverflow.com/questions/25487619/java-generic-function-to-parse-enums-from-strings
	 */
	private static <E extends Enum<E>> E parseEnum(String str, Class<E> enumClass)
	{
		return Enum.valueOf(enumClass, str);
	}
	
}
