package com.seibel.lod.core.a7.pos;

import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.util.LodUtil;

import java.util.function.Consumer;

public class DhSectionPos {
    public final byte sectionDetail;
    public final int sectionX; // in sectionDetail level grid
    public final int sectionZ; // in sectionDetail level grid

    public DhSectionPos(byte sectionDetail, int sectionX, int sectionZ) {
        this.sectionDetail = sectionDetail;
        this.sectionX = sectionX;
        this.sectionZ = sectionZ;
    }

    public DhLodPos getCenter(byte returnDetailLevel) {
        LodUtil.assertTrue(returnDetailLevel <= sectionDetail, "returnDetailLevel must be less than sectionDetail");
        if (returnDetailLevel == sectionDetail)
            return new DhLodPos(sectionDetail, sectionX, sectionZ);
        byte offset = (byte) (sectionDetail - returnDetailLevel);
        return new DhLodPos(returnDetailLevel, (sectionX << offset)+(1 << (offset -1)),
                (sectionZ << offset)+(1 << (offset -1)));
    }
    public DhLodPos getCorner(byte returnDetailLevel) {
        LodUtil.assertTrue(returnDetailLevel <= sectionDetail, "returnDetailLevel must be less than sectionDetail");
        byte offset = (byte) (sectionDetail - returnDetailLevel);
        return new DhLodPos(returnDetailLevel, sectionX << offset, sectionZ << offset);
    }
    public DhLodUnit getWidth(byte returnDetailLevel) {
        LodUtil.assertTrue(returnDetailLevel <= sectionDetail, "returnDetailLevel must be less than sectionDetail");
        byte offset = (byte) (sectionDetail - returnDetailLevel);
        return new DhLodUnit(sectionDetail, 1 << offset);
    }
    public DhLodPos getCenter() {
        return getCenter((byte)0);
    }
    public DhLodPos getCorner() {
        return getCorner((byte) (sectionDetail-1));
    }
    public DhLodUnit getWidth() {
        return getWidth(sectionDetail);
    }

    public DhSectionPos getChild(int child0to3){
        if (child0to3 < 0 || child0to3 > 3) throw new IllegalArgumentException("child0to3 must be between 0 and 3");
        if (sectionDetail <= 0) throw new IllegalStateException("section detail must be greater than 0");
        return new DhSectionPos((byte) (sectionDetail - 1),
                sectionX * 2 + (child0to3 & 1),
                sectionZ * 2 + ((child0to3 & 2) >> 1));
    }

    public void forEachChild(Consumer<DhSectionPos> callback){
        for (int i = 0; i < 4; i++) {
            callback.accept(getChild(i));
        }
    }

    public DhSectionPos getParent(){
        return new DhSectionPos((byte) (sectionDetail + 1), sectionX >> 1, sectionZ >> 1);
    }

    public DhSectionPos getAdjacent(ELodDirection dir) {
        return new DhSectionPos(sectionDetail, sectionX + dir.getNormal().x, sectionZ + dir.getNormal().z);
    }

    public DhLodPos getSectionBBoxPos() {
        return new DhLodPos(sectionDetail, sectionX, sectionZ);
    }

    /**
     *  NOTE: This does not consider yOffset!
     */
    public boolean overlaps(DhSectionPos other){
        return getSectionBBoxPos().overlaps(other.getSectionBBoxPos());
    }

    @Override
    public String toString() {
        return "{" + sectionDetail +
                "*" + sectionX +
                "," + sectionZ +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DhSectionPos that = (DhSectionPos) o;
        return sectionDetail == that.sectionDetail &&
                sectionX == that.sectionX &&
                sectionZ == that.sectionZ;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(sectionDetail) ^
                Integer.hashCode(sectionX) ^
                Integer.hashCode(sectionZ);
    }

    // Serialize() is different from toString() as this requires it to NEVER be changed, and should be in a short format
    public String serialize() {
        return "[" + sectionDetail + ',' + sectionX + ',' + sectionZ + ']';
    }
}
