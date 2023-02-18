package com.seibel.lod.core.file.fullDatafile;

import com.seibel.lod.core.datatype.IFullDataSource;
import com.seibel.lod.core.datatype.full.ChunkSizedFullDataSource;
import com.seibel.lod.core.file.metaData.MetaData;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IFullDataSourceProvider extends AutoCloseable {
    void addScannedFile(Collection<File> detectedFiles);

    CompletableFuture<IFullDataSource> read(DhSectionPos pos);
    void write(DhSectionPos sectionPos, ChunkSizedFullDataSource chunkData);
    CompletableFuture<Void> flushAndSave();

    //long getCacheVersion(DhSectionPos sectionPos);
    //boolean isCacheVersionValid(DhSectionPos sectionPos, long cacheVersion);

    CompletableFuture<IFullDataSource> onCreateDataFile(FullDataMetaFile file);
    IFullDataSource onDataFileLoaded(IFullDataSource source, MetaData metaData, Consumer<IFullDataSource> onUpdated, Function<IFullDataSource, Boolean> updater);
    CompletableFuture<IFullDataSource> onDataFileRefresh(IFullDataSource source, MetaData metaData, Function<IFullDataSource, Boolean> updater, Consumer<IFullDataSource> onUpdated);
    File computeDataFilePath(DhSectionPos pos);
    Executor getIOExecutor();

}