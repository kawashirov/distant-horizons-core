package com.seibel.lod.api.methods.events.interfaces;

import com.seibel.lod.core.interfaces.dependencyInjection.IBindable;

/**
 * A combination of all interfaces required by all
 * DH Api events.
 *
 * @param <T> This is the datatype that will be passed into the event handler's method.
 *  					  
 * @author James Seibel
 * @version 2022-9-6
 */
public interface IDhApiEvent<T> extends IBindable
{
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
	default boolean removeAfterFiring() { return false; };
	
	
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
	boolean fireEvent(T input);
	
}
