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
 
package com.seibel.lod.core.dataFormat;

public class ColorFormat
{
	public final static int BLUE_SHIFT = 0;
	public final static int GREEN_SHIFT = BLUE_SHIFT + 8;
	public final static int RED_SHIFT = BLUE_SHIFT + 16;
	public final static int ALPHA_SHIFT = BLUE_SHIFT + 24;
	
	public final static long ALPHA_MASK = 0b1111;
	public final static long RED_MASK = 0b1111_1111;
	public final static long GREEN_MASK = 0b1111_1111;
	public final static long BLUE_MASK = 0b1111_1111;
	
	public static int createColorData(int alpha, int red, int green, int blue)
	{
		int colorData = 0;
		colorData += (alpha & ALPHA_MASK) << ALPHA_SHIFT;
		colorData += (red & RED_MASK) << RED_SHIFT;
		colorData += (green & GREEN_MASK) << GREEN_SHIFT;
		colorData += (blue & BLUE_MASK) << BLUE_SHIFT;
		
		return colorData;
	}
	
	public static short getAlpha(long dataPoint)
	{
		return (short) ((dataPoint >>> ALPHA_SHIFT) & ALPHA_MASK);
	}
	
	public static short getRed(long dataPoint)
	{
		return (short) ((dataPoint >>> RED_SHIFT) & RED_MASK);
	}
	
	public static short getGreen(long dataPoint)
	{
		return (short) ((dataPoint >>> GREEN_SHIFT) & GREEN_MASK);
	}
	
	public static short getBlue(long dataPoint)
	{
		return (short) ((dataPoint >>> BLUE_SHIFT) & BLUE_MASK);
	}
}
