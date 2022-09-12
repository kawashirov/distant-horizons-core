package com.seibel.lod.core.datatype;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.level.ILevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.io.datafile.DataMetaFile;

import java.io.IOException;
import java.io.OutputStream;

public interface LodDataSource {
    DhSectionPos getSectionPos();
    byte getDataDetail();
    byte getDataVersion();

    void update(ChunkSizedData data);

    // Saving related
    void saveData(ILevel level, DataMetaFile file, OutputStream dataStream) throws IOException;
}
