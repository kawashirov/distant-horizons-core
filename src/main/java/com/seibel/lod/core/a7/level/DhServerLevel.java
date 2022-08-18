package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.a7.util.FileScanner;
import com.seibel.lod.core.a7.save.io.file.DataFileHandler;
import com.seibel.lod.core.a7.save.structure.LocalSaveStructure;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class DhServerLevel implements IServerLevel {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();

    public final LocalSaveStructure save;
    public final DataFileHandler dataFileHandler;
    public final IServerLevelWrapper level;

    public DhServerLevel(LocalSaveStructure save, IServerLevelWrapper level) {
        this.save = save;
        this.level = level;
        save.getDataFolder(level).mkdirs();
        dataFileHandler = new DataFileHandler(this, save.getDataFolder(level), null); //FIXME: GenerationQueue
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
    public IServerLevelWrapper getServerLevelWrapper() {
        return level;
    }
}
