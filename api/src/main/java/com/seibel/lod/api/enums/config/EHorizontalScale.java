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

package com.seibel.lod.api.enums.config;

/**
 * Low <br>
 * Medium <br>
 * High <br>
 * <br>
 * this is a quality scale for the detail drop-off
 * 
 * @author Leonardo Amato
 * @version 9-25-2021
 */
public enum EHorizontalScale
{
	/** Lods are 2D with heightMap */
	LOW(64),
	
	/** Lods expand in three dimension */
	MEDIUM(128),
	
	/** Lods expand in three dimension */
	HIGH(256);
	
	public final int distanceUnit;
	
	EHorizontalScale(int distanceUnit)
	{
		this.distanceUnit = distanceUnit;
	}
}