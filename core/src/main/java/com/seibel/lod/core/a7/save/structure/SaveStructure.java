package com.seibel.lod.core.a7.save.structure;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.io.File;

public abstract class SaveStructure implements AutoCloseable {

    public static final String RENDER_CACHE_FOLDER = "cache";
    public static final String DATA_FOLDER = "data";

    protected static final Logger LOGGER = DhLoggerBuilder.getLogger();

    public abstract File tryGetLevelFolder(ILevelWrapper wrapper);

    public abstract File getRenderCacheFolder(ILevelWrapper world);
    public abstract File getDataFolder(ILevelWrapper world);
}

