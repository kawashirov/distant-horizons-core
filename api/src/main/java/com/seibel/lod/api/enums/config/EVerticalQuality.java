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
 * heightmap <br>
 * multi_lod <br>
 *
 * @author Leonardo Amato
 * @version 2023-2-5
 */
public enum EVerticalQuality
{
	HEIGHT_MAP(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }),
	LOW(new int[] { 4, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1 }),
	MEDIUM(new int[] { 6, 4, 3, 2, 2, 1, 1, 1, 1, 1, 1 }),
	HIGH(new int[] { 8, 6, 4, 2, 2, 2, 2, 1, 1, 1, 1 }),
	ULTRA(new int[] { 16, 8, 4, 2, 2, 2, 2, 1, 1, 1, 1 });
	
	public final int[] maxVerticalData;
	
	
	
	EVerticalQuality(int[] maxVerticalData) { this.maxVerticalData = maxVerticalData; }
	
	
	
	/** returns null if out of range */
	public static EVerticalQuality previous(EVerticalQuality mode)
	{
		switch (mode)
		{
		case ULTRA:
			return EVerticalQuality.HIGH;
		case HIGH:
			return EVerticalQuality.MEDIUM;
		case MEDIUM:
			return EVerticalQuality.LOW;
		case LOW:
		default:
			return null;
		}
	}
	
	/** returns null if out of range */
	public static EVerticalQuality next(EVerticalQuality mode)
	{
		switch (mode)
		{
		case MEDIUM:
			return EVerticalQuality.HIGH;
		case LOW:
			return EVerticalQuality.MEDIUM;
		case HIGH:
			return EVerticalQuality.ULTRA;
		case ULTRA:
		default:
			return null;
		}
	}
	
    public int calculateMaxVerticalData(byte dataDetail)
	{
		if (dataDetail >= this.maxVerticalData.length)
		{
			dataDetail = (byte) (this.maxVerticalData.length - 1);
		}
		return this.maxVerticalData[dataDetail];
    }
	
}