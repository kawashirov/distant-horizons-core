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

import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.worldGenerator.ICoreDhApiWorldGenerator;
import com.seibel.lod.core.api.external.coreImplementations.interfaces.wrappers.world.ICoreDhApiLevelWrapper;
import com.seibel.lod.core.util.StringUtil;

import java.util.HashMap;

/**
 * This class takes care of dependency injection for world generators. <Br>
 * This is done so other mods can override our world generator(s) to improve or replace them.
 *
 * @author James Seibel
 * @version 2022-9-8
 */
public class WorldGeneratorInjector
{
	public static final WorldGeneratorInjector INSTANCE = new WorldGeneratorInjector();

	private final HashMap<ICoreDhApiLevelWrapper, OverrideInjector> worldGeneratorByLevelWrapper = new HashMap<>();
	/** World generators that aren't bound to a specific level and are used if no other world generators are bound. */
	private final OverrideInjector backupUniversalWorldGenerators;

	/**
	 * This is used to determine if an override is part of Distant Horizons'
	 * Core or not.
	 * This probably isn't the best way of going about this, but it works for now.
	 */
	private final String corePackagePath;



	public WorldGeneratorInjector()
	{
		String thisPackageName = this.getClass().getPackage().getName();
		int secondPackageEndingIndex = StringUtil.nthIndexOf(thisPackageName, ".", 3);
		this.corePackagePath = thisPackageName.substring(0, secondPackageEndingIndex); // this should be "com.seibel.lod"

		this.backupUniversalWorldGenerators = new OverrideInjector(this.corePackagePath);
	}

	/** This constructor should only be used for testing different corePackagePaths. */
	public WorldGeneratorInjector(String newCorePackagePath)
	{
		this.corePackagePath = newCorePackagePath;

		this.backupUniversalWorldGenerators = new OverrideInjector(this.corePackagePath);
	}



	/**
	 * Binds the backup world generator. <Br>
	 * See {@link DependencyInjector#bind(Class, IBindable) bind(Class, IBindable)} for full documentation.
	 *
	 * @throws IllegalArgumentException if a non-Distant Horizons world generator with the priority CORE is passed in
	 * @see DependencyInjector#bind(Class, IBindable)
	 */
	public void bind(ICoreDhApiWorldGenerator worldGeneratorImplementation)  throws IllegalStateException, IllegalArgumentException
	{
		bind(null, worldGeneratorImplementation);
	}

	/**
	 * Binds the world generator to the given level. <Br>
	 * See {@link DependencyInjector#bind(Class, IBindable) bind(Class, IBindable)} for full documentation.
	 *
	 * @throws IllegalArgumentException if a non-Distant Horizons world generator with the priority CORE is passed in
	 * @see DependencyInjector#bind(Class, IBindable)
	 */
	public void bind(ICoreDhApiLevelWrapper levelForWorldGenerator, ICoreDhApiWorldGenerator worldGeneratorImplementation)  throws IllegalStateException, IllegalArgumentException
	{
		if (levelForWorldGenerator != null)
		{
			// bind this generator to a specific level
			if (!worldGeneratorByLevelWrapper.containsKey(levelForWorldGenerator))
			{
				worldGeneratorByLevelWrapper.put(levelForWorldGenerator, new OverrideInjector(this.corePackagePath));
			}

			worldGeneratorByLevelWrapper.get(levelForWorldGenerator).bind(ICoreDhApiWorldGenerator.class, worldGeneratorImplementation);
		}
		else
		{
			// a null level wrapper binds the generator to all levels
			backupUniversalWorldGenerators.bind(ICoreDhApiWorldGenerator.class, worldGeneratorImplementation);
		}
	}



	/**
	 * Returns the backup world generator with the highest priority. <br>
	 * See {@link OverrideInjector#get(Class) get(Class)} for more documentation.
	 *
	 * @see OverrideInjector#get(Class)
	 */
	public ICoreDhApiWorldGenerator get() throws ClassCastException
	{
		return backupUniversalWorldGenerators.get(ICoreDhApiWorldGenerator.class);
	}

	/**
	 * Returns the bound world generator with the highest priority. <br>
	 * (Returns a backup world generator if no world generators have been bound for this specific level.) <br>
	 * See {@link OverrideInjector#get(Class) get(Class)} for more documentation.
	 *
	 * @see OverrideInjector#get(Class)
	 */
	public ICoreDhApiWorldGenerator get(ICoreDhApiLevelWrapper levelForWorldGenerator) throws ClassCastException
	{
		if (!worldGeneratorByLevelWrapper.containsKey(levelForWorldGenerator))
		{
			// no generator exists for this specific level.
			// check for a backup universal world generator
			return backupUniversalWorldGenerators.get(ICoreDhApiWorldGenerator.class);
		}

		// use the existing world generator
		return worldGeneratorByLevelWrapper.get(levelForWorldGenerator).get(ICoreDhApiWorldGenerator.class);
	}



	/** Removes all bound world generators. */
	public void clearBoundDependencies() // TODO this should be done when leaving the current world/server
	{
		this.worldGeneratorByLevelWrapper.clear();
		this.backupUniversalWorldGenerators.clear();
	}
	
}
