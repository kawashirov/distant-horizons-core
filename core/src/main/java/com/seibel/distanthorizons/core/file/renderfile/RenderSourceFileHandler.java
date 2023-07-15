package com.seibel.distanthorizons.core.file.renderfile;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.distanthorizons.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.distanthorizons.core.file.structure.AbstractSaveStructure;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhLodPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.dataObjects.render.ColumnRenderSource;
import com.seibel.distanthorizons.core.dataObjects.transformers.DataRenderTransformer;
import com.seibel.distanthorizons.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.render.renderer.DebugRenderer;
import com.seibel.distanthorizons.core.util.FileScanUtil;
import com.seibel.distanthorizons.core.util.FileUtil;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.util.objects.UncheckedInterruptedException;
import com.seibel.distanthorizons.core.config.Config;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.seibel.distanthorizons.core.util.FileScanUtil.RENDER_FILE_POSTFIX;

public class RenderSourceFileHandler implements ILodRenderSourceProvider
{
	public static final boolean USE_LAZY_LOADING = true;
	public static final long RENDER_SOURCE_TYPE_ID = ColumnRenderSource.TYPE_ID;
	
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private final ExecutorService fileHandlerThreadPool = ThreadUtil.makeSingleThreadPool("Render Source File Handler");

	private final ConcurrentHashMap<DhSectionPos, File> unloadedFiles = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<DhSectionPos, RenderMetaDataFile> filesBySectionPos = new ConcurrentHashMap<>();
	
	private final IDhClientLevel level;
	private final File saveDir;
	/** This is the lowest (highest numeric) detail level that this {@link RenderSourceFileHandler} is keeping track of. */
	AtomicInteger topDetailLevel = new AtomicInteger(6);
	private final IFullDataSourceProvider fullDataSourceProvider;


	public RenderSourceFileHandler(IFullDataSourceProvider sourceProvider, IDhClientLevel level, AbstractSaveStructure saveStructure)
	{
        this.fullDataSourceProvider = sourceProvider;
        this.level = level;
        this.saveDir = saveStructure.getRenderCacheFolder(level.getLevelWrapper());
		if (!this.saveDir.exists() && !this.saveDir.mkdirs())
		{
			LOGGER.warn("Unable to create render data folder, file saving may fail.");
		}
		FileScanUtil.scanFiles(saveStructure, level.getLevelWrapper(), null, this);
    }
	
	
	
	//===============//
	// file handling //
	//===============//
	
	/**
	 * Caller must ensure that this method is called only once,
	 * and that the given files are not used before this method is called.
	 */
	@Override
	public void addScannedFile(Collection<File> detectedFiles)
	{
		if (USE_LAZY_LOADING)
		{
			this.lazyAddScannedFile(detectedFiles);
		}
		else
		{
			this.immediateAddScannedFile(detectedFiles);
		}
	}

	private void lazyAddScannedFile(Collection<File> detectedFiles)
	{
		for (File file : detectedFiles)
		{
			if (file == null || !file.exists())
			{
				// can rarely happen if the user rapidly travels between dimensions
				LOGGER.warn("Null or non-existent render file: " + ((file != null) ? file.getPath() : "NULL"));
				continue;
			}
			
			
			try
			{
				DhSectionPos pos = this.decodePositionByFile(file);
				if (pos != null)
				{
					this.unloadedFiles.put(pos, file);
					this.topDetailLevel.updateAndGet(v -> Math.max(v, pos.sectionDetailLevel));
				}
			}
			catch (Exception e)
			{
				LOGGER.error("Failed to read data meta file at "+file+": ", e);
				FileUtil.renameCorruptedFile(file);
			}
		}
	}

	private void immediateAddScannedFile(Collection<File> newRenderFiles)
	{
		HashMultimap<DhSectionPos, RenderMetaDataFile> filesByPos = HashMultimap.create();
		
		// Sort files by pos.
		for (File file : newRenderFiles)
		{
			try
			{
				RenderMetaDataFile metaFile = RenderMetaDataFile.createFromExistingFile(this, file);
				filesByPos.put(metaFile.pos, metaFile);
			}
			catch (IOException e)
			{
				LOGGER.error("Failed to read render meta file at ["+file+"]. Error: ", e);
				FileUtil.renameCorruptedFile(file);
			}
		}
		
		// Warn for multiple files with the same pos, and then select the one with the latest timestamp.
		for (DhSectionPos pos : filesByPos.keySet())
		{
			Collection<RenderMetaDataFile> metaFiles = filesByPos.get(pos);
			RenderMetaDataFile fileToUse;
			if (metaFiles.size() > 1)
			{
				//fileToUse = metaFiles.stream().findFirst().orElse(null); // use the first file in the list
				
				// use the file's last modified date
				fileToUse = Collections.max(metaFiles, Comparator.comparingLong(renderMetaDataFile ->
						renderMetaDataFile.file.lastModified()));
				
//				fileToUse = Collections.max(metaFiles, Comparator.comparingLong(renderMetaDataFile -> 
//						renderMetaDataFile.metaData.dataVersion.get()));
				{
					StringBuilder sb = new StringBuilder();
					sb.append("Multiple files with the same pos: ");
					sb.append(pos);
					sb.append("\n");
					for (RenderMetaDataFile metaFile : metaFiles)
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
					for (RenderMetaDataFile metaFile : metaFiles)
					{
						if (metaFile == fileToUse)
						{
							continue;
						}
						
						File oldFile = new File(metaFile.file + ".old");
						try
						{
							if (!metaFile.file.renameTo(oldFile))
								throw new RuntimeException("Renaming failed");
						}
						catch (Exception e)
						{
							LOGGER.error("Failed to rename file: [" + metaFile.file + "] to [" + oldFile + "]", e);
						}
					}
				}
			}
			else
			{
				fileToUse = metaFiles.iterator().next();
			}
			// Add this file to the list of files.
			this.filesBySectionPos.put(pos, fileToUse);
			// increase the lowest detail level if a new lower detail file is found
			this.topDetailLevel.updateAndGet(v -> Math.max(v, pos.sectionDetailLevel));
		}
	}

	protected RenderMetaDataFile getLoadOrMakeFile(DhSectionPos pos, boolean allowCreateFile)
	{
		RenderMetaDataFile metaFile = this.filesBySectionPos.get(pos);
		if (metaFile != null)
		{
			return metaFile;
		}
		

		File fileToLoad = this.unloadedFiles.get(pos);
		if (fileToLoad != null && !fileToLoad.exists())
		{
			fileToLoad = null;
			this.unloadedFiles.remove(pos);
		}
		
		// File does exist, but not loaded yet.
		if (fileToLoad != null)
		{
			synchronized (this)
			{
				// Double check locking for loading file, as loading file means also loading the metadata, which
				// while not... Very expensive, is still better to avoid multiple threads doing it, and dumping the
				// duplicated work to the trash. Therefore, eating the overhead of 'synchronized' is worth it.
				metaFile = this.filesBySectionPos.get(pos);
				if (metaFile != null)
				{
					return metaFile; // someone else loaded it already.
				}
					
				try
				{
					metaFile = RenderMetaDataFile.createFromExistingFile(this, fileToLoad);
					this.topDetailLevel.updateAndGet(v -> Math.max(v, pos.sectionDetailLevel));
					this.filesBySectionPos.put(pos, metaFile);
					return metaFile;
				}
				catch (IOException e)
				{
					LOGGER.error("Failed to read render meta file at " + fileToLoad + ": ", e);
					FileUtil.renameCorruptedFile(fileToLoad);
				}
				finally
				{
					this.unloadedFiles.remove(pos);
				}
			}
		}
		
		
		if (!allowCreateFile)
		{
			return null;
		}
		
		// File does not exist, create it.
		// In this case, since 'creating' a file object doesn't actually do anything heavy on IO yet, we use CAS
		// to avoid overhead of 'synchronized', and eat the mini-overhead of possibly creating duplicate objects.
		try
		{
			metaFile = RenderMetaDataFile.createNewFileForPos(this, pos);
		}
		catch (IOException e)
		{
			LOGGER.error("IOException on creating new data file at {}", pos, e);
			return null;
		}
		
		this.topDetailLevel.updateAndGet(v -> Math.max(v, pos.sectionDetailLevel));
		// This is a CAS with expected null value.
		RenderMetaDataFile metaFileCas = this.filesBySectionPos.putIfAbsent(pos, metaFile);
		return metaFileCas == null ? metaFile : metaFileCas;
	}

	/** This call is concurrent. I.e. it supports multiple threads calling this method at the same time. */
    @Override
    public CompletableFuture<ColumnRenderSource> readAsync(DhSectionPos pos)
	{
		// don't continue if the handler has been shut down
		if (this.fileHandlerThreadPool.isTerminated())
		{
			return CompletableFuture.completedFuture(null);
		}
		
        RenderMetaDataFile metaFile = this.getLoadOrMakeFile(pos, true);

		// On error, (when it returns null,) return an empty render source
		if (metaFile == null) return CompletableFuture.completedFuture(ColumnRenderSource.createEmptyRenderSource(pos));
		
        return metaFile.loadOrGetCachedDataSourceAsync(this.fileHandlerThreadPool, this.level).handle(
			(renderSource, exception) ->
			{
				if (exception != null)
				{
					LOGGER.error("Uncaught error on "+pos+":", exception);
				}
				
				return (renderSource != null) ? renderSource : ColumnRenderSource.createEmptyRenderSource(pos);
			});
    }
	
	public CompletableFuture<ColumnRenderSource> onCreateRenderFileAsync(RenderMetaDataFile file)
	{
		final int verticalSize = Config.Client.Advanced.Graphics.Quality.verticalQuality.get()
				.calculateMaxVerticalData((byte) (file.pos.sectionDetailLevel - ColumnRenderSource.SECTION_SIZE_OFFSET));
		
		return CompletableFuture.completedFuture(
				new ColumnRenderSource(file.pos, verticalSize, this.level.getMinY()));
	}
	
	
	
	//=============//
	// data saving //
	//=============//
	
    /**
	 * This call is concurrent. I.e. it supports multiple threads calling this method at the same time. <br>
	 * This allows fast writes of new data to the render source, without having to wait for the data to be written to disk.
	 */
    @Override
    public void writeChunkDataToFile(DhSectionPos sectionPos, ChunkSizedFullDataAccessor chunkDataView)
	{
		// convert to the lowest detail level so all detail levels are updated
		this.fastWriteDataToSourceRecursively(chunkDataView, DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
		this.fullDataSourceProvider.write(sectionPos, chunkDataView);
    }

	private void fastWriteDataToSourceRecursively(ChunkSizedFullDataAccessor chunk, byte sectionDetailLevel)
	{
		DhLodPos boundingPos = chunk.getLodPos();
		DhLodPos sectPosMin = boundingPos.convertToDetailLevel(sectionDetailLevel);
		int width = sectionDetailLevel > boundingPos.detailLevel ? 1 : boundingPos.getWidthAtDetail(sectionDetailLevel);
		for (int ox = 0; ox < width; ox++) {
			for (int oz = 0; oz < width; oz++) {
				DhSectionPos sectPos = new DhSectionPos(sectionDetailLevel, sectPosMin.x + ox, sectPosMin.z + oz);
				RenderMetaDataFile metaFile = this.filesBySectionPos.get(sectPos); // bypass the getLoadOrMakeFile(), as we only want in-cache files.
				if (metaFile != null)
				{
					metaFile.updateChunkIfSourceExists(chunk, this.level);
				}
			}
		}
		if (sectionDetailLevel < topDetailLevel.get()) {
			fastWriteDataToSourceRecursively(chunk, (byte) (sectionDetailLevel + 1));
		}
	}

	
    /** This call is concurrent. I.e. it supports multiple threads calling this method at the same time. */
    @Override
	public CompletableFuture<Void> flushAndSaveAsync()
	{
		LOGGER.info("Shutting down "+ RenderSourceFileHandler.class.getSimpleName()+"...");
		
		ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
		for (RenderMetaDataFile metaFile : this.filesBySectionPos.values())
		{
			futures.add(metaFile.flushAndSave(this.fileHandlerThreadPool));
		}
		
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
				.whenComplete((voidObj, exception) -> LOGGER.info("Finished shutting down "+ RenderSourceFileHandler.class.getSimpleName()) );
	}
	
	
	
	//================//
	// cache updating //
	//================//

    private CompletableFuture<Void> updateCacheAsync(ColumnRenderSource renderSource, RenderMetaDataFile file)
	{
		DebugRenderer.BoxWithLife box = new DebugRenderer.BoxWithLife(new DebugRenderer.Box(renderSource.sectionPos, 74f, 86f, 0.1f, Color.red), 1.0, 32f, Color.green.darker());

		// get the full data source loading future
		CompletableFuture<IFullDataSource> fullDataSourceFuture = this.fullDataSourceProvider.read(renderSource.getSectionPos());
		fullDataSourceFuture = fullDataSourceFuture.thenApply((fullDataSource) -> 
			{
				// the fullDataSource can be null if the thread this was running on was interrupted
				box.box.color = Color.yellow.darker();
				return fullDataSource;
			}).exceptionally((ex) -> 
			{
				LOGGER.error("Exception when getting data for updateCache()", ex);
				return null;
			});

		// future returned 
		CompletableFuture<Void> transformationCompleteFuture = new CompletableFuture<>();
		
		// convert the full data source into a render source
		//LOGGER.info("Recreating cache for {}", data.getSectionPos());
		DataRenderTransformer.transformDataSourceAsync(fullDataSourceFuture, this.level)
				.whenComplete((newRenderSource, ex) -> 
				{
					if (ex == null)
					{
						this.writeRenderSourceToFile(renderSource, file, newRenderSource);
					}
					else
					{
						if (ex instanceof InterruptedException)
						{
							// expected if the transformer is shut down, the exception can be ignored
//							LOGGER.warn("RenderSource file transforming interrupted.");
							
							int ignoreEmptyWarning = 0; // explicitly handling these exceptions is important so we know where they are going and if there is an issue we can easily re-enable the logging
						}
						else if (ex instanceof RejectedExecutionException || ex.getCause() instanceof RejectedExecutionException)
						{
							// expected if the transformer was already shut down, the exception can be ignored
//							LOGGER.warn("RenderSource file transforming interrupted.");
							
							int ignoreEmptyWarning = 0;
						}
						else if (!UncheckedInterruptedException.isThrowableInterruption(ex))
						{
							LOGGER.error("Exception when updating render file using data source: ", ex);
						}
					}
					box.close();
					transformationCompleteFuture.complete(null);
				});
		return transformationCompleteFuture;
	}
	
	/** TODO at some point this method may need to be made "async" like {@link RenderSourceFileHandler#onReadRenderSourceLoadedFromCacheAsync} since the insides are async */ 
    public ColumnRenderSource onRenderFileLoaded(ColumnRenderSource renderSource, RenderMetaDataFile file)
	{
		this.updateCacheAsync(renderSource, file).join();
        return renderSource;
    }
	
	public CompletableFuture<Void> onReadRenderSourceLoadedFromCacheAsync(RenderMetaDataFile file, ColumnRenderSource data) {
		return this.updateCacheAsync(data, file);
	}
	
    private void writeRenderSourceToFile(ColumnRenderSource currentRenderSource, RenderMetaDataFile file, ColumnRenderSource newRenderSource)
	{
        if (currentRenderSource == null || newRenderSource == null)
		{
			return;
		}
		
        currentRenderSource.updateFromRenderSource(newRenderSource);
		currentRenderSource.localVersion.incrementAndGet();
		
        //file.metaData.dataVersion.set(newDataVersion);
        file.baseMetaData.dataLevel = currentRenderSource.getDataDetail();
        file.baseMetaData.dataTypeId = RENDER_SOURCE_TYPE_ID;
        file.baseMetaData.binaryDataFormatVersion = currentRenderSource.getRenderDataFormatVersion();
        file.save(currentRenderSource);
    }
/*
    public boolean refreshRenderSource(ColumnRenderSource renderSource)
	{
        RenderMetaDataFile file = this.filesBySectionPos.get(renderSource.getSectionPos());
        if (renderSource.isEmpty())
		{
            if (file == null || file.baseMetaData == null)
			{
                return false;
            }
        }
		
        LodUtil.assertTrue(file != null);
        LodUtil.assertTrue(file.baseMetaData != null);
//        if (!this.fullDataSourceProvider.isCacheVersionValid(file.pos, file.metaData.dataVersion.get()))
//		{
			this.updateCacheAsync(renderSource, file).join();
            return true;
//        }
		
//        return false;
    }
	
	*/
	
	//=====================//
	// clearing / shutdown //
	//=====================//
	
	@Override
	public void close()
	{
		ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
		for (RenderMetaDataFile metaFile : this.filesBySectionPos.values())
		{
			futures.add(metaFile.flushAndSave(this.fileHandlerThreadPool));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
	}
	
	
	public void deleteRenderCache()
	{
		// delete each file in the cache directory
		File[] renderFiles = this.saveDir.listFiles();
		if (renderFiles != null)
		{
			for (File renderFile : renderFiles)
			{
				if (!renderFile.delete())
				{
					LOGGER.warn("Unable to delete render file: " + renderFile.getPath());
				}
			}
		}
		
		// clear the cached files
		this.filesBySectionPos.clear();
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	public File computeRenderFilePath(DhSectionPos pos) { return new File(this.saveDir, pos.serialize() + RENDER_FILE_POSTFIX);}

	@Nullable
	public DhSectionPos decodePositionByFile(File file)
	{
		String fileName = file.getName();
		if (!fileName.endsWith(RENDER_FILE_POSTFIX)) return null;
		fileName = fileName.substring(0, fileName.length() - RENDER_FILE_POSTFIX.length());
		return DhSectionPos.deserialize(fileName);
	}
	
}
