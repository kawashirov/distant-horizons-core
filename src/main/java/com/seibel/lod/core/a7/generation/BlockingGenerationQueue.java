package com.seibel.lod.core.a7.generation;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.pos.DhSectionPos;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class BlockingGenerationQueue {
    final BiConsumer<DhSectionPos, ChunkSizedData> writeConsumer;




    public BlockingGenerationQueue(BiConsumer<DhSectionPos, ChunkSizedData> writeConsumer) {
        this.writeConsumer = writeConsumer;
    }

    public CompletableFuture<LodDataSource> generate(DhSectionPos sectPos, Supplier<LodDataSource> creator) {

    }


}
