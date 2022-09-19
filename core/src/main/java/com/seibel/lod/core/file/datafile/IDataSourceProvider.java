package com.seibel.lod.core.file.datafile;

import com.seibel.lod.core.datatype.LodDataSource;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.file.MetaFile;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IDataSourceProvider extends AutoCloseable {
    void addScannedFile(Collection<File> detectedFiles);

    CompletableFuture<LodDataSource> read(DhSectionPos pos);
    void write(DhSectionPos sectionPos, ChunkSizedData chunkData);
    CompletableFuture<Void> flushAndSave();

    long getLatestCacheVersion(DhSectionPos sectionPos);

    CompletableFuture<LodDataSource> onCreateDataFile(DataMetaFile file);
    LodDataSource onDataFileLoaded(LodDataSource source, MetaFile.MetaData metaData, Consumer<LodDataSource> onUpdated, Function<LodDataSource, Boolean> updater);
    CompletableFuture<LodDataSource> onDataFileRefresh(LodDataSource source, Function<LodDataSource, Boolean> updater, Consumer<LodDataSource> onUpdated);
    File computeDataFilePath(DhSectionPos pos);
    Executor getIOExecutor();

}
