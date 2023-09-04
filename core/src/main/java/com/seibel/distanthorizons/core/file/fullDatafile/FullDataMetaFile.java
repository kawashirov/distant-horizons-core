/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.core.file.fullDatafile;

import java.awt.*;
import java.io.*;
import java.lang.ref.*;
import java.nio.channels.ClosedByInterruptException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.seibel.distanthorizons.core.dataObjects.fullData.sources.CompleteFullDataSource;
import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.distanthorizons.core.file.metaData.AbstractMetaDataContainerFile;
import com.seibel.distanthorizons.core.file.metaData.BaseMetaData;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhLodPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.dataObjects.fullData.loader.AbstractFullDataSourceLoader;
import com.seibel.distanthorizons.core.util.AtomicsUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataInputStream;
import com.seibel.distanthorizons.core.render.renderer.DebugRenderer;
import com.seibel.distanthorizons.core.render.renderer.IDebugRenderable;
import org.apache.logging.log4j.Logger;

/** Represents a File that contains a {@link IFullDataSource}. */
public class FullDataMetaFile extends AbstractMetaDataContainerFile implements IDebugRenderable
{
	public static final String FILE_SUFFIX = ".lod";
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(FullDataMetaFile.class.getSimpleName());
	
	// === Object lifetime tracking ===
	/** if true both data source creation and garbage collection will be logged */
	private static final boolean LOG_DATA_SOURCE_LIVES = false;
	private static final ReferenceQueue<IFullDataSource> LIFE_CYCLE_DEBUG_QUEUE = new ReferenceQueue<>();
	private static final ReferenceQueue<IFullDataSource> SOFT_REF_DEBUG_QUEUE = new ReferenceQueue<>();
	private static final Set<DataObjTracker> LIFE_CYCLE_DEBUG_SET = ConcurrentHashMap.newKeySet();
	private static final Set<DataObjSoftTracker> SOFT_REF_DEBUG_SET = ConcurrentHashMap.newKeySet();
	// ===========================
	
	
	
	public boolean doesFileExist;
	//TODO: Atm can't find a better way to store when genQueue is checked.
	public boolean genQueueChecked = false;
	
	public AbstractFullDataSourceLoader fullDataSourceLoader;
	public Class<? extends IFullDataSource> fullDataSourceClass;
	
	
	private volatile boolean markedNeedUpdate = false;
	
	private final IDhLevel level;
	private final IFullDataSourceProvider fullDataSourceProvider;
	
	/**
	 * Can be cleared if the garbage collector determines there isn't enough space. <br><br>
	 *
	 * When clearing, don't set to null, instead create a SoftReference containing null.
	 * This makes null checks simpler.
	 */
	private SoftReference<IFullDataSource> cachedFullDataSourceRef = new SoftReference<>(null);
	private final AtomicReference<CompletableFuture<IFullDataSource>> dataSourceLoadFutureRef = new AtomicReference<>(null);
	
	// === Concurrent Write tracking ===
	private final AtomicReference<GuardedMultiAppendQueue> writeQueueRef = new AtomicReference<>(new GuardedMultiAppendQueue());
	private GuardedMultiAppendQueue backWriteQueue = new GuardedMultiAppendQueue();
	// ===========================
	
	
	
	//==============//
	// constructors //
	//==============//
	
	/**
	 * NOTE: should only be used if there is NOT an existing file.
	 * @throws IOException if a file already exists for this position
	 */
	public static FullDataMetaFile createNewFileForPos(IFullDataSourceProvider fullDataSourceProvider, IDhLevel clientLevel, DhSectionPos pos) throws IOException { return new FullDataMetaFile(fullDataSourceProvider, clientLevel, pos); }
	private FullDataMetaFile(IFullDataSourceProvider fullDataSourceProvider, IDhLevel level, DhSectionPos pos) throws IOException
	{
		super(fullDataSourceProvider.computeDataFilePath(pos), pos);
		checkAndLogPhantomDataSourceLifeCycles();
		
		this.fullDataSourceProvider = fullDataSourceProvider;
		this.level = level;
		LodUtil.assertTrue(this.baseMetaData == null);
		this.doesFileExist = false;
		DebugRenderer.register(this);
	}
	
	
	/**
	 * NOTE: should only be used if there IS an existing file.
	 * @throws IOException if the file was formatted incorrectly
	 * @throws FileNotFoundException if no file exists for the given path
	 */
	public static FullDataMetaFile createFromExistingFile(IFullDataSourceProvider fullDataSourceProvider, IDhLevel level, File file) throws IOException { return new FullDataMetaFile(fullDataSourceProvider, level, file); }
	private FullDataMetaFile(IFullDataSourceProvider fullDataSourceProvider, IDhLevel level, File file) throws IOException, FileNotFoundException
	{
		super(file);
		checkAndLogPhantomDataSourceLifeCycles();
		
		this.fullDataSourceProvider = fullDataSourceProvider;
		this.level = level;
		LodUtil.assertTrue(this.baseMetaData != null);
		this.doesFileExist = true;
		
		this.fullDataSourceLoader = AbstractFullDataSourceLoader.getLoader(this.baseMetaData.dataTypeId, this.baseMetaData.binaryDataFormatVersion);
		if (this.fullDataSourceLoader == null)
		{
			// TODO add a hard coded dictionary of known ID name combos so we can easily see in the log if the ID is valid or if the data was corrupted/old
			throw new IOException("Invalid file: Data type loader not found: " + this.baseMetaData.dataTypeId + "(v" + this.baseMetaData.binaryDataFormatVersion + ")");
		}
		
		this.fullDataSourceClass = this.fullDataSourceLoader.fullDataSourceClass;
		DebugRenderer.register(this);
	}
	
	
	
	//==========//
	// get data //
	//==========//
	
	/**
	 * Try get cached data source. Used for temp impl of re-queueing world gen tasks.
	 * (Read-only access! As writes should always be done async)
	 */
	public IFullDataSource getCachedDataSourceNowOrNull() 
	{ 
		checkAndLogPhantomDataSourceLifeCycles(); 
		return this.cachedFullDataSourceRef.get(); 
	}
	
	private void makeUpdateCompletionStage(CompletableFuture<IFullDataSource> completer, CompletableFuture<IFullDataSource> currentStage)
	{
		currentStage.thenCompose((fullDataSource) -> 
						{
							this.markedNeedUpdate = false;
							return this.fullDataSourceProvider.onDataFileUpdate(fullDataSource, this, this::_updateAndWriteDataSource, this::_applyWriteQueueToFullDataSource);
						})
				.whenComplete((fullDataSource, ex) ->
				{
					if (ex != null && !LodUtil.isInterruptOrReject(ex))
					{
						LOGGER.error("Error updating file [" + this.file + "]: ", ex);
					}
					
					if (fullDataSource != null)
					{
						new DataObjTracker(fullDataSource);
						new DataObjSoftTracker(this, fullDataSource);
					}
					
					//LOGGER.info("Updated file "+this.file);
					if (this.pos.sectionDetailLevel == DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL)
						DebugRenderer.makeParticle(
								new DebugRenderer.BoxParticle(
										new DebugRenderer.Box(this.pos, 64f, 72f, 0.03f, Color.green.darker()),
										0.2, 32f
								)
						);
					
					this.cachedFullDataSourceRef = new SoftReference<>(fullDataSource);
					this.dataSourceLoadFutureRef.set(null);
					completer.complete(fullDataSource);
					
					if (this.markedNeedUpdate)
					{
						// trigger another update
						this.getOrLoadCachedDataSourceAsync();
					}
				});
	}
	
	private void makeLoadCompletionStage(ExecutorService executorService, CompletableFuture<IFullDataSource> completer)
	{
		this.makeUpdateCompletionStage(completer, CompletableFuture.supplyAsync(() -> 
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
				// can happen if there is a missing file or the file was incorrectly formatted, or terminated early
				throw new CompletionException(ex);
			}
			return fullDataSource;
		}, executorService));
	}
	
	private void makeCreateCompletionStage(CompletableFuture<IFullDataSource> completer)
	{
		this.makeUpdateCompletionStage(completer, this.fullDataSourceProvider.onCreateDataFile(this)
				.thenApply((fullDataSource) ->
				{
					this.baseMetaData = this._makeBaseMetaData(fullDataSource);
					return fullDataSource;
				}));
	}
	
	
	
	public CompletableFuture<IFullDataSource> getOrLoadCachedDataSourceAsync()
	{
		checkAndLogPhantomDataSourceLifeCycles();
		
		CompletableFuture<IFullDataSource> dataSourceLoadFuture = this.getCachedDataSourceAsync();
		if (dataSourceLoadFuture != null)
		{
			// return the in-process future
			return dataSourceLoadFuture;
		}
		else
		{
			// there is no cached data, we'll have to load it
			
			dataSourceLoadFuture = new CompletableFuture<>();
			if (!this.dataSourceLoadFutureRef.compareAndSet(null, dataSourceLoadFuture))
			{
				// two threads attempted to start this job at the same time, only use the first future
				dataSourceLoadFuture = this.dataSourceLoadFutureRef.get();
			}
		}
		
		
		
		if (!this.doesFileExist)
		{
			// create a new Meta file and data source
			
			this.makeCreateCompletionStage(dataSourceLoadFuture);
		}
		else
		{
			// load the existing Meta file and data source
			
			if (this.baseMetaData == null)
			{
				throw new IllegalStateException("Meta data not loaded!");
			}
			
			
			ExecutorService executorService = this.fullDataSourceProvider.getIOExecutor();
			if (!executorService.isTerminated())
			{
				// load the data source
				this.makeLoadCompletionStage(executorService, dataSourceLoadFuture);
			}
			else
			{
				// don't load anything if the provider has been shut down	
				this.dataSourceLoadFutureRef.set(null);
				dataSourceLoadFuture.complete(null);
				return dataSourceLoadFuture;
			}
		}
		
		return dataSourceLoadFuture;
	}
	
	
	private BaseMetaData _makeBaseMetaData(IFullDataSource data)
	{
		AbstractFullDataSourceLoader loader = AbstractFullDataSourceLoader.getLoader(data.getClass(), data.getBinaryDataFormatVersion());
		return new BaseMetaData(data.getSectionPos(), -1,
				data.getDataDetailLevel(), data.getWorldGenStep(), (loader == null ? 0 : loader.datatypeId), data.getBinaryDataFormatVersion(), Long.MAX_VALUE);
	}
	
	/** @return returns null if {@link FullDataMetaFile#cachedFullDataSourceRef} is empty and no cached {@link IFullDataSource} exists. */
	private CompletableFuture<IFullDataSource> getCachedDataSourceAsync()
	{
		// this data source is being written to, use the existing future
		CompletableFuture<IFullDataSource> dataSourceLoadFuture = this.dataSourceLoadFutureRef.get();
		if (dataSourceLoadFuture != null)
		{
			return dataSourceLoadFuture;
		}
		
		
		// attempt to get the cached data source
		IFullDataSource cachedFullDataSource = this.cachedFullDataSourceRef.get();
		if (cachedFullDataSource == null)
		{
			// no cached data exists and no one is trying to load it
			return null;
		}
		else
		{
			// cached data exists
			
			boolean dataNeedsUpdating = !this.writeQueueRef.get().queue.isEmpty() || this.markedNeedUpdate;
			if (!dataNeedsUpdating)
			{
				// return the cached data
				return CompletableFuture.completedFuture(cachedFullDataSource);
			}
			else
			{
				// update the data using the write queue, wait for the update to finish, then return the data source  
				
				// Create a new future if one doesn't already exist
				CompletableFuture<IFullDataSource> newFuture = new CompletableFuture<>();
				CompletableFuture<IFullDataSource> oldFuture = AtomicsUtil.compareAndExchange(this.dataSourceLoadFutureRef, null, newFuture);
				
				if (oldFuture != null)
				{
					// An update is already in progress, return its future.
					return oldFuture;
				}
				else
				{
					ExecutorService executorService = this.fullDataSourceProvider.getIOExecutor();
					if (!executorService.isTerminated())
					{
						// write for the update to finish before returning the data source
						this.makeUpdateCompletionStage(newFuture, CompletableFuture.supplyAsync(() -> cachedFullDataSource, executorService));
					}
					else
					{
						// don't update anything if the provider has been shut down
						this.dataSourceLoadFutureRef.set(null);
						newFuture.complete(null);
					}
					
					return newFuture;
				}
			}
		}
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
		checkAndLogPhantomDataSourceLifeCycles();
		
		DhLodPos chunkLodPos = new DhLodPos(LodUtil.CHUNK_DETAIL_LEVEL, chunkAccessor.pos.x, chunkAccessor.pos.z);
		
		LodUtil.assertTrue(this.pos.getSectionBBoxPos().overlapsExactly(chunkLodPos), "Chunk pos " + chunkLodPos + " doesn't exactly overlap with section " + this.pos);
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
		
		this.flushAndSaveAsync();
		//LOGGER.info("write queue length for pos "+this.pos+": " + writeQueue.queue.size());
	}
	
	
	/** Applies any queued {@link ChunkSizedFullDataAccessor} to this metadata's {@link IFullDataSource} and writes the data to file. */
	public CompletableFuture<Void> flushAndSaveAsync()
	{
		checkAndLogPhantomDataSourceLifeCycles();
		boolean isEmpty = this.writeQueueRef.get().queue.isEmpty() && !markedNeedUpdate;
		if (!isEmpty)
		{
			// This will flush the data to disk.
			return this.getOrLoadCachedDataSourceAsync().thenApply((fullDataSource) -> null /* ignore the result, just wait for the load to finish*/ );
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
				LOGGER.warn("Failed to delete data file at " + this.file);
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
				LodUtil.assertTrue(this.fullDataSourceLoader != null, "No loader for " + fullDataSource.getClass() + " (v" + fullDataSource.getBinaryDataFormatVersion() + ")");
				
				this.fullDataSourceClass = fullDataSource.getClass();
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
				LOGGER.error("Failed to save updated data file at " + this.file + " for section " + this.pos, e);
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
		return !isEmpty || !doesFileExist;
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
	
	
	public void markNeedUpdate() { this.markedNeedUpdate = true; }
	
	
	
	//===========//
	// debugging //
	//===========//
	
	/** can be used to log when data sources have been garbage collected */
	public static void checkAndLogPhantomDataSourceLifeCycles()
	{
		DataObjTracker phantomRef = (DataObjTracker) LIFE_CYCLE_DEBUG_QUEUE.poll();
		// wait for the tracker to be garbage collected(?)
		while (phantomRef != null)
		{
			if (LOG_DATA_SOURCE_LIVES)
			{
				LOGGER.info("Full Data at pos: " + phantomRef.pos + " has been freed. [" + LIFE_CYCLE_DEBUG_SET.size() + "] Full Data sources remaining.");
			}
			
			phantomRef.close();
			phantomRef = (DataObjTracker) LIFE_CYCLE_DEBUG_QUEUE.poll();
		}
		
		
		DataObjSoftTracker softRef = (DataObjSoftTracker) SOFT_REF_DEBUG_QUEUE.poll();
		while (softRef != null)
		{
			if (LOG_DATA_SOURCE_LIVES)
			{
				LOGGER.info("Full Data at pos: " + softRef.file.pos + " has been soft released.");
			}
			
			softRef.close();
			softRef = (DataObjSoftTracker) SOFT_REF_DEBUG_QUEUE.poll();
		}
	}
	
	@Override
	public void debugRender(DebugRenderer debugRenderer)
	{
		if (this.pos.sectionDetailLevel > DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL)
		{
			return;
		}
		
		IFullDataSource cached = this.cachedFullDataSourceRef.get();
		if (this.markedNeedUpdate)
		{
			debugRenderer.renderBox(new DebugRenderer.Box(this.pos, 80f, 96f, 0.05f, Color.red));
		}
		
		Color color = Color.black;
		if (cached != null)
		{
			if (cached instanceof CompleteFullDataSource)
			{
				color = Color.GREEN;
			}
			else
			{
				color = Color.YELLOW;
			}
			
		}
		else if (this.dataSourceLoadFutureRef.get() != null)
		{
			color = Color.BLUE;
		}
		else if (this.doesFileExist)
		{
			color = Color.RED;
		}
		
		boolean needsUpdate = !this.writeQueueRef.get().queue.isEmpty() || this.markedNeedUpdate;
		if (needsUpdate)
		{
			color = color.darker().darker();
		}
		
		debugRenderer.renderBox(new DebugRenderer.Box(this.pos, 80f, 96f, 0.05f, color));
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
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
	
	
	
	//================//
	// helper classes //
	//================//
	
	//TODO: use ConcurrentAppendSingleSwapContainer<LodDataSource> instead of below:
	private static class GuardedMultiAppendQueue
	{
		ReentrantReadWriteLock appendLock = new ReentrantReadWriteLock();
		ConcurrentLinkedQueue<ChunkSizedFullDataAccessor> queue = new ConcurrentLinkedQueue<>();
		
	}
	
	/** used to debug data source soft reference garbage collection */
	private static class DataObjTracker extends PhantomReference<IFullDataSource> implements Closeable
	{
		public final DhSectionPos pos;
		
		
		DataObjTracker(IFullDataSource data)
		{
			super(data, LIFE_CYCLE_DEBUG_QUEUE);
			
			if (LOG_DATA_SOURCE_LIVES)
			{
				LOGGER.info("Phantom created on {}! count: {}", data.getSectionPos(), LIFE_CYCLE_DEBUG_SET.size());
			}
			
			LIFE_CYCLE_DEBUG_SET.add(this);
			this.pos = data.getSectionPos();
		}
		
		@Override
		public void close() { LIFE_CYCLE_DEBUG_SET.remove(this); }
		
	}
	
	/** used to debug data source soft reference garbage collection */
	private static class DataObjSoftTracker extends SoftReference<IFullDataSource> implements Closeable
	{
		public final FullDataMetaFile file;
		
		
		DataObjSoftTracker(FullDataMetaFile file, IFullDataSource data)
		{
			super(data, SOFT_REF_DEBUG_QUEUE);
			SOFT_REF_DEBUG_SET.add(this);
			this.file = file;
		}
		
		@Override
		public void close() { SOFT_REF_DEBUG_SET.remove(this); }
		
	}
	
}
