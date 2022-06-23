package com.seibel.lod.core.a7.datatype;

import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;
import com.seibel.lod.core.a7.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface LodDataSource {
    DhSectionPos getSectionPos();
    byte getDataDetail();
    void setLocalVersion(int localVer);
    byte getDataVersion();


    // Saving related
    void saveData(ILevel level, DataMetaFile file, OutputStream dataStream) throws IOException;
    default File generateFilePathAndName(File levelFolderPath, ILevel level, DhSectionPos sectionPos) {
        return new File(levelFolderPath, String.format("%s_v%d-%s%s", getClass().getSimpleName(), getDataVersion(),
                sectionPos.serialize(), IOUtil.LOD_FILE_EXTENSION));
    }
}
