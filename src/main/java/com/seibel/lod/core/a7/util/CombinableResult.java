package com.seibel.lod.core.a7.util;

public interface CombinableResult<T> {
    T combineWith(T b, T c, T d);
}
