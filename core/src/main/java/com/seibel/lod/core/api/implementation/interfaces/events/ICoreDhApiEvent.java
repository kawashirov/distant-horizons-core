package com.seibel.lod.core.api.implementation.interfaces.events;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

/**
 * A combination of all interfaces required by all
 * DH Api events.
 * 
 * @param <CoreInputType> This is the datatype that will be passed in from Core
 * 							when the event is fired.
 *
 * @author James Seibel
 * @version 2022-9-6
 */
public interface ICoreDhApiEvent<CoreInputType> extends IBindable
{
	//==========//
	// internal //
	//==========//
	
	/** Returns true if the event can be canceled. */
	boolean getCancelable();
	
	/**
	 * Called internally by Distant Horizons when the event happens.
	 * This method shouldn't directly be overridden and
	 * should call a more specific method instead.
	 *
	 * @param input the parameter object passed in from the event source. Can be null.
	 * @return whether the event should be canceled or not.
	 * 		   A canceled event will still fire the other event handlers that are queued.
	 */
	boolean fireEvent(CoreInputType input);
	
	
	
	//==========//
	// external //
	//==========//
	
	/**
	 * Returns if the event should be automatically unbound
	 * after firing. <br>
	 * Can be useful for one time setup events or waiting for a specific game state. <br> <Br>
	 *
	 * Defaults to False (the event will not be removed after firing).
	 */
	boolean removeAfterFiring();
	
}
