package com.seibel.lod.core.a7.pos;

import com.seibel.lod.core.util.LodUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DhLodPos implements Comparable<DhLodPos> {
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
    public int getWidth(byte detail) {
        LodUtil.assertTrue(detail <= this.detail);
        return 1 << (this.detail - detail);
    }

    public static int blockWidth(byte detail) {
        return 1 << detail;
    }

    public DhBlockPos2D getCenter() {
        return new DhBlockPos2D(getX().toBlock() + (getWidth() >> 1), getZ().toBlock() + (getWidth() >> 1));
    }
    public DhBlockPos2D getCorner() {
        return new DhBlockPos2D(getX().toBlock(), getZ().toBlock());
    }

    public DhLodPos getCorner(byte newDetail) {
        LodUtil.assertTrue(newDetail <= detail);
        return new DhLodPos(newDetail, x << (detail-newDetail), z << (detail-newDetail));
    }

    public DhLodPos convertUpwardsTo(byte newDetail) {
        LodUtil.assertTrue(newDetail >= detail);
        return new DhLodPos(newDetail, Math.floorDiv(x, 1<<(newDetail-detail)), Math.floorDiv(z, 1<<(newDetail-detail)));
    }
    public DhLodPos getChild(int child0to3) {
        if (child0to3 < 0 || child0to3 > 3) throw new IllegalArgumentException("child0to3 must be between 0 and 3");
        if (detail <= 0) throw new IllegalStateException("detail must be greater than 0");
        return new DhLodPos((byte) (detail - 1),
                x * 2 + (child0to3 & 1),
                z * 2 + ((child0to3 & 2) >> 1));
    }
    public int getChildIndexOfParent() {
        return (x & 1) + ((z & 1) << 1);
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

    @Override
    public int compareTo(@NotNull DhLodPos o) {
        return detail != o.detail ? Integer.compare(detail, o.detail) : x != o.x ? Integer.compare(x, o.x) : Integer.compare(z, o.z);
    }
}
