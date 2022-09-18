package com.seibel.lod.core.datatype.transform;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.level.ILevel;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.util.*;
import com.seibel.lod.core.util.objects.EventLoop;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import org.apache.logging.log4j.LogManager;

//FIXME: To-Be-Used class
public class ChunkToLodBuilder {
    public static final ConfigBasedLogger LOGGER = new ConfigBasedLogger(LogManager.getLogger(),
            () -> Config.Client.Advanced.Debugging.DebugSwitch.logLodBuilderEvent.get());
    static class Task {
        final DhChunkPos chunkPos;
        final CompletableFuture<ChunkSizedData> future;
        Task(DhChunkPos chunkPos, CompletableFuture<ChunkSizedData> future) {
            this.chunkPos = chunkPos;
            this.future = future;
        }
    }
    private final ConcurrentHashMap<DhChunkPos, IChunkWrapper> latestChunkToBuild = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<Task> taskToBuild = new ConcurrentLinkedDeque<>();
    private final ExecutorService executor = LodUtil.makeThreadPool(4, ChunkToLodBuilder.class);
    private final AtomicInteger runningCount = new AtomicInteger(0);

    public CompletableFuture<ChunkSizedData> tryGenerateData(IChunkWrapper chunk) {
        if (chunk == null) throw new NullPointerException("ChunkWrapper cannot be null!");
        IChunkWrapper oldChunk = latestChunkToBuild.put(chunk.getChunkPos(), chunk); // an Exchange operation
        // If there's old chunk, that means we just replaced an unprocessed old request on generating data on this pos.
        //   if so, we can just return null to signal this, as the old request's future will instead be the proper one
        //   that will return the latest generated data.
        if (oldChunk != null) return null;
        // Otherwise, it means we're the first to do so. Lets submit our task to this entry.
        CompletableFuture<ChunkSizedData> future = new CompletableFuture<>();
        taskToBuild.addLast(new Task(chunk.getChunkPos(), future));
        return future;
    }

    public void tick() {
        while (true) {
            if (runningCount.get() > 8192) return;
            Task task = taskToBuild.pollFirst();
            if (task == null) return; // There's no jobs.
            IChunkWrapper latestChunk = latestChunkToBuild.remove(task.chunkPos); // Basically an Exchange operation
            if (latestChunk == null) {
                LOGGER.error("Somehow Task at {} has latestChunk as null! Skipping task!", task.chunkPos);
                task.future.complete(null);
                return;
            }

            runningCount.incrementAndGet();
            CompletableFuture.supplyAsync(() -> {
                long time = System.nanoTime();
                if (LodDataBuilder.canGenerateLodFromChunk(latestChunk)) {
                    ChunkSizedData data = LodDataBuilder.createChunkData(latestChunk);
                    if (data != null) {
                        long time2 = System.nanoTime();
                        LOGGER.info("Processed Task at {} using {}", task.chunkPos, Duration.ofNanos(time2 - time));
                        task.future.complete(data);
                        return true;
                    }
                }
                return false;
            }, executor).handle((b, ex) -> {
                runningCount.decrementAndGet();
                if (ex == null && b) return true;
                if (ex != null) {
                    LOGGER.error("Error while processing Task at {}!", task.chunkPos, ex);
                }
                // Failed to build due to chunk not meeting requirement.
                IChunkWrapper casChunk = latestChunkToBuild.putIfAbsent(task.chunkPos, latestChunk); // CAS operation with expected=null
                if (casChunk == null) // That means CAS have been successful
                    taskToBuild.addLast(task); // Then add back the same old task.
                else // Else, it means someone managed to sneak in a new gen request in this pos. Then lets drop this old task.
                    task.future.complete(null);
                return false;
            });
        }
    }

    private void _tick() {
    }

}
