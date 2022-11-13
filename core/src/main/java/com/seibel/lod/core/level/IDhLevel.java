package com.seibel.lod.core.level;

import com.seibel.lod.core.file.datafile.DataFileHandler;
import com.seibel.lod.core.file.datafile.IDataSourceProvider;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.util.concurrent.CompletableFuture;

public interface IDhLevel extends AutoCloseable
{
    int getMinY();
    CompletableFuture<Void> save();
	
    void dumpRamUsage();
	
    /** May return either a client or server level wrapper. */
    ILevelWrapper getLevelWrapper();
	
    void updateChunk(IChunkWrapper chunk);
	
	IDataSourceProvider getFileHandler();
	
}
