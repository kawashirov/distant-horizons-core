package com.seibel.lod.core.a7.datatype.full.accessor;

import com.seibel.lod.core.a7.datatype.full.FullFormat;
import com.seibel.lod.core.a7.datatype.full.IdBiomeBlockStateMap;

public class FullArrayView implements IFullDataView {
    protected final long[][] dataArrays;
    protected final int offset;
    protected final int size;
    protected final int dataSize;
    protected final IdBiomeBlockStateMap mapping;

    public FullArrayView(IdBiomeBlockStateMap mapping, long[][] dataArrays, int size) {
        if (dataArrays.length != size*size)
            throw new IllegalArgumentException(
                    "tried constructing dataArrayView with invalid input!");
        this.dataArrays = dataArrays;
        this.size = size;
        this.dataSize = size;
        this.mapping = mapping;
        offset = 0;
    }
    public FullArrayView(FullArrayView source, int size, int offsetX, int offsetZ) {
        if (source.size < size || source.size < size+offsetX || source.size < size+offsetZ)
            throw new IllegalArgumentException(
                    "tried constructing dataArrayView subview with invalid input!");
        dataArrays = source.dataArrays;
        this.size = size;
        this.dataSize = source.dataSize;
        mapping = source.mapping;
        offset = source.offset + offsetX * dataSize + offsetZ;
    }

    @Override
    public IdBiomeBlockStateMap getMapping() {
        return mapping;
    }

    @Override
    public SingleFullArrayView get(int index) {
        return get(index/size, index%size);
    }

    @Override
    public SingleFullArrayView get(int x, int z) {
        return new SingleFullArrayView(mapping, dataArrays, x*size + z + offset);
    }

    @Override
    public int width() {
        return size;
    }

    @Override
    public FullArrayView subView(int size, int ox, int oz) {
        return new FullArrayView(this, size, ox, oz);
    }

    //WARNING: It will potentially share the underlying array object!
    public void shadowCopyTo(FullArrayView target) {
        if (target.size != size)
            throw new IllegalArgumentException("Target view must have same size as this view");
        if (target.mapping.equals(mapping)) {
            for (int x = 0; x < size; x++) {
                System.arraycopy(dataArrays, offset + x * dataSize,
                        target.dataArrays, target.offset + x * target.dataSize, size);
            }
        }
        else {
            int[] map = target.mapping.computeAndMergeMapFrom(mapping);
            for (int x = 0; x < size; x++) {
                for (int o=0; o<size; o++) {
                    long[] sourceData = dataArrays[offset + x * dataSize + o];
                    long[] newData = new long[sourceData.length];
                    for (int i = 0; i < newData.length; i++) {
                        newData[i] = FullFormat.remap(map, sourceData[i]);
                    }
                    target.dataArrays[target.offset + x * target.dataSize + o] = newData;
                }
            }
        }
    }
}
