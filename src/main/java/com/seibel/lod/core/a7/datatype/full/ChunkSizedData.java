package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.a7.datatype.full.accessor.FullArrayView;

public class ChunkSizedData extends FullArrayView {
    public final byte dataDetail;
    public final int x;
    public final int z;
    public ChunkSizedData(byte dataDetail, int x, int z) {
        super(new IdBiomeBlockStateMap(), new long[16*16][0], 16);
        this.dataDetail = dataDetail;
        this.x = x;
        this.z = z;
    }

    public void setSingleColumn(long[] data, int x, int z) {
        dataArrays[x*16+z] = data;
    }
}