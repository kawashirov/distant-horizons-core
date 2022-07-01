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

package com.seibel.lod.core.api.external.apiObjects.enums;

/**
 * NEVER, 	<br>
 * DYNAMIC, <br>
 * ALWAYS	<br>
 *
 * <p>
 * This represents how far the LODs should overlap with
 * the vanilla Minecraft terrain.
 * 
 * @author James Seibel
 * @version 2022-6-30
 */
public enum EDhApiVanillaOverdraw
{
	// Reminder:
	// when adding items up the API minor version
	// when removing items up the API major version
	
	
	/**
	 * Don't draw LODs where a minecraft chunk could be.
	 * Use Overdraw Offset to tweak the border thickness.
	 */
	NEVER,
	
	/**
	 * Draw LODs over the farther minecraft chunks.
	 * Dynamically decides the border thickness
	 */
	DYNAMIC,
	
	/** Draw LODs over all minecraft chunks. */
	ALWAYS,
}