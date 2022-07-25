package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.util.FileScanner;
import com.seibel.lod.core.a7.save.io.file.LocalDataFileHandler;
import com.seibel.lod.core.a7.save.structure.LocalSaveStructure;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class DhServerLevel implements IServerLevel {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();

    public final LocalSaveStructure save;
    public final LocalDataFileHandler dataFileHandler;
    public final ILevelWrapper level;

    public DhServerLevel(LocalSaveStructure save, ILevelWrapper level) {
        this.save = save;
        this.level = level;
        save.getDataFolder(level).mkdirs();
        dataFileHandler = new LocalDataFileHandler(this, save.getDataFolder(level));
        FileScanner.scanFile(save, level, dataFileHandler, null);
        LOGGER.info("Started DHLevel for {} with saves at {}", level, save);
    }

    public void serverTick() {
        //Nothing for now
    }

    @Override
    public int getMinY() {
        return level.getMinHeight();
    }

    @Override
    public void dumpRamUsage() {
        //TODO
    }
    @Override
    public void close() {
        dataFileHandler.close();
        LOGGER.info("Closed DHLevel for {}", level);
    }
    @Override
    public CompletableFuture<Void> save() {
        return dataFileHandler.flushAndSave();
    }

    @Override
    public void doWorldGen() {
        // FIXME: No world gen for server side only for now
    }

    @Override
    public ILevelWrapper getLevelWrapper() {
        return level;
    }
}
