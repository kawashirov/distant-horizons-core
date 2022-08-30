package com.seibel.lod.core.api.implementation.interfaces.events;

/**
 * Contains any methods that can be implemented by
 * mod developers that wish to register events with the DH Api. <br> <br>
 *
 * All Api events should implement this.
 *
 * @param <InputType> This is the datatype that should be passed into the
 * 					  event handler's method.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public interface IDhApiEventExternal<InputType> extends IDhApiEventInternal<InputType>
{
	/**
	 * Returns if the event should be automatically unbound
	 * after firing. <br>
	 * Can be useful for one time setup events or waiting for a specific game state. <br> <Br>
	 *
	 * Defaults to False (the event will not be removed after firing).
	 */
	default boolean removeAfterFiring() { return false; };
	
}
