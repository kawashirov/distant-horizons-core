package com.seibel.lod.core.a7.datatype.full.accessor;

import com.seibel.lod.core.a7.datatype.full.IdBiomeBlockStateMap;
import com.seibel.lod.core.util.LodUtil;

import java.util.Iterator;

public interface IFullDataView {
    IdBiomeBlockStateMap getMapping();

    SingleFullArrayView get(int index);
    SingleFullArrayView get(int x, int z);
    int width();
    default Iterator<SingleFullArrayView> iterator() {
        return new Iterator<SingleFullArrayView>() {
            private int index = 0;
            private final int size = width()*width();
            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public SingleFullArrayView next() {
                LodUtil.assertTrue(hasNext(), "No more data to iterate!");
                return get(index++);
            }
        };
    }
    IFullDataView subView(int size, int ox, int oz);
}
