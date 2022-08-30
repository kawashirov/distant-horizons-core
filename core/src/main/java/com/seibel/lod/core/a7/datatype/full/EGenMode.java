package com.seibel.lod.core.a7.datatype.full;

public enum EGenMode {
    Empty,
    Surface,
    Feature,
    Complete;
    public static EGenMode get(byte genMode) {
        return EGenMode.values()[genMode];
    }
    public static byte get(EGenMode genMode) {
        return (byte) genMode.ordinal();
    }
}
