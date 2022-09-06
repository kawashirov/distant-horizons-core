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

package com.seibel.lod.api.items.enums.config;

/**
 * NONE, <br>
 * NON_FULL, <br>
 * NO_COLLISION, <br>
 * BOTH, <br>
 *
 * @author Leonardo Amato
 * @version 2022-7-1
 */
public enum EDhApiBlocksToAvoid
{
	// Reminder:
	// when adding items up the API minor version
	// when removing items up the API major version
	
	NONE(false, false),
	
	NON_FULL(true, false),
	
	NO_COLLISION(false, true),
	
	BOTH(true, true);
	
	public final boolean nonFull;
	public final boolean noCollision;
	
	EDhApiBlocksToAvoid(boolean nonFull, boolean noCollision)
	{
		this.nonFull = nonFull;
		this.noCollision = noCollision;
	}
}