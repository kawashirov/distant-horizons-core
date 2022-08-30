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

public class PositionDataFormat
{
	public final static byte LOD_COUNT_SHIFT = 6;
	public final static byte CORRECT_LIGHT_SHIFT = 5;
	public final static byte GEN_TYPE_SHIFT = 2;
	public final static byte VOID_SHIFT = 1;
	public final static byte EXISTENCE_SHIFT = 0;
	
	//We are able to count up to 64 different lods in a column
	public final static short LOD_COUNT_MASK = 0b11_1111;
	public final static short CORRECT_LIGHT_MASK = 0b1;
	public final static short GEN_TYPE_MASK = 0b111;
	public final static short VOID_MASK = 0b1;
	public final static short EXISTENCE_MASK = 0b1;
	
	public final static int EMPTY_DATA = 0;
	public final static int VOID_DATA = VOID_MASK<<VOID_SHIFT + EXISTENCE_MASK<<EXISTENCE_SHIFT;
	
	public static short createVoidPositionData(byte generationMode)
	{
		short positionData = 0;
		positionData |= (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
		positionData |= VOID_MASK << VOID_SHIFT;
		positionData |= EXISTENCE_MASK << EXISTENCE_SHIFT;
		
		return positionData;
	}
	
	public static short createPositionData(int lodCount, boolean correctLight, byte generationMode)
	{
		short positionData = 0;
		positionData |= (lodCount & LOD_COUNT_MASK) << LOD_COUNT_SHIFT;
		positionData |= (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
		if (correctLight)
			positionData |= CORRECT_LIGHT_MASK << CORRECT_LIGHT_SHIFT;
		positionData |= EXISTENCE_MASK << EXISTENCE_SHIFT;
		
		return positionData;
	}
	
	public static byte getLodCount(short dataPoint)
	{
		return (byte) ((dataPoint >>> LOD_COUNT_SHIFT) & LOD_COUNT_MASK);
	}
	public static boolean getFlag(short dataPoint)
	{
		return ((dataPoint >>> CORRECT_LIGHT_SHIFT) & CORRECT_LIGHT_MASK) == 1;
	}
	public static byte getGenerationMode(short dataPoint)
	{
		return (byte) ((dataPoint >>> GEN_TYPE_SHIFT) & GEN_TYPE_MASK);
	}
	public static boolean isVoid(short dataPoint)
	{
		return (((dataPoint >>> VOID_SHIFT) & VOID_MASK) == 1);
	}
	public static boolean doesItExist(short dataPoint)
	{
		return (((dataPoint >>> EXISTENCE_SHIFT) & EXISTENCE_MASK) == 1);
	}
	
	public static short setLodCount(short dataPoint, short lodCount)
	{
		return (short) (dataPoint | ((lodCount & LOD_COUNT_MASK) << LOD_COUNT_SHIFT));
	}
	public static short setFlag(short dataPoint)
	{
		return (short) (dataPoint | ((CORRECT_LIGHT_MASK) << CORRECT_LIGHT_SHIFT));
	}
	public static short setGenerationMode(short dataPoint, byte generationMode)
	{
		return (short) (dataPoint | ((generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT));
	}
	public static short setVoid(short dataPoint)
	{
		return (short) (dataPoint | (VOID_MASK << VOID_SHIFT));
	}
	public static short setExistence(short dataPoint)
	{
		return (short) (dataPoint | (EXISTENCE_MASK << EXISTENCE_SHIFT));
	}
	
}
