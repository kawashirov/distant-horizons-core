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

import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.ICoreDhApiOverrideable;
import com.seibel.lod.core.util.StringUtil;

import java.util.HashMap;

/**
 * This class takes care of dependency injection for overridable objects. <Br>
 * This is done so other mods can override our methods to improve features down the line.
 *
 * @author James Seibel
 * @version 2022-9-8
 */
public class OverrideInjector
{
	public static final OverrideInjector INSTANCE = new OverrideInjector();

	private final HashMap<Class<? extends ICoreDhApiOverrideable>, OverridePriorityListContainer> overrideContainerByInterface = new HashMap<>();
	
	
	/** 
	 * All core overrides should have this priority. <Br>
	 * Should be lower than MIN_OVERRIDE_PRIORITY.
	 */
	public static final int CORE_PRIORITY = -1;
	/** 
	 * The lowest priority non-core overrides can have.
	 * Should be higher than CORE_PRIORITY.
	 */
	public static final int MIN_NON_CORE_OVERRIDE_PRIORITY = 0;
	/** The priority given to overrides that don't explicitly define a priority. */
	public static final int DEFAULT_NON_CORE_OVERRIDE_PRIORITY = 10;
	
	/**
	 * This is used to determine if an override is part of Distant Horizons'
	 * Core or not.
	 * This probably isn't the best way of going about this, but it works for now.
	 */
	private final String corePackagePath;
	
	
	
	public OverrideInjector()
	{
		String thisPackageName = this.getClass().getPackage().getName();
		int secondPackageEndingIndex = StringUtil.nthIndexOf(thisPackageName, ".", 3);
		
		this.corePackagePath = thisPackageName.substring(0, secondPackageEndingIndex); // this should be "com.seibel.lod"
	}
	
	/** This constructor should only be used for testing different corePackagePaths. */
	public OverrideInjector(String newCorePackagePath) { this.corePackagePath = newCorePackagePath; }
	
	
	
	/**
	 * See {@link DependencyInjector#bind(Class, IBindable) bind(Class, IBindable)} for full documentation.
	 *
	 * @throws IllegalArgumentException if a non-Distant Horizons Override with the priority CORE is passed in or a invalid priority value.
	 * @throws IllegalStateException if another override with the given priority already has been bound.
	 * @see DependencyInjector#bind(Class, IBindable)
	 */
	public void bind(Class<? extends ICoreDhApiOverrideable> dependencyInterface, ICoreDhApiOverrideable dependencyImplementation)  throws IllegalStateException, IllegalArgumentException
	{
		// make sure a override container exists
		OverridePriorityListContainer overrideContainer = this.overrideContainerByInterface.get(dependencyInterface);
		if (overrideContainer == null)
		{
			overrideContainer = new OverridePriorityListContainer();
			this.overrideContainerByInterface.put(dependencyInterface, overrideContainer);
		}
		
		
		// validate the new override //
		
		// check if this override is a core override
		if (dependencyImplementation.getPriority() == CORE_PRIORITY)
		{
			// this claims to be a core override, is that true?
			String packageName = dependencyImplementation.getClass().getPackage().getName();
			if (!packageName.startsWith(this.corePackagePath))
			{
				throw new IllegalArgumentException("Only Distant Horizons internal objects can use the Override Priority [" + CORE_PRIORITY + "]. Please use a higher number.");
			}
		}
		
		// make sure the override has a valid priority
		else if (dependencyImplementation.getPriority() < MIN_NON_CORE_OVERRIDE_PRIORITY)
		{
			throw new IllegalArgumentException("Invalid priority value [" + dependencyImplementation.getPriority() + "], override priorities must be [" + MIN_NON_CORE_OVERRIDE_PRIORITY + "] or greater.");
		}
		
		// check if an override already exists with this priority
		ICoreDhApiOverrideable existingOverride = overrideContainer.getOverrideWithPriority(dependencyImplementation.getPriority());
		if (existingOverride != null)
		{
			throw new IllegalStateException("An override already exists with the priority [" + dependencyImplementation.getPriority() + "].");
		}
		
		
		// bind the override
		overrideContainer.addOverride(dependencyImplementation);
	}
	
	/**
	 * Returns the bound dependency with the highest priority. <br>
	 * See {@link DependencyInjector#get(Class, boolean) get(Class, boolean)} for full documentation.
	 *
	 * @see DependencyInjector#get(Class, boolean)
	 */
	@SuppressWarnings("unchecked")
	public <T extends ICoreDhApiOverrideable> T get(Class<T> interfaceClass) throws ClassCastException
	{
		OverridePriorityListContainer overrideContainer = this.overrideContainerByInterface.get(interfaceClass);
		return overrideContainer != null ? (T) overrideContainer.getOverrideWithHighestPriority() : null;
	}
	
	/**
	 * Returns a dependency of type T with the specified priority if one has been bound. <br>
	 * If there is a dependency, but it was bound with a different priority this will return null. <br> <br>
	 *
	 * See {@link DependencyInjector#get(Class, boolean) get(Class, boolean)} for more documentation.
	 *
	 * @see DependencyInjector#get(Class, boolean)
	 */
	@SuppressWarnings("unchecked")
	public <T extends ICoreDhApiOverrideable> T get(Class<T> interfaceClass, int priority) throws ClassCastException
	{
		OverridePriorityListContainer overrideContainer = this.overrideContainerByInterface.get(interfaceClass);
		return overrideContainer != null ? (T) overrideContainer.getOverrideWithPriority(priority) : null;
	}
	
	
	
	/** Removes all bound overrides. */
	public void clear() { this.overrideContainerByInterface.clear(); }
	
	
	
	
	
	
}
