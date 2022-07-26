package com.seibel.lod.core.a7.pos;

import com.seibel.lod.core.util.LodUtil;

import java.util.Objects;

public class DhLodPos {
    public final byte detail;
    public final int x;
    public final int z;

    public DhLodPos(byte detail, int x, int z) {
        this.detail = detail;
        this.x = x;
        this.z = z;
    }

    public String toString() {
        return "[" + detail + "*" + x + "," + z + "]";
    }

    public DhLodUnit getX() {
        return new DhLodUnit(detail, x);
    }

    public DhLodUnit getZ() {
        return new DhLodUnit(detail, z);
    }

    public int getWidth() {
        return 1 << detail;
    }
    public static int getWidth(byte detail) {
        return 1 << detail;
    }

    public DhBlockPos2D getCenter() {
        return new DhBlockPos2D(getX().toBlock() + (getWidth() >> 1), getZ().toBlock() + (getWidth() >> 1));
    }
    public DhBlockPos2D getCorner() {
        return new DhBlockPos2D(getX().toBlock(), getZ().toBlock());
    }

    public DhLodPos convertUpwardsTo(byte newDetail) {
        LodUtil.assertTrue(newDetail >= detail);
        return new DhLodPos(newDetail, Math.floorDiv(x, 1<<(newDetail-detail)), Math.floorDiv(z, 1<<(newDetail-detail)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DhLodPos dhLodPos = (DhLodPos) o;
        return detail == dhLodPos.detail && x == dhLodPos.x && z == dhLodPos.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(detail, x, z);
    }

    public boolean overlaps(DhLodPos other) {
        if (equals(other)) return true;
        if (detail == other.detail) return false;
        if (detail > other.detail) {
            return this.equals(other.convertUpwardsTo(this.detail));
        } else {
            return other.equals(this.convertUpwardsTo(other.detail));
        }
    }

    public DhLodPos add(DhLodUnit width) {
        if (width.detail < detail) throw new IllegalArgumentException("add called with width.detail < pos detail");
        return new DhLodPos(detail, x + width.convertTo(detail).value, z + width.convertTo(detail).value);
    }
}
