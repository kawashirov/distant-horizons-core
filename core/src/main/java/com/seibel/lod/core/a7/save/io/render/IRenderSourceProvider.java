package com.seibel.lod.core.a7.save.io.render;

import com.seibel.lod.core.a7.datatype.LodRenderSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface IRenderSourceProvider extends AutoCloseable {
    CompletableFuture<LodRenderSource> read(DhSectionPos pos);
    void addScannedFile(Collection<File> detectedFiles);
    void write(DhSectionPos sectionPos, ChunkSizedData chunkData);
    CompletableFuture<Void> flushAndSave();
}
