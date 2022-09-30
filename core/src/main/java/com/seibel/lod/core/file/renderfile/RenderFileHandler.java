package com.seibel.lod.core.file.renderfile;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.PlaceHolderRenderSource;
import com.seibel.lod.core.datatype.ILodRenderSource;
import com.seibel.lod.core.datatype.AbstractRenderSourceLoader;
import com.seibel.lod.core.datatype.column.ColumnRenderSource;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.datatype.transform.DataRenderTransformer;
import com.seibel.lod.core.file.datafile.IDataSourceProvider;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.objects.UncheckedInterruptedException;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class RenderFileHandler implements IRenderSourceProvider {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    final ExecutorService renderCacheThread = LodUtil.makeSingleThreadPool("RenderCacheThread");
    final ConcurrentHashMap<DhSectionPos, RenderMetaFile> files = new ConcurrentHashMap<>();
    final IDhClientLevel level;
    final File saveDir;
    final IDataSourceProvider dataSourceProvider;

    public RenderFileHandler(IDataSourceProvider sourceProvider, IDhClientLevel level, File saveRootDir) {
        this.dataSourceProvider = sourceProvider;
        this.level = level;
        this.saveDir = saveRootDir;
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
                    RenderMetaFile metaFile = new RenderMetaFile(this, file);
                    filesByPos.put(metaFile.pos, metaFile);
                } catch (IOException e) {
                    LOGGER.error("Failed to read render meta file at {}: ", file, e);
                    File corruptedFile = new File(file.getParentFile(), file.getName() + ".corrupted");
                    if (corruptedFile.exists()) corruptedFile.delete();
                    if (file.renameTo(corruptedFile)) {
                        LOGGER.error("Renamed corrupted file to {}", file.getName() + ".corrupted");
                    } else {
                        LOGGER.error("Failed to rename corrupted file to {}. Will try and delete file", file.getName() + ".corrupted");
                        file.delete();
                    }
                }
            }
        }

        // Warn for multiple files with the same pos, and then select the one with the latest timestamp.
        for (DhSectionPos pos : filesByPos.keySet()) {
            Collection<RenderMetaFile> metaFiles = filesByPos.get(pos);
            RenderMetaFile fileToUse;
            if (metaFiles.size() > 1) {
                fileToUse = Collections.max(metaFiles, Comparator.comparingLong(a -> a.metaData.dataVersion.get()));
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
    public CompletableFuture<ILodRenderSource> read(DhSectionPos pos) {
        RenderMetaFile metaFile = files.get(pos);
        if (metaFile == null) {
            RenderMetaFile newMetaFile;
            try {
                newMetaFile = new RenderMetaFile(this, pos);
            } catch (IOException e) {
                LOGGER.error("IOException on creating new render file at {}", pos, e);
                return null;
            }
            metaFile = files.putIfAbsent(pos, newMetaFile); // This is a CAS with expected null value.
            if (metaFile == null) metaFile = newMetaFile;
        }
        return metaFile.loadOrGetCached(renderCacheThread, level).handle(
                (render, e) -> {
                    if (e != null) {
                        LOGGER.error("Uncaught error on {}:", pos, e);
                    }
                    if (render != null) return render;
                    return new PlaceHolderRenderSource(pos);
                }
        );
    }

    /*
     * This call is concurrent. I.e. it supports multiple threads calling this method at the same time.
     */
    @Override
    public void write(DhSectionPos sectionPos, ChunkSizedData chunkData) {
        if (chunkData.getBBoxLodPos().convertUpwardsTo((byte)6).equals(new DhLodPos((byte)6, 10, -11))) {
            int doNothing = 0;
        }

        recursive_write(sectionPos,chunkData);
        dataSourceProvider.write(sectionPos, chunkData);
    }

    private void recursive_write(DhSectionPos sectPos, ChunkSizedData chunkData) {
        if (!sectPos.getSectionBBoxPos().overlaps(new DhLodPos((byte) (4 + chunkData.dataDetail), chunkData.x, chunkData.z))) return;
        if (sectPos.sectionDetail > ColumnRenderSource.SECTION_SIZE_OFFSET) {
            recursive_write(sectPos.getChild(0), chunkData);
            recursive_write(sectPos.getChild(1), chunkData);
            recursive_write(sectPos.getChild(2), chunkData);
            recursive_write(sectPos.getChild(3), chunkData);
        }
        RenderMetaFile metaFile = files.get(sectPos);
        if (metaFile != null) { // Fast path: if there is a file for this section, just write to it.
            metaFile.updateChunkIfNeeded(chunkData, level);
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

    public File computeRenderFilePath(DhSectionPos pos) {
        return new File(saveDir, pos.serialize() + ".lod");
    }

    public CompletableFuture<ILodRenderSource> onCreateRenderFile(RenderMetaFile file) {
        final int vertSize = Config.Client.Graphics.Quality.verticalQuality
                .get().calculateMaxVerticalData((byte) (file.pos.sectionDetail - ColumnRenderSource.SECTION_SIZE_OFFSET));
        return CompletableFuture.completedFuture(
                new ColumnRenderSource(file.pos, vertSize, level.getMinY()));
    }

    private final ConcurrentHashMap<DhSectionPos, Object> cacheRecreationGuards = new ConcurrentHashMap<>();

    private void updateCache(ILodRenderSource data, RenderMetaFile file) {
        if (cacheRecreationGuards.putIfAbsent(file.pos, new Object()) != null) return;
        final WeakReference<ILodRenderSource> dataRef = new WeakReference<>(data);
        CompletableFuture<ILodDataSource> dataFuture = dataSourceProvider.read(data.getSectionPos());
        final long version = dataSourceProvider.getLatestCacheVersion(data.getSectionPos());
                DataRenderTransformer.asyncTransformDataSource(
                        dataFuture.thenApply((d) -> {
                            if (dataRef.get() == null) throw new UncheckedInterruptedException();
                            LodUtil.assertTrue(d != null);
                            return d;
                        }).exceptionally((ex) -> {
                            if (ex != null)
                                LOGGER.error("Uncaught exception when getting data for updateCache()", ex);
                            return null;
                        })
                        , level)
                .thenAccept((newData) -> write(dataRef.get(), file, newData, version))
                .exceptionally((ex) -> {
                    if (!UncheckedInterruptedException.isThrowableInterruption(ex))
                        LOGGER.error("Exception when updating render file using data source: ", ex);
                    return null;
                }).thenRun(() -> cacheRecreationGuards.remove(file.pos));

    }

    public ILodRenderSource onRenderFileLoaded(ILodRenderSource data, RenderMetaFile file) {
        long newCacheVersion = dataSourceProvider.getLatestCacheVersion(file.pos);
        //NOTE: Do this instead of direct compare so values that wrapped around still works correctly.
        if (newCacheVersion - file.metaData.dataVersion.get() <= 0)
            return data;
        updateCache(data, file);
        return data;
    }

    public ILodRenderSource onLoadingRenderFile(RenderMetaFile file) {
        return null; //Default behaviour
    }

    private void write(ILodRenderSource target, RenderMetaFile file,
                       ILodRenderSource newData, long newDataVersion) {
        if (target == null) return;
        if (newData == null) return;
        target.weakWrite(newData);
        file.metaData.dataVersion.set(newDataVersion);
        file.metaData.dataLevel = target.getDataDetail();
        file.loader = AbstractRenderSourceLoader.getLoader(target.getClass(), target.getRenderVersion());
        file.dataType = target.getClass();
        file.metaData.dataTypeId = file.loader.renderTypeId;
        file.metaData.loaderVersion = target.getRenderVersion();
        file.save(target, level);
    }

    public void onReadRenderSourceFromCache(RenderMetaFile file, ILodRenderSource data) {
        long newCacheVersion = dataSourceProvider.getLatestCacheVersion(file.pos);
        //NOTE: Do this instead of direct compare so values that wrapped around still works correctly.
        if (newCacheVersion - file.metaData.dataVersion.get() > 0)
            updateCache(data, file);
    }

    public boolean refreshRenderSource(ILodRenderSource source) {
        RenderMetaFile file = files.get(source.getSectionPos());
        if (source instanceof PlaceHolderRenderSource) {
            if (file == null || file.metaData == null) {
                return false;
            }
        }
        LodUtil.assertTrue(file != null);
        LodUtil.assertTrue(file.metaData != null);
        long newCacheVersion = dataSourceProvider.getLatestCacheVersion(file.pos);
        //NOTE: Do this instead of direct compare so values that wrapped around still works correctly.
        if (newCacheVersion - file.metaData.dataVersion.get() <= 0)
            return false;
        updateCache(source, file);
        return true;
    }

}
