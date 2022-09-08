package com.seibel.lod.core.a7.save.io.file;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.full.FullFormat;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.DHChunkPos;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface IDataSourceProvider extends AutoCloseable {
    void addScannedFile(Collection<File> detectedFiles);

    CompletableFuture<LodDataSource> read(DhSectionPos pos);
    void write(DhSectionPos sectionPos, ChunkSizedData chunkData);
    CompletableFuture<Void> flushAndSave();

    long getLatestCacheVersion(DhSectionPos sectionPos);

    CompletableFuture<LodDataSource> onCreateDataFile(DataMetaFile file);
    LodDataSource onDataFileLoaded(LodDataSource source, Consumer<LodDataSource> updater);
    CompletableFuture<LodDataSource> onDataFileRefresh(LodDataSource source, Consumer<LodDataSource> updater);
    File computeDataFilePath(DhSectionPos pos);
    Executor getIOExecutor();

}
