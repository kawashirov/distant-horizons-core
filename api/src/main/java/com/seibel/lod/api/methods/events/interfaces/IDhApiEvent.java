package com.seibel.lod.api.methods.events.interfaces;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.events.ICoreDhApiEvent;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

/**
 * A combination of all interfaces required by all
 * DH Api events.
 *
 * @param <ApiInputType> This is the datatype that will be passed into the
 * 					  	 	event handler's method.
 * @param <CoreInputType> This is the datatype that will be passed in from Core
 * 							when the event is fired.
 *  					  
 * @author James Seibel
 * @version 2022-9-6
 */
public interface IDhApiEvent<ApiInputType, CoreInputType> extends ICoreDhApiEvent<CoreInputType>, IBindable
{
	/**
	 * Returns if the event should be automatically unbound
	 * after firing. <br>
	 * Can be useful for one time setup events or waiting for a specific game state. <br> <Br>
	 *
	 * Defaults to False (the event will not be removed after firing).
	 */
	@Override
	default boolean removeAfterFiring() { return false; };
	
}
