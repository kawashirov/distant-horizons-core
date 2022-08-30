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

import com.seibel.lod.core.a7.pos.DhBlockPos2D;

import java.util.Objects;

public class DHBlockPos {
    public static final boolean DO_CHECKS = false;

    // 26 bits wide as that just encompasses the maximum possible value
    // of +- 30,000,000 blocks in each direction. Yes this packing method
    // is how Minecraft packs it.

    // NOTE: Remember to ALWAYS check that DHBlockPos packing is EXACTLY
    // the same as Minecraft's!!!!
    public static final int PACKED_X_LENGTH = 26;
    public static final int PACKED_Z_LENGTH = 26;
    public static final int PACKED_Y_LENGTH = 12;
    public static final long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
    public static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
    public static final long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;
    public static final int PACKED_Y_OFFSET = 0;
    public static final int PACKED_Z_OFFSET = PACKED_Y_LENGTH;
    public static final int PACKED_X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;

    public final int x;
    public final int y;
    public final int z;

    public DHBlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public DHBlockPos() {
        this(0, 0, 0);
    }
    public DHBlockPos(DHBlockPos pos) {
        this(pos.x, pos.y, pos.z);
    }

    public DHBlockPos(DhBlockPos2D pos, int y) {
        this(pos.x, y, pos.z);
    }

    @Deprecated
    public int getX()
    {
        return x;
    }
    @Deprecated
    public int getY()
    {
        return y;
    }
    @Deprecated
    public int getZ()
    {
        return z;
    }

    public static long asLong(int x, int y, int z) {
        if (DO_CHECKS) {
            if ((x & ~PACKED_X_MASK) != 0) {
                throw new IllegalArgumentException("x is out of range: " + x);
            }
            if ((y & ~PACKED_Y_MASK) != 0) {
                throw new IllegalArgumentException("y is out of range: " + y);
            }
            if ((z & ~PACKED_Z_MASK) != 0) {
                throw new IllegalArgumentException("z is out of range: " + z);
            }
        }
        return ((long)x & PACKED_X_MASK) << PACKED_X_OFFSET |
                ((long)y & PACKED_Y_MASK) << PACKED_Y_OFFSET |
                ((long)z & PACKED_Z_MASK) << PACKED_Z_OFFSET;
    }

    public static int getX(long packed) { // X is at the top
        return (int)(packed <<(64 - PACKED_X_OFFSET - PACKED_X_LENGTH) >> (64 - PACKED_X_LENGTH));
    }
    public static int getY(long packed) { // Y is at the bottom
        return (int)(packed <<(64 - PACKED_Y_OFFSET - PACKED_Y_LENGTH) >> (64 - PACKED_Y_LENGTH));
    }
    public static int getZ(long packed) { // Z is at the middle
        return (int)(packed <<(64 - PACKED_Z_OFFSET - PACKED_Z_LENGTH) >> (64 - PACKED_Z_LENGTH));
    }
    public DHBlockPos(long packed) {
        this(getX(packed), getY(packed), getZ(packed));
    }

    public long asLong() {
        return asLong(x, y, z);
    }

    public DHBlockPos offset(int x, int y, int z)
    {
        return new DHBlockPos(this.x + x, this.y + y, this.z + z);
    }

    public int getManhattanDistance(DHBlockPos otherPos)
    {
        return Math.abs(this.getX() - otherPos.getX()) + Math.abs(this.getY() - otherPos.getY()) + Math.abs(this.getZ() - otherPos.getZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DHBlockPos that = (DHBlockPos) o;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
    @Override
    public String toString() {
        return "DHBlockPos[" +
                "" + x +
                ", " + y +
                ", " + z +
                ']';
    }

    public static void _DebugCheckPacker(int x, int y, int z, long expected) {
        long packed = asLong(x, y, z);
        if (packed != expected) {
            throw new IllegalArgumentException("Packed values don't match: " + packed + " != " + expected);
        }
        DHBlockPos pos = new DHBlockPos(packed);
        if (pos.getX() != x || pos.getY() != y || pos.getZ() != z) {
            throw new IllegalArgumentException("Values after decode don't match: " + pos + " != " + x + ", " + y + ", " + z);
        }
    }
}
