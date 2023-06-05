package com.seibel.lod.core.dataObjects.transformers;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.config.listeners.ConfigChangeListener;
import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.lod.core.dataObjects.render.ColumnRenderLoader;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.file.fullDatafile.FullDataFileHandler;
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
    
	private static ExecutorService transformerThreadPool = null;
	private static ConfigChangeListener<Integer> configListener;
	
	
	
	//==============//
	// transformers //
	//==============//
	
    public static CompletableFuture<ColumnRenderSource> transformDataSourceAsync(IFullDataSource fullDataSource, IDhClientLevel level)
	{
        return CompletableFuture.supplyAsync(() -> transform(fullDataSource, level), transformerThreadPool);
    }
	
    public static CompletableFuture<ColumnRenderSource> transformDataSourceAsync(CompletableFuture<IFullDataSource> fullDataSourceFuture, IDhClientLevel level)
	{
        return fullDataSourceFuture.thenApplyAsync((fullDataSource) -> transform(fullDataSource, level), transformerThreadPool);
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
		// static setup
		if (configListener == null)
		{
			configListener = new ConfigChangeListener<>(Config.Client.Advanced.Threading.numberOfDataConverterThreads, (threadCount) -> { setThreadPoolSize(threadCount); });
		}
		
		
		if (transformerThreadPool == null || transformerThreadPool.isTerminated())
		{
			LOGGER.info("Starting "+DataRenderTransformer.class.getSimpleName());
			setThreadPoolSize(Config.Client.Advanced.Threading.numberOfDataConverterThreads.get());
		}
	}
	public static void setThreadPoolSize(int threadPoolSize) { transformerThreadPool = ThreadUtil.makeThreadPool(threadPoolSize, "Data/Render Transformer"); }
	
	/** 
	 * Stops any executing tasks and destroys the executor. <br>
	 * Does nothing if the executor isn't running.
	 */
	public static void shutdownExecutorService()
	{
		if (transformerThreadPool != null)
		{
			LOGGER.info("Stopping "+DataRenderTransformer.class.getSimpleName());
			transformerThreadPool.shutdownNow();
		}
	}
	
}
