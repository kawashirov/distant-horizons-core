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
import com.seibel.lod.core.enums.override.EApiOverridePriority;
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
	
	private static final DependencyInjector<IDhApiOverrideable> PRIMARY_INJECTOR = new DependencyInjector<>(IDhApiOverrideable.class, false);
	private static final DependencyInjector<IDhApiOverrideable> SECONDARY_INJECTOR = new DependencyInjector<>(IDhApiOverrideable.class, false);
	private static final DependencyInjector<IDhApiOverrideable> CORE_INJECTOR = new DependencyInjector<>(IDhApiOverrideable.class, false);
	
	private static final GenericEnumConverter<EApiOverridePriority, EDhApiOverridePriority> enumConverter = new GenericEnumConverter<>(EApiOverridePriority.class, EDhApiOverridePriority.class);
	
	
	/**
	 * See {@link DependencyInjector#bind(Class, IBindable) bind(Class, IBindable)} for full documentation.
	 *
	 * @see DependencyInjector#bind(Class, IBindable)
	 */
	public void bind(Class<? extends IDhApiOverrideable> dependencyInterface, IDhApiOverrideable dependencyImplementation)  throws IllegalStateException, IllegalArgumentException
	{
		if (getCorePriorityEnum(dependencyImplementation) == EApiOverridePriority.PRIMARY)
		{
			PRIMARY_INJECTOR.bind(dependencyInterface, dependencyImplementation);
		}
		else if (getCorePriorityEnum(dependencyImplementation) == EApiOverridePriority.SECONDARY)
		{
			SECONDARY_INJECTOR.bind(dependencyInterface, dependencyImplementation);
		}
		else
		{
			// not the best way of doing this, but it should work
			String thisPackageName = this.getClass().getPackage().getName();
			int secondPackageEndingIndex = StringUtil.nthIndexOf(thisPackageName, ".", 3);
			String thisPackageBeginningName = thisPackageName.substring(0, secondPackageEndingIndex); // this should be "com.seibel.lod"
			
			if (dependencyImplementation.getClass().getPackage().getName().startsWith(thisPackageBeginningName))
			{
				CORE_INJECTOR.bind(dependencyInterface, dependencyImplementation);
			}
			else
			{
				throw new IllegalArgumentException("Only Distant Horizons internal objects can use the Override Priority [" + EApiOverridePriority.CORE + "]. Please use [" + EApiOverridePriority.PRIMARY + "] or [" + EApiOverridePriority.SECONDARY + "] instead.");
			}
		}
	}
	
	/**
	 * Returns the bound dependency with the highest priority. <br>
	 * See {@link DependencyInjector#get(Class, boolean) get(Class, boolean)} for full documentation.
	 *
	 * @see DependencyInjector#get(Class, boolean)
	 */
	public <T extends IDhApiOverrideable> T get(Class<? extends IDhApiOverrideable> interfaceClass) throws ClassCastException
	{
		T value = PRIMARY_INJECTOR.get(interfaceClass);
		if (value == null)
		{
			value = SECONDARY_INJECTOR.get(interfaceClass);
		}
		if (value == null)
		{
			value = CORE_INJECTOR.get(interfaceClass);
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
	public <T extends IDhApiOverrideable> T get(Class<? extends IDhApiOverrideable> interfaceClass, EApiOverridePriority overridePriority) throws ClassCastException
	{
		T value;
		if (overridePriority == EApiOverridePriority.PRIMARY)
		{
			value = PRIMARY_INJECTOR.get(interfaceClass);
		}
		else if (overridePriority == EApiOverridePriority.SECONDARY)
		{
			value = SECONDARY_INJECTOR.get(interfaceClass);
		}
		else
		{
			value = CORE_INJECTOR.get(interfaceClass);
		}
		
		return value;
	}
	
	
	/** Small helper method so we don't have to use DhApi enums. */
	private EApiOverridePriority getCorePriorityEnum(IDhApiOverrideable override)
	{
		return enumConverter.convertToCoreType(override.getOverrideType());
	}
	
}
