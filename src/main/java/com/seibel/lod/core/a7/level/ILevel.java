package com.seibel.lod.core.a7.level;

import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import java.util.concurrent.CompletableFuture;

public interface ILevel extends AutoCloseable {
    int getMinY();
    CompletableFuture<Void> save();

    void dumpRamUsage();

    ILevelWrapper getLevelWrapper();
}
