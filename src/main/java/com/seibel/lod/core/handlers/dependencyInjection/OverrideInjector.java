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

import com.seibel.lod.core.api.external.items.enums.override.EDhApiOverridePriority;
import com.seibel.lod.core.api.external.items.interfaces.override.IDhApiOverrideable;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.enums.override.EOverridePriority;
import com.seibel.lod.core.util.StringUtil;

/**
 * This class takes care of dependency injection for overridable objects. <Br>
 * This is done so other mods can override our methods to improve features down the line.
 *
 * @author James Seibel
 * @version 2022-7-19
 */
public class OverrideInjector
{
	public static final OverrideInjector INSTANCE = new OverrideInjector();
	
	private final DependencyInjector<IDhApiOverrideable> primaryInjector = new DependencyInjector<>(IDhApiOverrideable.class, false);
	private final DependencyInjector<IDhApiOverrideable> secondaryInjector = new DependencyInjector<>(IDhApiOverrideable.class, false);
	private final DependencyInjector<IDhApiOverrideable> coreInjector = new DependencyInjector<>(IDhApiOverrideable.class, false);
	
	private static final GenericEnumConverter<EOverridePriority, EDhApiOverridePriority> ENUM_CONVERTER = new GenericEnumConverter<>(EOverridePriority.class, EDhApiOverridePriority.class);
	
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
	public OverrideInjector(String newCorePackagePath)
	{
		this.corePackagePath = newCorePackagePath;
	}
	
	
	
	/**
	 * See {@link DependencyInjector#bind(Class, IBindable) bind(Class, IBindable)} for full documentation.
	 *
	 * @throws IllegalArgumentException if a non-Distant Horizons Override with the priority CORE is passed in
	 * @see DependencyInjector#bind(Class, IBindable)
	 */
	public void bind(Class<? extends IDhApiOverrideable> dependencyInterface, IDhApiOverrideable dependencyImplementation)  throws IllegalStateException, IllegalArgumentException
	{
		if (getCorePriorityEnum(dependencyImplementation) == EOverridePriority.PRIMARY)
		{
			primaryInjector.bind(dependencyInterface, dependencyImplementation);
		}
		else if (getCorePriorityEnum(dependencyImplementation) == EOverridePriority.SECONDARY)
		{
			secondaryInjector.bind(dependencyInterface, dependencyImplementation);
		}
		else
		{
			String packageName = dependencyImplementation.getClass().getPackage().getName();
			if (packageName.startsWith(this.corePackagePath))
			{
				coreInjector.bind(dependencyInterface, dependencyImplementation);
			}
			else
			{
				throw new IllegalArgumentException("Only Distant Horizons internal objects can use the Override Priority [" + EOverridePriority.CORE + "]. Please use [" + EOverridePriority.PRIMARY + "] or [" + EOverridePriority.SECONDARY + "] instead.");
			}
		}
	}
	
	/**
	 * Returns the bound dependency with the highest priority. <br>
	 * See {@link DependencyInjector#get(Class, boolean) get(Class, boolean)} for full documentation.
	 *
	 * @see DependencyInjector#get(Class, boolean)
	 */
	public <T extends IDhApiOverrideable> T get(Class<T> interfaceClass) throws ClassCastException
	{
		T value = primaryInjector.get(interfaceClass);
		if (value == null)
		{
			value = secondaryInjector.get(interfaceClass);
		}
		if (value == null)
		{
			value = coreInjector.get(interfaceClass);
		}
		
		return value;
	}
	
	/**
	 * Returns a dependency of type T with the specified priority if one has been bound. <br>
	 * If there is a dependency, but it was bound with a different priority this will return null. <br> <br>
	 *
	 * See {@link DependencyInjector#get(Class, boolean) get(Class, boolean)} for more documentation.
	 *
	 * @see DependencyInjector#get(Class, boolean)
	 */
	public <T extends IDhApiOverrideable> T get(Class<T> interfaceClass, EOverridePriority overridePriority) throws ClassCastException
	{
		T value;
		if (overridePriority == EOverridePriority.PRIMARY)
		{
			value = primaryInjector.get(interfaceClass);
		}
		else if (overridePriority == EOverridePriority.SECONDARY)
		{
			value = secondaryInjector.get(interfaceClass);
		}
		else
		{
			value = coreInjector.get(interfaceClass);
		}
		
		return value;
	}
	
	
	/** Small helper method so we don't have to use DhApi enums. */
	private EOverridePriority getCorePriorityEnum(IDhApiOverrideable override)
	{
		return ENUM_CONVERTER.convertToCoreType(override.getOverrideType());
	}
	
}
