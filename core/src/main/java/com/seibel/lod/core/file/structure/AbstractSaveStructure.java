package com.seibel.lod.core.file.structure;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Abstract class for determining where LOD data should be saved to.
 * 
 * @version 2022-12-17
 */
public abstract class AbstractSaveStructure implements AutoCloseable
{
    public static final String RENDER_CACHE_FOLDER = "cache";
    public static final String DATA_FOLDER = "data";
	
    protected static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	/** 
	 * Attempts to return the folder that contains LOD data for the given {@link ILevelWrapper}.
	 * If no appropriate folder exists, one will be created. <br><br>
	 * 
	 * This will always return a folder, however that folder may not be the best match
	 * if multiverse support is enabled.
	 * */
    public abstract File tryGetOrCreateLevelFolder(ILevelWrapper wrapper);
	
	/** Will return null if no parent folder exists for the given {@link ILevelWrapper}.*/
    public abstract File getRenderCacheFolder(ILevelWrapper world);
	/** Will return null if no parent folder exists for the given {@link ILevelWrapper}.*/
    public abstract File getDataFolder(ILevelWrapper world);
	
}

