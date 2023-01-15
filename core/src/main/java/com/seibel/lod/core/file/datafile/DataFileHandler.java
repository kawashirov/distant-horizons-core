package com.seibel.lod.core.file.datafile;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.datatype.IIncompleteDataSource;
import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.datatype.full.FullDataSource;
import com.seibel.lod.core.datatype.full.SparseDataSource;
import com.seibel.lod.core.datatype.full.SpottyDataSource;
import com.seibel.lod.core.file.metaData.MetaData;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class DataFileHandler implements IDataSourceProvider
{
    // Note: Single main thread only for now. May make it multi-thread later, depending on the usage.
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    final ExecutorService fileReaderThread = LodUtil.makeThreadPool(4, "FileReaderThread");
    final ConcurrentHashMap<DhSectionPos, DataMetaFile> files = new ConcurrentHashMap<>();
    final IDhLevel level;
    final File saveDir;
    AtomicInteger topDetailLevel = new AtomicInteger(-1);
    final int minDetailLevel = FullDataSource.SECTION_SIZE_OFFSET;

    public DataFileHandler(IDhLevel level, File saveRootDir)
	{
        this.saveDir = saveRootDir;
        this.level = level;
    }

    /*
    * Caller must ensure that this method is called only once,
    *  and that this object is not used before this method is called.
     */
    @Override
    public void addScannedFile(Collection<File> detectedFiles)
	{
        HashMultimap<DhSectionPos, DataMetaFile> filesByPos = HashMultimap.create();
        LOGGER.info("Detected {} valid files in {}", detectedFiles.size(), this.saveDir);

        { // Sort files by pos.
            for (File file : detectedFiles)
			{
                try
				{
                    DataMetaFile metaFile = new DataMetaFile(this, this.level, file);
                    filesByPos.put(metaFile.pos, metaFile);
                }
				catch (IOException e)
				{
                    LOGGER.error("Failed to read data meta file at {}: ", file, e);
                    File corruptedFile = new File(file.getParentFile(), file.getName() + ".corrupted");
                    
					if (corruptedFile.exists())
					{
						if (!corruptedFile.delete())
						{
							LOGGER.error("Failed to delete corrupted meta data file at {}: ", corruptedFile, e);
						}
					}
					
                    if (file.renameTo(corruptedFile))
					{
                        LOGGER.error("Renamed corrupted file to {}", file.getName() + ".corrupted");
                    }
					else
					{
                        LOGGER.error("Failed to rename corrupted file to {}. Will try and delete file", file.getName() + ".corrupted");
                        if (file.delete())
						{
							LOGGER.error("Failed to delete corrupted meta data file at {}: ", file, e);
						}
                    }
                }
            }
        }

        // Warn for multiple files with the same pos, and then select the one with latest timestamp.
        for (DhSectionPos pos : filesByPos.keySet())
		{
            Collection<DataMetaFile> metaFiles = filesByPos.get(pos);
            DataMetaFile fileToUse;
            if (metaFiles.size() > 1)
			{
                fileToUse = Collections.max(metaFiles, Comparator.comparingLong(a -> a.metaData.dataVersion.get()));
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Multiple files with the same pos: ");
                    sb.append(pos);
                    sb.append("\n");
                    for (DataMetaFile metaFile : metaFiles)
					{
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
                    for (DataMetaFile metaFile : metaFiles)
					{
                        if (metaFile == fileToUse)
						{
							continue;
						}
                        File oldFile = new File(metaFile.path + ".old");
                        try
						{
                            if (!metaFile.path.renameTo(oldFile))
							{
								throw new RuntimeException("Renaming failed");
							}
                        }
						catch (Exception e)
						{
                            LOGGER.error("Failed to rename file: " + metaFile.path + " to " + oldFile, e);
                        }
                    }
                }
            }
			else
			{
                fileToUse = metaFiles.iterator().next();
            }
            // Add file to the list of files.
			this.topDetailLevel.updateAndGet(v -> Math.max(v, fileToUse.pos.sectionDetail));
			this.files.put(pos, fileToUse);
        }
    }
	
    protected DataMetaFile atomicGetOrMakeFile(DhSectionPos pos)
	{
        DataMetaFile metaFile = this.files.get(pos);
        if (metaFile == null)
		{
            DataMetaFile newMetaFile;
            try
			{
                newMetaFile = new DataMetaFile(this, this.level, pos);
            }
			catch (IOException e)
			{
                LOGGER.error("IOException on creating new data file at {}", pos, e);
                return null;
            }
            metaFile = this.files.putIfAbsent(pos, newMetaFile); // This is a CAS with expected null value.
            if (metaFile == null) 
			{
				metaFile = newMetaFile;
			}
        }
        return metaFile;
    }
	
    protected void selfSearch(DhSectionPos basePos, DhSectionPos pos, ArrayList<DataMetaFile> existFiles, ArrayList<DhSectionPos> missing)
	{
        byte detail = pos.sectionDetail;
        boolean allEmpty = true;
        outerLoop:
        while (--detail >= this.minDetailLevel)
		{
            DhLodPos min = pos.getCorner().getCorner(detail);
            int count = pos.getSectionBBoxPos().getBlockWidth(detail);
            for (int ox = 0; ox<count; ox++)
			{
                for (int oz = 0; oz<count; oz++)
				{
                    DhSectionPos subPos = new DhSectionPos(detail, ox+min.x, oz+min.z);
                    LodUtil.assertTrue(pos.overlaps(basePos) && subPos.overlaps(pos));

                    //TODO: The following check is temp as we only samples corner points per data, which means
                    // on a very different level, we may not need the entire section at all.
                    if (!FullDataSource.neededForPosition(basePos, subPos)) continue;

                    if (this.files.containsKey(subPos))
					{
                        allEmpty = false;
                        break outerLoop;
                    }
                }
            }
        }
		
        if (allEmpty)
		{
            missing.add(pos);
        }
		else 
		{
            {
                DhSectionPos childPos = pos.getChildByIndex(0);
                if (FullDataSource.neededForPosition(basePos, childPos))
				{
                    DataMetaFile metaFile = this.files.get(childPos);
                    if (metaFile != null)
					{
                        existFiles.add(metaFile);
                    }
					else if (childPos.sectionDetail == this.minDetailLevel)
					{
                        missing.add(childPos);
                    }
					else
					{
						this.selfSearch(basePos, childPos, existFiles, missing);
                    }
                }
            }
			
            {
                DhSectionPos childPos = pos.getChildByIndex(1);
                if (FullDataSource.neededForPosition(basePos, childPos))
				{
                    DataMetaFile metaFile = this.files.get(childPos);
                    if (metaFile != null)
					{
                        existFiles.add(metaFile);
                    } else if (childPos.sectionDetail == this.minDetailLevel)
					{
                        missing.add(childPos);
                    }
					else
					{
						this.selfSearch(basePos, childPos, existFiles, missing);
                    }
                }
            }
			
            {
                DhSectionPos childPos = pos.getChildByIndex(2);
                if (FullDataSource.neededForPosition(basePos, childPos))
				{
                    DataMetaFile metaFile = this.files.get(childPos);
                    if (metaFile != null)
					{
                        existFiles.add(metaFile);
                    }
					else if (childPos.sectionDetail == this.minDetailLevel)
					{
                        missing.add(childPos);
                    }
					else
					{
						this.selfSearch(basePos, childPos, existFiles, missing);
                    }
                }
            }
			
            {
                DhSectionPos childPos = pos.getChildByIndex(3);
                if (FullDataSource.neededForPosition(basePos, childPos))
				{
                    DataMetaFile metaFile = this.files.get(childPos);
                    if (metaFile != null)
					{
                        existFiles.add(metaFile);
                    }
					else if (childPos.sectionDetail == this.minDetailLevel)
					{
                        missing.add(childPos);
                    }
					else
					{
						this.selfSearch(basePos, childPos, existFiles, missing);
                    }
                }
            }
        }
    }
	
	
	/**
	 * Returns the 64 x 64 data source for the given section position. <Br>
	 * If the section hasn't been generated this will also send a generation call, which may take a while. <Br>
	 * The returned data source may be null. <Br> <Br>
	 * 
	 * This call is concurrent. I.e. it supports being called by multiple threads at the same time.
	 */
	@Override
    public CompletableFuture<ILodDataSource> read(DhSectionPos pos)
	{
		this.topDetailLevel.updateAndGet(v -> Math.max(v, pos.sectionDetail));
        DataMetaFile metaFile = this.atomicGetOrMakeFile(pos);
        if (metaFile == null)
		{
			return CompletableFuture.completedFuture(null);
		}
        return metaFile.loadOrGetCached();
    }
	
    /** This call is concurrent. I.e. it supports being called by multiple threads at the same time. */
    @Override
    public void write(DhSectionPos sectionPos, ChunkSizedData chunkData)
	{
        DhLodPos chunkPos = new DhLodPos((byte) (chunkData.dataDetail+4), chunkData.x, chunkData.z);
        LodUtil.assertTrue(chunkPos.overlaps(sectionPos.getSectionBBoxPos()), "Chunk {} does not overlap section {}", chunkPos, sectionPos);
        chunkPos = chunkPos.convertToDetailLevel((byte) this.minDetailLevel);
		this.recursiveWrite(new DhSectionPos(chunkPos.detailLevel, chunkPos.x, chunkPos.z), chunkData);
    }
    private void recursiveWrite(DhSectionPos sectionPos, ChunkSizedData chunkData)
	{
        DataMetaFile metaFile = this.files.get(sectionPos);
        if (metaFile != null)
		{ 
			// Fast path: if there is a file for this section, just write to it.
            metaFile.addToWriteQueue(chunkData);
        }
		
        if (sectionPos.sectionDetail <= this.topDetailLevel.get())
		{
			this.recursiveWrite(sectionPos.getParentPos(), chunkData);
        }
    }
	
    /** This call is concurrent. I.e. it supports multiple threads calling this method at the same time. */
    @Override
    public CompletableFuture<Void> flushAndSave()
	{
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        for (DataMetaFile metaFile : this.files.values())
		{
            futures.add(metaFile.flushAndSave());
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
	
    @Override
    public long getCacheVersion(DhSectionPos sectionPos)
	{
        DataMetaFile file = this.files.get(sectionPos);
        if (file == null)
		{
			return 0;
		}
        return file.getCacheVersion();
    }
	
    @Override
    public boolean isCacheVersionValid(DhSectionPos sectionPos, long cacheVersion)
	{
        DataMetaFile file = this.files.get(sectionPos);
        if (file == null)
		 {
			return cacheVersion >= 0;
		}
        return file.isCacheVersionValid(cacheVersion);
    }
	
    @Override
    public CompletableFuture<ILodDataSource> onCreateDataFile(DataMetaFile file)
	{
        DhSectionPos pos = file.pos;
        ArrayList<DataMetaFile> existFiles = new ArrayList<>();
        ArrayList<DhSectionPos> missing = new ArrayList<>();
		this.selfSearch(pos, pos, existFiles, missing);
        LodUtil.assertTrue(!missing.isEmpty() || !existFiles.isEmpty());
        if (missing.size() == 1 && existFiles.isEmpty() && missing.get(0).equals(pos))
		{
            // None exist.
            IIncompleteDataSource incompleteDataSource = pos.sectionDetail <= SparseDataSource.MAX_SECTION_DETAIL ?
                    SparseDataSource.createEmpty(pos) : SpottyDataSource.createEmpty(pos);
            return CompletableFuture.completedFuture(incompleteDataSource);
        }
		else
		{
            for (DhSectionPos missingPos : missing)
			{
                DataMetaFile newFile = this.atomicGetOrMakeFile(missingPos);
                if (newFile != null)
				{
					existFiles.add(newFile);
				}
            }
            final ArrayList<CompletableFuture<Void>> futures = new ArrayList<>(existFiles.size());
            final IIncompleteDataSource dataSource = pos.sectionDetail <= SparseDataSource.MAX_SECTION_DETAIL ?
                    SparseDataSource.createEmpty(pos) : 
					SpottyDataSource.createEmpty(pos);

            for (DataMetaFile f : existFiles)
			{
                futures.add(f.loadOrGetCached()
                        .exceptionally((ex) -> null)
                        .thenAccept((data) ->
						{
                            if (data != null)
							{
                                LOGGER.info("Merging data from {} into {}", data.getSectionPos(), pos);
                                dataSource.sampleFrom(data);
                            }
                        })
                );
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .thenApply((v) -> dataSource.trySelfPromote());
        }
    }
	
    @Override
    public ILodDataSource onDataFileLoaded(ILodDataSource source, MetaData metaData,
                                          Consumer<ILodDataSource> onUpdated, Function<ILodDataSource, Boolean> updater)
	{
        boolean changed = updater.apply(source);
        if (changed)
		{
			metaData.dataVersion.incrementAndGet();
		}
		
        if (source instanceof IIncompleteDataSource)
		{
            ILodDataSource newSource = ((IIncompleteDataSource) source).trySelfPromote();
            changed |= newSource != source;
            source = newSource;
        }
		
        if (changed) 
		{
			onUpdated.accept(source);
		}
        return source;
    }
    @Override
    public CompletableFuture<ILodDataSource> onDataFileRefresh(ILodDataSource source, MetaData metaData, Function<ILodDataSource, Boolean> updater, Consumer<ILodDataSource> onUpdated)
	{
        return CompletableFuture.supplyAsync(() ->
		{
            ILodDataSource sourceLocal = source;
            boolean changed = updater.apply(sourceLocal);
            if (changed) metaData.dataVersion.incrementAndGet();
            if (sourceLocal instanceof IIncompleteDataSource)
			{
                ILodDataSource newSource = ((IIncompleteDataSource) sourceLocal).trySelfPromote();
                changed |= newSource != sourceLocal;
                sourceLocal = newSource;
            }
            if (changed)
			{
				onUpdated.accept(sourceLocal);
			}
            return sourceLocal;
        }, this.fileReaderThread);
    }
	
    @Override
    public File computeDataFilePath(DhSectionPos pos)
	{
        return new File(this.saveDir, pos.serialize() + ".lod");
    }
	
    @Override
    public Executor getIOExecutor()
	{
        return this.fileReaderThread;
    }
	
	
    @Override
    public void close()
	{
        DataMetaFile.debugCheck();
         //TODO
    }
}
