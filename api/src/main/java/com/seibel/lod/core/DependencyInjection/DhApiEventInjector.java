/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.DependencyInjection;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.core.events.ApiEventDefinitionHandler;
import com.seibel.lod.core.interfaces.dependencyInjection.IBindable;
import com.seibel.lod.core.interfaces.dependencyInjection.IDhApiEventInjector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class takes care of dependency injection for API events.
 * 
 * @author James Seibel
 * @version 2022-11-20
 */
public class DhApiEventInjector extends DependencyInjector<IDhApiEvent> implements IDhApiEventInjector // Note to self: Don't try adding a generic type to IDhApiEvent, the consturctor won't accept it
{
	private static final Logger LOGGER = LogManager.getLogger(DhApiEventInjector.class.getSimpleName());
	private static final HashMap<Class<? extends IDhApiEvent>, Object> FIRED_ONE_TIME_EVENT_PARAMETERS_BY_EVENT_INTERFACE = new HashMap<>();
	
	public static final DhApiEventInjector INSTANCE = new DhApiEventInjector();
	
	
	
	private DhApiEventInjector() { super(IDhApiEvent.class, true); }
	
	
	
	@Override
	public void bind(Class<? extends IDhApiEvent> eventInterface, IDhApiEvent eventImplementation) throws IllegalStateException, IllegalArgumentException
	{
		// is this a one time event?
		if (ApiEventDefinitionHandler.getEventDefinition(eventInterface).isOneTimeEvent)
		{
			// has this one time event been fired yet?
			if (FIRED_ONE_TIME_EVENT_PARAMETERS_BY_EVENT_INTERFACE.containsKey(eventInterface))
			{
				// the one time event has happened, fire the handler
				
				// this has to be an unsafe cast since the hash map can't hold the generic objects
				Object parameter = FIRED_ONE_TIME_EVENT_PARAMETERS_BY_EVENT_INTERFACE.get(eventInterface);
				eventImplementation.fireEvent(parameter);
			}
		}
		
		// bind the event handler
		super.bind(eventInterface, eventImplementation);
	}
	
	@Override
	public boolean unbind(Class<? extends IDhApiEvent> eventInterface, Class<? extends IDhApiEvent> eventClassToRemove) throws IllegalArgumentException
	{
		// make sure the given dependency implements the necessary interfaces
		boolean implementsInterface = this.checkIfClassImplements(eventClassToRemove, eventInterface) ||
									  this.checkIfClassExtends(eventClassToRemove, eventInterface);
		boolean implementsBindable = this.checkIfClassImplements(eventClassToRemove, this.bindableInterface);
		
		// display any errors
		if (!implementsInterface)
		{
			throw new IllegalArgumentException("The event handler [" + eventClassToRemove.getSimpleName() + "] doesn't implement or extend: [" + eventInterface.getSimpleName() + "].");
		}
		if (!implementsBindable)
		{
			throw new IllegalArgumentException("The event handler [" + eventClassToRemove.getSimpleName() + "] doesn't implement the interface: [" + IBindable.class.getSimpleName() + "].");
		}
		
		
		// actually remove the dependency
		if (this.dependencies.containsKey(eventInterface))
		{
			ArrayList<IDhApiEvent> dependencyList = this.dependencies.get(eventInterface);
			int indexToRemove = -1;
			for(int i = 0; i < dependencyList.size(); i++)
			{
				IBindable dependency = dependencyList.get(i);
				if (dependency.getClass().equals(eventClassToRemove))
				{
					indexToRemove = i;
					break;
				}
			}
			
			if (indexToRemove != -1)
			{
				return dependencyList.remove(indexToRemove) != null;
			}
		}
		
		// no item was removed
		return false;
	}
	
	@Override
	public <T, U extends IDhApiEvent<T>> boolean fireAllEvents(Class<U> eventInterface, T eventParameterObject)
	{
		boolean cancelEvent = false;
		
		// if this is a one time event, record it
		if (ApiEventDefinitionHandler.getEventDefinition(eventInterface).isOneTimeEvent && 
			!FIRED_ONE_TIME_EVENT_PARAMETERS_BY_EVENT_INTERFACE.containsKey(eventInterface))
		{
			FIRED_ONE_TIME_EVENT_PARAMETERS_BY_EVENT_INTERFACE.put(eventInterface, eventParameterObject);
		}
		
		
		// fire each bound event
		ArrayList<U> eventList = this.getAll(eventInterface);
		for (IDhApiEvent<T> event : eventList)
		{
			if (event != null)
			{
				try
				{
					// fire each event and record if any of them
					// request to cancel the event.
					cancelEvent |= event.fireEvent(eventParameterObject);
				}
				catch (Exception e)
				{
					LOGGER.error("Exception thrown by event handler [" + event.getClass().getSimpleName() + "] for event type [" + eventInterface.getSimpleName() + "], error:" + e.getMessage(), e);
				}
			}
		}
		
		return cancelEvent;
	}
	
}
