package com.seibel.lod.core.a7.datatype;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;

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
