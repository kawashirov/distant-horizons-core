package com.seibel.lod.core.file.datafile;

import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.file.metaData.MetaData;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IDataSourceProvider extends AutoCloseable {
    void addScannedFile(Collection<File> detectedFiles);

    CompletableFuture<ILodDataSource> read(DhSectionPos pos);
    void write(DhSectionPos sectionPos, ChunkSizedData chunkData);
    CompletableFuture<Void> flushAndSave();

    long getCacheVersion(DhSectionPos sectionPos);
    boolean isCacheVersionValid(DhSectionPos sectionPos, long cacheVersion);

    CompletableFuture<ILodDataSource> onCreateDataFile(DataMetaFile file);
    ILodDataSource onDataFileLoaded(ILodDataSource source, MetaData metaData, Consumer<ILodDataSource> onUpdated, Function<ILodDataSource, Boolean> updater);
    CompletableFuture<ILodDataSource> onDataFileRefresh(ILodDataSource source, MetaData metaData, Function<ILodDataSource, Boolean> updater, Consumer<ILodDataSource> onUpdated);
    File computeDataFilePath(DhSectionPos pos);
    Executor getIOExecutor();

}
