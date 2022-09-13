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

package com.seibel.lod.core.dependencyInjection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class takes care of tracking objects used in dependency injection.
 *
 * @param <BindableType> extends IBindable and defines what interfaces this dependency handler can deal with.
 * @author James Seibel
 * @version 2022-8-15
 */
public class DependencyInjector<BindableType extends IBindable> implements IDependencyInjector<BindableType>
{
	protected final Map<Class<? extends BindableType>, ArrayList<BindableType>> dependencies = new HashMap<>();
	
	/** Internal class reference to BindableType since we can't get it any other way. */
	protected final Class<? extends BindableType> bindableInterface;
	
	protected final boolean allowDuplicateBindings;
	
	
	public DependencyInjector(Class<BindableType> newBindableInterface)
	{
		this.bindableInterface = newBindableInterface;
		this.allowDuplicateBindings = false;
	}
	
	public DependencyInjector(Class<BindableType> newBindableInterface, boolean newAllowDuplicateBindings)
	{
		this.bindableInterface = newBindableInterface;
		this.allowDuplicateBindings = newAllowDuplicateBindings;
	}
	
	
	
	
	@Override
	public void bind(Class<? extends BindableType> dependencyInterface, BindableType dependencyImplementation) throws IllegalStateException, IllegalArgumentException
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
			throw new IllegalArgumentException("The dependency [" + dependencyImplementation.getClass().getSimpleName() + "] doesn't implement or extend: [" + dependencyInterface.getSimpleName() + "].");
		}
		if (!implementsBindable)
		{
			throw new IllegalArgumentException("The dependency [" + dependencyImplementation.getClass().getSimpleName() + "] doesn't implement the interface: [" + IBindable.class.getSimpleName() + "].");
		}
		
		
		// make sure the hashSet has an array to hold the dependency
		if (!dependencies.containsKey(dependencyInterface))
		{
			dependencies.put(dependencyInterface, new ArrayList<BindableType>());
		}
		
		// add the dependency
		dependencies.get(dependencyInterface).add(dependencyImplementation);
	}
	@Override
	public boolean checkIfClassImplements(Class<?> classToTest, Class<?> interfaceToLookFor)
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
	@Override
	public boolean checkIfClassExtends(Class<?> classToTest, Class<?> extensionToLookFor)
	{
		return extensionToLookFor.isAssignableFrom(classToTest);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends BindableType> T get(Class<T> interfaceClass) throws ClassCastException
	{
		return (T) getInternalLogic(interfaceClass, false).get(0);
	}
	
	@Override
	public <T extends BindableType> ArrayList<T> getAll(Class<T> interfaceClass) throws ClassCastException
	{
		return getInternalLogic(interfaceClass, false);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends BindableType> T get(Class<T> interfaceClass, boolean allowIncompleteDependencies) throws ClassCastException
	{
		return (T) getInternalLogic(interfaceClass, allowIncompleteDependencies).get(0);
	}
	
	/**
	 * Always returns a list of size 1 or greater,
	 * if no dependencies have been bound the list will contain null.
	 */
	@SuppressWarnings("unchecked")
	private <T extends BindableType> ArrayList<T> getInternalLogic(Class<T> interfaceClass, boolean allowIncompleteDependencies) throws ClassCastException
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
	
	
	
	/** Removes all bound dependencies. */
	@Override
	public void clear()
	{
		this.dependencies.clear();
	}
	
	
	
	/** Runs delayed setup for any dependencies that require it. */
	@Override
	public void runDelayedSetup()
	{
		for (Class<? extends BindableType> interfaceKey : dependencies.keySet())
		{
			IBindable concreteObject = get(interfaceKey, true);
			if (!concreteObject.getDelayedSetupComplete())
			{
				concreteObject.finishDelayedSetup();
			}
		}
	}
	
}
