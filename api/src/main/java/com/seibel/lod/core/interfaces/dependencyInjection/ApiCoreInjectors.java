package com.seibel.lod.core.interfaces.dependencyInjection;

import com.seibel.lod.core.DependencyInjection.DhApiEventInjector;

/**
 * This singleton holds the dependency injectors used
 * between Core and the API.
 * IE: this is how Core and the API talk to each other.
 * 
 * @author James Seibel
 * @version 2022-9-13
 */
public class ApiCoreInjectors
{
	private static ApiCoreInjectors INSTANCE;
	
	
	public final IDhApiEventInjector eventInjector = new DhApiEventInjector();
	
	
	
	public static ApiCoreInjectors getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new ApiCoreInjectors();
		}
		return INSTANCE;
	}
	
	
}
