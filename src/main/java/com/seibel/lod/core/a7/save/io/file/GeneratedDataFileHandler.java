package com.seibel.lod.core.a7.save.io.file;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.generation.GenerationQueue;
import com.seibel.lod.core.a7.level.IServerLevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class GeneratedDataFileHandler extends DataFileHandler {

    public GeneratedDataFileHandler(IServerLevel level, File saveRootDir, GenerationQueue queue) {
        super(level, saveRootDir, dataSourceCreator);
    }
}
