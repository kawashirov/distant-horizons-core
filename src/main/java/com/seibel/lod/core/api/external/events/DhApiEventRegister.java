package com.seibel.lod.core.api.external.events;

import com.seibel.lod.core.api.external.events.interfaces.IDhApiEvent;
import com.seibel.lod.core.api.external.shared.objects.DhApiResult;

/**
 * Handles adding/removing event handlers.
 *
 * @author James Seibel
 * @version 2022-7-13
 */
public class DhApiEventRegister
{
	/**
	 * Registers the given event handler. <Br>
	 * Only one eventHandler of a specific class can be added at a time.
	 * If multiple of the same eventHandler are added DhApiResult will return
	 * the name of the already added handler and success = false.
	 */
	public static DhApiResult on(IDhApiEvent eventHandler)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Removes the given event handler for this event if one has been registered. <br>
	 * If no eventHandler of the given class has been registered the result will return
	 * success = false.
	 */
	public static DhApiResult off(IDhApiEvent eventHandler)
	{
		throw new UnsupportedOperationException();
	}
	
	
}
