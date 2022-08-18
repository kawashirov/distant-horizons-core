package com.seibel.lod.core.a7.generation;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.util.CombinableResult;

import java.util.ArrayList;

public class GenerationResult implements CombinableResult<GenerationResult> {
    public final ArrayList<ChunkSizedData> dataList = new ArrayList<>();

    @Override
    public GenerationResult combineWith(GenerationResult b, GenerationResult c, GenerationResult d) {
        dataList.ensureCapacity(dataList.size() + b.dataList.size() + c.dataList.size() + d.dataList.size());
        dataList.addAll(b.dataList);
        dataList.addAll(c.dataList);
        dataList.addAll(d.dataList);
        return this;
    }
}
