package com.seibel.lod.api.methods.events;

import com.seibel.lod.api.objects.DhApiResult;
import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.core.DependencyInjection.DhApiEventInjector;

/**
 * Handles adding/removing event handlers.
 *
 * @author James Seibel
 * @version 2022-9-16
 */
public class DhApiEventRegister
{
	/**
	 * Registers the given event handler. <Br>
	 * Only one eventHandler of a specific class can be registered at a time.
	 * If multiple of the same eventHandler are added DhApiResult will return
	 * the name of the already added handler and success = false.
	 */
	public static DhApiResult<Void> on(Class<? extends IDhApiEvent> eventInterface, IDhApiEvent eventHandlerImplementation)
	{
		try
		{
			DhApiEventInjector.INSTANCE.bind(eventInterface, eventHandlerImplementation);
			return DhApiResult.createSuccess();
		}
		catch (IllegalStateException e)
		{
			return DhApiResult.createFail(e.getMessage());
		}
	}

	/**
	 * Unregisters the given event handler for this event if one has been registered. <br>
	 * If no eventHandler of the given class has been registered the result will return
	 * success = false.
	 */
	public static DhApiResult<Void> off(Class<? extends IDhApiEvent> eventInterface, Class<IDhApiEvent> eventHandlerClass)
	{
		if (DhApiEventInjector.INSTANCE.unbind(eventInterface, eventHandlerClass))
		{
			return DhApiResult.createSuccess();
		}
		else
		{
			return DhApiResult.createFail("No event handler [" + eventHandlerClass.getSimpleName() + "] was bound for the event [" + eventInterface.getSimpleName() + "].");
		}
	}
	
}
