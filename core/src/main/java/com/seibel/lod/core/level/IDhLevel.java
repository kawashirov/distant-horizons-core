package com.seibel.lod.core.level;

import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.file.structure.AbstractSaveStructure;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.util.concurrent.CompletableFuture;

public interface IDhLevel extends AutoCloseable
{
    int getMinY();
    CompletableFuture<Void> saveAsync();
	
    void dumpRamUsage();
	
    /** 
	 * May return either a client or server level wrapper. <br>
	 * Should not return null
	 */
    ILevelWrapper getLevelWrapper();
	
    void updateChunkAsync(IChunkWrapper chunk);
	
	IFullDataSourceProvider getFileHandler();

	AbstractSaveStructure getSaveStructure();


	
}
