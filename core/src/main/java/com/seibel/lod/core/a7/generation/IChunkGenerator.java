package com.seibel.lod.core.a7.generation;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.transform.LodDataBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.util.gridList.ArrayGridList;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface IChunkGenerator extends IGenerator {
    CompletableFuture<Void> generateChunks(DHChunkPos chunkPosMin, byte granularity, byte targetDataDetail, Consumer<IChunkWrapper> resultConsumer);

    @Override
    default CompletableFuture<Void> generate(DHChunkPos chunkPosMin, byte granularity, byte targetDataDetail, Consumer<ChunkSizedData> resultConsumer) {
        return generateChunks(chunkPosMin, granularity, targetDataDetail, (chunk) -> {
            resultConsumer.accept(LodDataBuilder.createChunkData(chunk));
        });
    }

}
