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

import com.seibel.lod.core.api.external.items.interfaces.override.IDhApiWorldGenerator;
import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiLevelWrapper;

import java.util.HashMap;

/**
 * This class takes care of dependency injection for world generators. <Br>
 * This is done so other mods can override our world generator(s) to improve or replace them.
 *
 * @author James Seibel
 * @version 2022-7-26
 */
public class WorldGeneratorInjector
{
	public static final WorldGeneratorInjector INSTANCE = new WorldGeneratorInjector();
	
	private final HashMap<IDhApiLevelWrapper, OverrideInjector<IDhApiWorldGenerator>> worldGeneratorByLevelWrapper = new HashMap<>();
	/** World generators that aren't bound to a specific level and are used if no other world generators are bound. */
	private final OverrideInjector<IDhApiWorldGenerator> backupUniversalWorldGenerators = new OverrideInjector<>();
	
	
	
	/**
	 * Binds the backup world generator. <Br>
	 * See {@link DependencyInjector#bind(Class, IBindable) bind(Class, IBindable)} for full documentation.
	 *
	 * @throws IllegalArgumentException if a non-Distant Horizons world generator with the priority CORE is passed in
	 * @see DependencyInjector#bind(Class, IBindable)
	 */
	public void bind(IDhApiWorldGenerator worldGeneratorImplementation)  throws IllegalStateException, IllegalArgumentException
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
	public void bind(IDhApiLevelWrapper levelForWorldGenerator, IDhApiWorldGenerator worldGeneratorImplementation)  throws IllegalStateException, IllegalArgumentException
	{
		if (levelForWorldGenerator != null)
		{
			// bind this generator to a specific level
			if (!worldGeneratorByLevelWrapper.containsKey(levelForWorldGenerator))
			{
				worldGeneratorByLevelWrapper.put(levelForWorldGenerator, new OverrideInjector<>());
			}
			
			worldGeneratorByLevelWrapper.get(levelForWorldGenerator).bind(IDhApiWorldGenerator.class, worldGeneratorImplementation);
		}
		else
		{
			// a null level wrapper binds the generator to all levels
			backupUniversalWorldGenerators.bind(IDhApiWorldGenerator.class, worldGeneratorImplementation);
		}
	}
	
	
	
	/**
	 * Returns the backup world generator with the highest priority. <br>
	 * See {@link OverrideInjector#get(Class) get(Class)} for more documentation.
	 *
	 * @see OverrideInjector#get(Class)
	 */
	public IDhApiWorldGenerator get() throws ClassCastException
	{
		return backupUniversalWorldGenerators.get(IDhApiWorldGenerator.class);
	}
	
	/**
	 * Returns the bound world generator with the highest priority. <br>
	 * (Returns a backup world generator if no world generators have been bound for this specific level.) <br>
	 * See {@link OverrideInjector#get(Class) get(Class)} for more documentation.
	 *
	 * @see OverrideInjector#get(Class)
	 */
	public IDhApiWorldGenerator get(IDhApiLevelWrapper levelForWorldGenerator) throws ClassCastException
	{
		if (!worldGeneratorByLevelWrapper.containsKey(levelForWorldGenerator))
		{
			// no generator exists for this specific level.
			// check for a backup universal world generator
			return backupUniversalWorldGenerators.get(IDhApiWorldGenerator.class);
		}
		
		// use the existing world generator
		return worldGeneratorByLevelWrapper.get(levelForWorldGenerator).get(IDhApiWorldGenerator.class);
	}
	
	
}
