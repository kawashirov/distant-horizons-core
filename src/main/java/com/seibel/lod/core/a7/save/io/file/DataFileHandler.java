package com.seibel.lod.core.a7.save.io.file;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.full.FullDataSource;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.level.IServerLevel;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
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
import java.util.function.Function;

public class DataFileHandler implements IDataSourceProvider {
    // Note: Single main thread only for now. May make it multi-thread later, depending on the usage.
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    final ExecutorService fileReaderThread = LodUtil.makeThreadPool(4, "FileReaderThread");
    final ConcurrentHashMap<DhSectionPos, DataMetaFile> files = new ConcurrentHashMap<>();
    final ILevel level;
    final File saveDir;
    AtomicInteger topDetailLevel = new AtomicInteger(-1);
    final int minDetailLevel = FullDataSource.SECTION_SIZE_OFFSET;
    final Function<DhSectionPos, CompletableFuture<LodDataSource>> dataSourceCreator;


    public DataFileHandler(ILevel level, File saveRootDir,
                           Function<DhSectionPos, CompletableFuture<LodDataSource>> dataSourceCreator) {
        this.saveDir = saveRootDir;
        this.level = level;
        this.dataSourceCreator = dataSourceCreator;
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

    private DataMetaFile atomicGetOrMakeFile(DhSectionPos pos) {
        DataMetaFile metaFile = files.get(pos);
        if (metaFile == null) {
            File file = computeDefaultFilePath(pos);
            //FIXME: Handle file already exists issue. Possibly by renaming the file.
            LodUtil.assertTrue(!file.exists(), "File {} already exist for path {}", file, pos);
            CompletableFuture<LodDataSource> gen = new CompletableFuture<>();
            DataMetaFile newMetaFile = new DataMetaFile(level, file, pos, gen);
            metaFile = files.putIfAbsent(pos, newMetaFile); // This is a CAS with expected null value.
            if (metaFile == null) {
                buildFile(pos, gen);
                metaFile = newMetaFile;
            } else {
                gen.cancel(true);
            }
        }
        return metaFile;
    }

    private void selfSearch(DhSectionPos basePos, DhSectionPos pos, ArrayList<DataMetaFile> existFiles, ArrayList<DhSectionPos> missing) {
        byte detail = pos.sectionDetail;
        boolean allEmpty = true;
        outerLoop:
        while (--detail >= minDetailLevel) {
            DhLodPos min = pos.getCorner().getCorner(detail);
            int count = pos.getSectionBBoxPos().getWidth(detail);
            for (int ox = 0; ox<count; ox++) {
                for (int oz = 0; oz<count; oz++) {
                    DhSectionPos subPos = new DhSectionPos(detail, ox+min.x, oz+min.z);
                    LodUtil.assertTrue(pos.overlaps(basePos) && subPos.overlaps(pos));

                    //TODO: The following check is temp as we only samples corner points per data, which means
                    // on a very different level, we may not need the entire section at all.
                    if (!FullDataSource.neededForPosition(basePos, subPos)) continue;

                    if (files.containsKey(subPos)) {
                        allEmpty = false;
                        break outerLoop;
                    }
                }
            }
        }

        if (allEmpty) {
            missing.add(pos);
        } else {
            {
                DhSectionPos childPos = pos.getChild(0);
                if (FullDataSource.neededForPosition(basePos, childPos)) {
                    DataMetaFile metaFile = files.get(childPos);
                    if (metaFile != null) {
                        existFiles.add(metaFile);
                    } else if (childPos.sectionDetail == minDetailLevel) {
                        missing.add(childPos);
                    } else {
                        selfSearch(basePos, childPos, existFiles, missing);
                    }
                }
            }
            {
                DhSectionPos childPos = pos.getChild(1);
                if (FullDataSource.neededForPosition(basePos, childPos)) {
                    DataMetaFile metaFile = files.get(childPos);
                    if (metaFile != null) {
                        existFiles.add(metaFile);
                    } else if (childPos.sectionDetail == minDetailLevel) {
                        missing.add(childPos);
                    } else {
                        selfSearch(basePos, childPos, existFiles, missing);
                    }
                }
            }
            {
                DhSectionPos childPos = pos.getChild(2);
                if (FullDataSource.neededForPosition(basePos, childPos)) {
                    DataMetaFile metaFile = files.get(childPos);
                    if (metaFile != null) {
                        existFiles.add(metaFile);
                    } else if (childPos.sectionDetail == minDetailLevel) {
                        missing.add(childPos);
                    } else {
                        selfSearch(basePos, childPos, existFiles, missing);
                    }
                }
            }
            {
                DhSectionPos childPos = pos.getChild(3);
                if (FullDataSource.neededForPosition(basePos, childPos)) {
                    DataMetaFile metaFile = files.get(childPos);
                    if (metaFile != null) {
                        existFiles.add(metaFile);
                    } else if (childPos.sectionDetail == minDetailLevel) {
                        missing.add(childPos);
                    } else {
                        selfSearch(basePos, childPos, existFiles, missing);
                    }
                }
            }
        }
    }


    private void buildFile(DhSectionPos pos, CompletableFuture<LodDataSource> gen) {
        ArrayList<DataMetaFile> existFiles = new ArrayList<>();
        ArrayList<DhSectionPos> missing = new ArrayList<>();
        selfSearch(pos, pos, existFiles, missing);
        LodUtil.assertTrue(!missing.isEmpty() || !existFiles.isEmpty());
        if (missing.size() == 1 && existFiles.isEmpty() && missing.get(0).equals(pos)) {
            dataSourceCreator.apply(pos).whenComplete((f, ex) -> {
                if (ex != null) {
                    gen.completeExceptionally(ex);
                } else {
                    gen.complete(f);
                }
            });
            return;
        }


        LOGGER.info("Creating file at {} using {} existing files and {} new files.", pos, existFiles.size(), missing.size());
        ArrayList<CompletableFuture<LodDataSource>> futures = new ArrayList<>(existFiles.size() + missing.size());
        for (DhSectionPos missingPos : missing) {
            existFiles.add(atomicGetOrMakeFile(missingPos));
        }
        FullDataSource fullDataSource = FullDataSource.createEmpty(pos);
        for (DataMetaFile metaFile : existFiles) {
            futures.add(
                    metaFile.loadOrGetCached(fileReaderThread).whenComplete((data, ex) -> {
                        if (ex != null) return;
                        if (!(data instanceof FullDataSource)) return;
                        fullDataSource.writeFromLower((FullDataSource) data);
                    })
            );
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, ex) -> {
            if (ex != null) {
                gen.completeExceptionally(ex);
            } else {
                gen.complete(fullDataSource);
            }
        });
    }

    /*
    * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public CompletableFuture<LodDataSource> read(DhSectionPos pos) {
        topDetailLevel.updateAndGet(v -> Math.max(v, pos.sectionDetail));
        DataMetaFile metaFile = atomicGetOrMakeFile(pos);
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
