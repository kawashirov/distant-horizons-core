package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;

public interface IServerLevel extends ILevel {
    void serverTick();
    void doWorldGen();
}
