package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.a7.datatype.full.accessor.FullArrayView;

public class ChunkSizedData extends FullArrayView {
    public final byte dataDetail;
    public final int minX;
    public final int minZ;
    public ChunkSizedData(byte dataDetail, int minX, int minZ) {
        super(new IdBiomeBlockStateMap(), new long[16*16][0], 16);
        this.dataDetail = dataDetail;
        this.minX = minX;
        this.minZ = minZ;
    }

    public void setSingleColumn(long[] data, int x, int z) {
        dataArrays[x*16+z] = data;
    }
}