package com.seibel.lod.core.file.datafile;

import com.seibel.lod.core.datatype.LodDataSource;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.datatype.full.FullDataSource;
import com.seibel.lod.core.datatype.full.SparseDataSource;
import com.seibel.lod.core.generation.GenerationQueue;
import com.seibel.lod.core.level.IServerLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class GeneratedDataFileHandler extends DataFileHandler {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();

    AtomicReference<GenerationQueue> queue = new AtomicReference<>(null);
    // TODO: Should I include a lib that impl weak concurrent hash map?
    final Map<LodDataSource, GenTask> genQueue = Collections.synchronizedMap(new WeakHashMap<>());

    class GenTask extends GenerationQueue.GenTaskTracker {
        final DhSectionPos pos;
        WeakReference<LodDataSource> targetData;
        LodDataSource loadedTargetData = null;
        GenTask(DhSectionPos pos, WeakReference<LodDataSource> targetData) {
            this.pos = pos;
            this.targetData = targetData;
        }
        @Override
        public boolean isValid() {
            return targetData.get() != null;
        }
        @Override
        public Consumer<ChunkSizedData> getConsumer() {
            if (loadedTargetData == null) {
                loadedTargetData = targetData.get();
                if (loadedTargetData == null) return null;
            }
            return (chunk) -> {
                if (chunk.getBBoxLodPos().overlaps(loadedTargetData.getSectionPos().getSectionBBoxPos()))
                    write(loadedTargetData.getSectionPos(), chunk);
            };
        }

        void releaseStrongReference() {
            loadedTargetData = null;
        }
    }


    public GeneratedDataFileHandler(IServerLevel level, File saveRootDir) {
        super(level, saveRootDir);
    }

    public void setGenerationQueue(GenerationQueue newQueue) {
        boolean worked = queue.compareAndSet(null, newQueue);
        LodUtil.assertTrue(worked, "previous queue is still here!");
        synchronized (genQueue) {
            for (Map.Entry<LodDataSource, GenTask> entry : genQueue.entrySet()) {
                LodDataSource source = entry.getKey();
                DhSectionPos pos = source.getSectionPos();
                GenTask task = entry.getValue();
                queue.get().submitGenTask(pos.getSectionBBoxPos(), source.getDataDetail(), task)
                        .whenComplete(
                                (b, ex) -> {
                                    if (ex != null) LOGGER.error("Uncaught Gen Task Exception at {}:", pos, ex);
                                    LodDataSource data = task.targetData.get();
                                    if (ex == null && b) {
                                        files.get(task.pos).metaData.dataVersion.incrementAndGet();
                                        genQueue.remove(data, task);
                                        return;
                                    }
                                    task.releaseStrongReference();
                                }
                        );
            }
        }
    }

    public GenerationQueue popGenerationQueue() {
        GenerationQueue cas = queue.getAndSet(null);
        LodUtil.assertTrue(cas != null, "there are no previous live generation queue!");
        return cas;
    }

    @Override
    public CompletableFuture<LodDataSource> onCreateDataFile(DataMetaFile file) {
        DhSectionPos pos = file.pos;
        ArrayList<DataMetaFile> existFiles = new ArrayList<>();
        ArrayList<DhSectionPos> missing = new ArrayList<>();
        selfSearch(pos, pos, existFiles, missing);
        LodUtil.assertTrue(!missing.isEmpty() || !existFiles.isEmpty());
        if (missing.size() == 1 && existFiles.isEmpty() && missing.get(0).equals(pos)) {
            // None exist.
            SparseDataSource dataSource = SparseDataSource.createEmpty(pos);
            GenerationQueue getQueue = queue.get();
            GenTask task = new GenTask(pos, new WeakReference<>(dataSource));
            genQueue.put(dataSource, task);
            if (getQueue != null) {
                getQueue.submitGenTask(dataSource.getSectionPos().getSectionBBoxPos(),
                        dataSource.getDataDetail(), task)
                        .whenComplete(
                        (b, ex) -> {
                            if (ex != null) LOGGER.error("Uncaught Gen Task Exception at {}:", pos, ex);
                            LodDataSource data = task.targetData.get();
                            if (ex == null && b) {
                                files.get(task.pos).metaData.dataVersion.incrementAndGet();
                                genQueue.remove(data, task);
                                return;
                            }
                            task.releaseStrongReference();
                        }
                );
            }
            return CompletableFuture.completedFuture(dataSource);
        } else {
            for (DhSectionPos missingPos : missing) {
                DataMetaFile newfile = atomicGetOrMakeFile(missingPos);
                if (newfile != null) existFiles.add(newfile);
            }
            final ArrayList<CompletableFuture<Void>> futures = new ArrayList<>(existFiles.size());
            final SparseDataSource dataSource = SparseDataSource.createEmpty(pos);

            for (DataMetaFile f : existFiles) {
                futures.add(f.loadOrGetCached()
                        .exceptionally((ex) -> null)
                        .thenAccept((data) -> {
                            if (data != null) {
                                if (data instanceof SparseDataSource)
                                    dataSource.sampleFrom((SparseDataSource) data);
                                else if (data instanceof FullDataSource)
                                    dataSource.sampleFrom((FullDataSource) data);
                                else LodUtil.assertNotReach();
                            }
                        })
                );
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .thenApply((v) -> dataSource.trySelfPromote());
        }
    }
}
