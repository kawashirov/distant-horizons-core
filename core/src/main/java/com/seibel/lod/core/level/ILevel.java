package com.seibel.lod.core.level;

import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.util.concurrent.CompletableFuture;

public interface ILevel extends AutoCloseable
{
    int getMinY();
    CompletableFuture<Void> save();

    void dumpRamUsage();
    
    /** May return either a client or server level wrapper. */
    ILevelWrapper getLevelWrapper();

    void updateChunk(IChunkWrapper chunk);
}
