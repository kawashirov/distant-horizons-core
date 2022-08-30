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

@Deprecated //Unused
public class DataMergeUtil
{
	public static void shrinkArray(short[] array, int packetSize, int start, int length, int arraySize)
	{
		start *= packetSize;
		length *= packetSize;
		arraySize *= packetSize;
		for (int i = 0; i < arraySize - start; i++)
		{
			array[start + i] = array[start + length + i];
			//remove comment to not leave garbage at the end
			//array[start + packetSize + i] = 0;
		}
	}
	
	public static void extendArray(short[] array, int packetSize, int start, int length, int arraySize)
	{
		start *= packetSize;
		length *= packetSize;
		arraySize *= packetSize;
		for (int i = arraySize - start - 1; i >= 0; i--)
		{
			array[start + length + i] = array[start + i];
			array[start + i] = 0;
		}
	}
}
