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
 * LOW <br>
 * MEDIUM <br>
 * HIGH <br>
 * ULTRA <br>
 *
 * @author Leonardo Amato
 * @version 2023-2-5
 */
public enum EVerticalQuality
{
//	HEIGHT_MAP(new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }), // commented out for now since it causes issues with transparency, may be re-added if the transparency issue is fixed
	LOW(new int[] { 4, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1 }),
	MEDIUM(new int[] { 6, 4, 3, 2, 2, 1, 1, 1, 1, 1, 1 }),
	HIGH(new int[] { 8, 6, 4, 2, 2, 2, 2, 1, 1, 1, 1 }),
	ULTRA(new int[] { 16, 8, 4, 2, 2, 2, 2, 1, 1, 1, 1 });
	
	/** represents how many LODs can be rendered in a single vertical slice */
	public final int[] maxVerticalData;
	
	
	
	EVerticalQuality(int[] maxVerticalData) { this.maxVerticalData = maxVerticalData; }
	
	
	
    public int calculateMaxVerticalData(byte dataDetail)
	{
		if (dataDetail >= this.maxVerticalData.length)
		{
			dataDetail = (byte) (this.maxVerticalData.length - 1);
		}
		return this.maxVerticalData[dataDetail];
    }
	
}