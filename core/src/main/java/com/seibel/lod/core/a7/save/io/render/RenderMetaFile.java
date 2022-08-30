package com.seibel.lod.core.a7.save.io.render;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.LodRenderSource;
import com.seibel.lod.core.a7.datatype.RenderSourceLoader;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.transform.DataRenderTransformer;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.save.io.MetaFile;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class RenderMetaFile extends MetaFile {
    private final IClientLevel level;
    public RenderSourceLoader loader;
    public Class<? extends LodRenderSource> dataType;

    // The '?' type should either be:
    //    SoftReference<LodRenderSource>, or	- File that may still be loaded
    //    CompletableFuture<LodRenderSource>,or - File that is being loaded
    //    null									- Nothing is loaded or being loaded
    AtomicReference<Object> data = new AtomicReference<>(null);

    //FIXME: This can cause concurrent modification of LodRenderSource.
    //       Not sure if it will cause issues or not.
    public void updateChunkIfNeeded(ChunkSizedData chunkData) {
        DhLodPos chunkPos = new DhLodPos((byte) (chunkData.dataDetail + 4), chunkData.x, chunkData.z);
        LodUtil.assertTrue(pos.getSectionBBoxPos().overlaps(chunkPos), "Chunk pos {} doesn't overlap with section {}", chunkPos, pos);

        CompletableFuture<LodRenderSource> source = _readCached(data.get());
        if (source == null) return;
        if (source.isDone()) source.join().write(chunkData);
    }

    public CompletableFuture<Void> flushAndSave(ExecutorService renderCacheThread) {
        if (!path.exists()) return CompletableFuture.completedFuture(null); // No need to save if the file doesn't exist.
        CompletableFuture<LodRenderSource> source = _readCached(data.get());
        if (source == null) return CompletableFuture.completedFuture(null); // If there is no cached data, there is no need to save.
        return source.thenAccept((a)->{}); // Otherwise, wait for the data to be read (which also flushes changes to the file).
    }

    @FunctionalInterface
    public interface CacheValidator {
        boolean isCacheValid(DhSectionPos sectionPos, long timestamp);
    }
    @FunctionalInterface
    public interface CacheSourceProducer {
        CompletableFuture<LodDataSource> getSourceFuture(DhSectionPos sectionPos);
    }
    CacheValidator validator;
    CacheSourceProducer source;

    // Load a metaFile in this path. It also automatically read the metadata.
    public RenderMetaFile(CacheValidator validator, CacheSourceProducer source,
                          IClientLevel level, File path) throws IOException {
        super(path);
        this.level = level;
        loader = RenderSourceLoader.getLoader(dataTypeId, loaderVersion);
        if (loader == null) {
            throw new IOException("Invalid file: Data type loader not found: "
                    + dataTypeId + "(v" + loaderVersion + ")");
        }
        dataType = loader.clazz;
        this.validator = validator;
        this.source = source;
    }

    // Make a new MetaFile. It doesn't load or write any metadata itself.
    public RenderMetaFile(CacheValidator validator, CacheSourceProducer source,
                          IClientLevel level, File path, DhSectionPos pos) {
        super(path, pos);
        this.level = level;
        this.validator = validator;
        this.source = source;
    }

    // Suppress casting of CompletableFuture<?> to CompletableFuture<LodRenderSource>
    @SuppressWarnings("unchecked")
    private CompletableFuture<LodRenderSource> _readCached(Object obj) {
        // Has file cached in RAM and not freed yet.
        if ((obj instanceof SoftReference<?>)) {
            Object inner = ((SoftReference<?>)obj).get();
            if (inner != null) {
                LodUtil.assertTrue(inner instanceof LodRenderSource);
                return CompletableFuture.completedFuture((LodRenderSource)inner);
            }
        }

        //==== Cached file out of scope. ====
        // Someone is already trying to complete it. so just return the obj.
        if ((obj instanceof CompletableFuture<?>)) {
            return (CompletableFuture<LodRenderSource>)obj;
        }
        return null;
    }

    // Cause: Generic Type runtime casting cannot safety check it.
    // However, the Union type ensures the 'data' should only contain the listed type.
    public CompletableFuture<LodRenderSource> loadOrGetCached(Executor fileReaderThreads) {
        Object obj = data.get();

        CompletableFuture<LodRenderSource> cached = _readCached(obj);
        if (cached != null) return cached;

        // Create an empty and non-completed future.
        // Note: I do this before actually filling in the future so that I can ensure only
        //   one task is submitted to the thread pool.
        CompletableFuture<LodRenderSource> future = new CompletableFuture<>();

        // Would use faster and non-nesting Compare and exchange. But java 8 doesn't have it! :(
        boolean worked = data.compareAndSet(obj, future);
        if (!worked) return loadOrGetCached(fileReaderThreads);

        // Now, there should only ever be one thread at a time here due to the CAS operation above.

        // Would use CompletableFuture.completeAsync(...), But, java 8 doesn't have it! :(
        //return future.completeAsync(this::loadAndUpdateRenderSource, fileReaderThreads);
        CompletableFuture.supplyAsync(() -> buildFuture(fileReaderThreads), fileReaderThreads)
                .thenCompose((sourceCompletableFuture) -> sourceCompletableFuture)
                .whenComplete((renderSource, e) -> {
            if (e != null) {
                LOGGER.error("Uncaught error loading file {}: ", path, e);
                future.complete(null);
                data.set(null);
            } else {
                future.complete(renderSource);
                data.set(new SoftReference<>(renderSource));
            }
        });
        return future;
    }

    private CompletableFuture<LodRenderSource> buildFuture(Executor executorService) {
        if (path.exists()) {
            try {
                updateMetaData();
                if (validator.isCacheValid(pos, timestamp)) {
                    // Load the file.
                    try (FileInputStream fio = getDataContent()) {
                        return CompletableFuture.completedFuture(
                                loader.loadRender(this, fio, level));
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to read render cache at {}:", path, e);
                LOGGER.warn("Will delete cache file.");
                path.delete();
            }
        }
        // Otherwise, re-query and make the RenderSource
        CompletableFuture<LodDataSource> dataFuture = source.getSourceFuture(pos);
        return dataFuture.thenCombineAsync(
                DataRenderTransformer.asyncTransformDataSource(dataFuture, level),
                this::write, executorService);
    }

    private FileInputStream getDataContent() throws IOException {
        FileInputStream fin = new FileInputStream(path);
        int toSkip = METADATA_SIZE;
        while (toSkip > 0) {
            long skipped = fin.skip(toSkip);
            if (skipped == 0) {
                throw new IOException("Invalid file: Failed to skip metadata.");
            }
            toSkip -= skipped;
        }
        if (toSkip != 0) {
            throw new IOException("File IO Error: Failed to skip metadata.");
        }
        return fin;
    }

    @Override
    protected void updateMetaData() throws IOException {
        super.updateMetaData();
        loader = RenderSourceLoader.getLoader(dataTypeId, loaderVersion);
        if (loader == null) {
            throw new IOException("Invalid file: Data type loader not found: " + dataTypeId + "(v" + loaderVersion + ")");
        }
        dataType = loader.clazz;
        dataTypeId = loader.renderTypeId;
    }

    private LodRenderSource write(LodDataSource parent, LodRenderSource render) {
        if (parent == null) return null;
        try {
            //TODO: Update Timestamp & stuff based on parent
            dataLevel = parent.getDataDetail();
            loader = RenderSourceLoader.getLoader(render.getClass(), render.getRenderVersion());
            dataType = render.getClass();
            dataTypeId = loader.renderTypeId;
            loaderVersion = render.getRenderVersion();
            super.writeData((out) -> {
                try {
                    render.saveRender(level, this, out);
                } catch (IOException e) {
                    LOGGER.error("Failed to save data for file {}", path, e);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to write data for file {}", path, e);
        }
        return render;
    }
}
