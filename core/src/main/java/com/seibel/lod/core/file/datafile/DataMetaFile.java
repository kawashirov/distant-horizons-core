package com.seibel.lod.core.file.datafile;

import java.io.*;
import java.lang.ref.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.AbstractDataSourceLoader;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.file.metaData.MetaData;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.file.metaData.AbstractMetaDataFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.AtomicsUtil;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

public class DataMetaFile extends AbstractMetaDataFile
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(DataMetaFile.class.getSimpleName());
	
	private final IDhLevel level;
	private final IDataSourceProvider handler;
	private boolean doesFileExist;

	public AbstractDataSourceLoader loader;
	public Class<? extends ILodDataSource> dataType;
	// The '?' type should either be:
	//    SoftReference<LodDataSource>, or		    - Non-dirty file that can be GCed
	//    CompletableFuture<LodDataSource>, or      - File that is being loaded. No guarantee that the type is promotable or not
	//    null									    - Nothing is loaded or being loaded
	AtomicReference<Object> data = new AtomicReference<Object>(null);

	//TODO: use ConcurrentAppendSingleSwapContainer<LodDataSource> instead of below:
	private static class GuardedMultiAppendQueue {
		ReentrantReadWriteLock appendLock = new ReentrantReadWriteLock();
		ConcurrentLinkedQueue<ChunkSizedData> queue = new ConcurrentLinkedQueue<>();
	}

	// ===Concurrent Write stuff===
	AtomicReference<GuardedMultiAppendQueue> writeQueue =
			new AtomicReference<>(new GuardedMultiAppendQueue());
	GuardedMultiAppendQueue _backQueue = new GuardedMultiAppendQueue();
	// ===========================

	private AtomicReference<CompletableFuture<ILodDataSource>> inCacheWriteAccessFuture = new AtomicReference<>(null);

	// ===Object lifetime stuff===
	private static final ReferenceQueue<ILodDataSource> lifeCycleDebugQueue = new ReferenceQueue<>();
	private static final Set<DataObjTracker> lifeCycleDebugSet = ConcurrentHashMap.newKeySet();
	private static class DataObjTracker extends PhantomReference<ILodDataSource> implements Closeable {
		private final DhSectionPos pos;
		DataObjTracker(ILodDataSource data) {
			super(data, lifeCycleDebugQueue);
			//LOGGER.info("Phantom created on {}! count: {}", data.getSectionPos(), lifeCycleDebugSet.size());
			lifeCycleDebugSet.add(this);
			pos = data.getSectionPos();
		}
		@Override
		public void close() {
			lifeCycleDebugSet.remove(this);
		}
	}
    // ===========================



	// Create a new metaFile
	public DataMetaFile(IDataSourceProvider handler, IDhLevel level, DhSectionPos pos) throws IOException {
		super(handler.computeDataFilePath(pos), pos);
		debugCheck();
		this.handler = handler;
		this.level = level;
		LodUtil.assertTrue(metaData == null);
		doesFileExist = false;
	}

	public DataMetaFile(IDataSourceProvider handler, IDhLevel level, File path) throws IOException {
		super(path);
		debugCheck();
		this.handler = handler;
		this.level = level;
		LodUtil.assertTrue(metaData != null);
		loader = AbstractDataSourceLoader.getLoader(metaData.dataTypeId, metaData.loaderVersion);
		if (loader == null) {
			throw new IOException("Invalid file: Data type loader not found: "
					+ metaData.dataTypeId + "(v" + metaData.loaderVersion + ")");
		}
		dataType = loader.clazz;
		doesFileExist = true;
	}

	public CompletableFuture<Void> flushAndSave() {
		debugCheck();
		boolean isEmpty = writeQueue.get().queue.isEmpty();
		if (!isEmpty) {
			return loadOrGetCached().thenApply((unused) -> null); // This will flush the data to disk.
		} else {
			return CompletableFuture.completedFuture(null);
		}
	}

	public long getCacheVersion() {
		debugCheck();
		MetaData getData = metaData;
		return getData == null ? 0 : metaData.dataVersion.get();
	}

	public boolean isCacheVersionValid(long cacheVersion) {
		debugCheck();
		boolean noWrite = writeQueue.get().queue.isEmpty();
		if (!noWrite) {
			return false;
		} else {
			MetaData getData = metaData;
			//NOTE: Do this instead of direct compare so values that wrapped around still works correctly.
			return (getData == null ? 0 : metaData.dataVersion.get()) - cacheVersion <= 0;
		}
	}

	public void addToWriteQueue(ChunkSizedData datatype) {
		debugCheck();
		DhLodPos chunkPos = new DhLodPos((byte) (datatype.dataDetail + 4), datatype.x, datatype.z);
		LodUtil.assertTrue(pos.getSectionBBoxPos().overlaps(chunkPos), "Chunk pos {} doesn't overlap with section {}", chunkPos, pos);
		//LOGGER.info("Write Chunk {} to file {}", chunkPos, pos);

		GuardedMultiAppendQueue queue = writeQueue.get();
		// Using read lock is OK, because the queue's underlying data structure is thread-safe.
		// This lock is only used to insure on polling the queue, that the queue is not being
		// modified by another thread.
		Lock appendLock = queue.appendLock.readLock();
		appendLock.lock();
		try {
			queue.queue.add(datatype);
		} finally {
			appendLock.unlock();
		}
	}

	// Cause: Generic Type runtime casting cannot safety check it.
	// However, the Union type ensures the 'data' should only contain the listed type.
	public CompletableFuture<ILodDataSource> loadOrGetCached() {
		debugCheck();
		Object obj = data.get();

		CompletableFuture<ILodDataSource> cached = _readCached(obj);
		if (cached != null) return cached;

		CompletableFuture<ILodDataSource> future = new CompletableFuture<>();

		// Would use faster and non-nesting Compare and exchange. But java 8 doesn't have it! :(
		boolean worked = data.compareAndSet(obj, future);
		if (!worked) return loadOrGetCached();

		// After cas. We are in exclusive control.
		if (!doesFileExist) {
			handler.onCreateDataFile(this)
					.thenApply((data) -> {
						metaData = makeMetaData(data);
						return data;
					})
					.thenApply((data) -> handler.onDataFileLoaded(data, metaData, this::saveChanges, this::applyWriteQueue))
					.whenComplete((v, e) -> {
						if (e != null) {
							LOGGER.error("Uncaught error on creation {}: ", path, e);
							future.complete(null);
							data.set(null);
						} else {
							future.complete(v);
							new DataObjTracker(v);
							data.set(new SoftReference<>(v));
						}
					});
		} else {
			CompletableFuture.supplyAsync(() -> {
						if (metaData == null)
							throw new IllegalStateException("Meta data not loaded!");
						// Load the file.
						ILodDataSource data;
						try (FileInputStream fio = getDataContent()){
							data = loader.loadData(this, fio, level);
						} catch (IOException e) {
							throw new CompletionException(e);
						}
						// Apply the write queue
						LodUtil.assertTrue(inCacheWriteAccessFuture.get() == null,"No one should be writing to the cache while we are in the process of " +
								"loading one into the cache! Is this a deadlock?");
						data = handler.onDataFileLoaded(data, metaData, this::saveChanges, this::applyWriteQueue);
						// Finally, return the data.
						return data;
					}, handler.getIOExecutor())
					.whenComplete((f, e) -> {
						if (e != null) {
							LOGGER.error("Error loading file {}: ", path, e);
							future.complete(null);
							data.set(null);
						} else {
							future.complete(f);
							new DataObjTracker(f);
							data.set(new SoftReference<>(f));
						}
					});
		}

		// Would use CompletableFuture.completeAsync(...), But, java 8 doesn't have it! :(
		//return future.completeAsync(this::loadAndUpdateDataSource, fileReaderThreads);
		return future;
	}

	private static MetaData makeMetaData(ILodDataSource data) {
		AbstractDataSourceLoader loader = AbstractDataSourceLoader.getLoader(data.getClass(), data.getDataVersion());
		return new MetaData(data.getSectionPos(), -1, 1,
				data.getDataDetail(), loader == null ? 0 : loader.datatypeId, data.getDataVersion());
	}

	// "unchecked": Suppress casting of CompletableFuture<?> to CompletableFuture<LodDataSource>
	// "PointlessBooleanExpression": Suppress explicit (boolean == false) check for more understandable CAS operation code.
	@SuppressWarnings({"unchecked"})
	private CompletableFuture<ILodDataSource> _readCached(Object obj) {
		// Has file cached in RAM and not freed yet.
		if ((obj instanceof SoftReference<?>)) {
			Object inner = ((SoftReference<?>)obj).get();
			if (inner != null) {
				LodUtil.assertTrue(inner instanceof ILodDataSource);
				boolean isEmpty = writeQueue.get().queue.isEmpty();
				// If the queue is empty, and the CAS on inCacheWriteLock succeeds, then we are the thread
				// that will be applying the changes to the cache.
				if (!isEmpty) {
					// Do a CAS on inCacheWriteLock to ensure that we are the only thread that is writing to the cache,
					// or if we fail, then that means someone else is already doing it, and we can just return the future
					CompletableFuture<ILodDataSource> future = new CompletableFuture<>();
					CompletableFuture<ILodDataSource> cas = AtomicsUtil.compareAndExchange(inCacheWriteAccessFuture, null, future);
					if (cas == null) {
						try {
							data.set(future);
							handler.onDataFileRefresh((ILodDataSource) inner, metaData, this::applyWriteQueue, this::saveChanges).handle((v, e) -> {
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
							return CompletableFuture.completedFuture((ILodDataSource) inner);
						}
					} else {
						// or, return the future that will be completed when the write is done.
						return cas;
					}
				} else {
					// or, return the cached data.
					return CompletableFuture.completedFuture((ILodDataSource) inner);
				}
			}
		}
		
		//==== Cached file out of scrope. ====
		// Someone is already trying to complete it. so just return the obj.
		if ((obj instanceof CompletableFuture<?>)) {
			return (CompletableFuture<ILodDataSource>)obj;
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

	private void saveChanges(ILodDataSource data) {
		if (data.isEmpty()) {
			if (path.exists()) if (!path.delete()) LOGGER.warn("Failed to delete data file at {}", path);
			doesFileExist = false;
		} else {
			LOGGER.info("Saving data file of {}", data.getSectionPos());
			try {
				// Write/Update data
				LodUtil.assertTrue(metaData != null);
				metaData.dataLevel = data.getDataDetail();
				loader = AbstractDataSourceLoader.getLoader(data.getClass(), data.getDataVersion());
				LodUtil.assertTrue(loader != null, "No loader for {} (v{})", data.getClass(), data.getDataVersion());
				dataType = data.getClass();
				metaData.dataTypeId = loader == null ? 0 : loader.datatypeId;
				metaData.loaderVersion = data.getDataVersion();
				super.writeData((out) -> data.saveData(level, this, out));
				doesFileExist = true;
			} catch (IOException e) {
				LOGGER.error("Failed to save updated data file at {} for sect {}", path, pos, e);
			}
		}
	}

	// Return whether any write has happened to the data
	private boolean applyWriteQueue(ILodDataSource data) {
		// Poll the write queue
		// First check if write queue is empty, then swap the write queue.
		// Must be done in this order to ensure isMemoryAddressValid work properly. See isMemoryAddressValid() for details.
		boolean isEmpty = writeQueue.get().queue.isEmpty();
		if (!isEmpty) {
			swapWriteQueue();
			int count = _backQueue.queue.size();
			for (ChunkSizedData chunk : _backQueue.queue) {
				data.update(chunk);
			}
			_backQueue.queue.clear();
			LOGGER.info("Updated Data file at {} for sect {} with {} chunk writes.", path, pos, count);
		}
		return !isEmpty;
	}

	private FileInputStream getDataContent() throws IOException {
		FileInputStream fin = new FileInputStream(path);
		int toSkip = METADATA_SIZE;
		while (toSkip > 0) {
			long skipped = fin.skip(toSkip);
			if (skipped == 0) {
				throw new IOException("Invalid file: Failed to skip metadata.");
			}
			toSkip -= skipped;
		}
		if (toSkip != 0) {
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
