package com.seibel.lod.core.api.implementation.interfaces.events;

/**
 * Contains methods that should only be used internally
 * by Distant Horizons and should all be locked with
 * the "final" keyword. <br>
 * (IE: whether an event is cancelable or not should only be defined by the DH API) <br> <br>
 *
 * All Api events should implement this.
 *
 * @param <InputType> This is the datatype that should be passed into the
 * 					  event handler's method.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public interface IDhApiEventInternal<InputType>
{
	/** Returns true if the event can be canceled. */
	boolean getCancelable();
	
	/**
	 * Called internally by Distant Horizons when the event happens.
	 * This method shouldn't directly be overridden and instead
	 * should point to the more specific event method.
	 *
	 * @param input the parameter object passed in from the event source. Can be null.
	 * @return whether the event should be canceled or not.
	 * 		   A canceled event will still fire the other event handlers that are queued.
	 */
	boolean onEvent(InputType input);
	
}
