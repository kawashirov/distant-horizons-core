package com.seibel.lod.core.a7;

import com.seibel.lod.core.a7.datatype.column.ColumnRenderLoader;
import com.seibel.lod.core.a7.datatype.full.FullDataLoader;
import com.seibel.lod.core.a7.datatype.full.SparseDataLoader;

public class Initializer {
    public static void init() {
        ColumnRenderLoader unused = new ColumnRenderLoader(); // Auto register into the loader system
        FullDataLoader unused2 = new FullDataLoader(); // Auto register into the loader system
        SparseDataLoader unused3 = new SparseDataLoader(); // Auto register
    }
}
