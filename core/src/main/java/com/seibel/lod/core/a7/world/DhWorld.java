package com.seibel.lod.core.a7.world;

import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public abstract class DhWorld implements Closeable
{
    protected static final Logger LOGGER = DhLoggerBuilder.getLogger();

    public final WorldEnvironment environment;

    protected DhWorld(WorldEnvironment environment) {
        this.environment = environment;
    }
    public abstract ILevel getOrLoadLevel(ILevelWrapper wrapper);

    public abstract ILevel getLevel(ILevelWrapper wrapper);
    public abstract Iterable<? extends ILevel> getAllLoadedLevels();
    
    public abstract void unloadLevel(ILevelWrapper wrapper);
    public abstract CompletableFuture<Void> saveAndFlush();

    @Override
    public abstract void close();
}
