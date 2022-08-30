package com.seibel.lod.core.a7.generation;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.transform.LodDataBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.util.gridList.ArrayGridList;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import java.util.concurrent.CompletableFuture;

public interface IChunkGenerator extends IGenerator {
    CompletableFuture<ArrayGridList<IChunkWrapper>> generateChunks(DHChunkPos chunkPosMin, byte granularity);

    @Override
    default CompletableFuture<ArrayGridList<ChunkSizedData>> generate(DHChunkPos chunkPosMin, byte granularity) {
        return generateChunks(chunkPosMin, granularity).thenApply(chunks -> {
            ArrayGridList<ChunkSizedData> chunkData = new ArrayGridList<>(chunks.gridSize);
            chunks.forEachPos((x, y) -> chunkData.set(x, y, LodDataBuilder.createChunkData(chunks.get(x, y))));
            return chunkData;
        });
    }

}
