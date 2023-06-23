package com.seibel.distanthorizons.core.dataObjects.transformers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.listeners.ConfigChangeListener;
import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.ConfigBasedLogger;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.util.*;
import org.apache.logging.log4j.LogManager;

public class ChunkToLodBuilder implements AutoCloseable
{
	public static final ConfigBasedLogger LOGGER = new ConfigBasedLogger(LogManager.getLogger(), () -> Config.Client.Advanced.Logging.logLodBuilderEvent.get());
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	public static final long MAX_TICK_TIME_NS = 1000000000L / 20L;
	
	
	private final ConcurrentHashMap<DhChunkPos, IChunkWrapper> latestChunkToBuild = new ConcurrentHashMap<>();
	private final ConcurrentLinkedDeque<Task> taskToBuild = new ConcurrentLinkedDeque<>();
	private final AtomicInteger runningCount = new AtomicInteger(0);
	
	private int threadCount = -1;
	private ExecutorService executorThreadPool = null;
	private ConfigChangeListener<Integer> configListener;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public ChunkToLodBuilder() { this.setupExecutorService(); }
	
	
	
	//=================//
	// data generation //
	//=================//
	
	public CompletableFuture<ChunkSizedFullDataAccessor> tryGenerateData(IChunkWrapper chunkWrapper)
	{
		if (chunkWrapper == null)
		{
			throw new NullPointerException("ChunkWrapper cannot be null!");
		}
		
		IChunkWrapper oldChunk = this.latestChunkToBuild.put(chunkWrapper.getChunkPos(), chunkWrapper); // an Exchange operation
		// If there's old chunk, that means we just replaced an unprocessed old request on generating data on this pos.
		//   if so, we can just return null to signal this, as the old request's future will instead be the proper one
		//   that will return the latest generated data.
		if (oldChunk != null)
		{
			return null;
		}
		
		// Otherwise, it means we're the first to do so. Let's submit our task to this entry.
		CompletableFuture<ChunkSizedFullDataAccessor> future = new CompletableFuture<>();
		this.taskToBuild.addLast(new Task(chunkWrapper.getChunkPos(), future));
		return future;
	}
	
	public void tick()
	{
		if (this.runningCount.get() >= this.threadCount)
		{
			return;
		}
		else if (this.taskToBuild.isEmpty())
		{
			return;
		}
		else if (!MC.playerExists())
		{
			// MC hasn't finished loading (or is currently unloaded)
			
			// can be uncommented if tasks aren't being cleared correctly
			//this.clearCurrentTasks();
			return;
		}
		
		
		for (int i = 0; i< this.threadCount; i++)
		{
			this.runningCount.incrementAndGet();
			CompletableFuture.runAsync(() ->
			{
				try
				{
					this._tick();
				}
				finally
				{
					this.runningCount.decrementAndGet();
				}
			}, this.executorThreadPool);
		}
	}
	private void _tick()
	{
		long time = System.nanoTime();
		int count = 0;
		boolean allDone = false;
		while (true)
		{
			// run until we either run out of time, or all tasks are complete
			if (System.nanoTime() - time > MAX_TICK_TIME_NS && !this.taskToBuild.isEmpty())
			{
				break;
			}
			
			Task task = this.taskToBuild.pollFirst();
			if (task == null)
			{
				allDone = true;
				break;
			}
			
			count++;
			IChunkWrapper latestChunk = this.latestChunkToBuild.remove(task.chunkPos); // Basically an Exchange operation
			if (latestChunk == null)
			{
				LOGGER.error("Somehow Task at "+task.chunkPos+" has latestChunk as null. Skipping task.");
				task.future.complete(null);
				continue;
			}
			
			try
			{
				if (LodDataBuilder.canGenerateLodFromChunk(latestChunk))
				{
					ChunkSizedFullDataAccessor data = LodDataBuilder.createChunkData(latestChunk);
					if (data != null)
					{
						task.future.complete(data);
						continue;
					}
				}
			}
			catch (Exception ex)
			{
				LOGGER.error("Error while processing Task at "+task.chunkPos, ex);
			}
			
			// Failed to build due to chunk not meeting requirement.
			IChunkWrapper casChunk = this.latestChunkToBuild.putIfAbsent(task.chunkPos, latestChunk); // CAS operation with expected=null
			if (casChunk == null || latestChunk.isStillValid()) // That means CAS have been successful
			{
				this.taskToBuild.addLast(task); // Then add back the same old task.
			}
			else // Else, it means someone managed to sneak in a new gen request in this pos. Then lets drop this old task.
			{
				task.future.complete(null);
			}
			
			count--;
		}
		
		long time2 = System.nanoTime();
		if (!allDone)
		{
			//LOGGER.info("Completed {} tasks in {} in this tick", count, Duration.ofNanos(time2 - time));
		}
		else if (count > 0)
		{
			//LOGGER.info("Completed all {} tasks in {}", count, Duration.ofNanos(time2 - time));
		}
	}
	
	/**
	 * should be called whenever changing levels/worlds 
	 * to prevent trying to generate LODs for chunk(s) that are no longer loaded
	 * (which can cause exceptions)
	 */
	public void clearCurrentTasks()
	{
		this.taskToBuild.clear();
		this.latestChunkToBuild.clear();
	}
	
	
	
	//==========================//
	// executor handler methods //
	//==========================//
	
	/**
	 * Creates a new executor. <br>
	 * Does nothing if an executor already exists.
	 */
	public void setupExecutorService()
	{
		// static setup
		if (this.configListener == null)
		{
			this.configListener = new ConfigChangeListener<>(Config.Client.Advanced.MultiThreading.numberOfChunkLodConverterThreads, (threadCount) -> { this.setThreadPoolSize(threadCount); });
		}
		
		
		if (this.executorThreadPool == null || this.executorThreadPool.isTerminated())
		{
			LOGGER.info("Starting "+ChunkToLodBuilder.class.getSimpleName());
			this.setThreadPoolSize(Config.Client.Advanced.MultiThreading.numberOfChunkLodConverterThreads.get());
		}
	}
	public void setThreadPoolSize(int threadPoolSize)
	{
		this.threadCount = threadPoolSize;
		this.executorThreadPool = ThreadUtil.makeThreadPool(threadPoolSize, ChunkToLodBuilder.class);
	}
	
	/**
	 * Stops any executing tasks and destroys the executor. <br>
	 * Does nothing if the executor isn't running.
	 */
	public void shutdownExecutorService()
	{
		if (this.executorThreadPool != null)
		{
			LOGGER.info("Stopping "+ChunkToLodBuilder.class.getSimpleName());
			this.executorThreadPool.shutdownNow();
		}
	}
	
	
	
	//==============//
	// base methods //
	//==============//
	
	@Override
	public void close()
	{
		this.shutdownExecutorService();
		this.clearCurrentTasks();
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	private static class Task
	{
		final DhChunkPos chunkPos;
		final CompletableFuture<ChunkSizedFullDataAccessor> future;
		
		Task(DhChunkPos chunkPos, CompletableFuture<ChunkSizedFullDataAccessor> future)
		{
			this.chunkPos = chunkPos;
			this.future = future;
		}
	}
	
}
