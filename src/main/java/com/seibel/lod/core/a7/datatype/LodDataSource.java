package com.seibel.lod.core.a7.datatype;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;
import com.seibel.lod.core.a7.util.IOUtil;
import com.seibel.lod.core.objects.DHChunkPos;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface LodDataSource {
    DhSectionPos getSectionPos();
    byte getDataDetail();
    void setLocalVersion(int localVer);
    byte getDataVersion();

    void update(ChunkSizedData data);

    // Saving related
    void saveData(ILevel level, DataMetaFile file, OutputStream dataStream) throws IOException;
}
