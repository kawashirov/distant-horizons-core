package com.seibel.lod.core.file.fullDatafile;

import java.io.*;
import java.lang.ref.*;
import java.nio.channels.ClosedByInterruptException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.seibel.lod.core.dataObjects.fullData.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.AbstractFullDataSourceLoader;
import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.file.metaData.BaseMetaData;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.file.metaData.AbstractMetaDataContainerFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.AtomicsUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import org.apache.logging.log4j.Logger;

/**
 * Related to the stored Blockstate/Biome ID data. 
 */
public class FullDataMetaFile extends AbstractMetaDataContainerFile
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(FullDataMetaFile.class.getSimpleName());
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	private final IDhLevel level;
	private final IFullDataSourceProvider handler;
	private boolean doesFileExist;

	public AbstractFullDataSourceLoader loader;
	public Class<? extends IFullDataSource> dataType;
	// The '?' type should either be:
	//    SoftReference<LodDataSource>, or		    - Non-dirty file that can be GCed
	//    CompletableFuture<LodDataSource>, or      - File that is being loaded. No guarantee that the type is promotable or not
	//    null									    - Nothing is loaded or being loaded
	AtomicReference<Object> data = new AtomicReference<Object>(null);

	//TODO: use ConcurrentAppendSingleSwapContainer<LodDataSource> instead of below:
	private static class GuardedMultiAppendQueue {
		ReentrantReadWriteLock appendLock = new ReentrantReadWriteLock();
		ConcurrentLinkedQueue<ChunkSizedFullDataSource> queue = new ConcurrentLinkedQueue<>();
	}

	// ===Concurrent Write stuff===
	AtomicReference<GuardedMultiAppendQueue> writeQueue =
			new AtomicReference<>(new GuardedMultiAppendQueue());
	GuardedMultiAppendQueue _backQueue = new GuardedMultiAppendQueue();
	// ===========================

	private AtomicReference<CompletableFuture<IFullDataSource>> inCacheWriteAccessFuture = new AtomicReference<>(null);

	// ===Object lifetime stuff===
	private static final ReferenceQueue<IFullDataSource> lifeCycleDebugQueue = new ReferenceQueue<>();
	private static final Set<DataObjTracker> lifeCycleDebugSet = ConcurrentHashMap.newKeySet();
	private static class DataObjTracker extends PhantomReference<IFullDataSource> implements Closeable
	{
		private final DhSectionPos pos;
		
		DataObjTracker(IFullDataSource data)
		{
			super(data, lifeCycleDebugQueue);
			//LOGGER.info("Phantom created on {}! count: {}", data.getSectionPos(), lifeCycleDebugSet.size());
			lifeCycleDebugSet.add(this);
			this.pos = data.getSectionPos();
		}
		
		@Override
		public void close() { lifeCycleDebugSet.remove(this); }
		
	}
    // ===========================



	// Create a new metaFile
	public FullDataMetaFile(IFullDataSourceProvider handler, IDhLevel level, DhSectionPos pos) throws IOException {
		super(handler.computeDataFilePath(pos), pos);
		debugCheck();
		this.handler = handler;
		this.level = level;
		LodUtil.assertTrue(metaData == null);
		doesFileExist = false;
	}

	public FullDataMetaFile(IFullDataSourceProvider handler, IDhLevel level, File path) throws IOException {
		super(path);
		debugCheck();
		this.handler = handler;
		this.level = level;
		LodUtil.assertTrue(metaData != null);
		loader = AbstractFullDataSourceLoader.getLoader(metaData.dataTypeId, metaData.loaderVersion);
		if (loader == null) {
			throw new IOException("Invalid file: Data type loader not found: "
					+ metaData.dataTypeId + "(v" + metaData.loaderVersion + ")");
		}
		dataType = loader.clazz;
		doesFileExist = true;
	}

	public CompletableFuture<Void> flushAndSave()
	{
		debugCheck();
		boolean isEmpty = this.writeQueue.get().queue.isEmpty();
		if (!isEmpty)
		{
			return this.loadOrGetCachedAsync().thenApply((unused) -> null); // This will flush the data to disk.
		}
		else
		{
			return CompletableFuture.completedFuture(null);
		}
	}

//	public long getCacheVersion() {
//		debugCheck();
//		return (this.metaData == null) ? 0 : this.metaData.dataVersion.get();
//	}

//	public boolean isCacheVersionValid(long cacheVersion)
//	{
//		debugCheck();
//		boolean noWrite = this.writeQueue.get().queue.isEmpty();
//		if (!noWrite)
//		{
//			return false;
//		}
//		else
//		{
//			BaseMetaData getData = this.metaData;
//			//NOTE: Do this instead of direct compare so values that wrapped around still work correctly.
//			return (getData == null ? 0 : this.metaData.dataVersion.get()) - cacheVersion <= 0;
//		}
//	}
	
	public void addToWriteQueue(ChunkSizedFullDataSource datatype)
	{
		debugCheck();
		DhLodPos chunkPos = new DhLodPos((byte) (datatype.dataDetail + 4), datatype.x, datatype.z);
		LodUtil.assertTrue(pos.getSectionBBoxPos().overlaps(chunkPos), "Chunk pos "+chunkPos+" doesn't overlap with section "+pos);
		//LOGGER.info("Write Chunk {} to file {}", chunkPos, pos);
		
		GuardedMultiAppendQueue writeQueue = this.writeQueue.get();
		// Using read lock is OK, because the queue's underlying data structure is thread-safe.
		// This lock is only used to insure on polling the queue, that the queue is not being
		// modified by another thread.
		Lock appendLock = writeQueue.appendLock.readLock();
		appendLock.lock();
		try
		{
			writeQueue.queue.add(datatype);
		}
		finally
		{
			appendLock.unlock();
		}
	}
	
	// Cause: Generic Type runtime casting cannot safety check it.
	// However, the Union type ensures the 'data' should only contain the listed type.
	public CompletableFuture<IFullDataSource> loadOrGetCachedAsync()
	{
		debugCheck();
		Object obj = this.data.get();
		
		CompletableFuture<IFullDataSource> cached = this._readCachedAsync(obj);
		if (cached != null)
		{
			return cached;
		}
		
		CompletableFuture<IFullDataSource> future = new CompletableFuture<>();
		
		// Would use faster and non-nesting Compare and exchange. But java 8 doesn't have it! :(
		boolean worked = this.data.compareAndSet(obj, future); // TODO obj and future are different object types, would this ever return true?
		if (!worked)
		{
			return this.loadOrGetCachedAsync();
		}
		
		// After cas. We are in exclusive control.
		if (!this.doesFileExist)
		{
			this.handler.onCreateDataFile(this)
					.thenApply((data) -> 
					{
						this.metaData = makeMetaData(data);
						return data;
					})
					.thenApply((data) -> this.handler.onDataFileLoaded(data, this.metaData, this::saveChanges, this::applyWriteQueue))
					.whenComplete((fullDataSource, exception) ->
					{
						if (exception != null)
						{
							LOGGER.error("Uncaught error on creation "+this.file+": ", exception);
							future.complete(null);
							this.data.set(null);
						}
						else
						{
							future.complete(fullDataSource);
							new DataObjTracker(fullDataSource);
							this.data.set(new SoftReference<>(fullDataSource));
						}
					});
		}
		else
		{
			CompletableFuture.supplyAsync(() ->
					{
						if (this.metaData == null)
						{
							throw new IllegalStateException("Meta data not loaded!"); // TODO should this be a CompletionException?
						}
						
						// Load the file.
						IFullDataSource data;
						try (FileInputStream inputStream = this.getDataContent())
						{
							data = this.loader.loadData(this, inputStream, this.level);
						}
						catch (Exception e)
						{
							// can happen if there is a missing file or the file was incorrectly formatted
							throw new CompletionException(e);
						}
						
						// Apply the write queue
						LodUtil.assertTrue(this.inCacheWriteAccessFuture.get() == null,
								"No one should be writing to the cache while we are in the process of " +
										"loading one into the cache! Is this a deadlock?");
						
						data = this.handler.onDataFileLoaded(data, this.metaData, this::saveChanges, this::applyWriteQueue);
						return data;
					}, this.handler.getIOExecutor())
					.exceptionally((e) ->
					{
						LOGGER.error("Error loading file {}: ", this.file, e);
						this.data.set(null);
						
						future.completeExceptionally(e);
						return null; // the return value here doesn't matter
					})
					.whenComplete((dataSource, e) -> 
					{
						future.complete(dataSource);
						new DataObjTracker(dataSource);
						this.data.set(new SoftReference<>(dataSource));
					});
		}
		
		// Would use CompletableFuture.completeAsync(...), But, java 8 doesn't have it! :(
		//return future.completeAsync(this::loadAndUpdateDataSource, fileReaderThreads);
		return future;
	}

	private static BaseMetaData makeMetaData(IFullDataSource data) {
		AbstractFullDataSourceLoader loader = AbstractFullDataSourceLoader.getLoader(data.getClass(), data.getDataVersion());
		return new BaseMetaData(data.getSectionPos(), -1,
				data.getDataDetail(), loader == null ? 0 : loader.datatypeId, data.getDataVersion());
	}

	// "unchecked": Suppress casting of CompletableFuture<?> to CompletableFuture<LodDataSource>
	// "PointlessBooleanExpression": Suppress explicit (boolean == false) check for more understandable CAS operation code.
	@SuppressWarnings({"unchecked"})
	private CompletableFuture<IFullDataSource> _readCachedAsync(Object obj) {
		// Has file cached in RAM and not freed yet.
		if ((obj instanceof SoftReference<?>)) {
			Object inner = ((SoftReference<?>)obj).get();
			if (inner != null) {
				LodUtil.assertTrue(inner instanceof IFullDataSource);
				boolean isEmpty = writeQueue.get().queue.isEmpty();
				// If the queue is empty, and the CAS on inCacheWriteLock succeeds, then we are the thread
				// that will be applying the changes to the cache.
				if (!isEmpty) {
					// Do a CAS on inCacheWriteLock to ensure that we are the only thread that is writing to the cache,
					// or if we fail, then that means someone else is already doing it, and we can just return the future
					CompletableFuture<IFullDataSource> future = new CompletableFuture<>();
					CompletableFuture<IFullDataSource> cas = AtomicsUtil.compareAndExchange(inCacheWriteAccessFuture, null, future);
					if (cas == null) {
						try {
							data.set(future);
							handler.onDataFileRefresh((IFullDataSource) inner, metaData, this::applyWriteQueue, this::saveChanges).handle((v, e) -> {
								if (e != null) {
									LOGGER.error("Error refreshing data {}: ", pos, e);
									future.complete(null);
									data.set(null);
								} else {
									future.complete(v);
									new DataObjTracker(v);
									data.set(new SoftReference<>(v));
								}
								inCacheWriteAccessFuture.set(null);
								return v;
							});
							return future;
						} catch (Exception e) {
							LOGGER.error("Error while doing refreshes to LodDataSource at {}: ", pos, e);
							return CompletableFuture.completedFuture((IFullDataSource) inner);
						}
					} else {
						// or, return the future that will be completed when the write is done.
						return cas;
					}
				} else {
					// or, return the cached data.
					return CompletableFuture.completedFuture((IFullDataSource) inner);
				}
			}
		}
		
		//==== Cached file out of scrope. ====
		// Someone is already trying to complete it. so just return the obj.
		if ((obj instanceof CompletableFuture<?>)) {
			return (CompletableFuture<IFullDataSource>)obj;
		}
		return null;
	}

	private void swapWriteQueue() {
		GuardedMultiAppendQueue queue = writeQueue.getAndSet(_backQueue);
		// Acquire write lock and then release it again as we only need to ensure that the queue
		// is not being appended to by another thread. Note that the above atomic swap &
		// the guarantee that all append first acquire the appendLock means after the locK() call,
		// there will be no other threads able to or is currently appending to the queue.
		// Note: The above needs the getAndSet() to have at least Release Memory order.
		// (not that java supports anything non volatile for getAndSet()...)
		queue.appendLock.writeLock().lock();
		queue.appendLock.writeLock().unlock();
		_backQueue = queue;
	}
	
	private void saveChanges(IFullDataSource fullDataSource)
	{
		if (fullDataSource.isEmpty())
		{
			if (file.exists() && !file.delete())
			{
				LOGGER.warn("Failed to delete data file at {}", file);
			}
			doesFileExist = false;
		}
		else
		{
			//LOGGER.info("Saving data file of {}", data.getSectionPos());
			try
			{
				// Write/Update data
				LodUtil.assertTrue(metaData != null);
				metaData.dataLevel = fullDataSource.getDataDetail();
				loader = AbstractFullDataSourceLoader.getLoader(fullDataSource.getClass(), fullDataSource.getDataVersion());
				LodUtil.assertTrue(loader != null, "No loader for "+fullDataSource.getClass()+" (v"+fullDataSource.getDataVersion()+")");
				dataType = fullDataSource.getClass();
				metaData.dataTypeId = (loader == null) ? 0 : loader.datatypeId;
				metaData.loaderVersion = fullDataSource.getDataVersion();
				super.writeData((outputStream) -> fullDataSource.saveData(level, this, outputStream));
				doesFileExist = true;
			}
			catch (ClosedByInterruptException e) // thrown by buffers that are interrupted
			{
				// expected if the file handler is shut down, the exception can be ignored
//				LOGGER.warn("FullData file writing interrupted.", e);
			}
			catch (IOException e)
			{
				LOGGER.error("Failed to save updated data file at "+file+" for sect "+pos, e);
			}
		}
	}
	
	/** @return whether any writing has happened to the data */
	private boolean applyWriteQueue(IFullDataSource fullDataSource)
	{
		// Poll the write queue
		// First check if write queue is empty, then swap the write queue.
		// Must be done in this order to ensure isMemoryAddressValid work properly. See isMemoryAddressValid() for details.
		boolean isEmpty = this.writeQueue.get().queue.isEmpty();
		if (!isEmpty)
		{
			this.swapWriteQueue();
			int count = this._backQueue.queue.size();
			for (ChunkSizedFullDataSource chunk : this._backQueue.queue)
			{
				fullDataSource.update(chunk);
			}
			this._backQueue.queue.clear();
			//LOGGER.info("Updated Data file at {} for sect {} with {} chunk writes.", path, pos, count);
		}
		return !isEmpty;
	}
	
	private FileInputStream getDataContent() throws IOException
	{
		FileInputStream fin = new FileInputStream(this.file);
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


	public static void debugCheck() {
		DataObjTracker phantom = (DataObjTracker) lifeCycleDebugQueue.poll();
		while (phantom != null) {
			LOGGER.info("Data {} is freed. {} remaining", phantom.pos, lifeCycleDebugSet.size());
			phantom.close();
			phantom = (DataObjTracker) lifeCycleDebugQueue.poll();
		}
	}
}
