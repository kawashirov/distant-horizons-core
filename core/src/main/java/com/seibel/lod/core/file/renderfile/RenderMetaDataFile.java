package com.seibel.lod.core.file.renderfile;

import com.seibel.lod.core.datatype.ILodRenderSource;
import com.seibel.lod.core.datatype.AbstractRenderSourceLoader;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.file.metaData.MetaData;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.file.metaData.MetaDataFile;
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

public class RenderMetaDataFile extends MetaDataFile
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    public AbstractRenderSourceLoader loader;
    public Class<? extends ILodRenderSource> dataType;
	
    // The '?' type should either be:
    //    SoftReference<LodRenderSource>, or	- File that may still be loaded
    //    CompletableFuture<LodRenderSource>,or - File that is being loaded
    //    null									- Nothing is loaded or being loaded
    AtomicReference<Object> data = new AtomicReference<>(null);
	
//	@FunctionalInterface
//	public interface CacheValidator
//	{
//		boolean isCacheValid(DhSectionPos sectionPos, long timestamp);
//	}
//	@FunctionalInterface
//	public interface CacheSourceProducer
//	{
//		CompletableFuture<ILodDataSource> getSourceFuture(DhSectionPos sectionPos);
//	}
//	CacheValidator validator;
//	CacheSourceProducer source;
	private final RenderFileHandler fileHandler;
	private boolean doesFileExist;
	
	
	
	/** Creates a new metaFile */
	public RenderMetaDataFile(RenderFileHandler fileHandler, DhSectionPos pos) throws IOException
	{
		super(fileHandler.computeRenderFilePath(pos), pos);
		this.fileHandler = fileHandler;
		LodUtil.assertTrue(this.metaData == null);
		this.doesFileExist = false;
	}
	
	/** Uses the existing metaFile */
	public RenderMetaDataFile(RenderFileHandler fileHandler, File path) throws IOException
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
		this.dataType = this.loader.clazz;
		this.doesFileExist = true;
	}
	
	
	
	// FIXME: This can cause concurrent modification of LodRenderSource.
    //       Not sure if it will cause issues or not.
	public void updateChunkIfNeeded(ChunkSizedData chunkData, IDhClientLevel level)
	{
		DhLodPos chunkPos = new DhLodPos((byte) (chunkData.dataDetail + 4), chunkData.x, chunkData.z);
		LodUtil.assertTrue(this.pos.getSectionBBoxPos().overlaps(chunkPos), "Chunk pos {} doesn't overlap with section {}", chunkPos, pos);
			
		CompletableFuture<ILodRenderSource> source = this._readCached(this.data.get());
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
		
		CompletableFuture<ILodRenderSource> source = this._readCached(this.data.get());
		if (source == null)
		{
			return CompletableFuture.completedFuture(null); // If there is no cached data, there is no need to save.
		}
		
		return source.thenAccept((a) -> { }); // Otherwise, wait for the data to be read (which also flushes changes to the file).
	}
	
    // Suppress casting of CompletableFuture<?> to CompletableFuture<LodRenderSource>
    @SuppressWarnings("unchecked")
	private CompletableFuture<ILodRenderSource> _readCached(Object obj)
	{
		// Has file cached in RAM and not freed yet.
		if ((obj instanceof SoftReference<?>))
		{
			Object inner = ((SoftReference<?>) obj).get();
			if (inner != null)
			{
				LodUtil.assertTrue(inner instanceof ILodRenderSource);
				fileHandler.onReadRenderSourceFromCache(this, (ILodRenderSource) inner);
				return CompletableFuture.completedFuture((ILodRenderSource) inner);
			}
		}
		
		//==== Cached file out of scope. ====
		// Someone is already trying to complete it. so just return the obj.
		if ((obj instanceof CompletableFuture<?>))
		{
			return (CompletableFuture<ILodRenderSource>) obj;
		}
		return null;
	}
	
    // Cause: Generic Type runtime casting cannot safety check it.
	// However, the Union type ensures the 'data' should only contain the listed type.
	public CompletableFuture<ILodRenderSource> loadOrGetCached(Executor fileReaderThreads, IDhLevel level)
	{
		Object obj = this.data.get();
	
		CompletableFuture<ILodRenderSource> cached = this._readCached(obj);
		if (cached != null)
		{
			return cached;
		}
	
		// Create an empty and non-completed future.
		// Note: I do this before actually filling in the future so that I can ensure only
		//   one task is submitted to the thread pool.
		CompletableFuture<ILodRenderSource> future = new CompletableFuture<>();
	
		// Would use faster and non-nesting Compare and exchange. But java 8 doesn't have it! :(
		boolean worked = this.data.compareAndSet(obj, future);
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
					.thenApply((d) -> this.fileHandler.onRenderFileLoaded(d, this))
					.whenComplete((v, e) -> 
					{
						if (e != null)
						{
							LOGGER.error("Uncaught error on creation {}: ", this.path, e);
							future.complete(null);
							this.data.set(null);
						}
						else
						{
							future.complete(v);
							//new DataObjTracker(v); //TODO: Obj Tracker??? For debug?
							this.data.set(new SoftReference<>(v));
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
						ILodRenderSource data;
						data = this.fileHandler.onLoadingRenderFile(this);
						if (data == null)
						{
							try (FileInputStream fio = getDataContent())
							{
								data = this.loader.loadRender(this, fio, level);
							}
							catch (IOException e)
							{
								throw new CompletionException(e);
							}
						}
						data = this.fileHandler.onRenderFileLoaded(data, this);
						return data;
					}, fileReaderThreads)
					.whenComplete((f, e) -> 
					{
						if (e != null)
						{
							LOGGER.error("Error loading file {}: ", this.path, e);
							future.complete(null);
							this.data.set(null);
						}
						else
						{
							future.complete(f);
							this.data.set(new SoftReference<>(f));
						}
					});
		}
		return future;
	}
	
    private static MetaData makeMetaData(ILodRenderSource data)
	{
		AbstractRenderSourceLoader loader = AbstractRenderSourceLoader.getLoader(data.getClass(), data.getRenderVersion());
		return new MetaData(data.getSectionPos(), -1, -1,
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
	
    public void save(ILodRenderSource data, IDhClientLevel level)
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
			LOGGER.info("Saving updated render file v[{}] at sect {}", this.metaData.dataVersion.get(), this.pos);
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
