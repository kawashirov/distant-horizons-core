package com.seibel.lod.core.file.renderfile;

import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.lod.core.dataObjects.render.ColumnRenderLoader;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.file.metaData.BaseMetaData;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.file.metaData.AbstractMetaDataContainerFile;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.objects.dataStreams.DhDataInputStream;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.concurrent.*;

public class RenderMetaDataFile extends AbstractMetaDataContainerFile
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	/**
	 * Can be cleared if the garbage collector determines there isn't enough space. <br><br>
	 *
	 * When clearing, don't set to null, instead create a SoftReference containing null. 
	 * This will make null checks simpler.
	 */
	private SoftReference<ColumnRenderSource> cachedRenderDataSourceRef = new SoftReference<>(null);
	
	private final RenderSourceFileHandler fileHandler;
	private boolean doesFileExist;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	/**
	 * NOTE: should only be used if there is NOT an existing file.
	 * @throws IOException if a file already exists for this position 
	 */
	public static RenderMetaDataFile createNewFileForPos(RenderSourceFileHandler fileHandler, DhSectionPos pos) throws IOException
	{
		return new RenderMetaDataFile(fileHandler, pos);
	}
	private RenderMetaDataFile(RenderSourceFileHandler fileHandler, DhSectionPos pos) throws IOException
	{
		super(fileHandler.computeRenderFilePath(pos), pos);
		this.fileHandler = fileHandler;
		LodUtil.assertTrue(this.baseMetaData == null);
		this.doesFileExist = this.file.exists();
	}
	
	/**
	 * NOTE: should only be used if there IS an existing file.
	 * @throws IOException if no file exists for this position 
	 */
	public static RenderMetaDataFile createFromExistingFile(RenderSourceFileHandler fileHandler, File path) throws IOException
	{
		return new RenderMetaDataFile(fileHandler, path);
	}
	private RenderMetaDataFile(RenderSourceFileHandler fileHandler, File path) throws IOException
	{
		super(path);
		this.fileHandler = fileHandler;
		LodUtil.assertTrue(this.baseMetaData != null);
		
		this.doesFileExist = this.file.exists();
	}
	
	
	
	// FIXME: This can cause concurrent modification of LodRenderSource.
    //       Not sure if it will cause issues or not.
	public void updateChunkIfSourceExists(ChunkSizedFullDataAccessor chunkDataView, IDhClientLevel level)
	{
		DhLodPos chunkPos = chunkDataView.getLodPos();
		LodUtil.assertTrue(this.pos.getSectionBBoxPos().overlapsExactly(chunkPos), "Chunk pos "+chunkPos+" doesn't overlap with section "+this.pos);
		
		// update the render source if one exists
		CompletableFuture<ColumnRenderSource> readSourceFuture = this.getCachedDataSourceAsync();
		if (readSourceFuture != null)
		{
			readSourceFuture.thenAccept((renderSource) -> renderSource.fastWrite(chunkDataView, level));
		}
		
	}
	
    public CompletableFuture<Void> flushAndSave(ExecutorService renderCacheThread)
	{
		if (!this.file.exists())
		{
			return CompletableFuture.completedFuture(null); // No need to save if the file doesn't exist.
		}
		
		CompletableFuture<ColumnRenderSource> source = this.getCachedDataSourceAsync();
		if (source == null)
		{
			return CompletableFuture.completedFuture(null); // If there is no cached data, there is no need to save.
		}
		
		return source.thenAccept((columnRenderSource) -> { }); // Otherwise, wait for the data to be read (which also flushes changes to the file).
	}
	private CompletableFuture<ColumnRenderSource> getCachedDataSourceAsync()
	{
		// attempt to get the cached data source
		ColumnRenderSource cachedRenderDataSource = this.cachedRenderDataSourceRef.get();
		if (cachedRenderDataSource != null)
		{
			return this.fileHandler.onReadRenderSourceLoadedFromCacheAsync(this, cachedRenderDataSource)
					// wait for the handler to finish before returning the renderSource
					.handle((voidObj, ex) -> cachedRenderDataSource);
		}
		
		
		
		// the data source hasn't been loaded 
		// and isn't in the process of being loaded
		return null;
	}
	
	public CompletableFuture<ColumnRenderSource> loadOrGetCachedDataSourceAsync(Executor fileReaderThreads, IDhLevel level)
	{
		CompletableFuture<ColumnRenderSource> getCachedFuture = this.getCachedDataSourceAsync();
		if (getCachedFuture != null)
		{
			return getCachedFuture;
		}
		
		
		
		// Create an empty and non-completed future.
		// Note: I do this before actually filling in the future so that I can ensure only
		//   one task is submitted to the thread pool.
		CompletableFuture<ColumnRenderSource> loadRenderSourceFuture = new CompletableFuture<>();
		if (!this.doesFileExist)
		{
			// create a new Meta file
			
			this.fileHandler.onCreateRenderFileAsync(this)
				.thenApply((renderSource) -> 
				{
					this.baseMetaData = this.makeMetaData(renderSource);
					return renderSource;
				})
				.thenApply((renderSource) -> this.fileHandler.onRenderFileLoaded(renderSource, this))
				.whenComplete((renderSource, ex) -> 
				{
					if (ex != null)
					{
						LOGGER.error("Uncaught error on creation {}: ", this.file, ex);
						loadRenderSourceFuture.complete(null);
						this.cachedRenderDataSourceRef = new SoftReference<>(null);
					}
					else
					{
						loadRenderSourceFuture.complete(renderSource);
						this.cachedRenderDataSourceRef = new SoftReference<>(renderSource);
					}
				});
		}
		else
		{
			CompletableFuture.supplyAsync(() -> 
				{
					if (this.baseMetaData == null)
					{
						throw new IllegalStateException("Meta data not loaded!");
					}
					
					// Load the file.
					ColumnRenderSource renderSource;
					try (FileInputStream fileInputStream = this.getFileInputStream();
						DhDataInputStream compressedStream = new DhDataInputStream(fileInputStream))
					{
						renderSource = ColumnRenderLoader.INSTANCE.loadRenderSource(this, compressedStream, level);
					}
					catch (IOException ex)
					{
						throw new CompletionException(ex);
					}
					
					renderSource = this.fileHandler.onRenderFileLoaded(renderSource, this);
					return renderSource;
				}, fileReaderThreads)
				.whenComplete((renderSource, ex) -> 
				{
					if (ex != null)
					{
						LOGGER.error("Error loading file {}: ", this.file, ex);
						loadRenderSourceFuture.complete(null);
						this.cachedRenderDataSourceRef = new SoftReference<>(null);
					}
					else
					{
						loadRenderSourceFuture.complete(renderSource);
						this.cachedRenderDataSourceRef = new SoftReference<>(renderSource);
					}
				});
		}
		
		
		
		return loadRenderSourceFuture;
	}
	
    private BaseMetaData makeMetaData(ColumnRenderSource renderSource)
	{
		return new BaseMetaData(renderSource.getSectionPos(), -1,
				renderSource.getDataDetail(), renderSource.worldGenStep, RenderSourceFileHandler.RENDER_SOURCE_TYPE_ID, renderSource.getRenderDataFormatVersion());
	}
	
    private FileInputStream getFileInputStream() throws IOException
	{
		FileInputStream fin = new FileInputStream(this.file);
		int toSkip = METADATA_SIZE_IN_BYTES;
		while (toSkip > 0)
		{
			long skipped = fin.skip(toSkip);
			if (skipped == 0)
			{
				throw new IOException("Invalid file: Failed to skip metadata.");
			}
			toSkip -= skipped;
		}
		
		if (toSkip != 0)
		{
			throw new IOException("File IO Error: Failed to skip metadata.");
		}
		else
		{
			return fin;	
		}
	}
	
    public void save(ColumnRenderSource renderSource)
	{
		if (renderSource.isEmpty())
		{
			if (this.file.exists())
			{
				if (!this.file.delete())
				{
					LOGGER.warn("Failed to delete render file at {}", this.file);
				}
			}
			this.doesFileExist = false;
		}
		else
		{
			//LOGGER.info("Saving updated render file v[{}] at sect {}", this.metaData.dataVersion.get(), this.pos);
			try
			{
				super.writeData((out) -> renderSource.writeData(out));
				this.doesFileExist = true;
			}
			catch (IOException e)
			{
				LOGGER.error("Failed to save updated render file at {} for sect {}", this.file, this.pos, e);
			}
		}
	}
	
}
