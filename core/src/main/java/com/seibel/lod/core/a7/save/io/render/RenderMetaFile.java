package com.seibel.lod.core.a7.save.io.render;

import com.seibel.lod.core.a7.datatype.DataSourceLoader;
import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.LodRenderSource;
import com.seibel.lod.core.a7.datatype.RenderSourceLoader;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.transform.DataRenderTransformer;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.save.io.MetaFile;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;
import com.seibel.lod.core.a7.save.io.file.IDataSourceProvider;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class RenderMetaFile extends MetaFile
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(RenderMetaFile.class.getSimpleName());
	
    //private final IClientLevel level;
	
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
    final RenderFileHandler handler;
    boolean doesFileExist;


    // Create a new metaFile
    public RenderMetaFile(RenderFileHandler handler, DhSectionPos pos) throws IOException {
        super(handler.computeRenderFilePath(pos), pos);
        this.handler = handler;
        LodUtil.assertTrue(metaData == null);
        doesFileExist = false;
    }

    public RenderMetaFile(RenderFileHandler handler, File path) throws IOException {
        super(path);
        this.handler = handler;
        LodUtil.assertTrue(metaData != null);
        loader = RenderSourceLoader.getLoader(metaData.dataTypeId, metaData.loaderVersion);
        if (loader == null) {
            throw new IOException("Invalid file: Data type loader not found: "
                    + metaData.dataTypeId + "(v" + metaData.loaderVersion + ")");
        }
        dataType = loader.clazz;
        doesFileExist = true;
    }

    // Suppress casting of CompletableFuture<?> to CompletableFuture<LodRenderSource>
    @SuppressWarnings("unchecked")
    private CompletableFuture<LodRenderSource> _readCached(Object obj) {
        // Has file cached in RAM and not freed yet.
        if ((obj instanceof SoftReference<?>)) {
            Object inner = ((SoftReference<?>)obj).get();
            if (inner != null) {
                LodUtil.assertTrue(inner instanceof LodRenderSource);
                handler.onReadRenderSourceFromCache(this, (LodRenderSource) inner);
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
    public CompletableFuture<LodRenderSource> loadOrGetCached(Executor fileReaderThreads, ILevel level) {
        Object obj = data.get();

        CompletableFuture<LodRenderSource> cached = _readCached(obj);
        if (cached != null) return cached;

        // Create an empty and non-completed future.
        // Note: I do this before actually filling in the future so that I can ensure only
        //   one task is submitted to the thread pool.
        CompletableFuture<LodRenderSource> future = new CompletableFuture<>();

        // Would use faster and non-nesting Compare and exchange. But java 8 doesn't have it! :(
        boolean worked = data.compareAndSet(obj, future);
        if (!worked) return loadOrGetCached(fileReaderThreads, level);

        // Now, there should only ever be one thread at a time here due to the CAS operation above.


        // After cas. We are in exclusive control.
        if (!doesFileExist) {
            doesFileExist = true;
            handler.onCreateRenderFile(this)
                    .thenApply((data) -> {
                        metaData = makeMetaData(data);
                        return data;
                    })
                    .thenApply((d) -> handler.onRenderFileLoaded(d, this))
                    .whenComplete((v, e) -> {
                        if (e != null) {
                            LOGGER.error("Uncaught error on creation {}: ", path, e);
                            future.complete(null);
                            data.set(null);
                        } else {
                            future.complete(v);
                            //new DataObjTracker(v); //TODO: Obj Tracker??? For debug?
                            data.set(new SoftReference<>(v));
                        }
                    });


        } else {
            CompletableFuture.supplyAsync(() -> {
                        if (metaData == null)
                            throw new IllegalStateException("Meta data not loaded!");
                        // Load the file.
                        LodRenderSource data;
                        data = handler.onLoadingRenderFile(this);
                        if (data == null) {
                            try (FileInputStream fio = getDataContent()) {
                                data = loader.loadRender(this, fio, level);
                            } catch (IOException e) {
                                throw new CompletionException(e);
                            }
                        }
                        data = handler.onRenderFileLoaded(data, this);
                        return data;
                    }, fileReaderThreads)
                    .whenComplete((f, e) -> {
                        if (e != null) {
                            LOGGER.error("Error loading file {}: ", path, e);
                            future.complete(null);
                            data.set(null);
                        } else {
                            future.complete(f);
                            data.set(new SoftReference<>(f));
                        }
                    });
        }
        return future;
    }

    private static MetaData makeMetaData(LodRenderSource data) {
        RenderSourceLoader loader = RenderSourceLoader.getLoader(data.getClass(), data.getRenderVersion());
        return new MetaData(data.getSectionPos(), -1, -1,
                data.getDataDetail(), loader == null ? 0 : loader.renderTypeId, data.getRenderVersion());
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

    public void save(LodRenderSource data, IClientLevel level) {
        LodUtil.assertTrue(data == _readCached(this.data.get()).getNow(null));
        LOGGER.info("Saving updated render file v[{}] at sect {}", metaData.dataVersion.get(), pos);
        try {
            super.writeData((out) -> data.saveRender(level, this, out));
        } catch (IOException e) {
            LOGGER.error("Failed to save updated render file at {} for sect {}", path, pos, e);
        }
    }
}
