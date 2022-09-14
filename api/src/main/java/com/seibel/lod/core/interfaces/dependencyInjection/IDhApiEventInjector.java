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

package com.seibel.lod.core.interfaces.dependencyInjection;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;

/**
 * This class takes care of dependency injection for API events.
 *
 * @author James Seibel
 * @version 2022-9-13
 */
public interface IDhApiEventInjector extends IDependencyInjector<IDhApiEvent> // Note to self: Don't try adding a generic type to IDhApiEvent, the consturctor won't accept it
{
	
	/**
	 * Unlinks the given event handler, preventing the handler from being called in the future.
	 *
	 * @throws IllegalArgumentException if the implementation object doesn't implement the interface
	 * @return true if the handler was unbound, false if the handler wasn't bound.
	 */
	public boolean unbind(Class<? extends IDhApiEvent> dependencyInterface, Class<? extends IDhApiEvent> dependencyClassToRemove) throws IllegalArgumentException;
	
	/**
	 * Fires all bound events of the given type (does nothing if no events are bound).
	 *
	 * @param dependencyInterface event type
	 * @param eventParameterObject event parameter
	 * @return if any of the events returned that this event should be canceled.
	 * @param <T> the parameter type taken by the event handlers.
	 */
	public <T, U extends IDhApiEvent<T>> boolean fireAllEvents(Class<U> dependencyInterface, T eventParameterObject);
	
}
