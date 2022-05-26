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

package com.seibel.lod.core.enums.rendering;

/**
 * off, detail, detail wireframe
 * 
 * @author James Seibel
 * @version 8-28-2021
 */
public enum DebugMode
{
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
	public DebugMode getNext()
	{
		return next(this);
	}

	public static DebugMode next(DebugMode type) {
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

	public static DebugMode previous(DebugMode type) {
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
