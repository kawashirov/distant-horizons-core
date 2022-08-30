package com.seibel.lod.core.objects;

import com.seibel.lod.core.util.DataPointUtil;

public final class LodDataView {
    private final long[] data;
    private final int size;
    private final int offset;
    public LodDataView(long[] data, int size, int offset) {
        this.data = data;
        this.size = size;
        this.offset = offset;
    }
    public long get(int index) {
        return data[index + offset];
    }
    public void set(int index, long value) {
        data[index + offset] = value;
    }
    public int size() {
        return size;
    }
    public void copyTo(long[] target, int offset) {
        System.arraycopy(data, this.offset, target, offset, size);
    }
    public void copyTo(LodDataView target) {
        System.arraycopy(data, this.offset, target.data, target.offset, size);
    }

    public boolean mergeWith(LodDataView source, int verticalSize, boolean override) {
        if (size != source.size) {
            throw new IllegalArgumentException("Cannot merge views of different sizes");
        }
        boolean anyChange = false;
        for (int o=0; o<(source.size()*verticalSize); o+=verticalSize) {
            if (override) {
                if (DataPointUtil.compareDatapointPriority(source.get(o), get(o)) >= 0) {
                    anyChange = true;
                    System.arraycopy(source.data, source.offset+o, data, offset+o, verticalSize);
                }
            } else {
                if (DataPointUtil.compareDatapointPriority(source.get(o), get(o)) > 0) {
                    anyChange = true;
                    System.arraycopy(source.data, source.offset+o, data, offset+o, verticalSize);
                }
            }
        }
        return anyChange;

    }
}