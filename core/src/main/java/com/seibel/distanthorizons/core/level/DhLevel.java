package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.dataObjects.transformers.ChunkToLodBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

import java.util.concurrent.CompletableFuture;

public abstract class DhLevel implements IDhLevel {

    public final ChunkToLodBuilder chunkToLodBuilder;

    protected DhLevel() {
        this.chunkToLodBuilder = new ChunkToLodBuilder();
    }

    protected abstract void saveWrites(ChunkSizedFullDataAccessor data);


    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public void updateChunkAsync(IChunkWrapper chunk)
    {
        CompletableFuture<ChunkSizedFullDataAccessor> future = this.chunkToLodBuilder.tryGenerateData(chunk);
        if (future != null)
        {
            future.thenAccept(this::saveWrites);
        }
    }

    @Override
    public void close() {
        chunkToLodBuilder.close();
    }
}
