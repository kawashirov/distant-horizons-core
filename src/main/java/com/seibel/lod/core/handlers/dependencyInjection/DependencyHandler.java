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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class takes care of tracking objects used in dependency injection.
 *
 * @param <BindableType> extends IBindable and defines what interfaces this dependency handler can deal with.
 * @author James Seibel
 * @version 2022-7-16
 */
public class DependencyHandler<BindableType extends IBindable>
{
	protected final Map<Class<? extends BindableType>, ArrayList<BindableType>> dependencies = new HashMap<>();
	
	/** Internal class reference to BindableType since we can't get it any other way. */
	protected final Class<? extends BindableType> bindableInterface;
	
	protected final boolean allowDuplicateBindings;
	
	
	public DependencyHandler(Class<BindableType> newBindableInterface)
	{
		this.bindableInterface = newBindableInterface;
		this.allowDuplicateBindings = false;
	}
	
	public DependencyHandler(Class<BindableType> newBindableInterface, boolean newAllowDuplicateBindings)
	{
		this.bindableInterface = newBindableInterface;
		this.allowDuplicateBindings = newAllowDuplicateBindings;
	}
	
	
	
	
	/**
	 * Links the given implementation object to an interface, so it can be referenced later.
	 *
	 * @param dependencyInterface The interface (or parent class) the implementation object should implement.
	 * @param dependencyImplementation An object that implements the dependencyInterface interface.
	 * @throws IllegalStateException if the implementation object doesn't implement
	 *                               the interface or the interface has already been bound.
	 */
	public void bind(Class<? extends BindableType> dependencyInterface, BindableType dependencyImplementation) throws IllegalStateException
	{
		// duplicate check if requested
		if (dependencies.containsKey(dependencyInterface) && !this.allowDuplicateBindings)
		{
			throw new IllegalStateException("The dependency [" + dependencyInterface.getSimpleName() + "] has already been bound.");
		}
		
		
		// make sure the given dependency implements the necessary interfaces
		boolean implementsInterface = checkIfClassImplements(dependencyImplementation.getClass(), dependencyInterface)
				|| checkIfClassExtends(dependencyImplementation.getClass(), dependencyInterface);
		boolean implementsBindable = checkIfClassImplements(dependencyImplementation.getClass(), this.bindableInterface);
		
		// display any errors
		if (!implementsInterface)
		{
			throw new IllegalStateException("The dependency [" + dependencyImplementation.getClass().getSimpleName() + "] doesn't implement or extend: [" + dependencyInterface.getSimpleName() + "].");
		}
		if (!implementsBindable)
		{
			throw new IllegalStateException("The dependency [" + dependencyImplementation.getClass().getSimpleName() + "] doesn't implement the interface: [" + IBindable.class.getSimpleName() + "].");
		}
		
		
		// make sure the hashSet has an array to hold the dependency
		if (!dependencies.containsKey(dependencyInterface))
		{
			dependencies.put(dependencyInterface, new ArrayList<BindableType>());
		}
		
		// add the dependency
		dependencies.get(dependencyInterface).add(dependencyImplementation);
	}
	/**
	 * Checks if classToTest (or one of its ancestors)
	 * implements the given interface.
	 */
	protected boolean checkIfClassImplements(Class<?> classToTest, Class<?> interfaceToLookFor)
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
	/** Checks if classToTest extends the given class. */
	protected boolean checkIfClassExtends(Class<?> classToTest, Class<?> extensionToLookFor)
	{
		return extensionToLookFor.isAssignableFrom(classToTest);
	}
	
	
	/**
	 * This does not return incomplete dependencies. <Br>
	 * See {@link #get(Class, boolean) get(Class, boolean)} for full documentation.
	 *
	 * @see #get(Class, boolean)
	 */
	@SuppressWarnings("unchecked")
	public <T extends BindableType> T get(Class<?> interfaceClass) throws ClassCastException
	{
		return (T) getInternalLogic(interfaceClass, false).get(0);
	}
	
	/**
	 * Returns all dependencies of type T that have been bound. <br>
	 * Returns an empty list if no dependencies have been bound.
	 *
	 * @param <T> class of the dependency
	 *            (inferred from the objectClass parameter)
	 * @param interfaceClass Interface of the dependency
	 * @return the dependency of type T
	 * @throws ClassCastException If the dependency isn't able to be cast to type T.
	 *                            (this shouldn't normally happen, unless the bound object changed somehow)
	 */
	public <T extends BindableType> ArrayList<T> getAll(Class<?> interfaceClass) throws ClassCastException
	{
		return getInternalLogic(interfaceClass, false);
	}
	
	/**
	 * Returns a dependency of type T if one has been bound. <br>
	 * Returns null if a dependency hasn't been bound. <br> <br>
	 *
	 * If the handler's {@link #allowDuplicateBindings} is true this returns the first bound dependency.
	 *
	 * @param <T> class of the dependency
	 *            (inferred from the objectClass parameter)
	 * @param interfaceClass Interface of the dependency
	 * @param allowIncompleteDependencies If true this method will also return dependencies that haven't completed their delayed setup.
	 * @return the dependency of type T
	 * @throws ClassCastException If the dependency isn't able to be cast to type T.
	 *                            (this shouldn't normally happen, unless the bound object changed somehow)
	 */
	@SuppressWarnings("unchecked")
	public <T extends BindableType> T get(Class<?> interfaceClass, boolean allowIncompleteDependencies) throws ClassCastException
	{
		return (T) getInternalLogic(interfaceClass, allowIncompleteDependencies).get(0);
	}
	
	/**
	 * Always returns a list of size 1 or greater,
	 * if no dependencies have been bound the list will contain null.
	 */
	@SuppressWarnings("unchecked")
	private <T extends BindableType> ArrayList<T> getInternalLogic(Class<?> interfaceClass, boolean allowIncompleteDependencies) throws ClassCastException
	{
		ArrayList<BindableType> dependencyList = dependencies.get(interfaceClass);
		if (dependencyList != null && dependencyList.size() != 0)
		{
			// check if each dependencies' delayed setup has been completed
			for (IBindable dependency : dependencyList)
			{
				if (!dependency.getDelayedSetupComplete() && !allowIncompleteDependencies)
				{
					// a warning can be used here instead if desired
					//this.logger.warn("Got dependency of type [" + interfaceClass.getSimpleName() + "], but the dependency's delayed setup hasn't been run!");
					throw new IllegalStateException("Got dependency of type [" + interfaceClass.getSimpleName() + "], but the dependency's delayed setup hasn't been run!");
				}
			}
			
			return (ArrayList<T>) dependencyList;
		}
		
		
		// return an empty list to prevent null pointers
		ArrayList<T> emptyList = new ArrayList<T>();
		emptyList.add(null);
		return emptyList;
	}
	
	
	
	/** Runs delayed setup for any dependencies that require it. */
	public void runDelayedSetup()
	{
		for (Class<?> interfaceKey : dependencies.keySet())
		{
			IBindable concreteObject = get(interfaceKey, true);
			if (!concreteObject.getDelayedSetupComplete())
			{
				concreteObject.finishDelayedSetup();
			}
		}
	}
	
}
