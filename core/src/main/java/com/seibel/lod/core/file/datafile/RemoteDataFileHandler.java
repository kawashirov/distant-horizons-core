package com.seibel.lod.core.file.datafile;

import com.seibel.lod.core.level.IDhLevel;

import java.io.File;

public class RemoteDataFileHandler extends DataFileHandler {
    public RemoteDataFileHandler(IDhLevel level, File saveRootDir) {
        super(level, saveRootDir);
    }
}
