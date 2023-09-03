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

package com.seibel.distanthorizons.core.file.renderfile;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.file.metaData.AbstractMetaDataContainerFile;
import com.seibel.distanthorizons.core.file.metaData.BaseMetaData;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.renderer.DebugRenderer;
import com.seibel.distanthorizons.core.pos.DhLodPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.dataObjects.render.ColumnRenderLoader;
import com.seibel.distanthorizons.core.dataObjects.render.ColumnRenderSource;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.render.renderer.IDebugRenderable;
import com.seibel.distanthorizons.core.util.AtomicsUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataInputStream;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class RenderMetaDataFile extends AbstractMetaDataContainerFile implements IDebugRenderable
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	/**
	 * Can be cleared if the garbage collector determines there isn't enough space. <br><br>
	 *
	 * When clearing, don't set to null, instead create a SoftReference containing null.
	 * This will make null checks simpler.
	 */
	private SoftReference<ColumnRenderSource> cachedRenderDataSource = new SoftReference<>(null);
	private final AtomicReference<CompletableFuture<ColumnRenderSource>> renderSourceLoadFutureRef = new AtomicReference<>(null);
	
	private final RenderSourceFileHandler fileHandler;
	private boolean doesFileExist;
	
	private static final class CacheQueryResult
	{
		public final CompletableFuture<ColumnRenderSource> future;
		public final boolean needsLoad;
		public CacheQueryResult(CompletableFuture<ColumnRenderSource> future, boolean needsLoad)
		{
			this.future = future;
			this.needsLoad = needsLoad;
		}
		
	}
	
	@Override
	public void debugRender(DebugRenderer r)
	{
		ColumnRenderSource cached = cachedRenderDataSource.get();
		Color c = Color.black;
		if (cached != null)
		{
			c = Color.GREEN;
		}
		else if (renderSourceLoadFutureRef.get() != null)
		{
			c = Color.BLUE;
		}
		else if (doesFileExist)
		{
			c = Color.RED;
		}
		r.renderBox(new DebugRenderer.Box(pos, 64, 72, 0.05f, c));
	}
	
	//=============//
	// constructor //
	//=============//
	
	/** 
	 * Can be used instead of {@link RenderMetaDataFile#createFromExistingFile} or {@link RenderMetaDataFile#createNewFileForPos}, 
	 * if we are uncertain whether a file exists or not.
	 */
	public static RenderMetaDataFile createFromExistingOrNewFile(RenderSourceFileHandler fileHandler, DhSectionPos pos) throws IOException
	{
		File file = fileHandler.computeRenderFilePath(pos);
		if (file.exists())
		{
			return createFromExistingFile(fileHandler, file);
		}
		else
		{
			return createNewFileForPos(fileHandler, pos);
		}
	}
	
	
	/**
	 * NOTE: should only be used if there is NOT an existing file.
	 *
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
		DebugRenderer.register(this);
	}
	
	/**
	 * NOTE: should only be used if there IS an existing file.
	 *
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
		
		DebugRenderer.register(this);
	}
	
	public void updateChunkIfSourceExists(ChunkSizedFullDataAccessor chunkDataView, IDhClientLevel level)
	{
		DhLodPos chunkPos = chunkDataView.getLodPos();
		LodUtil.assertTrue(this.pos.getSectionBBoxPos().overlapsExactly(chunkPos), "Chunk pos " + chunkPos + " doesn't overlap with section " + this.pos);
		
		// update the render source if one exists
		CompletableFuture<ColumnRenderSource> renderSourceLoadFuture = getCachedDataSourceAsync(false);
		if (renderSourceLoadFuture == null) return;
		
		renderSourceLoadFuture.thenAccept((renderSource) -> {
			boolean dataUpdated = renderSource.updateWithChunkData(chunkDataView, level);
			
			//if (pos.sectionDetailLevel == DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL+5) {
			float offset = new Random(System.nanoTime() ^ Thread.currentThread().getId()).nextFloat() * 16f;
			Color debugColor = dataUpdated ? Color.blue : Color.red;
			DebugRenderer.makeParticle(
					new DebugRenderer.BoxParticle(
							new DebugRenderer.Box(chunkDataView.getLodPos(), 32f, 64f + offset, 0.07f, debugColor),
							2.0, 16f
					)
			);
			//}
		});
	}
	
	public CompletableFuture<Void> flushAndSaveAsync(ExecutorService renderCacheThread)
	{
		if (!this.file.exists())
		{
			return CompletableFuture.completedFuture(null); // No need to save if the file doesn't exist.
		}
		// FIXME: TODO: Change doTriggerUpdate to true. Currently is false cause a dead future making render handler hang,
		//   and that render cache aren't actually used really yet due to missing versioning atm. So disabling for now.
		CompletableFuture<ColumnRenderSource> source = getCachedDataSourceAsync(false);
		if (source == null)
		{
			return CompletableFuture.completedFuture(null); // If there is no cached data, there is no need to save.
		}
		return source.handle((columnRenderSource, ex) -> {
			if (ex != null && !LodUtil.isInterruptOrReject(ex))
				LOGGER.error("Failed to load render source for " + this.pos + " for flush and saving", ex);
			return null;
		}); // Otherwise, wait for the data to be read (which also flushes changes to the file).
	}
	private CacheQueryResult getOrStartCachedDataSourceAsync()
	{
		// use the existing future
		CompletableFuture<ColumnRenderSource> renderSourceLoadFuture = getCachedDataSourceAsync(true);
		if (renderSourceLoadFuture == null)
		{
			// Make a new future, and CAS it, or return the existing future
			CompletableFuture<ColumnRenderSource> newFuture = new CompletableFuture<>();
			CompletableFuture<ColumnRenderSource> cas = AtomicsUtil.compareAndExchange(renderSourceLoadFutureRef, null, newFuture);
			if (cas == null)
			{
				return new CacheQueryResult(newFuture, true);
			}
			else
			{
				return new CacheQueryResult(cas, false);
			}
		}
		else
		{
			return new CacheQueryResult(renderSourceLoadFuture, false);
		}
	}
	
	@Nullable
	private CompletableFuture<ColumnRenderSource> getCachedDataSourceAsync(boolean doTriggerUpdate)
	{
		// use the existing future
		CompletableFuture<ColumnRenderSource> renderSourceLoadFuture = this.renderSourceLoadFutureRef.get();
		if (renderSourceLoadFuture != null)
		{
			return renderSourceLoadFuture;
		}
		
		
		// attempt to get the cached render source
		ColumnRenderSource cachedRenderDataSource = this.cachedRenderDataSource.get();
		if (cachedRenderDataSource == null)
		{
			return null;
		}
		else
		{
			if (!doTriggerUpdate) return CompletableFuture.completedFuture(cachedRenderDataSource);
			
			// Make a new future, and CAS it, or return the existing future
			CompletableFuture<ColumnRenderSource> newFuture = new CompletableFuture<>();
			CompletableFuture<ColumnRenderSource> cas = AtomicsUtil.compareAndExchange(renderSourceLoadFutureRef, null, newFuture);
			if (cas == null)
			{
				this.fileHandler.onRenderSourceLoadedFromCacheAsync(this, cachedRenderDataSource)
						// wait for the handler to finish before returning the renderSource
						.handle((voidObj, ex) -> {
							if (ex != null)
							{
								LOGGER.error("Error while updating render source from cache", ex);
							}
							newFuture.complete(cachedRenderDataSource);
							renderSourceLoadFutureRef.set(null);
							return null;
						});
				return newFuture;
			}
			else
			{
				return cas;
			}
		}
	}
	
	public CompletableFuture<ColumnRenderSource> loadOrGetCachedDataSourceAsync(Executor fileReaderThreads, IDhLevel level)
	{
		CacheQueryResult getCachedFuture = this.getOrStartCachedDataSourceAsync();
		if (!getCachedFuture.needsLoad)
		{
			return getCachedFuture.future;
		}
		
		CompletableFuture<ColumnRenderSource> future = getCachedFuture.future;
		// load or create the render source
		if (!this.doesFileExist)
		{
			// create a new Meta file and render source
			
			
			// create the new render source
			byte dataDetailLevel = (byte) (this.pos.sectionDetailLevel - ColumnRenderSource.SECTION_SIZE_OFFSET);
			int verticalSize = Config.Client.Advanced.Graphics.Quality.verticalQuality.get().calculateMaxVerticalData(dataDetailLevel);
			ColumnRenderSource newColumnRenderSource = new ColumnRenderSource(this.pos, verticalSize, level.getMinY());
			
			this.baseMetaData = new BaseMetaData(
					newColumnRenderSource.getSectionPos(), -1, newColumnRenderSource.getDataDetail(), 
					newColumnRenderSource.worldGenStep, RenderSourceFileHandler.RENDER_SOURCE_TYPE_ID, 
					newColumnRenderSource.getRenderDataFormatVersion(), Long.MAX_VALUE);
			
			this.fileHandler.onRenderFileLoadedAsync(newColumnRenderSource, this)
					.whenComplete((renderSource, ex) ->
					{
						if (ex != null)
						{
							if (!LodUtil.isInterruptOrReject(ex))
							{
								LOGGER.error("Uncaught error on RenderMetaDataFile ColumnRenderSource creation for file: ["+this.file+"]. Error: ", ex);
							}
							
							// set the render source to null to prevent instances where a corrupt or incomplete render source was returned
							renderSource = null;
						}
						
						this.renderSourceLoadFutureRef.set(null);
						
						this.cachedRenderDataSource = new SoftReference<>(renderSource);
						future.complete(renderSource);
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
						return renderSource;
					}, fileReaderThreads)
					// TODO: Check for file version and only update if needed.
					.thenCompose((renderSource) -> this.fileHandler.onRenderFileLoadedAsync(renderSource, this))
					.whenComplete((renderSource, ex) ->
					{
						if (ex != null)
						{
							if (!LodUtil.isInterruptOrReject(ex))
								LOGGER.error("Error loading file {}: ", this.file, ex);
							cachedRenderDataSource = new SoftReference<>(null);
							renderSourceLoadFutureRef.set(null);
							future.complete(null);
						}
						else
						{
							cachedRenderDataSource = new SoftReference<>(renderSource);
							renderSourceLoadFutureRef.set(null);
							future.complete(renderSource);
						}
					});
		}
		return future;
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
