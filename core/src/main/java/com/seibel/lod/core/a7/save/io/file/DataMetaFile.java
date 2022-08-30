package com.seibel.lod.core.a7.save.io.file;

import java.awt.*;
import java.io.*;
import java.lang.ref.*;
import java.security.Provider;
import java.sql.Ref;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.DataSourceLoader;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.full.FullDataSource;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.save.io.MetaFile;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;

public class DataMetaFile extends MetaFile {
	private final ILevel level;
	public DataSourceLoader loader;
	public Class<? extends LodDataSource> dataType;
	AtomicInteger localVersion = new AtomicInteger(); // This MUST be atomic
	
	// The '?' type should either be:
	//    SoftReference<LodDataSource>, or		- Non-dirty file that can be GCed
	//    CompletableFuture<LodDataSource>, or  - File that is being loaded
	//    null									- Nothing is loaded or being loaded
	AtomicReference<Object> data = new AtomicReference<Object>(null);

	//TODO: use ConcurrentAppendSingleSwapContainer<LodDataSource> instead of below:
	private static class GuardedMultiAppendQueue {
		ReentrantReadWriteLock appendLock = new ReentrantReadWriteLock();
		ConcurrentLinkedQueue<ChunkSizedData> queue = new ConcurrentLinkedQueue<>();
	}
	AtomicReference<GuardedMultiAppendQueue> writeQueue =
			new AtomicReference<>(new GuardedMultiAppendQueue());
	GuardedMultiAppendQueue _backQueue = new GuardedMultiAppendQueue();
	private final AtomicBoolean inCacheWriteLock = new AtomicBoolean(false);

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

	// Load a metaFile in this path. It also automatically read the metadata.
	public DataMetaFile(ILevel level, File path) throws IOException {
		super(path);
		debugCheck();
		this.level = level;
		loader = DataSourceLoader.getLoader(dataTypeId, loaderVersion);
		if (loader == null) {
			throw new IOException("Invalid file: Data type loader not found: "
					+ dataTypeId + "(v" + loaderVersion + ")");
		}
		dataType = loader.clazz;
	}

	// Make a new MetaFile. It doesn't load or write any metadata itself.
	public DataMetaFile(ILevel level, File path, DhSectionPos pos, CompletableFuture<LodDataSource> creator) {
		super(path, pos);
		debugCheck();
		this.level = level;
		CompletableFuture<LodDataSource> future = new CompletableFuture<>();
		data.set(future);
		creator.thenApply((f) -> {
				applyWriteQueue(f);
				return f;
		}).whenComplete((f, e) -> {
			if (e != null) {
				LOGGER.error("Uncaught error on creation {}: ", path, e);
				future.complete(null);
				data.set(null);
			} else {
				future.complete(f);
				new DataObjTracker(f);
				data.set(new SoftReference<>(f));
			}
		});
	}
	
	public boolean isValid(int version) {
		debugCheck();
		boolean isValid;
		// First check if write queue is empty, then check if localVersion is equal to version.
		// Must be done in this order as writer will increment localVersion before polling in the write queue.
		// Note: Be careful with the localVerion read's memory order if we do switch over to java 1.9.
		// It should be acquire or higher!

		isValid = writeQueue.get().queue.isEmpty(); // The 'get()' & 'isEmpty()' enforce a memory barrier.
													// Also, we are just querying the state, and this means no
													// need to get any locks for the queue.
		isValid &= localVersion.get() == version; // The 'get()' enforce a memory barrier.
		return isValid;
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
					if (inCacheWriteLock.getAndSet(true) == false) {
						try {
							applyWriteQueue((LodDataSource) inner);
						} catch (Exception e) {
							LOGGER.error("Error while applying changes to LodDataSource at {}: ", pos, e);
						} finally {
							inCacheWriteLock.set(false);
						}
					}
				}
				// Finally, return the cached data.
				return CompletableFuture.completedFuture((LodDataSource)inner);
			}
		}
		
		//==== Cached file out of scrope. ====
		// Someone is already trying to complete it. so just return the obj.
		if ((obj instanceof CompletableFuture<?>)) {
			return (CompletableFuture<LodDataSource>)obj;
		}
		return null;
	}

	// Cause: Generic Type runtime casting cannot safety check it.
	// However, the Union type ensures the 'data' should only contain the listed type.
	public CompletableFuture<LodDataSource> loadOrGetCached(Executor fileReaderThreads) {
		debugCheck();
		Object obj = data.get();
		
		CompletableFuture<LodDataSource> cached = _readCached(obj);
		if (cached != null) return cached;

		CompletableFuture<LodDataSource> future = new CompletableFuture<>();
		
		// Would use faster and non-nesting Compare and exchange. But java 8 doesn't have it! :(
		boolean worked = data.compareAndSet(obj, future);
		if (!worked) return loadOrGetCached(fileReaderThreads);
		
		// Would use CompletableFuture.completeAsync(...), But, java 8 doesn't have it! :(
		//return future.completeAsync(this::loadAndUpdateDataSource, fileReaderThreads);
		CompletableFuture.supplyAsync(this::loadAndUpdateDataSource, fileReaderThreads)
				.whenComplete((f, e) -> {
			if (e != null) {
				LOGGER.error("Uncaught error loading file {}: ", path, e);
				future.complete(null);
				data.set(null);
			} else {
				future.complete(f);
				new DataObjTracker(f);
				data.set(new SoftReference<>(f));
			}
		});
		return future;
	}

	// Return whether any write has happened to the data
	private void applyWriteQueue(LodDataSource data) {
		// Poll the write queue
		// First check if write queue is empty, then swap the write queue.
		// Must be done in this order to ensure isValid work properly. See isValid() for details.
		boolean isEmpty = writeQueue.get().queue.isEmpty();
		int localVer;
		if (!isEmpty) {
			localVer = localVersion.incrementAndGet();
			swapWriteQueue();
			int count = _backQueue.queue.size();
			for (ChunkSizedData chunk : _backQueue.queue) {
				data.update(chunk);
			}
			_backQueue.queue.clear();
			write(data);
			LOGGER.info("Updated Data file at {} for sect {} with {} chunk writes.", path, pos, count);
		} else localVer = localVersion.get();
		data.setLocalVersion(localVer);
	}
	
	private LodDataSource loadAndUpdateDataSource() {
		LodDataSource data = loadFile();
		if (data == null) data = FullDataSource.createEmpty(pos);
		// Apply the write queue
		LodUtil.assertTrue(!inCacheWriteLock.get(),"No one should be writing to the cache while we are in the process of " +
				"loading one into the cache! Is this a deadlock?");
		applyWriteQueue(data);
		// Finally, return the data.
		return data;
	}

	private LodDataSource loadFile() {
		if (!path.exists()) return null;
		// Refresh the metadata.
		try {
			super.updateMetaData();
		} catch (Exception e) {
			LOGGER.warn("Metadata for file {} changed unexpectedly and in an invalid state. Dropping file.", path, e);
			return null;
		}
		if (loader == null) {
			//LOGGER.warn("No loader for file {}. Dropping file.", path); // Disable as data lod has no loader yet.
			return null;
		}

		// Load the file.
		try (FileInputStream fio = getDataContent()){
			return loader.loadData(this, fio, level);
		} catch (Exception e) {
			LOGGER.warn("Failed to load file {}. Dropping file.", path, e);
			return null;
		}
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


	public CompletableFuture<Void> flushAndSave(Executor fileWriterThreads) {
		debugCheck();
		boolean isEmpty = writeQueue.get().queue.isEmpty();
		if (!isEmpty) {
			return loadOrGetCached(fileWriterThreads).thenApply((unused) -> null); // This will flush the data to disk.
		} else {
			return CompletableFuture.completedFuture(null);
		}
	}

	@Override
	protected void updateMetaData() throws IOException {
		super.updateMetaData();
		loader = DataSourceLoader.getLoader(dataTypeId, loaderVersion);
		if (loader == null) {
			throw new IOException("Invalid file: Data type loader not found: " + dataTypeId + "(v" + loaderVersion + ")");
		}
		dataType = loader.clazz;
		dataTypeId = loader.datatypeId;
	}

	private void write(LodDataSource data) {
		try {
			dataLevel = data.getDataDetail();
			loader = DataSourceLoader.getLoader(data.getClass(), data.getDataVersion());
			// FIXME: Uncomment this and fix id when we have FullDataSource loader!
			//LodUtil.assertTrue(loader != null, "No loader for {} (v{})", data.getClass(), data.getDataVersion());
			dataType = data.getClass();
			dataTypeId = loader == null ? 0 : loader.datatypeId;
			loaderVersion = data.getDataVersion();
			timestamp = System.currentTimeMillis(); // TODO: Do we need to use server synced time?
			// Warn: This may become an attack vector! Be careful!
			super.writeData((out) -> {
				try {
					data.saveData(level, this, out);
				} catch (IOException e) {
					LOGGER.error("Failed to save data for file {}", path, e);
				}
			});
		} catch (IOException e) {
			LOGGER.error("Failed to write data for file {}", path, e);
		}
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
