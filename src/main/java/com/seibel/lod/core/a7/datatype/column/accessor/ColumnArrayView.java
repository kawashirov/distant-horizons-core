package com.seibel.lod.core.a7.datatype.column.accessor;


import java.util.Arrays;

public final class ColumnArrayView implements IColumnDataView {
    final long[] data;
    final int size; // size in longs
    final int offset; // offset in longs
    final int vertSize; // vertical size in longs

    public ColumnArrayView(long[] data, int size, int offset, int vertSize) {
        this.data = data;
        this.size = size;
        this.offset = offset;
        this.vertSize = vertSize;
    }
    @Override
    public long get(int index) {
        return data[index + offset];
    }
    public void set(int index, long value) {
        data[index + offset] = value;
    }
    @Override
    public int size() {
        return size;
    }

    @Override
    public int verticalSize() {
        return vertSize;
    }
    @Override
    public int dataCount() {
        return size / vertSize;
    }

    @Override
    public ColumnArrayView subView(int dataIndexStart, int dataCount) {
        return new ColumnArrayView(data, dataCount * vertSize, offset + dataIndexStart * vertSize, vertSize);
    }

    public void fill(long value) {
        Arrays.fill(data, offset, offset + size, value);
    }

    public void copyFrom(IColumnDataView source, int outputDataIndexOffset) {
        if (source.verticalSize() > vertSize) throw new IllegalArgumentException("source verticalSize must be <= self's verticalSize to copy");
        if (source.dataCount() + outputDataIndexOffset > dataCount()) throw new IllegalArgumentException("dataIndexStart + source.dataCount() must be <= self.dataCount() to copy");
        if (source.verticalSize() != vertSize) {
            for (int i = 0; i < source.dataCount(); i++) {
                int outputOffset = offset + outputDataIndexOffset * vertSize + i * vertSize;
                source.subView(i, 1).copyTo(data, outputOffset, source.verticalSize());
                Arrays.fill(data, outputOffset + source.verticalSize(),
                        outputOffset + vertSize, 0);
            }
        } else {
            source.copyTo(data, offset + outputDataIndexOffset * vertSize, source.size());
        }
    }
    public void copyFrom(IColumnDataView source) {
        copyFrom(source, 0);
    }

    @Override
    public void copyTo(long[] target, int offset, int size) {
        System.arraycopy(data, this.offset, target, offset, size);
    }

    public boolean mergeWith(ColumnArrayView source, boolean override) {
        if (size != source.size) {
            throw new IllegalArgumentException("Cannot merge views of different sizes");
        }
        if (vertSize != source.vertSize) {
            throw new IllegalArgumentException("Cannot merge views of different vertical sizes");
        }
        boolean anyChange = false;
        for (int o=0; o<(source.size()*vertSize); o+=vertSize) {
            if (override) {
                if (ColumnFormat.compareDatapointPriority(source.get(o), get(o)) >= 0) {
                    anyChange = true;
                    System.arraycopy(source.data, source.offset+o, data, offset+o, vertSize);
                }
            } else {
                if (ColumnFormat.compareDatapointPriority(source.get(o), get(o)) > 0) {
                    anyChange = true;
                    System.arraycopy(source.data, source.offset+o, data, offset+o, vertSize);
                }
            }
        }
        return anyChange;
    }

    public void changeVerticalSizeFrom(IColumnDataView source) {
        if (dataCount() != source.dataCount()) {
            throw new IllegalArgumentException("Cannot copy and resize to views with different dataCounts");
        }
        if (vertSize >= source.verticalSize()) {
            copyFrom(source);
        } else {
            for (int i=0; i<dataCount(); i++) {
                ColumnFormat.mergeMultiData(source.subView(i, 1), subView(i, 1));
            }
        }
    }

    public void mergeMultiDataFrom(IColumnDataView source) {
        if (dataCount() != 1) {
            throw new IllegalArgumentException("output dataCount must be 1");
        }
        ColumnFormat.mergeMultiData(source, this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("S:");
        sb.append(size);
        sb.append(" V:");
        sb.append(vertSize);
        sb.append(" O:");
        sb.append(offset);
        sb.append(" [");
        for (int i=0; i<size; i++) {
            sb.append(ColumnFormat.toString(data[offset+i]));
            if (i < size-1) sb.append(",\n");
        }
        sb.append("]");
        return sb.toString();
    }
}