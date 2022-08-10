package com.seibel.lod.core.a7.save.io.file;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.a7.datatype.DataSourceLoader;
import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.full.FullDataSource;
import com.seibel.lod.core.a7.datatype.full.FullFormat;
import com.seibel.lod.core.a7.level.IServerLevel;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalDataFileHandler implements IDataSourceProvider {
    // Note: Single main thread only for now. May make it multi-thread later, depending on the usage.
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    final ExecutorService fileReaderThread = LodUtil.makeThreadPool(4, "FileReaderThread");
    final ConcurrentHashMap<DhSectionPos, DataMetaFile> files = new ConcurrentHashMap<>();
    final IServerLevel level;
    final File saveDir;
    AtomicInteger topDetailLevel = new AtomicInteger(-1);
    final int minDetailLevel = FullDataSource.SECTION_SIZE_OFFSET;


    public LocalDataFileHandler(IServerLevel level, File saveRootDir) {
        this.saveDir = saveRootDir;
        this.level = level;
    }

    /*
    * Caller must ensure that this method is called only once,
    *  and that this object is not used before this method is called.
     */
    @Override
    public void addScannedFile(Collection<File> detectedFiles) {
        HashMultimap<DhSectionPos, DataMetaFile> filesByPos = HashMultimap.create();
        LOGGER.info("Detected {} valid files in {}", detectedFiles.size(), saveDir);

        { // Sort files by pos.
            for (File file : detectedFiles) {
                try {
                    DataMetaFile metaFile = new DataMetaFile(level, file);
                    filesByPos.put(metaFile.pos, metaFile);
                } catch (IOException e) {
                    LOGGER.error("Failed to read file {}. File will be deleted.", file, e);
                    if (!file.delete()) {
                        LOGGER.error("Failed to delete file {}.", file);
                    }
                }
            }
        }

        // Warn for multiple files with the same pos, and then select the one with latest timestamp.
        for (DhSectionPos pos : filesByPos.keySet()) {
            Collection<DataMetaFile> metaFiles = filesByPos.get(pos);
            DataMetaFile fileToUse;
            if (metaFiles.size() > 1) {
                fileToUse = Collections.max(metaFiles, Comparator.comparingLong(a -> a.timestamp));
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Multiple files with the same pos: ");
                    sb.append(pos);
                    sb.append("\n");
                    for (DataMetaFile metaFile : metaFiles) {
                        sb.append("\t");
                        sb.append(metaFile.path);
                        sb.append("\n");
                    }
                    sb.append("\tUsing: ");
                    sb.append(fileToUse.path);
                    sb.append("\n");
                    sb.append("(Other files will be renamed by appending \".old\" to their name.)");
                    LOGGER.warn(sb.toString());

                    // Rename all other files with the same pos to .old
                    for (DataMetaFile metaFile : metaFiles) {
                        if (metaFile == fileToUse) continue;
                        File oldFile = new File(metaFile.path + ".old");
                        try {
                            if (!metaFile.path.renameTo(oldFile)) throw new RuntimeException("Renaming failed");
                        } catch (Exception e) {
                            LOGGER.error("Failed to rename file: " + metaFile.path + " to " + oldFile, e);
                        }
                    }
                }
            } else {
                fileToUse = metaFiles.iterator().next();
            }
            // Add file to the list of files.
            topDetailLevel.updateAndGet(v -> Math.max(v, fileToUse.pos.sectionDetail));
            files.put(pos, fileToUse);
        }
    }

    /*
    * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public CompletableFuture<LodDataSource> read(DhSectionPos pos) {
        topDetailLevel.updateAndGet(v -> Math.max(v, pos.sectionDetail));
        DataMetaFile metaFile = files.get(pos);
        if (metaFile == null) {
            return CompletableFuture.completedFuture(null);
        }
        return metaFile.loadOrGetCached(fileReaderThread);
    }

    // This prevents new higher detail levels from being created by not updating the topDetailLevel.
    private CompletableFuture<LodDataSource> readWithoutUpdate(DhSectionPos pos) {
        DataMetaFile metaFile = files.get(pos);
        if (metaFile == null) {
            return CompletableFuture.completedFuture(null);
        }
        return metaFile.loadOrGetCached(fileReaderThread);
    }

    /*
    * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public void write(DhSectionPos sectionPos, ChunkSizedData chunkData) {
        DhLodPos chunkPos = new DhLodPos((byte) (chunkData.dataDetail+4), chunkData.x, chunkData.z);
        LodUtil.assertTrue(chunkPos.overlaps(sectionPos.getSectionBBoxPos()), "Chunk {} does not overlap section {}", chunkPos, sectionPos);
        chunkPos = chunkPos.convertUpwardsTo((byte) minDetailLevel); // TODO: Handle if chunkData has higher detail than lowestDetail.
        recursiveWrite(new DhSectionPos(chunkPos.detail, chunkPos.x, chunkPos.z), chunkData);
    }
    private void recursiveWrite(DhSectionPos sectionPos, ChunkSizedData chunkData) {
        DataMetaFile metaFile = files.get(sectionPos);
        if (metaFile != null) { // Fast path: if there is a file for this section, just write to it.
            metaFile.addToWriteQueue(chunkData);
        } else if (sectionPos.sectionDetail <= minDetailLevel) {
            File file = computeDefaultFilePath(sectionPos);
            //FIXME: Handle file already exists issue. Possibly by renaming the file.
            LodUtil.assertTrue(!file.exists(), "File {} already exist for path {}", file, sectionPos);
            CompletableFuture<LodDataSource> gen = new CompletableFuture<>();
            DataMetaFile newMetaFile = new DataMetaFile(level, file, sectionPos, gen);
            metaFile = files.putIfAbsent(sectionPos, newMetaFile); // This is a CAS with expected null value.
            if (metaFile == null) {
                newMetaFile.addToWriteQueue(chunkData);
                CompletableFuture.runAsync(() -> gen.complete(FullDataSource.createEmpty(sectionPos)), fileReaderThread)
                        .exceptionally((e) -> {
                            gen.completeExceptionally(e);
                            return null;
                        });
            } else {
                metaFile.addToWriteQueue(chunkData);
                gen.cancel(true);
            }
        } else {
            File file = computeDefaultFilePath(sectionPos);
            //FIXME: Handle file already exists issue. Possibly by renaming the file.
            LodUtil.assertTrue(!file.exists(), "File {} already exist for path {}", file, sectionPos);
            CompletableFuture<LodDataSource> gen = new CompletableFuture<>();
            DataMetaFile newMetaFile = new DataMetaFile(level, file, sectionPos, gen);
            metaFile = files.putIfAbsent(sectionPos, newMetaFile); // This is a CAS with expected null value.
            if (metaFile == null) {
                newMetaFile.addToWriteQueue(chunkData);
                // Create future that generate downsized file
                CompletableFuture<LodDataSource> subChunk0 = readWithoutUpdate(sectionPos.getChild(0));
                CompletableFuture<LodDataSource> subChunk1 = readWithoutUpdate(sectionPos.getChild(1));
                CompletableFuture<LodDataSource> subChunk2 = readWithoutUpdate(sectionPos.getChild(2));
                CompletableFuture<LodDataSource> subChunk3 = readWithoutUpdate(sectionPos.getChild(3));
                CompletableFuture.allOf(subChunk0, subChunk1, subChunk2, subChunk3)
                        .thenAccept(v ->
                                gen.complete(FullDataSource.createFromLower(sectionPos, new FullDataSource[]{
                                        (FullDataSource) subChunk0.join(),
                                        (FullDataSource) subChunk1.join(),
                                        (FullDataSource) subChunk2.join(),
                                        (FullDataSource) subChunk3.join()
                                }))
                        ).exceptionally((e) -> {
                            gen.completeExceptionally(e);
                            return null;
                        });
            } else {
                metaFile.addToWriteQueue(chunkData);
                gen.cancel(true);
            }
        }
        if (sectionPos.sectionDetail <= topDetailLevel.get()) {
            recursiveWrite(sectionPos.getParent(), chunkData);
        }
    }

    /*
     * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public CompletableFuture<Void> flushAndSave() {
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        for (DataMetaFile metaFile : files.values()) {
            futures.add(metaFile.flushAndSave(fileReaderThread));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public boolean isCacheValid(DhSectionPos sectionPos, long timestamp) {
        DataMetaFile file = files.get(sectionPos);
        if (file == null) return false;
        //TODO
        return true;
    }

    private File computeDefaultFilePath(DhSectionPos pos) { //TODO: Temp code as we haven't decided on the file naming & location yet.
        return new File(saveDir, pos.serialize() + ".lod");
    }

    @Override
    public void close() {
        DataMetaFile.debugCheck();
         //TODO
    }
}
