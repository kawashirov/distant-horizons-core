package com.seibel.lod.core.a7.save.io.render;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.a7.PlaceHolderQueue;
import com.seibel.lod.core.a7.datatype.PlaceHolderRenderSource;
import com.seibel.lod.core.a7.datatype.LodRenderSource;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.save.io.file.IDataSourceProvider;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class RenderFileHandler implements IRenderSourceProvider {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    final ExecutorService renderCacheThread = LodUtil.makeSingleThreadPool("RenderCacheThread");
    final ConcurrentHashMap<DhSectionPos, RenderMetaFile> files = new ConcurrentHashMap<>();
    final IClientLevel level;
    final File saveDir;
    final IDataSourceProvider dataSourceProvider;
    final PlaceHolderQueue placeHolderQueue;

    public RenderFileHandler(IDataSourceProvider sourceProvider, IClientLevel level, File saveRootDir, PlaceHolderQueue placeHolderQueue) {
        this.dataSourceProvider = sourceProvider;
        this.level = level;
        this.saveDir = saveRootDir;
        this.placeHolderQueue = placeHolderQueue;
    }

    /*
     * Caller must ensure that this method is called only once,
     *  and that this object is not used before this method is called.
     */
    @Override
    public void addScannedFile(Collection<File> detectedFiles) {
        HashMultimap<DhSectionPos, RenderMetaFile> filesByPos = HashMultimap.create();
        { // Sort files by pos.
            for (File file : detectedFiles) {
                try {
                    RenderMetaFile metaFile = new RenderMetaFile(
                            dataSourceProvider::isCacheValid,
                            dataSourceProvider::read,
                            level, file
                    );
                    filesByPos.put(metaFile.pos, metaFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Warn for multiple files with the same pos, and then select the one with latest timestamp.
        for (DhSectionPos pos : filesByPos.keySet()) {
            Collection<RenderMetaFile> metaFiles = filesByPos.get(pos);
            RenderMetaFile fileToUse;
            if (metaFiles.size() > 1) {
                fileToUse = Collections.max(metaFiles, Comparator.comparingLong(a -> a.timestamp));
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Multiple files with the same pos: ");
                    sb.append(pos);
                    sb.append("\n");
                    for (RenderMetaFile metaFile : metaFiles) {
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
                    for (RenderMetaFile metaFile : metaFiles) {
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
            files.put(pos, fileToUse);
        }
    }

    /*
     * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public CompletableFuture<LodRenderSource> read(DhSectionPos pos) {
        RenderMetaFile metaFile = files.computeIfAbsent(pos, (p) -> new RenderMetaFile(
                dataSourceProvider::isCacheValid,
                dataSourceProvider::read,
                level, computeDefaultFilePath(p), p));
        return metaFile.loadOrGetCached(renderCacheThread).handle(
                (render, e) -> {
                    if (e != null) {
                        LOGGER.error("Uncaught error on {}:", pos, e);
                    }
                    if (render != null) return render;
                    PlaceHolderRenderSource placeHolder = new PlaceHolderRenderSource(pos);
                    placeHolderQueue.track(placeHolder);
                    return placeHolder;
                }
        );
    }

    /*
     * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public void write(DhSectionPos sectionPos, ChunkSizedData chunkData) {
        dataSourceProvider.write(sectionPos, chunkData);
        RenderMetaFile metaFile = files.get(sectionPos);
        if (metaFile != null) { // Fast path: if there is a file for this section, just write to it.
            metaFile.updateChunkIfNeeded(chunkData);
        }
    }

    /*
     * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public CompletableFuture<Void> flushAndSave() {
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<CompletableFuture<Void>>();
        for (RenderMetaFile metaFile : files.values()) {
            futures.add(metaFile.flushAndSave(renderCacheThread));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private File computeDefaultFilePath(DhSectionPos pos) { //TODO: Temp code as we haven't decided on the file naming & location yet.
        return new File(saveDir, pos.serialize() + ".lod");
    }

    @Override
    public void close() {
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<CompletableFuture<Void>>();
        for (RenderMetaFile metaFile : files.values()) {
            futures.add(metaFile.flushAndSave(renderCacheThread));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
