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

public class VerticalDataFormat
{
	public final static short MIN_WORLD_HEIGHT = -2048;
	public final static short MAX_WORLD_HEIGHT = 2047;
	
	public final static byte HEIGHT_SHIFT = 20;
	public final static byte DEPTH_SHIFT = 8;
	public final static byte LEVEL_SHIFT = 3;
	public final static byte BOTTOM_TYPE_SHIFT = 2;
	public final static byte TRANSPARENCY_SHIFT = 1;
	public final static byte EXISTENCE_SHIFT = 0;
	
	
	public final static int FULL_MASK = ~0;
	
	public final static int HEIGHT_MASK = 0b1111_1111_1111;
	public final static int DEPTH_MASK = 0b1111_1111_1111;
	public final static int LEVEL_MASK = 0b111;
	public final static int TRANSPARENCY_MASK = 0b1;
	public final static int BOTTOM_TYPE_MASK = 0b1;
	public final static int EXISTENCE_MASK = 0b1;
	
	
	public final static int HEIGHT_RESET = ~(HEIGHT_MASK << HEIGHT_SHIFT);
	public final static int DEPTH_RESET = ~(DEPTH_MASK << DEPTH_SHIFT);
	public final static int LEVEL_RESET = ~(LEVEL_MASK << LEVEL_SHIFT);
	public final static int TRANSPARENCY_RESET = ~(TRANSPARENCY_MASK << BOTTOM_TYPE_SHIFT);
	public final static int BOTTOM_TYPE_RESET = ~(BOTTOM_TYPE_MASK << TRANSPARENCY_SHIFT);
	public final static int EXISTENCE_RESET = ~(EXISTENCE_MASK << EXISTENCE_SHIFT);
	
	public final static int EMPTY_LOD = 0;
	
	
	public static int createVerticalData(int height, int depth, int level, boolean transparent, boolean bottom)
	{
		int verticalData = 0;
		verticalData |= (height & HEIGHT_MASK) << HEIGHT_SHIFT;
		verticalData |= (depth & DEPTH_MASK) << DEPTH_SHIFT;
		verticalData |= (level & LEVEL_MASK) << LEVEL_SHIFT;
		if (bottom)
			verticalData |= BOTTOM_TYPE_MASK << BOTTOM_TYPE_SHIFT;
		if (transparent)
			verticalData |= TRANSPARENCY_MASK << TRANSPARENCY_SHIFT;
		verticalData |= EXISTENCE_MASK << EXISTENCE_SHIFT;
		
		return verticalData;
	}
	
	public static short getHeight(int verticalData)
	{
		return (short) ((verticalData >>> HEIGHT_SHIFT) & HEIGHT_MASK);
	}
	
	public static short getDepth(int verticalData)
	{
		return (short) ((verticalData >>> DEPTH_SHIFT) & DEPTH_MASK);
	}
	
	public static byte getLevel(int verticalData)
	{
		return (byte) ((verticalData >>> LEVEL_SHIFT) & LEVEL_MASK);
	}
	
	public static boolean isTransparent(int verticalData)
	{
		return ((verticalData >>> TRANSPARENCY_SHIFT) & TRANSPARENCY_MASK) == 1;
	}
	
	public static boolean isBottom(int verticalData)
	{
		return ((verticalData >>> BOTTOM_TYPE_SHIFT) & BOTTOM_TYPE_MASK) == 1;
	}
	
	public static boolean doesItExist(int verticalData)
	{
		return (((verticalData >>> EXISTENCE_SHIFT) & EXISTENCE_MASK) == 1);
	}
	
	
	public static int setHeight(int verticalData, int height)
	{
		return verticalData | ((height & HEIGHT_MASK) << HEIGHT_SHIFT);
	}
	
	public static int setDepth(int verticalData, int depth)
	{
		return verticalData | ((depth & DEPTH_MASK) << DEPTH_SHIFT);
	}
	
	public static int setLevel(int verticalData, int level)
	{
		return verticalData | ((level & LEVEL_MASK) << LEVEL_SHIFT);
	}
	
	public static int setTransparency(int verticalData)
	{
		return verticalData | ((TRANSPARENCY_MASK) << TRANSPARENCY_SHIFT);
	}
	
	public static int setBottom(int verticalData)
	{
		return verticalData | ((BOTTOM_TYPE_MASK) << BOTTOM_TYPE_SHIFT);
	}
	
	public static int setExistence(int verticalData)
	{
		return verticalData | ((EXISTENCE_MASK) << EXISTENCE_SHIFT);
	}
}
