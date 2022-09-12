package com.seibel.lod.core.pos;

import com.seibel.lod.core.util.LodUtil;

public class DhBlockPos2D {
    public final int x;
    public final int z;
    public DhBlockPos2D(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public DhBlockPos2D(DhBlockPos blockPos) {
        this.x = blockPos.x;
        this.z = blockPos.z;
    }

    public DhBlockPos2D add(DhBlockPos2D other) {
        return new DhBlockPos2D(x + other.x, z + other.z);
    }
    public DhBlockPos2D subtract(DhBlockPos2D other) {
        return new DhBlockPos2D(x - other.x, z - other.z);
    }
    public double dist(DhBlockPos2D other) {
        return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(z - other.z, 2));
    }
    public long distSquared(DhBlockPos2D other) {
        return LodUtil.pow2((long)x - other.x) + LodUtil.pow2((long)z - other.z);
    }

    public Pos2D toPos2D() {
        return new Pos2D(x, z);
    }

    public static DhBlockPos2D fromPos2D(Pos2D pos) {
        return new DhBlockPos2D(pos.x, pos.y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + z + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DhBlockPos2D) {
            DhBlockPos2D other = (DhBlockPos2D)obj;
            return x == other.x && z == other.z;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(x) ^ Integer.hashCode(z);
    }
}
