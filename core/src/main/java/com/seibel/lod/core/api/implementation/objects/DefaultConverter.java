package com.seibel.lod.core.api.implementation.objects;

import com.seibel.lod.core.api.implementation.interfaces.config.IConverter;


/**
 * Returns the object passed in, doesn't do any conversion. <br>
 * Helpful as the default converter in some cases.
 *
 * @author James Seibel
 * @version 2022-6-30
 */
public class DefaultConverter<T> implements IConverter<T, T>
{
	@Override
	public T convertToCoreType(T apiObject)
	{
		return apiObject;
	}
	
	@Override
	public T convertToApiType(T coreObject)
	{
		return coreObject;
	}
}