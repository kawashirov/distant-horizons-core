package com.seibel.lod.core.file.renderfile;

import com.seibel.lod.core.datatype.IRenderSource;
import com.seibel.lod.core.datatype.AbstractRenderSourceLoader;
import com.seibel.lod.core.datatype.full.ChunkSizedFullDataSource;
import com.seibel.lod.core.file.metaData.MetaData;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.file.metaData.AbstractMetaDataFile;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class RenderMetaDataFile extends AbstractMetaDataFile
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    public AbstractRenderSourceLoader loader;
    public Class<? extends IRenderSource> dataType;
	
    // The '?' type should either be:
    //    SoftReference<LodRenderSource>, or	- File that may still be loaded
    //    CompletableFuture<LodRenderSource>,or - File that is being loaded
    //    null									- Nothing is loaded or being loaded
    AtomicReference<Object> data = new AtomicReference<>(null);
	
	private final RenderFileHandler fileHandler;
	private boolean doesFileExist;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	/**
	 * NOTE: should only be used if there is NOT an existing file.
	 * @throws IOException if a file already exists for this position 
	 */
	public static RenderMetaDataFile createNewFileForPos(RenderFileHandler fileHandler, DhSectionPos pos) throws IOException
	{
		return new RenderMetaDataFile(fileHandler, pos);
	}
	private RenderMetaDataFile(RenderFileHandler fileHandler, DhSectionPos pos) throws IOException
	{
		super(fileHandler.computeRenderFilePath(pos), pos);
		this.fileHandler = fileHandler;
		LodUtil.assertTrue(this.metaData == null);
		this.doesFileExist = this.path.exists();
	}
	
	/**
	 * NOTE: should only be used if there IS an existing file.
	 * @throws IOException if no file exists for this position 
	 */
	public static RenderMetaDataFile createFromExistingFile(RenderFileHandler fileHandler, File path) throws IOException
	{
		return new RenderMetaDataFile(fileHandler, path);
	}
	private RenderMetaDataFile(RenderFileHandler fileHandler, File path) throws IOException
	{
		super(path);
		this.fileHandler = fileHandler;
		LodUtil.assertTrue(this.metaData != null);
		this.loader = AbstractRenderSourceLoader.getLoader(this.metaData.dataTypeId, this.metaData.loaderVersion);
		if (this.loader == null)
		{
			throw new IOException("Invalid file: Data type loader not found: "
					+ this.metaData.dataTypeId + "(v" + this.metaData.loaderVersion + ")");
		}
		this.dataType = this.loader.renderSourceClass;
		
		this.doesFileExist = this.path.exists();
	}
	
	
	
	// FIXME: This can cause concurrent modification of LodRenderSource.
    //       Not sure if it will cause issues or not.
	public void updateChunkIfNeeded(ChunkSizedFullDataSource chunkData, IDhClientLevel level)
	{
		DhLodPos chunkPos = new DhLodPos((byte) (chunkData.dataDetail + 4), chunkData.x, chunkData.z);
		LodUtil.assertTrue(this.pos.getSectionBBoxPos().overlaps(chunkPos), "Chunk pos {} doesn't overlap with section {}", chunkPos, pos);
			
		CompletableFuture<IRenderSource> source = this._readCached(this.data.get());
		if (source == null)
		{
			return;
		}
		
		source.thenAccept((renderSource) -> renderSource.fastWrite(chunkData, level));
	}
	
    public CompletableFuture<Void> flushAndSave(ExecutorService renderCacheThread)
	{
		if (!path.exists())
		{
			return CompletableFuture.completedFuture(null); // No need to save if the file doesn't exist.
		}
		
		CompletableFuture<IRenderSource> source = this._readCached(this.data.get());
		if (source == null)
		{
			return CompletableFuture.completedFuture(null); // If there is no cached data, there is no need to save.
		}
		
		return source.thenAccept((a) -> { }); // Otherwise, wait for the data to be read (which also flushes changes to the file).
	}
	
    // Suppress casting of CompletableFuture<?> to CompletableFuture<LodRenderSource>
    @SuppressWarnings("unchecked")
	private CompletableFuture<IRenderSource> _readCached(Object obj)
	{
		// Has file cached in RAM and not freed yet.
		if ((obj instanceof SoftReference<?>))
		{
			Object inner = ((SoftReference<?>) obj).get();
			if (inner != null)
			{
				LodUtil.assertTrue(inner instanceof IRenderSource);
				fileHandler.onReadRenderSourceFromCache(this, (IRenderSource) inner);
				return CompletableFuture.completedFuture((IRenderSource) inner);
			}
		}
		
		//==== Cached file out of scope. ====
		// Someone is already trying to complete it. so just return the obj.
		if ((obj instanceof CompletableFuture<?>))
		{
			return (CompletableFuture<IRenderSource>) obj;
		}
		return null;
	}
	
    // Cause: Generic Type runtime casting cannot safety check it.
	// However, the Union type ensures the 'data' should only contain the listed type.
	public CompletableFuture<IRenderSource> loadOrGetCached(Executor fileReaderThreads, IDhLevel level)
	{
		Object obj = this.data.get();
	
		CompletableFuture<IRenderSource> cached = this._readCached(obj);
		if (cached != null)
		{
			return cached;
		}
	
		// Create an empty and non-completed future.
		// Note: I do this before actually filling in the future so that I can ensure only
		//   one task is submitted to the thread pool.
		CompletableFuture<IRenderSource> loadRenderSourceFuture = new CompletableFuture<>();
	
		// Would use faster and non-nesting Compare and exchange. But java 8 doesn't have it! :(
		boolean worked = this.data.compareAndSet(obj, loadRenderSourceFuture);
		if (!worked)
		{
			return this.loadOrGetCached(fileReaderThreads, level);
		}
	
		// Now, there should only ever be one thread at a time here due to the CAS operation above.
	
	
		// After cas. We are in exclusive control.
		if (!this.doesFileExist)
		{
			this.fileHandler.onCreateRenderFile(this)
				.thenApply((data) -> 
				{
					this.metaData = makeMetaData(data);
					return data;
				})
				.thenApply((renderSource) -> this.fileHandler.onRenderFileLoaded(renderSource, this))
				.whenComplete((renderSource, exception) -> 
				{
					if (exception != null)
					{
						LOGGER.error("Uncaught error on creation {}: ", this.path, exception);
						loadRenderSourceFuture.complete(null);
						this.data.set(null);
					}
					else
					{
						loadRenderSourceFuture.complete(renderSource);
						//new DataObjTracker(v); //TODO: Obj Tracker??? For debug?
						this.data.set(new SoftReference<>(renderSource));
					}
				});
		}
		else
		{
			CompletableFuture.supplyAsync(() -> 
				{
					if (this.metaData == null)
					{
						throw new IllegalStateException("Meta data not loaded!");
					}
					
					// Load the file.
					IRenderSource renderSource;
					try (FileInputStream fio = this.getDataContent())
					{
						renderSource = this.loader.loadRenderSource(this, fio, level);
					}
					catch (IOException e)
					{
						throw new CompletionException(e);
					}
					
					renderSource = this.fileHandler.onRenderFileLoaded(renderSource, this);
					return renderSource;
				}, fileReaderThreads)
				.whenComplete((renderSource, e) -> 
				{
					if (e != null)
					{
						LOGGER.error("Error loading file {}: ", this.path, e);
						loadRenderSourceFuture.complete(null);
						this.data.set(null);
					}
					else
					{
						loadRenderSourceFuture.complete(renderSource);
						this.data.set(new SoftReference<>(renderSource));
					}
				});
		}
		return loadRenderSourceFuture;
	}
	
    private static MetaData makeMetaData(IRenderSource data)
	{
		AbstractRenderSourceLoader loader = AbstractRenderSourceLoader.getLoader(data.getClass(), data.getRenderVersion());
		return new MetaData(data.getSectionPos(), -1,
				data.getDataDetail(), loader == null ? 0 : loader.renderTypeId, data.getRenderVersion());
	}
	
    private FileInputStream getDataContent() throws IOException
	{
		FileInputStream fin = new FileInputStream(this.path);
		int toSkip = METADATA_SIZE;
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
		return fin;
	}
	
    public void save(IRenderSource data, IDhClientLevel level)
	{
		if (data.isEmpty())
		{
			if (this.path.exists())
			{
				if (!this.path.delete())
				{
					LOGGER.warn("Failed to delete render file at {}", this.path);
				}
			}
			this.doesFileExist = false;
		}
		else
		{
			//LOGGER.info("Saving updated render file v[{}] at sect {}", this.metaData.dataVersion.get(), this.pos);
			try
			{
				super.writeData((out) -> data.saveRender(level, this, out));
				this.doesFileExist = true;
			}
			catch (IOException e)
			{
				LOGGER.error("Failed to save updated render file at {} for sect {}", this.path, this.pos, e);
			}
		}
	}
	
}
