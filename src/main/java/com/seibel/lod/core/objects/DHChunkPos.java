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

import java.util.Objects;

import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.util.LevelPosUtil;
import com.seibel.lod.core.util.LodUtil;

public class DHChunkPos {
    public final int x; // Low 32 bits
    public final int z; // High 32 bits

    public DHChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public DHChunkPos(DHBlockPos blockPos) {
        this.x = blockPos.x >> 4; // Same as div 16
        this.z = blockPos.z >> 4; // Same as div 16
    }
    public DHChunkPos(DhBlockPos2D blockPos) {
        this.x = blockPos.x >> 4; // Same as div 16
        this.z = blockPos.z >> 4; // Same as div 16
    }

    public DHBlockPos center() {
        return new DHBlockPos(8 + x << 4, 0, 8 + z << 4);
    }
    public DHBlockPos corner() {
        return new DHBlockPos(x << 4, 0, z << 4);
    }

    public static long toLong(int x, int z) {
        return ((long)x & 0xFFFFFFFFL) << 32 | (long)z & 0xFFFFFFFFL;
    }

    public static int getX(long chunkPos) {
        return (int)(chunkPos >> 32);
    }
    public static int getZ(long chunkPos) {
        return (int)(chunkPos & 0xFFFFFFFFL);
    }

    public DHChunkPos(long packed) {
        this(getX(packed), getZ(packed));
    }

    @Deprecated
    public int getX()
    {
        return x;
    }

    @Deprecated
    public int getZ()
    {
        return z;
    }

    public int getMinBlockX()
    {
        return x << 4;
    }
    public int getMinBlockZ()
    {
        return z << 4;
    }

    public DhBlockPos2D getMinBlockPos() {
        return new DhBlockPos2D(x<<4, z<<4);
    }

    @Deprecated
    public int getRegionX()
    {
    	return LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, x, LodUtil.REGION_DETAIL_LEVEL);
    }

    @Deprecated
    public int getRegionZ()
    {
    	return LevelPosUtil.convert(LodUtil.CHUNK_DETAIL_LEVEL, z, LodUtil.REGION_DETAIL_LEVEL);
    }

    public long getLong() {
    	return toLong(x, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DHChunkPos that = (DHChunkPos) o;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
    	return "DHChunkPos[" + x + ", " + z + "]";
    }


    public static void _DebugCheckPacker(int x, int z, long expected) {
        long packed = toLong(x, z);
        if (packed != expected) {
            throw new IllegalArgumentException("Packed values don't match: " + packed + " != " + expected);
        }
        DHChunkPos pos = new DHChunkPos(packed);
        if (pos.x != x || pos.z != z) {
            throw new IllegalArgumentException("Values after decode don't match: " + pos + " != " + x + ", " + z);
        }
    }

}
