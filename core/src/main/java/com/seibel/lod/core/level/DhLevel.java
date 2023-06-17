package com.seibel.lod.core.level;

import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.lod.core.dataObjects.transformers.ChunkToLodBuilder;
import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.file.structure.AbstractSaveStructure;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

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
