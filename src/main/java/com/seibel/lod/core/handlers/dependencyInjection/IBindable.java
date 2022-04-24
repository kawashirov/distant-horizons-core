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
 * Necessary for all singletons that can be dependency injected.
 * 
 * @author James Seibel
 * @version 3-4-2022
 */
public interface IBindable
{
	/**
	 * Finish initializing this object. <br> <br>
	 * 
	 * Generally this should just used for getting other objects through
	 * dependency injection and is specifically designed to allow 
	 * for circular references. <br><br>
	 * 
	 * If no circular dependencies are required this method 
	 * doesn't have to be implemented.
	 */
	public default void finishDelayedSetup()
	{
		
	}
}
