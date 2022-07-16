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

import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * This class takes care of tracking objects used in dependency injection.
 *
 * @param <BindableType> extends IBindable and defines what interfaces this dependency handler can deal with.
 * @author James Seibel
 * @version 2022-7-15
 */
public class DependencyHandler<BindableType extends IBindable>
{
	protected final Logger logger;
	
	protected final Map<Class<? extends BindableType>, Object> dependencies = new HashMap<Class<? extends BindableType>, Object>();
	
	/** Internal class reference to BindableType since we can't get it any other way. */
	protected final Class<? extends BindableType> bindableInterface;
	
	
	
	public DependencyHandler(Class<BindableType> newBindableInterface, Logger newLogger)
	{
		this.bindableInterface = newBindableInterface;
		this.logger = newLogger;
	}
	
	

	/**
	 * Links the given implementation object to an interface, so it can be referenced later.
	 * 
	 * @param dependencyInterface The interface the implementation object should implement.
	 * @param dependencyImplementation An object that implements the dependencyInterface interface.
	 * @throws IllegalStateException if the implementation object doesn't implement 
	 *                               the interface or the interface has already been bound.
	 */
	public void bind(Class<? extends BindableType> dependencyInterface, Object dependencyImplementation) throws IllegalStateException
	{
		// make sure we haven't already bound this dependency
		if (dependencies.containsKey(dependencyInterface))
		{
			throw new IllegalStateException("The dependency [" + dependencyInterface.getSimpleName() + "] has already been bound.");
		}
		
		
		// make sure the given dependency implements the necessary interfaces
		boolean implementsInterface = checkIfClassImplements(dependencyImplementation.getClass(), dependencyInterface);
		boolean implementsBindable = checkIfClassImplements(dependencyImplementation.getClass(), this.bindableInterface);
		
		// display any errors
		if (!implementsInterface)
		{
			throw new IllegalStateException("The dependency [" + dependencyImplementation.getClass().getSimpleName() + "] doesn't implement the interface [" + dependencyInterface.getSimpleName() + "].");
		}
		if (!implementsBindable)
		{
			throw new IllegalStateException("The dependency [" + dependencyImplementation.getClass().getSimpleName() + "] doesn't implement the interface [" + IBindable.class.getSimpleName() + "].");
		}
		
		
		dependencies.put(dependencyInterface, dependencyImplementation);
	}
	/**
	 * Checks if classToTest (or one of its ancestors)
	 * implements the given interface.
	 */
	private boolean checkIfClassImplements(Class<?> classToTest, Class<?> interfaceToLookFor)
	{
		// check the parent class (if applicable)
		if (classToTest.getSuperclass() != Object.class && classToTest.getSuperclass() != null)
		{
			if (checkIfClassImplements(classToTest.getSuperclass(), interfaceToLookFor))
			{
				return true;
			}
		}
		
		
		// check interfaces
		for (Class<?> implementationInterface : classToTest.getInterfaces())
		{
			// recurse to check interface parents if necessary
			if (implementationInterface.getInterfaces().length != 0)
			{
				if (checkIfClassImplements(implementationInterface, interfaceToLookFor))
				{
					return true;
				}
			}
			
			if (implementationInterface.equals(interfaceToLookFor))
			{
				return true;
			}
		}
		
		return false;
	}
	

	/**
	 * Returns a dependency of type T if one has been bound.
	 * Returns null otherwise.
	 * 
	 * @param <T> class of the dependency
	 *            (inferred from the objectClass parameter)
	 * @param interfaceClass Interface of the dependency
	 * @return the dependency of type T
	 * @throws ClassCastException If the dependency isn't able to be cast to type T. 
	 *                            (this shouldn't normally happen, unless the bound object changed somehow)
	 */
	@SuppressWarnings("unchecked")
	public <T extends BindableType> T get(Class<?> interfaceClass) throws ClassCastException
	{
		T dependency = (T) dependencies.get(interfaceClass);
		if (dependency != null && !dependency.getDelayedSetupComplete())
		{
			// a warning can be used here instead if desired
			//this.logger.warn("Got dependency of type [" + interfaceClass.getSimpleName() + "], but the dependency's delayed setup hasn't been run!");
			throw new IllegalStateException("Got dependency of type [" + interfaceClass.getSimpleName() + "], but the dependency's delayed setup hasn't been run!");
		}
		
		return dependency;
	}
	
	
	/** Runs delayed setup for any dependencies that require it. */
	public void runDelayedSetup()
	{
		for (Class<?> interfaceKey : dependencies.keySet())
		{
			IBindable concreteObject = get(interfaceKey);
			if (!concreteObject.getDelayedSetupComplete())
			{
				concreteObject.finishDelayedSetup();
			}
		}
	}
	
}
