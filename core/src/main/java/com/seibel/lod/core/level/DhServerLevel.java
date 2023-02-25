package com.seibel.lod.core.level;

import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.util.FileScanUtil;
import com.seibel.lod.core.file.fullDatafile.FullDataFileHandler;
import com.seibel.lod.core.file.structure.LocalSaveStructure;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class DhServerLevel implements IDhServerLevel
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	public final LocalSaveStructure save;
	public final FullDataFileHandler dataFileHandler;
	public final IServerLevelWrapper level;
	
	public DhServerLevel(LocalSaveStructure save, IServerLevelWrapper level)
	{
		this.save = save;
		this.level = level;
		save.getFullDataFolder(level).mkdirs();
		this.dataFileHandler = new FullDataFileHandler(this, save.getFullDataFolder(level)); //FIXME: GenerationQueue
		FileScanUtil.scanFiles(save, level, this.dataFileHandler, null);
		LOGGER.info("Started DHLevel for {} with saves at {}", level, save);
	}
	
	public void serverTick()
	{
		//Nothing for now
	}
	
	@Override
	public int getMinY() { return this.level.getMinHeight(); }
	
	@Override
	public void dumpRamUsage()
	{
		//TODO
	}
	
	@Override
	public void close()
	{
		this.dataFileHandler.close();
		LOGGER.info("Closed DHLevel for {}", this.level);
	}
	
	@Override
	public CompletableFuture<Void> saveAsync() { return this.dataFileHandler.flushAndSave(); }
	
	@Override
	public void doWorldGen()
	{
		// FIXME: No world gen for server side only for now
	}
	
	@Override
	public IServerLevelWrapper getServerLevelWrapper() { return this.level; }
	
	@Override
	public ILevelWrapper getLevelWrapper() { return this.level; }
	
	@Override
	public IFullDataSourceProvider getFileHandler() { return this.dataFileHandler; }
	
	@Override 
	public void clearRenderDataCache()
	{
		// Do nothing, there is no render data on the server
	}
	
	@Override
	public void updateChunkAsync(IChunkWrapper chunk)
	{
		//TODO
	}
	
}
