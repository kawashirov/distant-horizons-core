package com.seibel.lod.core.world;

import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractDhWorld implements Closeable
{
    protected static final Logger LOGGER = DhLoggerBuilder.getLogger();

    public final EWorldEnvironment environment;

    protected AbstractDhWorld(EWorldEnvironment environment) {
        this.environment = environment;
    }
    public abstract IDhLevel getOrLoadLevel(ILevelWrapper wrapper);

    public abstract IDhLevel getLevel(ILevelWrapper wrapper);
    public abstract Iterable<? extends IDhLevel> getAllLoadedLevels();
    
    public abstract void unloadLevel(ILevelWrapper wrapper);
    public abstract CompletableFuture<Void> saveAndFlush();

    @Override
    public abstract void close();
	
}
