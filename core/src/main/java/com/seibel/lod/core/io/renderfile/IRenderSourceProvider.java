package com.seibel.lod.core.io.renderfile;

import com.seibel.lod.core.datatype.LodRenderSource;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface IRenderSourceProvider extends AutoCloseable {
    CompletableFuture<LodRenderSource> read(DhSectionPos pos);
    void addScannedFile(Collection<File> detectedFiles);
    void write(DhSectionPos sectionPos, ChunkSizedData chunkData);
    CompletableFuture<Void> flushAndSave();
    boolean refreshRenderSource(LodRenderSource source);
}
