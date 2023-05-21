package com.seibel.lod.core.file.fullDatafile;

import java.io.*;
import java.lang.ref.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.lod.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.loader.AbstractFullDataSourceLoader;
import com.seibel.lod.core.file.metaData.BaseMetaData;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.file.metaData.AbstractMetaDataContainerFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.AtomicsUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.objects.dataStreams.DhDataInputStream;
import org.apache.logging.log4j.Logger;

/**
 * Represents a File that contains a {@link IFullDataSource}.
 */
public class FullDataMetaFile extends AbstractMetaDataContainerFile
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(FullDataMetaFile.class.getSimpleName());
	
	
	private final IDhLevel level;
	private final IFullDataSourceProvider fullDataSourceProvider;
	private boolean doesFileExist;
	
	
	public AbstractFullDataSourceLoader fullDataSourceLoader;
	public Class<? extends IFullDataSource> dataType;
	
	/**
	 * Can be cleared if the garbage collector determines there isn't enough space. <br><br>
	 * 
	 * When clearing, don't set to null, instead create a SoftReference containing null. 
	 * This will make null checks simpler.
	 */
	private SoftReference<IFullDataSource> cachedFullDataSource = new SoftReference<>(null);
	private CompletableFuture<IFullDataSource> dataSourceWriteQueueFuture;
	
	
	
	//TODO: use ConcurrentAppendSingleSwapContainer<LodDataSource> instead of below:
	private static class GuardedMultiAppendQueue
	{
		ReentrantReadWriteLock appendLock = new ReentrantReadWriteLock();
		ConcurrentLinkedQueue<ChunkSizedFullDataAccessor> queue = new ConcurrentLinkedQueue<>();
	}
	
	
	// ===Concurrent Write stuff===
	private final AtomicReference<GuardedMultiAppendQueue> writeQueueRef = new AtomicReference<>(new GuardedMultiAppendQueue());
	private GuardedMultiAppendQueue backWriteQueue = new GuardedMultiAppendQueue();
	// ===========================
	
	
	private final AtomicReference<CompletableFuture<IFullDataSource>> inCacheWriteAccessFuture = new AtomicReference<>(null);
	
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
	
	
	
	//==============//
	// constructors //
	//==============//
	
	/** 
	 * Creates a new file. 
	 * @throws FileAlreadyExistsException if a file already exists. 
	 */
	public FullDataMetaFile(IFullDataSourceProvider fullDataSourceProvider, IDhLevel level, DhSectionPos pos) throws FileAlreadyExistsException
	{
		super(fullDataSourceProvider.computeDataFilePath(pos), pos);
		debugPhantomLifeCycleCheck();
		
		this.fullDataSourceProvider = fullDataSourceProvider;
		this.level = level;
		LodUtil.assertTrue(this.baseMetaData == null);
		this.doesFileExist = false;
	}
	
	/** 
	 * Uses an existing file.
	 * @throws IOException if the file was formatted incorrectly
	 * @throws FileNotFoundException if no file exists for the given path
	 */
	public FullDataMetaFile(IFullDataSourceProvider fullDataSourceProvider, IDhLevel level, File file) throws IOException, FileNotFoundException
	{
		super(file);
		debugPhantomLifeCycleCheck();
		
		this.fullDataSourceProvider = fullDataSourceProvider;
		this.level = level;
		LodUtil.assertTrue(this.baseMetaData != null);
		this.doesFileExist = true;
		
		this.fullDataSourceLoader = AbstractFullDataSourceLoader.getLoader(this.baseMetaData.dataTypeId, this.baseMetaData.binaryDataFormatVersion);
		if (this.fullDataSourceLoader == null)
		{
			throw new IOException("Invalid file: Data type loader not found: "+this.baseMetaData.dataTypeId+"(v"+this.baseMetaData.binaryDataFormatVersion +")");
		}
		
		this.dataType = this.fullDataSourceLoader.clazz;
	}
	
	
	
	//==========//
	// get data //
	//==========//
	
	// Cause: Generic Type runtime casting cannot safety check it.
	// However, the Union type ensures the 'data' should only contain the listed type.
	public CompletableFuture<IFullDataSource> loadOrGetCachedDataSourceAsync()
	{
		debugPhantomLifeCycleCheck();
		
		CompletableFuture<IFullDataSource> getCachedFuture = this.getCachedDataSourceAsync();
		if (getCachedFuture != null)
		{
			return getCachedFuture;
		}
		
		
		
		CompletableFuture<IFullDataSource> future = new CompletableFuture<>();
		if (!this.doesFileExist)
		{
			// create a new Meta file
			
			this.fullDataSourceProvider.onCreateDataFile(this)
				.thenApply((fullDataSource) -> 
				{
					this.baseMetaData = this._makeBaseMetaData(fullDataSource);
					return fullDataSource;
				})
				.thenApply((fullDataSource) -> this.fullDataSourceProvider.onDataFileLoaded(fullDataSource, this.baseMetaData, this::_updateAndWriteDataSource, this::_applyWriteQueueToFullDataSource))
				.whenComplete((fullDataSource, exception) ->
				{
					if (exception != null)
					{
						LOGGER.error("Uncaught error on creation "+this.file+": ", exception);
						future.complete(null);
						this.cachedFullDataSource = new SoftReference<>(null);
					}
					else
					{
						future.complete(fullDataSource);
						new DataObjTracker(fullDataSource);
						this.cachedFullDataSource = new SoftReference<>(fullDataSource);
					}
				});
		}
		else
		{
			// read in the existing meta file's data
			
			if (this.baseMetaData == null)
			{
				throw new IllegalStateException("Meta data not loaded!");
			}
			
			// don't continue if the provider has been shut down
			ExecutorService executorService = this.fullDataSourceProvider.getIOExecutor(); 
			if (executorService.isTerminated())
			{
				future.complete(null);
				return future;
			}
			
			
			CompletableFuture.supplyAsync(() ->
				{
					// Load the file.
					IFullDataSource fullDataSource;
					try (FileInputStream fileInputStream = this.getFileInputStream();
						DhDataInputStream compressedStream = new DhDataInputStream(fileInputStream))
					{
						fullDataSource = this.fullDataSourceLoader.loadData(this, compressedStream, this.level);
					}
					catch (Exception ex)
					{
						if (ex instanceof InterruptedException)
						{
							//LOGGER.warn(FullDataMetaFile.class.getSimpleName()+" loadOrGetCachedAsync interrupted.");
							return null;
						}
						
						// can happen if there is a missing file or the file was incorrectly formatted
						throw new CompletionException(ex);
					}
					
					// confirm that this thread is in control
					LodUtil.assertTrue(this.inCacheWriteAccessFuture.get() == null,
							"No one should be writing to the cache while we are in the process of " +
									"loading one into the cache! Is this a deadlock?");
					
					// fire the onDataLoaded method
					fullDataSource = this.fullDataSourceProvider.onDataFileLoaded(fullDataSource, this.baseMetaData, this::_updateAndWriteDataSource, this::_applyWriteQueueToFullDataSource);
					return fullDataSource;
					
				}, executorService)
				.exceptionally((ex) ->
				{
					if (ex instanceof InterruptedException)
					{
						// this exception can be ignored
						//LOGGER.warn(FullDataMetaFile.class.getSimpleName()+" loadOrGetCachedAsync interrupted.");
						return null;
					}
					else if (ex instanceof RejectedExecutionException)
					{
						// this exception can be ignored
						//LOGGER.warn(FullDataMetaFile.class.getSimpleName()+" loadOrGetCachedAsync attempted to use a closed thread pool.");
						return null;
					}
					
					
					LOGGER.error("Error loading file "+this.file+": ", ex);
					this.cachedFullDataSource = new SoftReference<>(null);
					
					future.completeExceptionally(ex);
					return null; // the return value here doesn't matter
				})
				.whenComplete((fullDataSource, e) -> 
				{
					future.complete(fullDataSource);
					new DataObjTracker(fullDataSource);
					this.cachedFullDataSource = new SoftReference<>(fullDataSource);
				});
		}
		
		
		return future;
	}
	/** @return a stream for the data contained in this file, skips the metadata from {@link AbstractMetaDataContainerFile}. */
	private FileInputStream getFileInputStream() throws IOException
	{
		FileInputStream fileInputStream = new FileInputStream(this.file);
		
		// skip the meta-data bytes
		int bytesToSkip = AbstractMetaDataContainerFile.METADATA_SIZE_IN_BYTES;
		while (bytesToSkip > 0)
		{
			long skippedByteCount = fileInputStream.skip(bytesToSkip);
			if (skippedByteCount == 0)
			{
				throw new IOException("Invalid file: Failed to skip metadata.");
			}
			bytesToSkip -= skippedByteCount;
		}
		
		if (bytesToSkip != 0)
		{
			throw new IOException("File IO Error: Failed to skip metadata.");
		}
		return fileInputStream;
	}
	private BaseMetaData _makeBaseMetaData(IFullDataSource data)
	{
		AbstractFullDataSourceLoader loader = AbstractFullDataSourceLoader.getLoader(data.getClass(), data.getBinaryDataFormatVersion());
		return new BaseMetaData(data.getSectionPos(), -1,
				data.getDataDetailLevel(), data.getWorldGenStep(), (loader == null ? 0 : loader.datatypeId), data.getBinaryDataFormatVersion());
	}
	/**
	 * @return one of the following:
	 * 		the cached {@link IFullDataSource}, 
	 * 		a future that will complete once the {@link FullDataMetaFile#writeQueueRef} has been written, 
	 * 		or null if nothing has been cached and nothing is being loaded
	 */
	private CompletableFuture<IFullDataSource> getCachedDataSourceAsync()
	{
		// this data source is being written to, use the existing future
		if (this.dataSourceWriteQueueFuture != null)
		{
			return this.dataSourceWriteQueueFuture;
		}
		
		
		
		// attempt to get the cached data source
		IFullDataSource cachedFullDataSource = this.cachedFullDataSource.get();
		if (cachedFullDataSource != null)
		{
			// The file is cached in RAM
			boolean writeQueueEmpty = this.writeQueueRef.get().queue.isEmpty();
			
			
			if (writeQueueEmpty)
			{
				// return the cached data
				return CompletableFuture.completedFuture(cachedFullDataSource);
			}
			else
			{
				// either write the queue or return the future that is waiting for the queue write
				
				// Do a CAS on inCacheWriteLock to ensure that we are the only thread that is writing to the cache,
				// or if we fail, then that means someone else is already doing it, and we can just return the future
				CompletableFuture<IFullDataSource> future = new CompletableFuture<>();
				CompletableFuture<IFullDataSource> compareAndSwapFuture = AtomicsUtil.compareAndExchange(this.inCacheWriteAccessFuture, null, future);
				if (compareAndSwapFuture != null)
				{
					// a write is already in progress, return its future.
					return compareAndSwapFuture;
				}
				else
				{
					// write the queue to the data source
					
					this.dataSourceWriteQueueFuture = future;
					
					this.fullDataSourceProvider.onDataFileRefresh(cachedFullDataSource, this.baseMetaData, this::_applyWriteQueueToFullDataSource, this::_updateAndWriteDataSource)
						.handle((fullDataSource, exception) -> 
						{
							if (exception != null)
							{
								LOGGER.error("Error refreshing data "+this.pos+": "+exception+" "+exception.getMessage());
								future.complete(null);
								this.cachedFullDataSource = new SoftReference<>(null);
							}
							else
							{
								future.complete(fullDataSource);
								new DataObjTracker(fullDataSource);
								this.cachedFullDataSource = new SoftReference<>(fullDataSource);
							}
							this.dataSourceWriteQueueFuture = null;
							
							this.inCacheWriteAccessFuture.set(null);
							return fullDataSource;
						});
					return future;
				}
			}
		}
		
		
		
		// the data source hasn't been loaded 
		// and isn't in the process of being loaded
		return null;
	}
	
	
	
	//===============//
	// data updating //
	//===============//
	
	/** 
	 * Adds the given {@link ChunkSizedFullDataAccessor} to the write queue,
	 * which will be applied to the object at some undefined time in the future.
	 */
	public void addToWriteQueue(ChunkSizedFullDataAccessor chunkAccessor)
	{
		debugPhantomLifeCycleCheck();
		
		DhLodPos chunkLodPos = new DhLodPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkAccessor.pos.x, chunkAccessor.pos.z);
		
		LodUtil.assertTrue(this.pos.getSectionBBoxPos().overlapsExactly(chunkLodPos), "Chunk pos "+chunkLodPos+" doesn't exactly overlap with section "+this.pos);
		//LOGGER.info("Write Chunk {} to file {}", chunkPos, pos);
		
		GuardedMultiAppendQueue writeQueue = this.writeQueueRef.get();
		// Using read lock is OK, because the queue's underlying data structure is thread-safe.
		// This lock is only used to insure on polling the queue, that the queue is not being
		// modified by another thread.
		ReentrantReadWriteLock.ReadLock appendLock = writeQueue.appendLock.readLock();
		appendLock.lock();
		try
		{
			writeQueue.queue.add(chunkAccessor);
		}
		finally
		{
			appendLock.unlock();
		}
		
		//LOGGER.info("write queue length for pos "+this.pos+": " + writeQueue.queue.size());
	}
	
	
	/** Applies any queued {@link ChunkSizedFullDataAccessor} to this metadata's {@link IFullDataSource} and writes the data to file. */
	public CompletableFuture<Void> flushAndSaveAsync()
	{
		debugPhantomLifeCycleCheck();
		boolean isEmpty = this.writeQueueRef.get().queue.isEmpty();
		if (!isEmpty)
		{
			// This will flush the data to disk.
			return this.loadOrGetCachedDataSourceAsync().thenApply((fullDataSource) -> null /* ignore the result, just wait for the load to finish*/);
		}
		else
		{
			return CompletableFuture.completedFuture(null);
		}
	}
	
	
	/** updates this object to match the given {@link IFullDataSource} and then writes the new data to file. */
	private void _updateAndWriteDataSource(IFullDataSource fullDataSource)
	{
		if (fullDataSource.isEmpty())
		{
			// delete the empty data source
			if (this.file.exists() && !this.file.delete())
			{
				LOGGER.warn("Failed to delete data file at "+this.file);
			}
			this.doesFileExist = false;
		}
		else
		{
			// update the data source and write the new data to file
			
			//LOGGER.info("Saving data file of {}", data.getSectionPos());
			try
			{
				// Write/Update data
				LodUtil.assertTrue(this.baseMetaData != null);
				
				this.baseMetaData.dataLevel = fullDataSource.getDataDetailLevel();
				this.fullDataSourceLoader = AbstractFullDataSourceLoader.getLoader(fullDataSource.getClass(), fullDataSource.getBinaryDataFormatVersion());
				LodUtil.assertTrue(this.fullDataSourceLoader != null, "No loader for "+fullDataSource.getClass()+" (v"+fullDataSource.getBinaryDataFormatVersion()+")");
				
				this.dataType = fullDataSource.getClass();
				this.baseMetaData.dataTypeId = (this.fullDataSourceLoader == null) ? 0 : this.fullDataSourceLoader.datatypeId;
				this.baseMetaData.binaryDataFormatVersion = fullDataSource.getBinaryDataFormatVersion();
				
				super.writeData((bufferedOutputStream) -> fullDataSource.writeToStream((bufferedOutputStream), this.level));
				this.doesFileExist = true;
			}
			catch (ClosedByInterruptException e) // thrown by buffers that are interrupted
			{
				// expected if the file handler is shut down, the exception can be ignored
//				LOGGER.warn("FullData file writing interrupted.", e);
			}
			catch (IOException e)
			{
				LOGGER.error("Failed to save updated data file at "+this.file+" for section "+this.pos, e);
			}
		}
	}
	
	/** @return true if the queue was not empty and data was applied to the {@link IFullDataSource}. */
	private boolean _applyWriteQueueToFullDataSource(IFullDataSource fullDataSource)
	{
		// Poll the write queue
		// First check if write queue is empty, then swap the write queue.
		// Must be done in this order to ensure isMemoryAddressValid work properly. See isMemoryAddressValid() for details.
		boolean isEmpty = this.writeQueueRef.get().queue.isEmpty();
		if (!isEmpty)
		{
			this._swapWriteQueue();
			for (ChunkSizedFullDataAccessor chunk : this.backWriteQueue.queue)
			{
				fullDataSource.update(chunk);
			}
			this.backWriteQueue.queue.clear();
			//LOGGER.info("Updated Data file at {} for sect {} with {} chunk writes.", path, pos, count);
		}
		return !isEmpty;
	}
	private void _swapWriteQueue()
	{
		GuardedMultiAppendQueue writeQueue = this.writeQueueRef.getAndSet(this.backWriteQueue);
		// Acquire write lock and then release it again as we only need to ensure that the queue
		// is not being appended to by another thread. Note that the above atomic swap &
		// the guarantee that all append first acquire the appendLock means after the locK() call,
		// there will be no other threads able to or is currently appending to the queue.
		// Note: The above needs the getAndSet() to have at least Release Memory order.
		// (not that java supports anything non volatile for getAndSet()...)
		writeQueue.appendLock.writeLock().lock();
		writeQueue.appendLock.writeLock().unlock();
		this.backWriteQueue = writeQueue;
	}
	
	
	
	//===========//
	// debugging //
	//===========//
	
	public static void debugPhantomLifeCycleCheck()
	{
		DataObjTracker phantom = (DataObjTracker) lifeCycleDebugQueue.poll();
		
		// wait for the tracker to be garbage collected(?)
		while (phantom != null)
		{
			//LOGGER.info("Full Data at pos: "+phantom.pos+" has been freed. "+lifeCycleDebugSet.size()+" Full Data files remaining.");
			phantom.close();
			phantom = (DataObjTracker) lifeCycleDebugQueue.poll();
		}
	}
	
}
