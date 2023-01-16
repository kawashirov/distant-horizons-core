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

public class RenderFileHandler implements IRenderSourceProvider
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    
	private final ExecutorService renderCacheThread = LodUtil.makeSingleThreadPool("RenderCacheThread");
	private final ConcurrentHashMap<DhSectionPos, RenderMetaDataFile> files = new ConcurrentHashMap<>();
	private final IDhClientLevel level;
	private final File saveDir;
	private final IDataSourceProvider dataSourceProvider;
	
	
	
    public RenderFileHandler(IDataSourceProvider sourceProvider, IDhClientLevel level, File saveRootDir)
	{
        this.dataSourceProvider = sourceProvider;
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
				RenderMetaDataFile metaFile = new RenderMetaDataFile(this, file);
				filesByPos.put(metaFile.pos, metaFile);
			}
			catch (IOException e)
			{
				LOGGER.error("Failed to read render meta file at [{}]. Error: ", file, e);
				String corruptedFileName = file.getName() + ".corrupted";
				
				File corruptedFile = new File(file.getParentFile(), corruptedFileName);
				if (corruptedFile.exists())
				{
					// could happen if there was a corrupted file before that was removed
					corruptedFile.delete();
				}
				
				
				if (file.renameTo(corruptedFile))
				{
					LOGGER.error("Renamed corrupted file to [{}].", file.getName() + ".corrupted");
				}
				else
				{
					LOGGER.error("Failed to rename corrupted file to [{}]. Attempting to delete file...", corruptedFileName);
					if (!file.delete())
					{
						LOGGER.error("Unable to delete corrupted file [{}].", corruptedFileName);
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
				fileToUse = Collections.max(metaFiles, Comparator.comparingLong(renderMetaDataFile -> 
						renderMetaDataFile.metaData.dataVersion.get()));
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
							continue;
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
			this.files.put(pos, fileToUse);
		}
	}
	
    /** This call is concurrent. I.e. it supports multiple threads calling this method at the same time. */
    @Override
    public CompletableFuture<ILodRenderSource> read(DhSectionPos pos)
	{
        RenderMetaDataFile metaFile = this.files.get(pos);
		if (metaFile == null)
		{
			RenderMetaDataFile newMetaFile;
			try
			{
				newMetaFile = new RenderMetaDataFile(this, pos);
			}
			catch (IOException e)
			{
				LOGGER.error("IOException on creating new render file at {}", pos, e);
				return null;
			}
			
			metaFile = this.files.putIfAbsent(pos, newMetaFile); // This is a CAS with expected null value.
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
						LOGGER.error("Uncaught error on {}:", pos, exception);
					}
					
					return (renderSource != null) ? renderSource : new PlaceHolderRenderSource(pos);
				}
        );
    }
	
    /* This call is concurrent. I.e. it supports multiple threads calling this method at the same time. */
    @Override
    public void write(DhSectionPos sectionPos, ChunkSizedData chunkData)
	{
		// can be used for debugging
        if (chunkData.getBBoxLodPos().convertToDetailLevel((byte)6).equals(new DhLodPos((byte)6, 10, -11)))
		{
            int doNothing = 0;
        }

        this.writeRecursively(sectionPos,chunkData);
		this.dataSourceProvider.write(sectionPos, chunkData);
    }
	
    private void writeRecursively(DhSectionPos sectPos, ChunkSizedData chunkData)
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
		
		RenderMetaDataFile metaFile = this.files.get(sectPos);
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
		for (RenderMetaDataFile metaFile : this.files.values())
		{
			futures.add(metaFile.flushAndSave(this.renderCacheThread));
		}
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
	}
	
    private File computeDefaultFilePath(DhSectionPos pos)
	{ 
		//TODO: Temp code as we haven't decided on the file naming & location yet.
        return new File(this.saveDir, pos.serialize() + ".lod");
    }

    @Override
    public void close()
	{
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        for (RenderMetaDataFile metaFile : this.files.values())
		{
            futures.add(metaFile.flushAndSave(this.renderCacheThread));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public File computeRenderFilePath(DhSectionPos pos)
	{
        return new File(this.saveDir, pos.serialize() + ".lod");
    }

    public CompletableFuture<ILodRenderSource> onCreateRenderFile(RenderMetaDataFile file)
	{
		final int vertSize = Config.Client.Graphics.Quality.verticalQuality
				.get().calculateMaxVerticalData((byte) (file.pos.sectionDetailLevel - ColumnRenderSource.SECTION_SIZE_OFFSET));
		
		return CompletableFuture.completedFuture(
				new ColumnRenderSource(file.pos, vertSize, this.level.getMinY()));
	}

    private final ConcurrentHashMap<DhSectionPos, Object> cacheRecreationGuards = new ConcurrentHashMap<>();

    private void updateCache(ILodRenderSource data, RenderMetaDataFile file)
	{
		if (this.cacheRecreationGuards.putIfAbsent(file.pos, new Object()) != null)
		{
			return;
		}
		
		final WeakReference<ILodRenderSource> dataRef = new WeakReference<>(data);
		CompletableFuture<ILodDataSource> dataFuture = this.dataSourceProvider.read(data.getSectionPos());
		dataFuture = dataFuture.thenApply((dataSource) -> 
		{
			if (dataRef.get() == null)
			{
				throw new UncheckedInterruptedException();
			}
			LodUtil.assertTrue(dataSource != null);
			return dataSource;
		}).exceptionally((ex) -> 
		{
			if (ex != null)
			{
				LOGGER.error("Uncaught exception when getting data for updateCache()", ex);
			}
			
			return null;
		});
	
		LOGGER.info("Recreating cache for {}", data.getSectionPos());
		DataRenderTransformer.asyncTransformDataSource(dataFuture, this.level)
				.thenAccept((newRenderDataSource) -> this.write(dataRef.get(), file, newRenderDataSource, this.dataSourceProvider.getCacheVersion(data.getSectionPos())))
				.exceptionally((ex) -> 
				{
					if (!UncheckedInterruptedException.isThrowableInterruption(ex))
					{
						LOGGER.error("Exception when updating render file using data source: ", ex);
					}
					return null;
				}).thenRun(() -> this.cacheRecreationGuards.remove(file.pos));
	}

    public ILodRenderSource onRenderFileLoaded(ILodRenderSource data, RenderMetaDataFile file)
	{
        if (!this.dataSourceProvider.isCacheVersionValid(file.pos, file.metaData.dataVersion.get()))
		{
			this.updateCache(data, file);
        }
        return data;
    }
	
    public ILodRenderSource onLoadingRenderFile(RenderMetaDataFile file) { return null; /* Default behavior: do nothing */ }
	
    private void write(ILodRenderSource target, RenderMetaDataFile file,
                       ILodRenderSource newData, long newDataVersion)
	{
        if (target == null || newData == null)
		{
			return;
		}
		
        target.updateFromRenderSource(newData);
        file.metaData.dataVersion.set(newDataVersion);
        file.metaData.dataLevel = target.getDataDetail();
        file.loader = AbstractRenderSourceLoader.getLoader(target.getClass(), target.getRenderVersion());
        file.dataType = target.getClass();
        file.metaData.dataTypeId = file.loader.renderTypeId;
        file.metaData.loaderVersion = target.getRenderVersion();
        file.save(target, this.level);
    }
	
    public void onReadRenderSourceFromCache(RenderMetaDataFile file, ILodRenderSource data)
	{
        if (!this.dataSourceProvider.isCacheVersionValid(file.pos, file.metaData.dataVersion.get()))
		{
			this.updateCache(data, file);
        }
    }
	
    public boolean refreshRenderSource(ILodRenderSource source)
	{
        RenderMetaDataFile file = this.files.get(source.getSectionPos());
        if (source instanceof PlaceHolderRenderSource)
		{
            if (file == null || file.metaData == null)
			{
                return false;
            }
        }
		
        LodUtil.assertTrue(file != null);
        LodUtil.assertTrue(file.metaData != null);
        if (!this.dataSourceProvider.isCacheVersionValid(file.pos, file.metaData.dataVersion.get()))
		{
			this.updateCache(source, file);
            return true;
        }
		
        return false;
    }
	
}
