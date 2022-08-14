package com.seibel.lod.core.a7;

import com.seibel.lod.core.a7.datatype.PlaceHolderRenderSource;

@Deprecated
public interface PlaceHolderQueue {
    void track(PlaceHolderRenderSource source); // Note: Implementation should only track a weak reference to the source.
}
