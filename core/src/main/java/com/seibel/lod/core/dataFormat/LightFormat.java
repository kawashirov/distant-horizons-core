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

public class LightFormat
{
	public final static byte INT_BLOCK_LIGHT_SHIFT = 16;
	public final static byte INT_SKY_LIGHT_SHIFT = 0;
	
	public final static byte BYTE_BLOCK_LIGHT_SHIFT = 4;
	public final static byte BYTE_SKY_LIGHT_SHIFT = 0;
	
	public final static byte BLOCK_LIGHT_MASK = 0b1111;
	public final static byte SKY_LIGHT_MASK = 0b1111;
	
	
	public static byte formatLightAsByte(byte skyLight, byte blockLight)
	{
		return (byte) (((skyLight & SKY_LIGHT_MASK) << (BYTE_SKY_LIGHT_SHIFT + 4)) | ((blockLight & BLOCK_LIGHT_MASK) << (BYTE_BLOCK_LIGHT_SHIFT + 4)));
	}
	
	public static int formatLightAsInt(byte skyLight, byte blockLight)
	{
		return ((skyLight & SKY_LIGHT_MASK) << INT_SKY_LIGHT_SHIFT) | ((blockLight & BLOCK_LIGHT_MASK) << INT_BLOCK_LIGHT_SHIFT);
	}
	
	public static int convertByteToIntFormat(byte lights)
	{
		return formatLightAsInt((byte) ((lights >>> BYTE_SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK), (byte) ((lights >>> BYTE_BLOCK_LIGHT_SHIFT) & BLOCK_LIGHT_MASK));
	}
	
	public static byte getSkyLight(byte lights)
	{
		return (byte) ((lights >>> BYTE_SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK);
	}
	
	public static byte getBlockLight(byte lights)
	{
		return (byte) ((lights >>> BYTE_BLOCK_LIGHT_SHIFT) & BLOCK_LIGHT_MASK);
	}
}
