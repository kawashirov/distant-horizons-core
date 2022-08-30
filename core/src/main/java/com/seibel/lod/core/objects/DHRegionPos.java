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

package com.seibel.lod.core.objects;

import com.seibel.lod.core.util.LodUtil;
/**
 * This object is similar to ChunkPos or BlockPos.
 * 
 * @author James Seibel
 * @version 8-21-2021
 */
public class DHRegionPos
{

	public int x;
	public int z;
	
	/** Sets x and z to 0 */
	public DHRegionPos()
	{
		x = 0;
		z = 0;
	}
	
	/** simple constructor that sets x and z to new x and z. */
	public DHRegionPos(int newX, int newZ)
	{
		x = newX;
		z = newZ;
	}
	
	/** Converts from a BlockPos to a RegionPos */
	public DHRegionPos(DHBlockPos pos)
	{
		x = Math.floorDiv(pos.x >> 4, LodUtil.REGION_WIDTH_IN_CHUNKS);
		z = Math.floorDiv(pos.z >> 4, LodUtil.REGION_WIDTH_IN_CHUNKS);
	}
	
	/** Converts from a ChunkPos to a RegionPos */
	public DHRegionPos(DHChunkPos pos)
	{
		x = Math.floorDiv(pos.getX(), LodUtil.REGION_WIDTH_IN_CHUNKS);
		z = Math.floorDiv(pos.getZ(), LodUtil.REGION_WIDTH_IN_CHUNKS);
	}

	public DHChunkPos centerChunkPos() {
		return new DHChunkPos(x * LodUtil.REGION_WIDTH_IN_CHUNKS + LodUtil.REGION_WIDTH_IN_CHUNKS / 2,
				z * LodUtil.REGION_WIDTH_IN_CHUNKS + LodUtil.REGION_WIDTH_IN_CHUNKS / 2);
	}
	public DHChunkPos cornerChunkPos() {
		return new DHChunkPos(x * LodUtil.REGION_WIDTH_IN_CHUNKS, z * LodUtil.REGION_WIDTH_IN_CHUNKS);
	}
	public DHBlockPos centerBlockPos() {
		return new DHBlockPos(x * LodUtil.REGION_WIDTH_IN_CHUNKS * 16 + LodUtil.REGION_WIDTH_IN_CHUNKS * 16 / 2,
				0, z * LodUtil.REGION_WIDTH_IN_CHUNKS * 16 + LodUtil.REGION_WIDTH_IN_CHUNKS * 16 / 2);
	}
	public DHBlockPos cornerBlockPos() {
		return new DHBlockPos(x * LodUtil.REGION_WIDTH_IN_CHUNKS * 16,
				0, z * LodUtil.REGION_WIDTH_IN_CHUNKS * 16);
	}
	
	@Override
	public boolean equals(Object o) {
		// If the object is compared with itself then return true 
        if (o == this) {
            return true;
        }
        // Check if o is an instance of RegionPos or not
        if (!(o instanceof DHRegionPos)) {
            return false;
        }
        DHRegionPos c = (DHRegionPos) o;
        return c.x==x &&c.z==z;
	}
	
	
	@Override
	public String toString()
	{
		return "(" + x + "," + z + ")";
	}
	
    public static long asLong(int i, int j) {
        return (long)i & 0xFFFFFFFFL | ((long)j & 0xFFFFFFFFL) << 32;
    }
    public static int getX(long l) {
        return (int)(l & 0xFFFFFFFFL);
    }
    public static int getZ(long l) {
        return (int)(l >>> 32 & 0xFFFFFFFFL);
    }
    
	@Override
	public int hashCode() {
		return Long.hashCode(asLong(x,z));
	}
}
