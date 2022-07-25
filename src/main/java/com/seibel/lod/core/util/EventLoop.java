package com.seibel.lod.core.util;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class EventLoop { //FIXME This should have close. We are leaking stuff.
    private final Logger logger = DhLoggerBuilder.getLogger();
    private final ExecutorService executorService;
    private final Runnable runnable;
    private CompletableFuture<Void> future;
    public EventLoop(ExecutorService executorService, Runnable runnable) {
        this.executorService = executorService;
        this.runnable = runnable;
    }
    public void tick() {
        if (future != null && future.isDone()) {
            try {
                future.join();
            } catch (Exception e) {
                logger.error("Uncaught exception in event loop", e);
            } finally {future = null;}
        }
        if (future == null) {
            future = CompletableFuture.runAsync(runnable, executorService);
        }
    }
    public void halt() {
        if (future != null) {
            future.cancel(true);
        }
    }
    public boolean isRunning() {
        return future != null && !future.isDone();
    }
}
