package com.seibel.lod.core.util;

public class Reference<T> {
    public T v;
    public Reference() {}
    public Reference(T v) {
        this.v = v;
    }
    public T swap(T v) {
        T old = this.v;
        this.v = v;
        return old;
    }
    public boolean isEmpty() {
        return v == null;
    }
}
