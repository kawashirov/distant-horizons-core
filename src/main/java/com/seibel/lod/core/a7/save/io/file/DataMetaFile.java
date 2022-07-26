package com.seibel.lod.core.a7.save.io.file;

import java.io.*;
import java.lang.ref.SoftReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.DataSourceLoader;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.full.FullDataSource;
import com.seibel.lod.core.a7.datatype.full.FullFormat;
import com.seibel.lod.core.a7.save.io.MetaFile;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;
import org.spongepowered.asm.mixin.injection.Inject;

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

	public void addToWriteQueue(ChunkSizedData datatype) {
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
		this.level = level;
		loader = DataSourceLoader.getLoader(dataTypeId, loaderVersion);
		if (loader == null) {
			throw new IOException("Invalid file: Data type loader not found: "
					+ dataTypeId + "(v" + loaderVersion + ")");
		}
		dataType = loader.clazz;
	}

	// Make a new MetaFile. It doesn't load or write any metadata itself.
	public DataMetaFile(ILevel level, File path, DhSectionPos pos) {
		super(path, pos);
		this.level = level;
	}
	
	public boolean isValid(int version) {
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


	// Suppress casting of CompletableFuture<?> to CompletableFuture<LodDataSource>
	@SuppressWarnings("unchecked")
	private CompletableFuture<LodDataSource> _readCached(Object obj) {
		// Has file cached in RAM and not freed yet.
		if ((obj instanceof SoftReference<?>)) {
			Object inner = ((SoftReference<?>)obj).get();
			if (inner != null) {
				LodUtil.assertTrue(inner instanceof LodDataSource);
				//TODO: Apply the write if queue is not empty
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
			}
			future.complete(f);
			data.set(new SoftReference<>(f));
		});
		return future;
	}
	
	private LodDataSource loadAndUpdateDataSource() {
		LodDataSource data = loadFile();
		if (data == null) data = FullDataSource.createEmpty(pos);

		// Poll the write queue
		// First check if write queue is empty, then swap the write queue.
		// Must be done in this order to ensure isValid work properly. See isValid() for details.
		boolean isEmpty = writeQueue.get().queue.isEmpty();
		int localVer;
		if (!isEmpty) {
			localVer = localVersion.incrementAndGet();
			swapWriteQueue();
			for (ChunkSizedData chunk : _backQueue.queue) {
				data.update(chunk);
			}
			write(data);
			LOGGER.info("Updated Data file at {} for sect {}", path, pos);
		} else localVer = localVersion.get();
		data.setLocalVersion(localVer);
		// Finally, return the data.
		return null;
	}

	private LodDataSource loadFile() {
		if (!path.exists()) return null;
		// Refresh the metadata.
		try {
			super.updateMetaData();
		} catch (IOException e) {
			LOGGER.warn("Metadata for file {} changed unexpectedly and in an invalid state. Dropping file.", path, e);
			return null;
		}

		// Load the file.
		try (FileInputStream fio = getDataContent()){
			return loader.loadData(this, fio, level);
		} catch (IOException e) {
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
			dataType = data.getClass();
			dataTypeId = loader.datatypeId;
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
}
