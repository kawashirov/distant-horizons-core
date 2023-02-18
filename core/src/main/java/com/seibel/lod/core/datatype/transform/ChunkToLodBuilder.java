package com.seibel.lod.core.datatype.transform;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.core.datatype.full.ChunkSizedFullDataSource;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.util.*;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import org.apache.logging.log4j.LogManager;

//FIXME: To-Be-Used class
public class ChunkToLodBuilder
{
    public static final ConfigBasedLogger LOGGER = new ConfigBasedLogger(LogManager.getLogger(), () -> Config.Client.Advanced.Debugging.DebugSwitch.logLodBuilderEvent.get());
    public static final long MAX_TICK_TIME_NS = 1000000000L / 20L;
    public static final int THREAD_COUNT = 1;
	
    private static class Task
	{
        final DhChunkPos chunkPos;
        final CompletableFuture<ChunkSizedFullDataSource> future;
		
        Task(DhChunkPos chunkPos, CompletableFuture<ChunkSizedFullDataSource> future)
		{
            this.chunkPos = chunkPos;
            this.future = future;
        }
    }
    private final ConcurrentHashMap<DhChunkPos, IChunkWrapper> latestChunkToBuild = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<Task> taskToBuild = new ConcurrentLinkedDeque<>();
    private final ExecutorService executor = LodUtil.makeThreadPool(THREAD_COUNT, ChunkToLodBuilder.class);
    private final AtomicInteger runningCount = new AtomicInteger(0);
	
	
	
    public CompletableFuture<ChunkSizedFullDataSource> tryGenerateData(IChunkWrapper chunk)
	{
        if (chunk == null) 
			throw new NullPointerException("ChunkWrapper cannot be null!");
		
        IChunkWrapper oldChunk = latestChunkToBuild.put(chunk.getChunkPos(), chunk); // an Exchange operation
        // If there's old chunk, that means we just replaced an unprocessed old request on generating data on this pos.
        //   if so, we can just return null to signal this, as the old request's future will instead be the proper one
        //   that will return the latest generated data.
        if (oldChunk != null) 
			return null;
		
        // Otherwise, it means we're the first to do so. Lets submit our task to this entry.
        CompletableFuture<ChunkSizedFullDataSource> future = new CompletableFuture<>();
        taskToBuild.addLast(new Task(chunk.getChunkPos(), future));
        return future;
    }
	
    public void tick()
	{
        if (runningCount.get() >= THREAD_COUNT) return;
        if (taskToBuild.isEmpty()) return;
        for (int i = 0; i<THREAD_COUNT; i++)
		{
            runningCount.incrementAndGet();
            CompletableFuture.runAsync(() ->
			{
                try
				{
                    _tick();
                }
				finally
				{
                    runningCount.decrementAndGet();
                }
            }, executor);
        }
    }
	
    private void _tick()
	{
        long time = System.nanoTime();
        int count = 0;
        boolean allDone = false;
        while (true)
		{
            if (System.nanoTime() - time > MAX_TICK_TIME_NS && !taskToBuild.isEmpty()) 
				break;
			
            Task task = taskToBuild.pollFirst();
            if (task == null)
			{
                allDone = true;
                break;
            }
			
            count++;
            IChunkWrapper latestChunk = latestChunkToBuild.remove(task.chunkPos); // Basically an Exchange operation
            if (latestChunk == null)
			{
                LOGGER.error("Somehow Task at {} has latestChunk as null! Skipping task!", task.chunkPos);
                task.future.complete(null);
                continue;
            }
			
            try
			{
                if (LodDataBuilder.canGenerateLodFromChunk(latestChunk))
				{
                    ChunkSizedFullDataSource data = LodDataBuilder.createChunkData(latestChunk);
                    if (data != null)
					{
                        task.future.complete(data);
                        continue;
                    }
                }
            }
			catch (Exception ex)
			{
                LOGGER.error("Error while processing Task at {}!", task.chunkPos, ex);
            }
			
            // Failed to build due to chunk not meeting requirement.
            IChunkWrapper casChunk = latestChunkToBuild.putIfAbsent(task.chunkPos, latestChunk); // CAS operation with expected=null
            if (casChunk == null || latestChunk.isStillValid()) // That means CAS have been successful
                taskToBuild.addLast(task); // Then add back the same old task.
            else // Else, it means someone managed to sneak in a new gen request in this pos. Then lets drop this old task.
                task.future.complete(null);
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
	
}
