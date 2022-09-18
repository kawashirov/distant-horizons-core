package com.seibel.lod.core.util.objects;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

public class EventLoop implements AutoCloseable {
    private final boolean PAUSE_ON_ERROR = ModInfo.IS_DEV_BUILD;
    private final Logger logger = DhLoggerBuilder.getLogger();
    private final ExecutorService executorService;
    private final Runnable runnable;
    private CompletableFuture<Void> future;
    private boolean isRunning = true;
    public EventLoop(ExecutorService executorService, Runnable runnable) {
        this.executorService = executorService;
        this.runnable = runnable;
    }
    public void tick() {
        if (future != null && future.isDone()) {
            try {
                future.join();
            } catch (CompletionException ce) {
                logger.error("Uncaught exception in event loop", ce.getCause());
                if (PAUSE_ON_ERROR) isRunning = false;
            } catch (Exception e) {
                logger.error("Exception in event loop", e);
                if (PAUSE_ON_ERROR) isRunning = false;
            } finally {future = null;}
        }
        if (future == null && isRunning) {
            future = CompletableFuture.runAsync(runnable, executorService);
        }
    }
    public void close() {
        if (future != null) {
            future.cancel(true);
        }
        future = null;
        executorService.shutdown();
    }
    public boolean isRunning() {
        return future != null && !future.isDone();
    }
}
