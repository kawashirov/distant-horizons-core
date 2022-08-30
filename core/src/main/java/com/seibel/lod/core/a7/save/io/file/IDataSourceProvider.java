package com.seibel.lod.core.a7.save.io.file;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.full.FullFormat;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.DHChunkPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface IDataSourceProvider extends AutoCloseable {
    void addScannedFile(Collection<File> detectedFiles);

    CompletableFuture<LodDataSource> read(DhSectionPos pos);
    void write(DhSectionPos sectionPos, ChunkSizedData chunkData);
    CompletableFuture<Void> flushAndSave();

    boolean isCacheValid(DhSectionPos sectionPos, long timestamp);
}
