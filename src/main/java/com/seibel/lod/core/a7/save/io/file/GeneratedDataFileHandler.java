package com.seibel.lod.core.a7.save.io.file;

import com.seibel.lod.core.a7.generation.GenerationQueue;
import com.seibel.lod.core.a7.level.IServerLevel;

import java.io.File;

public class GeneratedDataFileHandler extends DataFileHandler {
    public GeneratedDataFileHandler(IServerLevel level, File saveRootDir, GenerationQueue queue) {
        super(level, saveRootDir, queue::generate);
    }
}
