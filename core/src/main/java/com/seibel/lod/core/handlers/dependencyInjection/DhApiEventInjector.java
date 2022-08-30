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

package com.seibel.lod.core.handlers.dependencyInjection;

import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;
import com.seibel.lod.core.api.internal.a7.ClientApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * This class takes care of dependency injection for mods accessors. (for mod compatibility
 * support). <Br> <Br>
 *
 * If a IModAccessor returns null either that means the mod isn't loaded in the game
 * or an Accessor hasn't been implemented for the given Minecraft version.
 *
 * @author James Seibel
 * @author Leetom
 * @version 2022-8-15
 */
public class DhApiEventInjector extends DependencyInjector<IDhApiEvent> // Note to self: Don't try adding a generic type to IDhApiEvent, the consturctor won't accept it
{
	public static final DhApiEventInjector INSTANCE = new DhApiEventInjector();
	public static final Logger LOGGER = LogManager.getLogger(DhApiEventInjector.class.getSimpleName());
	
	
	public DhApiEventInjector()
	{
		super(IDhApiEvent.class, true);
	}
	
	
	/**
	 * Unlinks the given event handler, preventing the handler from being called in the future.
	 *
	 * @throws IllegalArgumentException if the implementation object doesn't implement the interface
	 * @return true if the handler was unbound, false if the handler wasn't bound.
	 */
	public boolean unbind(Class<? extends IDhApiEvent> dependencyInterface, Class<? extends IDhApiEvent> dependencyClassToRemove) throws IllegalArgumentException
	{
		// make sure the given dependency implements the necessary interfaces
		boolean implementsInterface = checkIfClassImplements(dependencyClassToRemove, dependencyInterface)
										|| checkIfClassExtends(dependencyClassToRemove, dependencyInterface);
		boolean implementsBindable = checkIfClassImplements(dependencyClassToRemove, this.bindableInterface);
		
		// display any errors
		if (!implementsInterface)
		{
			throw new IllegalArgumentException("The event handler [" + dependencyClassToRemove.getSimpleName() + "] doesn't implement or extend: [" + dependencyInterface.getSimpleName() + "].");
		}
		if (!implementsBindable)
		{
			throw new IllegalArgumentException("The event handler [" + dependencyClassToRemove.getSimpleName() + "] doesn't implement the interface: [" + IBindable.class.getSimpleName() + "].");
		}
		
		
		// actually remove the dependency
		if (this.dependencies.containsKey(dependencyInterface))
		{
			ArrayList<IDhApiEvent> dependencyList = this.dependencies.get(dependencyInterface);
			int indexToRemove = -1;
			for(int i = 0; i < dependencyList.size(); i++)
			{
				IBindable dependency = dependencyList.get(i);
				if (dependency.getClass().equals(dependencyClassToRemove))
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
	
	/**
	 * Fires all bound events of the given type (does nothing if no events are bound).
	 *
	 * @param dependencyInterface event type
	 * @param eventParameterObject event parameter
	 * @return if any of the events returned that this event should be canceled.
	 * @param <T> the parameter type taken by the event handlers.
	 */
	public <T, U extends IDhApiEvent<T>> boolean fireAllEvents(Class<U> dependencyInterface, T eventParameterObject)
	{
		boolean cancelEvent = false;
		
		ArrayList<U> eventList = this.getAll(dependencyInterface);
		for (IDhApiEvent<T> event : eventList)
		{
			if (event != null)
			{
				try
				{
					// fire each event and record if any of them
					// request to cancel the event.
					cancelEvent |= event.onEvent(eventParameterObject);
				}
				catch (Exception e)
				{
					// TODO log the failed event handler
					LOGGER.error("Exception thrown by event handler :" + e.getMessage(), e);
				}
			}
		}
		
		return cancelEvent;
	}
	
}
