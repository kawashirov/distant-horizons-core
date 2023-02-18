package com.seibel.lod.core.file.renderfile;

import com.google.common.collect.HashMultimap;
import com.seibel.lod.core.datatype.IFullDataSource;
import com.seibel.lod.core.datatype.PlaceHolderRenderSource;
import com.seibel.lod.core.datatype.IRenderSource;
import com.seibel.lod.core.datatype.AbstractRenderSourceLoader;
import com.seibel.lod.core.datatype.column.ColumnRenderSource;
import com.seibel.lod.core.datatype.full.ChunkSizedFullData;
import com.seibel.lod.core.datatype.transform.DataRenderTransformer;
import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
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

public class RenderFileHandler implements ILodRenderSourceProvider
{
	public static final String RENDER_FILE_EXTENSION = ".rlod";
	
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private final ExecutorService renderCacheThread = LodUtil.makeSingleThreadPool("RenderCacheThread");
	private final ConcurrentHashMap<DhSectionPos, RenderMetaDataFile> filesBySectionPos = new ConcurrentHashMap<>();
	
	private final IDhClientLevel level;
	private final File saveDir;
	private final IFullDataSourceProvider fullDataSourceProvider;
	
	private final ConcurrentHashMap<DhSectionPos, Object> cacheUpdateLockBySectionPos = new ConcurrentHashMap<>();
	
	
	
	
	public RenderFileHandler(IFullDataSourceProvider sourceProvider, IDhClientLevel level, File saveRootDir)
	{
        this.fullDataSourceProvider = sourceProvider;
        this.level = level;
        this.saveDir = saveRootDir;
    }
	
	
	
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
				String corruptedFileName = file.getName() + ".corrupted";
				
				File corruptedFile = new File(file.getParentFile(), corruptedFileName);
				if (corruptedFile.exists())
				{
					// could happen if there was a corrupted file before that was removed
					corruptedFile.delete();
				}
				
				
				if (file.renameTo(corruptedFile))
				{
					LOGGER.error("Renamed corrupted file to ["+corruptedFileName+"].");
				}
				else
				{
					LOGGER.error("Failed to rename corrupted file to ["+corruptedFileName+"]. Attempting to delete file...");
					if (!file.delete())
					{
						LOGGER.error("Unable to delete corrupted file ["+corruptedFileName+"].");
					}
				}
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
						renderMetaDataFile.path.lastModified()));
				
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
						sb.append(metaFile.path);
						sb.append("\n");
					}
					sb.append("\tUsing: ");
					sb.append(fileToUse.path);
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
						
						File oldFile = new File(metaFile.path + ".old");
						try
						{
							if (!metaFile.path.renameTo(oldFile))
								throw new RuntimeException("Renaming failed");
						}
						catch (Exception e)
						{
							LOGGER.error("Failed to rename file: [" + metaFile.path + "] to [" + oldFile + "]", e);
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
	
	
	
	//===============//
	// file handling //
	//===============//
	
    /** This call is concurrent. I.e. it supports multiple threads calling this method at the same time. */
    @Override
    public CompletableFuture<IRenderSource> read(DhSectionPos pos)
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
				
				return (renderSource != null) ? renderSource : new PlaceHolderRenderSource(pos);
			});
    }
	
    /* This call is concurrent. I.e. it supports multiple threads calling this method at the same time. */
    @Override
    public void write(DhSectionPos sectionPos, ChunkSizedFullData chunkData)
	{
        this.writeRecursively(sectionPos,chunkData);
		this.fullDataSourceProvider.write(sectionPos, chunkData); // TODO why is there fullData handling in the render file handler?
    }
    private void writeRecursively(DhSectionPos sectPos, ChunkSizedFullData chunkData)
	{
		if (!sectPos.getSectionBBoxPos().overlaps(new DhLodPos((byte) (4 + chunkData.dataDetail), chunkData.x, chunkData.z)))
		{
			return;
		}
		
		
		if (sectPos.sectionDetailLevel > ColumnRenderSource.SECTION_SIZE_OFFSET)
		{
			this.writeRecursively(sectPos.getChildByIndex(0), chunkData);
			this.writeRecursively(sectPos.getChildByIndex(1), chunkData);
			this.writeRecursively(sectPos.getChildByIndex(2), chunkData);
			this.writeRecursively(sectPos.getChildByIndex(3), chunkData);
		}
		
		RenderMetaDataFile metaFile = this.filesBySectionPos.get(sectPos);
		// Fast path: if there is a file for this section, just write to it.
		if (metaFile != null)
		{
			metaFile.updateChunkIfNeeded(chunkData, this.level);
		}
	}
	
    /** This call is concurrent. I.e. it supports multiple threads calling this method at the same time. */
    @Override
	public CompletableFuture<Void> flushAndSave()
	{
		ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
		for (RenderMetaDataFile metaFile : this.filesBySectionPos.values())
		{
			futures.add(metaFile.flushAndSave(this.renderCacheThread));
		}
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
	}

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

    public File computeRenderFilePath(DhSectionPos pos) { return new File(this.saveDir, pos.serialize() + RENDER_FILE_EXTENSION);}

    public CompletableFuture<IRenderSource> onCreateRenderFile(RenderMetaDataFile file)
	{
		final int vertSize = Config.Client.Graphics.Quality.verticalQuality.get()
				.calculateMaxVerticalData((byte) (file.pos.sectionDetailLevel - ColumnRenderSource.SECTION_SIZE_OFFSET));
		
		return CompletableFuture.completedFuture(
				new ColumnRenderSource(file.pos, vertSize, this.level.getMinY()));
	}

    private void updateCache(IRenderSource renderSource, RenderMetaDataFile file)
	{
		if (this.cacheUpdateLockBySectionPos.putIfAbsent(file.pos, new Object()) != null)
		{
			return;
		}
		
		final WeakReference<IRenderSource> renderSourceReference = new WeakReference<>(renderSource); // TODO why is this a week reference?
		CompletableFuture<IFullDataSource> fullDataSourceFuture = this.fullDataSourceProvider.read(renderSource.getSectionPos());
		fullDataSourceFuture = fullDataSourceFuture.thenApply((dataSource) -> 
			{
				if (renderSourceReference.get() == null)
				{
					throw new UncheckedInterruptedException();
				}
				LodUtil.assertTrue(dataSource != null);
				return dataSource;
			}
			).exceptionally((ex) -> 
			{
				LOGGER.error("Exception when getting data for updateCache()", ex);
				return null;
			});
		
		//LOGGER.info("Recreating cache for {}", data.getSectionPos());
		DataRenderTransformer.asyncTransformDataSource(fullDataSourceFuture, this.level)
				.thenAccept((newRenderSource) -> this.write(renderSourceReference.get(), file, newRenderSource))
				.exceptionally((ex) -> 
				{
					if (!UncheckedInterruptedException.isThrowableInterruption(ex))
					{
						LOGGER.error("Exception when updating render file using data source: ", ex);
					}
					return null;
				}
				).thenRun(() -> this.cacheUpdateLockBySectionPos.remove(file.pos));
	}
	
    public IRenderSource onRenderFileLoaded(IRenderSource renderSource, RenderMetaDataFile file)
	{
//		if (!this.fullDataSourceProvider.isCacheVersionValid(file.pos, file.metaData.dataVersion.get()))
//		{
			this.updateCache(renderSource, file);
//		}
		
        return renderSource;
    }
	
    private void write(IRenderSource currentRenderSource, RenderMetaDataFile file,
                       IRenderSource newRenderSource)
	{
        if (currentRenderSource == null || newRenderSource == null)
		{
			return;
		}
		
        currentRenderSource.updateFromRenderSource(newRenderSource);
		
        //file.metaData.dataVersion.set(newDataVersion);
        file.metaData.dataLevel = currentRenderSource.getDataDetail();
        file.loader = AbstractRenderSourceLoader.getLoader(currentRenderSource.getClass(), currentRenderSource.getRenderVersion());
        file.dataType = currentRenderSource.getClass();
        file.metaData.dataTypeId = file.loader.renderTypeId;
        file.metaData.loaderVersion = currentRenderSource.getRenderVersion();
        file.save(currentRenderSource, this.level);
    }
	
    public void onReadRenderSourceFromCache(RenderMetaDataFile file, IRenderSource data)
	{
//        if (!this.fullDataSourceProvider.isCacheVersionValid(file.pos, file.metaData.dataVersion.get()))
//		{
			this.updateCache(data, file);
//        }
    }
	
    public boolean refreshRenderSource(IRenderSource source)
	{
        RenderMetaDataFile file = this.filesBySectionPos.get(source.getSectionPos());
        if (source instanceof PlaceHolderRenderSource)
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
			this.updateCache(source, file);
            return true;
//        }
		
//        return false;
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
	
}
