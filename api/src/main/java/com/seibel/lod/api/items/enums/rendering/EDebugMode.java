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

package com.seibel.lod.api.items.enums.rendering;

/**
 * OFF,									<br>
 * SHOW_WIREFRAME,						<br>
 * SHOW_DETAIL,							<br>
 * SHOW_DETAIL_WIREFRAME,				<br>
 * SHOW_GENMODE,						<br>
 * SHOW_GENMODE_WIREFRAME,				<br>
 * SHOW_OVERLAPPING_QUADS,				<br>
 * SHOW_OVERLAPPING_QUADS_WIREFRAME,	<br>
 *
 * @author Leetom
 * @author James Seibel
 * @version 2022-7-2
 */
public enum EDebugMode
{
	// Reminder:
	// when adding items up the API minor version
	// when removing items up the API major version
	
	
	/** LODs are rendered normally */
	OFF,

	/** LOD draws in wireframe. */
	SHOW_WIREFRAME,
	
	/** LOD colors are based on their detail */
	SHOW_DETAIL,
	
	/** LOD colors are based on their detail, and draws in wireframe. */
	SHOW_DETAIL_WIREFRAME,
	
	/** LOD colors are based on their gen mode. */
	SHOW_GENMODE,
	
	/** LOD colors are based on their gen mode, and draws in wireframe. */
	SHOW_GENMODE_WIREFRAME,

	/** Only draw overlapping LOD quads. */
	SHOW_OVERLAPPING_QUADS,

	/** Only draw overlapping LOD quads, and draws in wireframe. */
	SHOW_OVERLAPPING_QUADS_WIREFRAME;
	
	/** returns the next debug mode */
	// Deprecated: use DebugMode.next() instead
	@Deprecated
	public EDebugMode getNext()
	{
		return next(this);
	}

	public static EDebugMode next(EDebugMode type) {
		switch (type) {
			case OFF: return SHOW_WIREFRAME;
			case SHOW_WIREFRAME: return SHOW_DETAIL;
			case SHOW_DETAIL: return SHOW_DETAIL_WIREFRAME;
			case SHOW_DETAIL_WIREFRAME: return SHOW_GENMODE;
			case SHOW_GENMODE: return SHOW_GENMODE_WIREFRAME;
			case SHOW_GENMODE_WIREFRAME: return SHOW_OVERLAPPING_QUADS;
			case SHOW_OVERLAPPING_QUADS: return SHOW_OVERLAPPING_QUADS_WIREFRAME;
			default: return OFF;
		}
	}

	public static EDebugMode previous(EDebugMode type) {
		switch (type) {
			case OFF: return SHOW_OVERLAPPING_QUADS_WIREFRAME;
			case SHOW_OVERLAPPING_QUADS_WIREFRAME: return SHOW_OVERLAPPING_QUADS;
			case SHOW_OVERLAPPING_QUADS: return SHOW_GENMODE_WIREFRAME;
			case SHOW_GENMODE_WIREFRAME: return SHOW_GENMODE;
			case SHOW_GENMODE: return SHOW_DETAIL_WIREFRAME;
			case SHOW_DETAIL_WIREFRAME: return SHOW_DETAIL;
			case SHOW_DETAIL: return SHOW_WIREFRAME;
			default: return OFF;
		}
	}
}
