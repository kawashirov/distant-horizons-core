package com.seibel.lod.core.pos;

public class DhLodUnit {
    public final byte detail;
    public final int value;

    public DhLodUnit(byte detail, int value) {
        this.detail = detail;
        this.value = value;
    }

    public int toBlock() {
        return value << detail;
    }

    public static DhLodUnit fromBlock(int block, byte targetDetail) {
        return new DhLodUnit(targetDetail, Math.floorDiv(block, 1<<targetDetail));
    }

    public DhLodUnit convertTo(byte targetDetail) {
        if (detail == targetDetail) {
            return this;
        }
        if (detail > targetDetail) { //TODO check if this is correct
            return new DhLodUnit(targetDetail, value << (detail - targetDetail));
        }
        return new DhLodUnit(targetDetail,  Math.floorDiv(value, 1<<(targetDetail-detail)));
    }
}
