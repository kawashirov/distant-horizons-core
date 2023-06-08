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

package com.seibel.lod.api.enums.rendering;

/**
 * OFF,								<br>
 * SHOW_DETAIL,						<br>
 * SHOW_GENMODE,					<br>
 * SHOW_OVERLAPPING_QUADS,			<br>
 * SHOW_RENDER_SOURCE_FLAG,			<br>
 *
 * @author Leetom
 * @author James Seibel
 * @version 2023-6-7
 */
public enum EDebugMode
{
	// Reminder:
	// when adding items up the API minor version
	// when removing items up the API major version
	
	
	/** LODs are rendered normally */
	OFF,
	
	/** LOD colors are based on their detail */
	SHOW_DETAIL,
	
	/** LOD colors are based on their gen mode. */
	SHOW_GENMODE,
	
	/** Only draw overlapping LOD quads. */
	SHOW_OVERLAPPING_QUADS,
	
	/** LOD colors are based on renderSource flags. */
	SHOW_RENDER_SOURCE_FLAG;
	
	
	public static EDebugMode next(EDebugMode type) 
	{
		switch (type) 
		{
			case OFF: return SHOW_DETAIL;
			case SHOW_DETAIL: return SHOW_GENMODE;
			case SHOW_GENMODE: return SHOW_OVERLAPPING_QUADS;
			case SHOW_OVERLAPPING_QUADS: return SHOW_RENDER_SOURCE_FLAG;
			default: return OFF;
		}
	}

	public static EDebugMode previous(EDebugMode type) 
	{
		switch (type) 
		{
			case OFF: return SHOW_RENDER_SOURCE_FLAG;
			case SHOW_RENDER_SOURCE_FLAG: return SHOW_OVERLAPPING_QUADS;
			case SHOW_OVERLAPPING_QUADS: return SHOW_GENMODE;
			case SHOW_GENMODE: return SHOW_DETAIL;
			default: return OFF;
		}
	}
}
