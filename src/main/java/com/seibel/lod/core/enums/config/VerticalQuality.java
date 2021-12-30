/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.enums.config;

import org.jetbrains.annotations.Nullable;

/**
 * heightmap <br>
 * multi_lod <br>
 * 
 * @author Leonardo Amato
 * @version 10-07-2021
 */
public enum VerticalQuality
{
	LOW(
			new int[] { 2,
					2,
					2,
					2,
					1,
					1,
					1,
					1,
					1,
					1,
					1 }
	),
	
	MEDIUM(
			new int[] { 4,
					4,
					2,
					2,
					2,
					1,
					1,
					1,
					1,
					1,
					1 }
	),
	
	HIGH(
			new int[] {
					8,
					8,
					4,
					4,
					2,
					2,
					2,
					1,
					1,
					1,
					1 }
	);
	
	public final int[] maxVerticalData;
	
	VerticalQuality(int[] maxVerticalData)
	{
		this.maxVerticalData = maxVerticalData;
	}
	
	// Note: return null if out of range
	@Nullable
	public static VerticalQuality previous(VerticalQuality mode) {
		switch (mode) {
		case HIGH:
			return VerticalQuality.MEDIUM;
		case MEDIUM:
			return VerticalQuality.LOW;
		case LOW:
			return null;
		default:
			return null;
		}
	}

	// Note: return null if out of range
	@Nullable
	public static VerticalQuality next(VerticalQuality mode) {
		switch (mode) {
		case HIGH:
			return null;
		case MEDIUM:
			return VerticalQuality.HIGH;
		case LOW:
			return VerticalQuality.MEDIUM;
		default:
			return null;
		}
	}
}