package com.seibel.lod.core.a7.save.io.file;

import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.util.LodUtil;

import java.io.File;

public class RemoteDataFileHandler extends DataFileHandler {
    public RemoteDataFileHandler(ILevel level, File saveRootDir) {
        super(level, saveRootDir, (pos) -> {
            LodUtil.assertNotReach("TODO");
            return null;
        });
    }
}
