package com.seibel.lod.core.io.datafile;

import java.io.*;
import java.lang.ref.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.seibel.lod.core.datatype.LodDataSource;
import com.seibel.lod.core.datatype.DataSourceLoader;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.io.MetaFile;
import com.seibel.lod.core.level.ILevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

public class DataMetaFile extends MetaFile
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(DataMetaFile.class.getSimpleName());
	
	private final ILevel level;
	private final IDataSourceProvider handler;
	private boolean doesFileExist;

	public DataSourceLoader loader;
	public Class<? extends LodDataSource> dataType;
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

	private final AtomicBoolean inCacheWriteAccessAsserter = new AtomicBoolean(false);

	// ===Object lifetime stuff===
	private static final ReferenceQueue<LodDataSource> lifeCycleDebugQueue = new ReferenceQueue<>();
	private static final Set<DataObjTracker> lifeCycleDebugSet = ConcurrentHashMap.newKeySet();
	private static class DataObjTracker extends PhantomReference<LodDataSource> implements Closeable {
		private final DhSectionPos pos;
		DataObjTracker(LodDataSource data) {
			super(data, lifeCycleDebugQueue);
			LOGGER.info("Phantom created on {}! count: {}", data.getSectionPos(), lifeCycleDebugSet.size());
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
	public DataMetaFile(IDataSourceProvider handler, ILevel level, DhSectionPos pos) throws IOException {
		super(handler.computeDataFilePath(pos), pos);
		debugCheck();
		this.handler = handler;
		this.level = level;
		LodUtil.assertTrue(metaData == null);
		doesFileExist = false;
	}

	public DataMetaFile(IDataSourceProvider handler, ILevel level, File path) throws IOException {
		super(path);
		debugCheck();
		this.handler = handler;
		this.level = level;
		LodUtil.assertTrue(metaData != null);
		loader = DataSourceLoader.getLoader(metaData.dataTypeId, metaData.loaderVersion);
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

	public long getDataVersion() {
		debugCheck();
		MetaData getData = metaData;
		return getData == null ? 0 : metaData.dataVersion.get();
	}

	public void addToWriteQueue(ChunkSizedData datatype) {
		debugCheck();
		DhLodPos chunkPos = new DhLodPos((byte) (datatype.dataDetail + 4), datatype.x, datatype.z);
		LodUtil.assertTrue(pos.getSectionBBoxPos().overlaps(chunkPos), "Chunk pos {} doesn't overlap with section {}", chunkPos, pos);

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
	public CompletableFuture<LodDataSource> loadOrGetCached() {
		debugCheck();
		Object obj = data.get();

		CompletableFuture<LodDataSource> cached = _readCached(obj);
		if (cached != null) return cached;

		CompletableFuture<LodDataSource> future = new CompletableFuture<>();

		// Would use faster and non-nesting Compare and exchange. But java 8 doesn't have it! :(
		boolean worked = data.compareAndSet(obj, future);
		if (!worked) return loadOrGetCached();

		// After cas. We are in exclusive control.
		if (!doesFileExist) {
			doesFileExist = true;
			handler.onCreateDataFile(this)
					.thenApply((data) -> {
						metaData = makeMetaData(data);
						return data;
					})
					.thenApply((data) -> handler.onDataFileLoaded(data, this::applyWriteQueue, this::saveChanges))
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
						LodDataSource data;
						try (FileInputStream fio = getDataContent()){
							data = loader.loadData(this, fio, level);
						} catch (IOException e) {
							throw new CompletionException(e);
						}
						// Apply the write queue
						LodUtil.assertTrue(!inCacheWriteAccessAsserter.get(),"No one should be writing to the cache while we are in the process of " +
								"loading one into the cache! Is this a deadlock?");
						data = handler.onDataFileLoaded(data, this::applyWriteQueue, this::saveChanges);
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

	private static MetaData makeMetaData(LodDataSource data) {
		DataSourceLoader loader = DataSourceLoader.getLoader(data.getClass(), data.getDataVersion());
		return new MetaData(data.getSectionPos(), -1, 1,
				data.getDataDetail(), loader == null ? 0 : loader.datatypeId, data.getDataVersion());
	}

	// "unchecked": Suppress casting of CompletableFuture<?> to CompletableFuture<LodDataSource>
	// "PointlessBooleanExpression": Suppress explicit (boolean == false) check for more understandable CAS operation code.
	@SuppressWarnings({"unchecked", "PointlessBooleanExpression"})
	private CompletableFuture<LodDataSource> _readCached(Object obj) {
		// Has file cached in RAM and not freed yet.
		if ((obj instanceof SoftReference<?>)) {
			Object inner = ((SoftReference<?>)obj).get();
			if (inner != null) {
				LodUtil.assertTrue(inner instanceof LodDataSource);
				boolean isEmpty = writeQueue.get().queue.isEmpty();
				// If the queue is empty, and the CAS on inCacheWriteLock succeeds, then we are the thread
				// that will be applying the changes to the cache.
				if (!isEmpty) {
					// Do a CAS on inCacheWriteLock to ensure that we are the only thread that is writing to the cache,
					// or if we fail, then that means someone else is already doing it, and we can just continue.
					// FIXME: Should we return a future that waits for the write to be done for CAS fail? Or should we just return the
					//       cached data that doesn't have all writes done immediately?
					//       The latter give us immediate access to the data, but we need to ensure concurrent reads and
					//       writes doesn't cause unexpected behavior down the line.
					//       For now, I'll go for the latter option and just hope nothing goes wrong...
					if (inCacheWriteAccessAsserter.getAndSet(true) == false) {
						try {
							return handler.onDataFileRefresh((LodDataSource) inner, this::applyWriteQueue, this::saveChanges);
						} catch (Exception e) {
							LOGGER.error("Error while applying changes to LodDataSource at {}: ", pos, e);
						} finally {
							inCacheWriteAccessAsserter.set(false);
						}
					} else {
						// or, return the cached data. FIXME: See above.
						return CompletableFuture.completedFuture((LodDataSource) inner);
					}
				} else {
					// or, return the cached data.
					return CompletableFuture.completedFuture((LodDataSource) inner);
				}
			}
		}
		
		//==== Cached file out of scrope. ====
		// Someone is already trying to complete it. so just return the obj.
		if ((obj instanceof CompletableFuture<?>)) {
			return (CompletableFuture<LodDataSource>)obj;
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

	private void saveChanges(LodDataSource data) {
		try {
			// Write/Update data
			LodUtil.assertTrue(metaData != null);
			metaData.dataLevel = data.getDataDetail();
			loader = DataSourceLoader.getLoader(data.getClass(), data.getDataVersion());
			LodUtil.assertTrue(loader != null, "No loader for {} (v{})", data.getClass(), data.getDataVersion());
			dataType = data.getClass();
			metaData.dataTypeId = loader == null ? 0 : loader.datatypeId;
			metaData.loaderVersion = data.getDataVersion();
			super.writeData((out) -> data.saveData(level, this, out));
		} catch (IOException e) {
			LOGGER.error("Failed to save updated data file at {} for sect {}", path, pos, e);
		}
	}

	// Return whether any write has happened to the data
	private boolean applyWriteQueue(LodDataSource data) {
		// Poll the write queue
		// First check if write queue is empty, then swap the write queue.
		// Must be done in this order to ensure isValid work properly. See isValid() for details.
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
