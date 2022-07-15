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

/**
 * This class takes care of dependency injection
 * for singletons.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public class SingletonHandler
{
	private static final DependencyHandler dependencyHandler = new DependencyHandler();
		
	// TODO: FIX Javadoc
	//  This is the exact same javadoc as in DependencyHandler.java
	//  Ths method doesnt even use dependencyInterface or dependencyImplementation
	/**
	 * Links the given implementation object to an interface, so it can be referenced later.
	 * 
	 * @param interfaceClass The interface the implementation object should implement.
	 * @param singletonReference An object that implements the dependencyInterface interface.
	 * @throws IllegalStateException if the implementation object doesn't implement 
	 *                               the interface or the interface has already been bound.
	 */
	public static void bind(Class<?> interfaceClass, Object singletonReference) throws IllegalStateException
	{
		dependencyHandler.bind(interfaceClass, singletonReference);
	}

	/**
	 * Returns a dependency of type T if one has been bound.
	 * Returns null otherwise.
	 * 
	 * @param <T> class of the dependency
	 *            (inferred from the objectClass parameter)
	 * @param interfaceClass Interface of the dependency
	 * @return the dependency of type T
	 * @throws NullPointerException If no dependency was bound.
	 * @throws ClassCastException If the dependency isn't able to be cast to type T. 
	 *                            (this shouldn't normally happen, unless the bound object changed somehow)
	 */
	public static <T> T get(Class<T> interfaceClass) throws NullPointerException, ClassCastException
	{
		T foundObject = dependencyHandler.get(interfaceClass);
		
		// throw an error if the given singleton doesn't exist.
		if (foundObject == null)
		{
			throw new NullPointerException("The singleton [" + interfaceClass.getSimpleName() + "] was never bound. If you are calling [bind], make sure it is happening before you call [get].");
		}
		
		return foundObject;
	}
	public static <T> T getOrNull(Class<T> interfaceClass) throws ClassCastException
	{
		return dependencyHandler.get(interfaceClass);
	}

	/**
	 * Should only be called after all Binds have been done.
	 * Calls the delayedSetup method for each dependency. <br> <br>
	 * 
	 * This is done so we can have circular dependencies.
	 */
	public static void finishBinding()
	{
		dependencyHandler.finishBinding();
	}
	
	/** returns whether the finishBinding method has been called */
	public static boolean getBindingFinished() 
	{
		return dependencyHandler.getBindingFinished();
	}

	public static void runDelayedSetup() {
		dependencyHandler.runDelayedSetup();
	}
}
