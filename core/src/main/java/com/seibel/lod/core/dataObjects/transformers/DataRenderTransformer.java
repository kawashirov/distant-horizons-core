package com.seibel.lod.core.dataObjects.transformers;

import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.lod.core.dataObjects.render.ColumnRenderLoader;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.ThreadUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/** TODO: Merge this with {@link FullDataToRenderDataTransformer} */
public class DataRenderTransformer
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
    
	private static ExecutorService transformerThreads = null;
	
	
	//==============//
	// transformers //
	//==============//
	
    public static CompletableFuture<ColumnRenderSource> transformDataSourceAsync(IFullDataSource fullDataSource, IDhClientLevel level)
	{
        return CompletableFuture.supplyAsync(() -> transform(fullDataSource, level), transformerThreads);
    }
	
    public static CompletableFuture<ColumnRenderSource> transformDataSourceAsync(CompletableFuture<IFullDataSource> fullDataSourceFuture, IDhClientLevel level)
	{
        return fullDataSourceFuture.thenApplyAsync((fullDataSource) -> transform(fullDataSource, level), transformerThreads);
    }
	
    private static ColumnRenderSource transform(IFullDataSource fullDataSource, IDhClientLevel level)
	{
        if (fullDataSource == null)
		{
			return null;
		}
		else if (MC.getWrappedClientWorld() == null)
		{
			// if the client is no longer loaded in the world, render sources cannot be created 
			return null;
		}
		
		try
		{
			return ColumnRenderLoader.INSTANCE.createRenderSource(fullDataSource, level);
		}
        catch (InterruptedException e)
		{
			return null;
		}
    }
	
	
	
	//==========================//
	// executor handler methods //
	//==========================//
	
	/**
	 * Creates a new executor. <br>
	 * Does nothing if an executor already exists.
	 */	
	public static void setupExecutorService()
	{
		if (transformerThreads == null || transformerThreads.isTerminated())
		{
			LOGGER.info("Starting "+DataRenderTransformer.class.getSimpleName());
			// TODO add config option to set pool size
			transformerThreads = ThreadUtil.makeThreadPool(4, "Data/Render Transformer");
		}
	}
	
	/** 
	 * Stops any executing tasks and destroys the executor. <br>
	 * Does nothing if the executor isn't running.
	 */
	public static void shutdownExecutorService()
	{
		if (transformerThreads != null)
		{
			LOGGER.info("Stopping "+DataRenderTransformer.class.getSimpleName());
			transformerThreads.shutdownNow();
		}
	}
	
}
