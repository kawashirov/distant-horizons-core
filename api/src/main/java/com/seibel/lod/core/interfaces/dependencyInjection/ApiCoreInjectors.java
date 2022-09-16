package com.seibel.lod.core.interfaces.dependencyInjection;

import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.core.DependencyInjection.DhApiEventInjector;

/**
 * This singleton holds the dependency injectors used
 * between Core and the API.
 * IE: this is how Core and the API talk to each other.
 * 
 * @author James Seibel
 * @version 2022-9-15
 */
public class ApiCoreInjectors
{
	private static ApiCoreInjectors INSTANCE;
	
	
	
	public final IDhApiEventInjector events = new DhApiEventInjector();
	
	/** 
	 * <strong>WARNING:</strong> will be null until after DH initializes for the first time. <br><br>
	 * 
	 * Use a {@link com.seibel.lod.api.methods.events.abstractEvents.DhApiAfterDhInitEvent DhApiAfterDhInitEvent}
	 * along with the {@link ApiCoreInjectors#events ApiCoreInjectors.events} to be notified when this can
	 * be safely used.
	 */
	public IDhApiConfig configs;
	
	
	public static ApiCoreInjectors getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new ApiCoreInjectors();
		}
		return INSTANCE;
	}
	
	
}
