package com.seibel.lod.core.file.renderfile;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.dataObjects.fullData.sources.IFullDataSource;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataView;
import com.seibel.lod.core.dataObjects.transformers.DataRenderTransformer;
import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.FileUtil;
import com.seibel.lod.core.util.ThreadUtil;
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
import java.util.concurrent.RejectedExecutionException;

public class RenderSourceFileHandler implements ILodRenderSourceProvider
{
	public static final String RENDER_FILE_EXTENSION = ".rlod";
	public static final long RENDER_SOURCE_TYPE_ID = ColumnRenderSource.TYPE_ID;
	
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private final ExecutorService renderCacheThread = ThreadUtil.makeSingleThreadPool("RenderCacheThread");
	private final ConcurrentHashMap<DhSectionPos, RenderMetaDataFile> filesBySectionPos = new ConcurrentHashMap<>();
	
	private final IDhClientLevel level;
	private final File saveDir;
	private final IFullDataSourceProvider fullDataSourceProvider;
	
	private final ConcurrentHashMap<DhSectionPos, Object> cacheUpdateLockBySectionPos = new ConcurrentHashMap<>();
	
	
	
	
	public RenderSourceFileHandler(IFullDataSourceProvider sourceProvider, IDhClientLevel level, File saveRootDir)
	{
        this.fullDataSourceProvider = sourceProvider;
        this.level = level;
        this.saveDir = saveRootDir;
    }
	
	
	
	//===============//
	// file handling //
	//===============//
	
	/**
	 * Caller must ensure that this method is called only once,
	 * and that the given files are not used before this method is called.
	 */
	@Override
	public void addScannedFile(Collection<File> newRenderFiles)
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
		}
	}
	
	/** This call is concurrent. I.e. it supports multiple threads calling this method at the same time. */
    @Override
    public CompletableFuture<ColumnRenderSource> readAsync(DhSectionPos pos)
	{
        RenderMetaDataFile metaFile = this.filesBySectionPos.get(pos);
		if (metaFile == null)
		{
			RenderMetaDataFile newMetaFile;
			try
			{
				File renderMetaFile = this.computeRenderFilePath(pos);
				boolean renderFileExists = renderMetaFile.exists();
				
				if (renderFileExists)
				{
					newMetaFile = RenderMetaDataFile.createFromExistingFile(this, renderMetaFile);
				}
				else
				{
					newMetaFile = RenderMetaDataFile.createNewFileForPos(this, pos);
				}
			}
			catch (IOException e)
			{
				LOGGER.error("IOException on creating new render file at "+pos, e);
				return null;
			}
			
			metaFile = this.filesBySectionPos.putIfAbsent(pos, newMetaFile); // This is a CAS with expected null value.
			if (metaFile == null)
			{
				metaFile = newMetaFile;
			}
		}
		
        return metaFile.loadOrGetCached(this.renderCacheThread, this.level).handle(
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
		final int vertSize = Config.Client.Graphics.Quality.verticalQuality.get()
				.calculateMaxVerticalData((byte) (file.pos.sectionDetailLevel - ColumnRenderSource.SECTION_SIZE_OFFSET));
		
		return CompletableFuture.completedFuture(
				new ColumnRenderSource(file.pos, vertSize, this.level.getMinY()));
	}
	
	
	
	//=============//
	// data saving //
	//=============//
	
    /**
	 * This call is concurrent. I.e. it supports multiple threads calling this method at the same time. <br>
	 * TODO why is there fullData handling in the render file handler? 
	 */
    @Override
    public void writeChunkDataToFile(DhSectionPos sectionPos, ChunkSizedFullDataView chunkDataView)
	{
        this.writeChunkDataToFileRecursively(sectionPos,chunkDataView);
		this.fullDataSourceProvider.write(sectionPos, chunkDataView);
    }
    private void writeChunkDataToFileRecursively(DhSectionPos sectPos, ChunkSizedFullDataView chunkDataView)
	{
		if (!sectPos.getSectionBBoxPos().overlapsExactly(chunkDataView.getLodPos()))
		{
			return;
		}
		
		
		if (sectPos.sectionDetailLevel > ColumnRenderSource.SECTION_SIZE_OFFSET)
		{
			this.writeChunkDataToFileRecursively(sectPos.getChildByIndex(0), chunkDataView);
			this.writeChunkDataToFileRecursively(sectPos.getChildByIndex(1), chunkDataView);
			this.writeChunkDataToFileRecursively(sectPos.getChildByIndex(2), chunkDataView);
			this.writeChunkDataToFileRecursively(sectPos.getChildByIndex(3), chunkDataView);
		}
		
		RenderMetaDataFile metaFile = this.filesBySectionPos.get(sectPos);
		// Fast path: if there is a file for this section, just write to it.
		if (metaFile != null)
		{
			metaFile.updateChunkIfNeeded(chunkDataView, this.level);
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
			futures.add(metaFile.flushAndSave(this.renderCacheThread));
		}
		
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
				.whenComplete((voidObj, exception) -> LOGGER.info("Finished shutting down "+ RenderSourceFileHandler.class.getSimpleName()) );
	}
	
	
	
	//================//
	// cache updating //
	//================//
	
    private CompletableFuture<Void> updateCacheAsync(ColumnRenderSource renderSource, RenderMetaDataFile file)
	{
		if (this.cacheUpdateLockBySectionPos.putIfAbsent(file.pos, new Object()) != null)
		{
			return CompletableFuture.completedFuture(null);
		}
		
		// get the full data source loading future
		final WeakReference<ColumnRenderSource> renderSourceReference = new WeakReference<>(renderSource); // TODO why is this a week reference?
		CompletableFuture<IFullDataSource> fullDataSourceFuture = this.fullDataSourceProvider.read(renderSource.getSectionPos());
		fullDataSourceFuture = fullDataSourceFuture.thenApply((fullDataSource) -> 
			{
				if (renderSourceReference.get() == null)
				{
					throw new UncheckedInterruptedException();
				}
				
				// the fullDataSource can be null if the thread this was running on was interrupted
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
						this.writeRenderSourceToFile(renderSourceReference.get(), file, newRenderSource);
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
					
					transformationCompleteFuture.complete(null);
				})
				.thenRun(() -> this.cacheUpdateLockBySectionPos.remove(file.pos));
		
		
		return transformationCompleteFuture;
	}
	
	/** TODO at some point this method may need to be made "async" like {@link RenderSourceFileHandler#onReadRenderSourceLoadedFromCacheAsync} since the insides are async */ 
    public ColumnRenderSource onRenderFileLoaded(ColumnRenderSource renderSource, RenderMetaDataFile file)
	{
		this.updateCacheAsync(renderSource, file).join();
        return renderSource;
    }
	
	public CompletableFuture<Void> onReadRenderSourceLoadedFromCacheAsync(RenderMetaDataFile file, ColumnRenderSource data) { return this.updateCacheAsync(data, file); }
	
    private void writeRenderSourceToFile(ColumnRenderSource currentRenderSource, RenderMetaDataFile file, ColumnRenderSource newRenderSource)
	{
        if (currentRenderSource == null || newRenderSource == null)
		{
			return;
		}
		
        currentRenderSource.updateFromRenderSource(newRenderSource);
		
        //file.metaData.dataVersion.set(newDataVersion);
        file.metaData.dataLevel = currentRenderSource.getDataDetail();
        file.metaData.dataTypeId = RENDER_SOURCE_TYPE_ID;
        file.metaData.loaderVersion = currentRenderSource.getRenderDataFormatVersion();
        file.save(currentRenderSource);
    }
	
    public boolean refreshRenderSource(ColumnRenderSource renderSource)
	{
        RenderMetaDataFile file = this.filesBySectionPos.get(renderSource.getSectionPos());
        if (renderSource.isEmpty())
		{
            if (file == null || file.metaData == null)
			{
                return false;
            }
        }
		
        LodUtil.assertTrue(file != null);
        LodUtil.assertTrue(file.metaData != null);
//        if (!this.fullDataSourceProvider.isCacheVersionValid(file.pos, file.metaData.dataVersion.get()))
//		{
			this.updateCacheAsync(renderSource, file).join();
            return true;
//        }
		
//        return false;
    }
	
	
	
	//=====================//
	// clearing / shutdown //
	//=====================//
	
	@Override
	public void close()
	{
		ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
		for (RenderMetaDataFile metaFile : this.filesBySectionPos.values())
		{
			futures.add(metaFile.flushAndSave(this.renderCacheThread));
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
					LOGGER.error("Unable to delete render file: " + renderFile.getPath());
				}
			}
		}
		
		// clear the cached files
		this.filesBySectionPos.clear();
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	public File computeRenderFilePath(DhSectionPos pos) { return new File(this.saveDir, pos.serialize() + RENDER_FILE_EXTENSION);}
	
	
}
