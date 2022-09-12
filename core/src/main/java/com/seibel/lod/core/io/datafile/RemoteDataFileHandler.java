package com.seibel.lod.core.io.datafile;

import com.seibel.lod.core.level.ILevel;

import java.io.File;

public class RemoteDataFileHandler extends DataFileHandler {
    public RemoteDataFileHandler(ILevel level, File saveRootDir) {
        super(level, saveRootDir);
    }
}
