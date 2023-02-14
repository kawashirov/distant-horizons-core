package com.seibel.lod.core.level;

import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
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
	
	IFullDataSourceProvider getFileHandler();
	
	/**
	 * Re-creates the color, render data. 
	 * This method should be called after resource packs are changed or LOD settings are modified.
	 */
	void clearRenderDataCache(); // TODO make all methods in this stack named the same
	
}
