package com.seibel.lod.core.api.external.methods.events;

import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;
import com.seibel.lod.core.api.external.items.objects.DhApiResult;
import com.seibel.lod.core.handlers.dependencyInjection.DhApiEventHandler;

/**
 * Handles adding/removing event handlers.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public class DhApiEventRegister
{
	/**
	 * Registers the given event handler. <Br>
	 * Only one eventHandler of a specific class can be registered at a time.
	 * If multiple of the same eventHandler are added DhApiResult will return
	 * the name of the already added handler and success = false.
	 */
	public static DhApiResult on(Class<? extends IDhApiEvent> eventInterface, IDhApiEvent eventHandlerImplementation)
	{
		try
		{
			DhApiEventHandler.INSTANCE.bind(eventInterface, eventHandlerImplementation);
			return new DhApiResult(true, "");
		}
		catch (IllegalStateException e)
		{
			return new DhApiResult(false, e.getMessage());
		}
	}
	
	/**
	 * Unregisters the given event handler for this event if one has been registered. <br>
	 * If no eventHandler of the given class has been registered the result will return
	 * success = false.
	 */
	public static DhApiResult off(Class<? extends IDhApiEvent> eventInterface, Class<IDhApiEvent> eventHandlerClass)
	{
		if (DhApiEventHandler.INSTANCE.unbind(eventInterface, eventHandlerClass))
		{
			return new DhApiResult(true, "");
		}
		else
		{
			return new DhApiResult(false, "No event handler [" + eventHandlerClass.getSimpleName() + "] was bound for the event [" + eventInterface.getSimpleName() + "].");
		}
	}
	
}
