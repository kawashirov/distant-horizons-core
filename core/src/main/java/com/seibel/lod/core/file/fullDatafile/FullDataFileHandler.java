package com.seibel.lod.core.file.fullDatafile;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.config.listeners.ConfigChangeListener;
import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.lod.core.dataObjects.fullData.sources.*;
import com.seibel.lod.core.dataObjects.fullData.sources.CompleteFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IIncompleteFullDataSource;
import com.seibel.lod.core.util.FileUtil;
import com.seibel.lod.core.file.metaData.BaseMetaData;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.ThreadUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class FullDataFileHandler implements IFullDataSourceProvider
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	protected static ExecutorService fileHandlerThreadPool;
	protected static ConfigChangeListener<Integer> configListener;
	
	protected final ConcurrentHashMap<DhSectionPos, FullDataMetaFile> fileBySectionPos = new ConcurrentHashMap<>();
	protected final IDhLevel level;
	protected final File saveDir;
	protected final AtomicInteger topDetailLevel = new AtomicInteger(-1);
	protected final int minDetailLevel = CompleteFullDataSource.SECTION_SIZE_OFFSET;
	
	
	
    public FullDataFileHandler(IDhLevel level, File saveRootDir)
	{
        this.saveDir = saveRootDir;
        this.level = level;
    }
	
	
	
    /**
    * Caller must ensure that this method is called only once,
    *  and that the {@link FullDataFileHandler} is not used before this method is called.
     */
    @Override
    public void addScannedFile(Collection<File> detectedFiles)
	{
        HashMultimap<DhSectionPos, FullDataMetaFile> filesByPos = HashMultimap.create();
        LOGGER.info("Detected {} valid files in {}", detectedFiles.size(), this.saveDir);

        { // Sort files by pos.
            for (File file : detectedFiles)
			{
                try
				{
                    FullDataMetaFile metaFile = new FullDataMetaFile(this, this.level, file);
                    filesByPos.put(metaFile.pos, metaFile);
                }
				catch (IOException e)
				{
                    LOGGER.error("Failed to read data meta file at "+file+": ", e);
                    FileUtil.renameCorruptedFile(file);
                }
            }
        }

        // Warn for multiple files with the same pos, and then select the one with the latest timestamp.
        for (DhSectionPos pos : filesByPos.keySet())
		{
            Collection<FullDataMetaFile> metaFiles = filesByPos.get(pos);
            FullDataMetaFile fileToUse;
            if (metaFiles.size() > 1)
			{
//                fileToUse = Collections.max(metaFiles, Comparator.comparingLong(a -> a.metaData.dataVersion.get()));
				
                fileToUse = Collections.max(metaFiles, Comparator.comparingLong(fullDataMetaFile -> fullDataMetaFile.file.lastModified()));
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Multiple files with the same pos: ");
                    sb.append(pos);
                    sb.append("\n");
                    for (FullDataMetaFile metaFile : metaFiles)
					{
                        sb.append("\t");
                        sb.append(metaFile.file);
                        sb.append("\n");
                    }
                    sb.append("\tUsing: ");
                    sb.append(fileToUse.file);
                    sb.append("\n");
                    sb.append("(Other files will be renamed by appending \".old\" to their name.)");
                    LOGGER.warn(sb.toString());

                    // Rename all other files with the same pos to .old
                    for (FullDataMetaFile metaFile : metaFiles)
					{
                        if (metaFile == fileToUse)
						{
							continue;
						}
                        File oldFile = new File(metaFile.file + ".old");
                        try
						{
                            if (!metaFile.file.renameTo(oldFile))
							{
								throw new RuntimeException("Renaming failed");
							}
                        }
						catch (Exception e)
						{
                            LOGGER.error("Failed to rename file: " + metaFile.file + " to " + oldFile, e);
                        }
                    }
                }
            }
			else
			{
                fileToUse = metaFiles.iterator().next();
            }
            // Add file to the list of files.
			this.topDetailLevel.updateAndGet(v -> Math.max(v, fileToUse.pos.sectionDetailLevel));
			this.fileBySectionPos.put(pos, fileToUse);
        }
    }
	
    protected FullDataMetaFile getOrMakeFile(DhSectionPos pos)
	{
        FullDataMetaFile metaFile = this.fileBySectionPos.get(pos);
        if (metaFile == null)
		{
            FullDataMetaFile newMetaFile;
            try
			{
                newMetaFile = new FullDataMetaFile(this, this.level, pos);
            }
			catch (IOException e)
			{
                LOGGER.error("IOException on creating new data file at {}", pos, e);
                return null;
            }
			
            metaFile = this.fileBySectionPos.putIfAbsent(pos, newMetaFile); // This is a CAS with expected null value.
            if (metaFile == null) 
			{
				metaFile = newMetaFile;
			}
        }
        return metaFile;
    }
	
	/**
	 * Populates the preexistingFiles and missingFilePositions ArrayLists.
	 * 
	 * @param preexistingFiles the list of {@link FullDataMetaFile}'s that have been created for the given position.
	 * @param missingFilePositions the list of {@link DhSectionPos}'s that don't have {@link FullDataMetaFile} created for them yet.
	 */
    protected void getDataFilesForPosition(DhSectionPos basePos, DhSectionPos pos, 
			ArrayList<FullDataMetaFile> preexistingFiles, ArrayList<DhSectionPos> missingFilePositions)
	{
        byte sectionDetail = pos.sectionDetailLevel;
        boolean allEmpty = true;
		
        outerLoop:
        while (--sectionDetail >= this.minDetailLevel)
		{
            DhLodPos minPos = pos.getCorner().getCornerLodPos(sectionDetail);
            int count = pos.getSectionBBoxPos().getWidthAtDetail(sectionDetail);
			
            for (int xOffset = 0; xOffset < count; xOffset++)
			{
                for (int zOffset = 0; zOffset < count; zOffset++)
				{
                    DhSectionPos subPos = new DhSectionPos(sectionDetail, xOffset+minPos.x, zOffset+minPos.z);
                    LodUtil.assertTrue(pos.overlaps(basePos) && subPos.overlaps(pos));

                    //TODO: The following check is temporary as we only sample corner points, which means
                    // on a very different level, we may not need the entire section at all.
                    if (!CompleteFullDataSource.firstDataPosCanAffectSecond(basePos, subPos))
					{
						continue;
					}
					
                    if (this.fileBySectionPos.containsKey(subPos))
					{
                        allEmpty = false;
                        break outerLoop;
                    }
                }
            }
        }
		
        if (allEmpty)
		{
			// there are no children to this quad tree,
			// add this leaf's position
            missingFilePositions.add(pos);
        }
		else 
		{
			// there are children in this quad tree, search them
			this.recursiveGetDataFilesForPosition(0, basePos, pos, preexistingFiles, missingFilePositions);
			this.recursiveGetDataFilesForPosition(1, basePos, pos, preexistingFiles, missingFilePositions);
			this.recursiveGetDataFilesForPosition(2, basePos, pos, preexistingFiles, missingFilePositions);
			this.recursiveGetDataFilesForPosition(3, basePos, pos, preexistingFiles, missingFilePositions);
        }
    }
	private void recursiveGetDataFilesForPosition(int childIndex, DhSectionPos basePos, DhSectionPos pos, ArrayList<FullDataMetaFile> preexistingFiles, ArrayList<DhSectionPos> missingFilePositions)
	{
		DhSectionPos childPos = pos.getChildByIndex(childIndex);
		if (CompleteFullDataSource.firstDataPosCanAffectSecond(basePos, childPos))
		{
			FullDataMetaFile metaFile = this.fileBySectionPos.get(childPos);
			if (metaFile != null)
			{
				// we have reached a populated leaf node in the quad tree
				preexistingFiles.add(metaFile);
			}
			else if (childPos.sectionDetailLevel == this.minDetailLevel)
			{
				// we have reached an empty leaf node in the quad tree
				missingFilePositions.add(childPos);
			}
			else
			{
				// recursively traverse down the tree
				this.getDataFilesForPosition(basePos, childPos, preexistingFiles, missingFilePositions);
			}
		}
	}
	
	
	/**
	 * Returns the {@link IFullDataSource} for the given section position. <Br>
	 * The returned data source may be null. <Br> <Br>
	 * 
	 * This call is concurrent. I.e. it supports being called by multiple threads at the same time.
	 */
	@Override
    public CompletableFuture<IFullDataSource> read(DhSectionPos pos)
	{
		this.topDetailLevel.updateAndGet(intVal -> Math.max(intVal, pos.sectionDetailLevel));
        FullDataMetaFile metaFile = this.getOrMakeFile(pos);
        if (metaFile == null)
		{
			return CompletableFuture.completedFuture(null);
		}
		
		
		// future wrapper necessary in order to handle file read errors
		CompletableFuture<IFullDataSource> futureWrapper = new CompletableFuture<>();
		metaFile.loadOrGetCachedDataSourceAsync().exceptionally((e) ->
			{
				FullDataMetaFile newMetaFile = this.removeCorruptedFile(pos, metaFile, e);
				
				futureWrapper.completeExceptionally(e);
				return null; // return value doesn't matter
			})
			.whenComplete((dataSource, e) ->
			{
				futureWrapper.complete(dataSource);
			});
		
		return futureWrapper;
    }
	
    /** This call is concurrent. I.e. it supports being called by multiple threads at the same time. */
    @Override
    public void write(DhSectionPos sectionPos, ChunkSizedFullDataAccessor chunkDataView)
	{
        DhLodPos chunkPos = chunkDataView.getLodPos();
        LodUtil.assertTrue(chunkPos.overlapsExactly(sectionPos.getSectionBBoxPos()), "Chunk "+chunkPos+" does not overlap section "+sectionPos);
		
        chunkPos = chunkPos.convertToDetailLevel((byte) this.minDetailLevel);
		this.writeChunkDataToMetaFile(new DhSectionPos(chunkPos.detailLevel, chunkPos.x, chunkPos.z), chunkDataView);
    }
    private void writeChunkDataToMetaFile(DhSectionPos sectionPos, ChunkSizedFullDataAccessor chunkData)
	{
        FullDataMetaFile metaFile = this.fileBySectionPos.get(sectionPos);
        if (metaFile != null)
		{ 
			// there is a file for this position
            metaFile.addToWriteQueue(chunkData);
        }
		
        if (sectionPos.sectionDetailLevel <= this.topDetailLevel.get())
		{
			// recursively attempt to get the meta file for this position
			this.writeChunkDataToMetaFile(sectionPos.getParentPos(), chunkData);
        }
    }
	
    /** This call is concurrent. I.e. it supports multiple threads calling this method at the same time. */
    @Override
    public CompletableFuture<Void> flushAndSave()
	{
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        for (FullDataMetaFile metaFile : this.fileBySectionPos.values())
		{
            futures.add(metaFile.flushAndSaveAsync());
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
	
//    @Override
//    public long getCacheVersion(DhSectionPos sectionPos)
//	{
//        FullDataMetaFile file = this.files.get(sectionPos);
//        if (file == null)
//		{
//			return 0;
//		}
//        return file.getCacheVersion();
//    }
	
//    @Override
//    public boolean isCacheVersionValid(DhSectionPos sectionPos, long cacheVersion)
//	{
//        FullDataMetaFile file = this.files.get(sectionPos);
//        if (file == null)
//		 {
//			return cacheVersion >= 0;
//		}
//        return file.isCacheVersionValid(cacheVersion);
//    }
	
    @Override
    public CompletableFuture<IFullDataSource> onCreateDataFile(FullDataMetaFile file)
	{
        DhSectionPos pos = file.pos;
        ArrayList<FullDataMetaFile> existFiles = new ArrayList<>();
        ArrayList<DhSectionPos> missing = new ArrayList<>();
		this.getDataFilesForPosition(pos, pos, existFiles, missing);
        LodUtil.assertTrue(!missing.isEmpty() || !existFiles.isEmpty());
        if (missing.size() == 1 && existFiles.isEmpty() && missing.get(0).equals(pos))
		{
            // None exist.
            IIncompleteFullDataSource incompleteDataSource = pos.sectionDetailLevel <= HighDetailIncompleteFullDataSource.MAX_SECTION_DETAIL ?
                    HighDetailIncompleteFullDataSource.createEmpty(pos) : LowDetailIncompleteFullDataSource.createEmpty(pos);
            return CompletableFuture.completedFuture(incompleteDataSource);
        }
		else
		{
            for (DhSectionPos missingPos : missing)
			{
                FullDataMetaFile newFile = this.getOrMakeFile(missingPos);
                if (newFile != null)
				{
					existFiles.add(newFile);
				}
            }
            final ArrayList<CompletableFuture<Void>> futures = new ArrayList<>(existFiles.size());
            final IIncompleteFullDataSource incompleteFullDataSource = pos.sectionDetailLevel <= HighDetailIncompleteFullDataSource.MAX_SECTION_DETAIL ? 
					HighDetailIncompleteFullDataSource.createEmpty(pos) : 
					LowDetailIncompleteFullDataSource.createEmpty(pos);

            for (FullDataMetaFile metaFile : existFiles)
			{
                futures.add(metaFile.loadOrGetCachedDataSourceAsync()
                        .thenAccept((data) ->
						{
                            if (data != null)
							{
                                //LOGGER.info("Merging data from {} into {}", data.getSectionPos(), pos);
                                incompleteFullDataSource.sampleFrom(data);
                            }
                        })
						.exceptionally((e) ->
						{
							FullDataMetaFile newMetaFile = this.removeCorruptedFile(pos, metaFile, e);
							return null;
						})
                );
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply((v) -> incompleteFullDataSource.tryPromotingToCompleteDataSource());
     
        }
    }
	private FullDataMetaFile removeCorruptedFile(DhSectionPos pos, FullDataMetaFile metaFile, Throwable exception)
	{
		LOGGER.error("Error reading Data file ["+pos+"]", exception);
		
		FileUtil.renameCorruptedFile(metaFile.file);
		// remove the FullDataMetaFile since the old one was corrupted
		this.fileBySectionPos.remove(pos);
		// create a new FullDataMetaFile to write new data to
		return this.getOrMakeFile(pos);
	}
	
	@Override
    public IFullDataSource onDataFileLoaded(IFullDataSource source, BaseMetaData metaData,
                                          Consumer<IFullDataSource> onUpdated, Function<IFullDataSource, Boolean> updater)
	{
        boolean changed = updater.apply(source);
//		if (changed)
//		{
//		  metaData.dataVersion.incrementAndGet();
//		}
		
        if (source instanceof IIncompleteFullDataSource)
		{
            IFullDataSource newSource = ((IIncompleteFullDataSource) source).tryPromotingToCompleteDataSource();
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
    public CompletableFuture<IFullDataSource> onDataFileRefresh(IFullDataSource source, BaseMetaData metaData, Function<IFullDataSource, Boolean> updater, Consumer<IFullDataSource> onUpdated)
	{
		if (fileHandlerThreadPool.isTerminated())
		{
			return CompletableFuture.completedFuture(source);
		}
		
		
        return CompletableFuture.supplyAsync(() ->
		{
            IFullDataSource sourceLocal = source;
			
            boolean changed = updater.apply(sourceLocal);
//          if (changed)
//			{
//				metaData.dataVersion.incrementAndGet();
//			}
			
			
            if (sourceLocal instanceof IIncompleteFullDataSource)
			{
                IFullDataSource newSource = ((IIncompleteFullDataSource) sourceLocal).tryPromotingToCompleteDataSource();
                changed |= newSource != sourceLocal;
                sourceLocal = newSource;
            }
			
            if (changed)
			{
				onUpdated.accept(sourceLocal);
			}
            return sourceLocal;
        }, fileHandlerThreadPool);
    }
	
    @Override
    public File computeDataFilePath(DhSectionPos pos) { return new File(this.saveDir, pos.serialize() + ".lod"); }
	
	
	
	//==========================//
	// executor handler methods //
	//==========================//
	
	/**
	 * Creates a new executor. <br>
	 * Does nothing if an executor already exists.
	 */
	public static void setupExecutorService()
	{
		// static setup
		if (configListener == null)
		{
			configListener = new ConfigChangeListener<>(Config.Client.Advanced.MultiThreading.numberOfFileHandlerThreads, (threadCount) -> { setThreadPoolSize(threadCount); });
		}
		
		
		if (fileHandlerThreadPool == null || fileHandlerThreadPool.isTerminated())
		{
			LOGGER.info("Starting "+FullDataFileHandler.class.getSimpleName());
			setThreadPoolSize(Config.Client.Advanced.MultiThreading.numberOfFileHandlerThreads.get());
		}
	}
	public static void setThreadPoolSize(int threadPoolSize) { fileHandlerThreadPool = ThreadUtil.makeThreadPool(threadPoolSize, FullDataFileHandler.class.getSimpleName()+"Thread"); }
	
	/**
	 * Stops any executing tasks and destroys the executor. <br>
	 * Does nothing if the executor isn't running.
	 */
	public static void shutdownExecutorService()
	{
		if (fileHandlerThreadPool != null)
		{
			LOGGER.info("Stopping "+ FullDataFileHandler.class.getSimpleName());
			fileHandlerThreadPool.shutdownNow();
		}
	}
	
	@Override
	public ExecutorService getIOExecutor() { return fileHandlerThreadPool; }
	
	
	
	//=========//
	// cleanup //
	//=========//
	
    @Override
    public void close()
	{
        FullDataMetaFile.debugPhantomLifeCycleCheck();
    }
	
}
