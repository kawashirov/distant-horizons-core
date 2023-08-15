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

package com.seibel.distanthorizons.api.enums.config;

/**
 * LOWEST <br>
 * LOW <br>
 * MEDIUM <br>
 * HIGH <br>
 * UNLIMITED <br>
 */
public enum EHorizontalQuality
{
	// Reminder:
	// when adding items up the API minor version
	// when removing items up the API major version
	
	
	// FIXME any quadraticBase less than 2.0f has issues with DetailDistanceUtil, and will always return the lowest detail level.
	//  So for now we are limiting the lowest value to 2.0
	//  LOWEST was originally 1.0f and LOW was 1.5f
	
	LOWEST(2.0f, 4),
	LOW(2.0f, 8),
	MEDIUM(2.0f, 12),
	HIGH(2.2f, 24),
	EXTREME(2.4f, 64),
	
	UNLIMITED(-1, -1);
	
	
	
	public final double quadraticBase;
	public final int distanceUnitInBlocks;
	
	EHorizontalQuality(double quadraticBase, int distanceUnitInBlocks)
	{
		this.quadraticBase = quadraticBase;
		this.distanceUnitInBlocks = distanceUnitInBlocks;
	}
	
}