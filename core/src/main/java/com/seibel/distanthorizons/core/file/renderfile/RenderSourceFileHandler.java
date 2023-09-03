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

import com.google.common.collect.HashMultimap;
import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.distanthorizons.core.dataObjects.transformers.FullDataToRenderDataTransformer;
import com.seibel.distanthorizons.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.distanthorizons.core.file.structure.AbstractSaveStructure;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import com.seibel.distanthorizons.core.pos.DhLodPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.dataObjects.render.ColumnRenderSource;
import com.seibel.distanthorizons.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.render.renderer.DebugRenderer;
import com.seibel.distanthorizons.core.util.FileScanUtil;
import com.seibel.distanthorizons.core.util.FileUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.util.objects.Reference;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.seibel.distanthorizons.core.util.FileScanUtil.RENDER_FILE_POSTFIX;

public class RenderSourceFileHandler implements ILodRenderSourceProvider
{
	public static final boolean USE_LAZY_LOADING = true;
	public static final boolean ALWAYS_INVALIDATE_CACHE = false;
	
	public static final long RENDER_SOURCE_TYPE_ID = ColumnRenderSource.TYPE_ID;
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private final ThreadPoolExecutor fileHandlerThreadPool;
	private final F3Screen.NestedMessage threadPoolMsg;
	
	private final ConcurrentHashMap<DhSectionPos, File> unloadedFileBySectionPos = new ConcurrentHashMap<>();
	/** contains the loaded {@link RenderMetaDataFile}'s */
	private final ConcurrentHashMap<DhSectionPos, RenderMetaDataFile> metaFileBySectionPos = new ConcurrentHashMap<>();
	
	private final IDhClientLevel level;
	private final File saveDir;
	/** This is the lowest (highest numeric) detail level that this {@link RenderSourceFileHandler} is keeping track of. */
	AtomicInteger topDetailLevel = new AtomicInteger(DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
	private final IFullDataSourceProvider fullDataSourceProvider;
	
	private final WeakHashMap<CompletableFuture<?>, ETaskType> taskTracker = new WeakHashMap<>();
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public RenderSourceFileHandler(IFullDataSourceProvider sourceProvider, IDhClientLevel level, AbstractSaveStructure saveStructure)
	{
		this.fullDataSourceProvider = sourceProvider;
		this.level = level;
		this.saveDir = saveStructure.getRenderCacheFolder(level.getLevelWrapper());
		if (!this.saveDir.exists() && !this.saveDir.mkdirs())
		{
			LOGGER.warn("Unable to create render data folder, file saving may fail.");
		}
		this.fileHandlerThreadPool = ThreadUtil.makeSingleThreadPool("Render Source File Handler [" + this.level.getLevelWrapper().getDimensionType().getDimensionName() + "]");
		
		
		this.threadPoolMsg = new F3Screen.NestedMessage(this::f3Log);
		
		FileScanUtil.scanRenderFiles(saveStructure, level.getLevelWrapper(), this);
	}
	
	/**
	 * Caller must ensure that this method is called only once,
	 * and that the given files are not used before this method is called. <br><br>
	 * 
	 * Used by {@link FileScanUtil#scanRenderFiles(AbstractSaveStructure, ILevelWrapper, ILodRenderSourceProvider)}
	 */
	@Override
	public void addScannedFiles(Collection<File> detectedFiles)
	{
		if (USE_LAZY_LOADING)
		{
			this.lazyAddScannedFile(detectedFiles);
		}
		else
		{
			this.immediateAddScannedFile(detectedFiles);
		}
	}
	private void lazyAddScannedFile(Collection<File> detectedFiles)
	{
		for (File file : detectedFiles)
		{
			if (file == null || !file.exists())
			{
				// can rarely happen if the user rapidly travels between dimensions
				LOGGER.warn("Null or non-existent render file: " + ((file != null) ? file.getPath() : "NULL"));
				continue;
			}
			
			
			try
			{
				DhSectionPos pos = this.decodePositionFromFileName(file);
				if (pos != null)
				{
					this.unloadedFileBySectionPos.put(pos, file);
					this.topDetailLevel.updateAndGet(currentTopDetailLevel -> Math.max(currentTopDetailLevel, pos.sectionDetailLevel));
				}
			}
			catch (Exception e)
			{
				LOGGER.error("Failed to read data meta file at " + file + ": ", e);
				FileUtil.renameCorruptedFile(file);
			}
		}
	}
	private void immediateAddScannedFile(Collection<File> newRenderFiles)
	{
		HashMultimap<DhSectionPos, RenderMetaDataFile> filesByPos = HashMultimap.create();
		
		// Sort files by pos.
		for (File file : newRenderFiles)
		{
			try
			{
				RenderMetaDataFile metaFile = RenderMetaDataFile.createFromExistingFile(this, file);
				filesByPos.put(metaFile.pos, metaFile);
			}
			catch (IOException e)
			{
				LOGGER.error("Failed to read render meta file at [" + file + "]. Error: ", e);
				FileUtil.renameCorruptedFile(file);
			}
		}
		
		// Warn for multiple files with the same pos, and then select the one with the latest timestamp.
		for (DhSectionPos pos : filesByPos.keySet())
		{
			Collection<RenderMetaDataFile> metaFiles = filesByPos.get(pos);
			RenderMetaDataFile fileToUse;
			if (metaFiles.size() > 1)
			{
				//fileToUse = metaFiles.stream().findFirst().orElse(null); // use the first file in the list
				
				// use the file's last modified date
				fileToUse = Collections.max(metaFiles, Comparator.comparingLong(renderMetaDataFile ->
						renderMetaDataFile.file.lastModified()));

//				fileToUse = Collections.max(metaFiles, Comparator.comparingLong(renderMetaDataFile -> 
//						renderMetaDataFile.metaData.dataVersion.get()));
				{
					StringBuilder sb = new StringBuilder();
					sb.append("Multiple files with the same pos: ");
					sb.append(pos);
					sb.append("\n");
					for (RenderMetaDataFile metaFile : metaFiles)
					{
						sb.append("\t");
						sb.append(metaFile.file);
						sb.append("\n");
					}
					sb.append("\tUsing: ");
					sb.append(fileToUse.file);
					sb.append("\n");
					sb.append("(Other files will be renamed by appending \".old\" to their name.)");
					LOGGER.warn(sb.toString());
					
					// Rename all other files with the same pos to .old
					for (RenderMetaDataFile metaFile : metaFiles)
					{
						if (metaFile == fileToUse)
						{
							continue;
						}
						
						File oldFile = new File(metaFile.file + ".old");
						try
						{
							if (!metaFile.file.renameTo(oldFile))
								throw new RuntimeException("Renaming failed");
						}
						catch (Exception e)
						{
							LOGGER.error("Failed to rename file: [" + metaFile.file + "] to [" + oldFile + "]", e);
						}
					}
				}
			}
			else
			{
				fileToUse = metaFiles.iterator().next();
			}
			// Add this file to the list of files.
			this.metaFileBySectionPos.put(pos, fileToUse);
			// increase the lowest detail level if a new lower detail file is found
			this.topDetailLevel.updateAndGet(v -> Math.max(v, pos.sectionDetailLevel));
		}
	}
	
	
	
	//===============//
	// file handling //
	//===============//
	
	/** This call is thread safe and can be called concurrently from multiple threads. */
	@Override
	public CompletableFuture<ColumnRenderSource> readAsync(DhSectionPos pos)
	{
		// don't continue if the handler has been shut down
		if (this.fileHandlerThreadPool.isTerminated())
		{
			return CompletableFuture.completedFuture(null);
		}
		
		
		
		RenderMetaDataFile metaFile = this.getLoadOrMakeFile(pos);
		if (metaFile == null)
		{
			return CompletableFuture.completedFuture(ColumnRenderSource.createEmptyRenderSource(pos));
		}
		
		CompletableFuture<ColumnRenderSource> getDataSourceFuture = metaFile.loadOrGetCachedDataSourceAsync(this.fileHandlerThreadPool, this.level)
				.handle((renderSource, exception) ->
				{
					if (exception != null)
					{
						LOGGER.error("Uncaught error in readAsync for pos: " + pos + ". Error:", exception);
					}
					
					return (renderSource != null) ? renderSource : ColumnRenderSource.createEmptyRenderSource(pos);
				});
		
		synchronized (this.taskTracker)
		{
			this.taskTracker.put(getDataSourceFuture, ETaskType.READ);
		}
		return getDataSourceFuture;
	}
	/** @return null if there was an issue */
	private RenderMetaDataFile getLoadOrMakeFile(DhSectionPos pos)
	{
		RenderMetaDataFile metaFile = this.metaFileBySectionPos.get(pos);
		if (metaFile != null)
		{
			// return the loaded file
			return metaFile;
		}
		
		
		// we don't have a loaded file, for that pos,
		// do we have an unloaded file for that pos?
		File fileToLoad = this.unloadedFileBySectionPos.get(pos);
		if (fileToLoad != null && !fileToLoad.exists())
		{
			fileToLoad = null;
			this.unloadedFileBySectionPos.remove(pos);
		}
		
		
		if (fileToLoad != null)
		{
			// A file exists, but isn't loaded yet.
			
			// Double check locking for loading file, as loading file means also loading the metadata, which
			// while not... Very expensive, is still better to avoid multiple threads doing it, and dumping the
			// duplicated work to the trash. Therefore, eating the overhead of 'synchronized' is worth it.
			synchronized (this)
			{
				// check if another thread already finished loading this file
				metaFile = this.metaFileBySectionPos.get(pos);
				if (metaFile != null)
				{
					return metaFile;
				}
				
				
				// attempt to load the file
				try
				{
					metaFile = RenderMetaDataFile.createFromExistingFile(this, fileToLoad);
					this.topDetailLevel.updateAndGet(currentTopDetailLevel -> Math.max(currentTopDetailLevel, pos.sectionDetailLevel));
					this.metaFileBySectionPos.put(pos, metaFile);
					return metaFile;
				}
				catch (IOException e)
				{
					LOGGER.error("Failed to read render meta file at " + fileToLoad + ": ", e);
					FileUtil.renameCorruptedFile(fileToLoad);
				}
				finally
				{
					this.unloadedFileBySectionPos.remove(pos);
				}
			}
		}
		
		
		// Either no file exists for this position
		// or the existing file was corrupted.
		// Create a new file.
		try
		{
			// createFromExistingOrNewFile() is used instead of createFromExistingFile()
			// due to a rare issue where the file may already exist but isn't in the file list
			metaFile = RenderMetaDataFile.createFromExistingOrNewFile(this, pos);
			
			this.topDetailLevel.updateAndGet(newDetailLevel -> Math.max(newDetailLevel, pos.sectionDetailLevel));
			
			// Compare And Swap to handle a concurrency issue where multiple threads created the same Meta File at the same time
			RenderMetaDataFile metaFileCas = this.metaFileBySectionPos.putIfAbsent(pos, metaFile);
			return (metaFileCas == null) ? metaFile : metaFileCas;
		}
		catch (IOException e)
		{
			LOGGER.error("IOException on creating new data file at "+pos, e);
			return null;
		}
	}
	
	
	
	//=============//
	// data saving //
	//=============//
	
	/**
	 * This call is thread safe and can be called concurrently from multiple threads. <br>
	 * This allows fast writes of new data to the render source, without having to wait for the data to be written to disk.
	 */
	@Override
	public void writeChunkDataToFile(DhSectionPos sectionPos, ChunkSizedFullDataAccessor chunkDataView)
	{
		// convert to the lowest detail level so all detail levels are updated
		this.writeChunkDataToFileRecursively(chunkDataView, DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
		this.fullDataSourceProvider.writeChunkDataToFile(sectionPos, chunkDataView);
	}
	private void writeChunkDataToFileRecursively(ChunkSizedFullDataAccessor chunk, byte sectionDetailLevel)
	{
		DhLodPos boundingPos = chunk.getLodPos();
		DhLodPos minSectionPos = boundingPos.convertToDetailLevel(sectionDetailLevel);
		
		int width = (sectionDetailLevel > boundingPos.detailLevel) ? 1 : boundingPos.getWidthAtDetail(sectionDetailLevel);
		for (int xOffset = 0; xOffset < width; xOffset++)
		{
			for (int zOffset = 0; zOffset < width; zOffset++)
			{
				DhSectionPos sectionPos = new DhSectionPos(sectionDetailLevel, minSectionPos.x + xOffset, minSectionPos.z + zOffset);
				RenderMetaDataFile metaFile = this.metaFileBySectionPos.get(sectionPos); // bypass the getLoadOrMakeFile() since we only want cached files.
				if (metaFile != null)
				{
					metaFile.updateChunkIfSourceExists(chunk, this.level);
				}
			}
		}
		
		if (sectionDetailLevel < this.topDetailLevel.get())
		{
			this.writeChunkDataToFileRecursively(chunk, (byte) (sectionDetailLevel + 1));
		}
	}
	
	
	/** This call is thread safe and can be called concurrently from multiple threads. */
	@Override
	public CompletableFuture<Void> flushAndSaveAsync()
	{
		LOGGER.info("Shutting down " + RenderSourceFileHandler.class.getSimpleName() + "...");
		
		ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
		for (RenderMetaDataFile metaFile : this.metaFileBySectionPos.values())
		{
			futures.add(metaFile.flushAndSaveAsync(this.fileHandlerThreadPool));
		}
		
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
				.whenComplete((voidObj, exception) -> LOGGER.info("Finished saving " + RenderSourceFileHandler.class.getSimpleName()));
	}
	
	
	
	//==========================//
	// meta file cache updating //
	//==========================//
	
	public CompletableFuture<ColumnRenderSource> onRenderFileLoadedAsync(ColumnRenderSource renderSource, RenderMetaDataFile file)
	{
		CompletableFuture<ColumnRenderSource> future = this.updateMetaFileCacheAsync(renderSource, file).handle((voidObj, ex) -> renderSource);
		
		synchronized (this.taskTracker)
		{
			this.taskTracker.put(future, ETaskType.ON_LOADED);
		}
		return future;
	}
	
	public CompletableFuture<Void> onRenderSourceLoadedFromCacheAsync(RenderMetaDataFile file, ColumnRenderSource renderSource) { return this.updateMetaFileCacheAsync(renderSource, file); }
	
	private CompletableFuture<Void> updateMetaFileCacheAsync(ColumnRenderSource renderSource, RenderMetaDataFile renderMetaFile)
	{
		DebugRenderer.BoxWithLife debugBox = new DebugRenderer.BoxWithLife(new DebugRenderer.Box(renderSource.sectionPos, 74f, 86f, 0.1f, Color.red), 1.0, 32f, Color.green.darker());
		
		
		// Skip updating the cache if the data file is already up-to-date
		FullDataMetaFile dataFile = this.fullDataSourceProvider.getFileIfExist(renderMetaFile.pos);
		if (!ALWAYS_INVALIDATE_CACHE && dataFile != null && dataFile.baseMetaData != null && dataFile.baseMetaData.checksum == renderMetaFile.baseMetaData.dataVersion.get()) // TODO can we make it so the version comparisons either both use the checksum or the dataVersion? Comparing checksum and dataVersion is kinda confusing
		{
			LOGGER.debug("Skipping render cache update for " + renderMetaFile.pos);
			renderSource.localVersion.incrementAndGet();
			return CompletableFuture.completedFuture(null);
		}
		
		
		
		final Reference<Integer> renderDataVersionRef = new Reference<>(Integer.MAX_VALUE);
		
		// get the full data source
		CompletableFuture<IFullDataSource> fullDataSourceFuture =
				this.fullDataSourceProvider.readAsync(renderSource.getSectionPos())
					.thenApply((fullDataSource) -> 
					{
						debugBox.box.color = Color.yellow.darker();
						
						// get the metaFile's version
						FullDataMetaFile renderSourceMetaFile = this.fullDataSourceProvider.getFileIfExist(renderMetaFile.pos);
						if (renderSourceMetaFile != null)
						{
							renderDataVersionRef.value = renderSourceMetaFile.baseMetaData.checksum;
						}
						
						return fullDataSource;
					}).exceptionally((ex) ->
					{
						LOGGER.error("Exception when getting data for updateCache()", ex);
						return null;
					});
		
		synchronized (this.taskTracker)
		{
			this.taskTracker.put(fullDataSourceFuture, ETaskType.UPDATE_READ_DATA);
		}
		
		
		
		// convert the full data source into a render source
		CompletableFuture<Void> transformFuture = FullDataToRenderDataTransformer.transformFullDataToRenderSourceAsync(fullDataSourceFuture, this.level)
				.handle((newRenderSource, ex) ->
				{
					if (ex == null)
					{
						try
						{
							renderMetaFile.baseMetaData.dataVersion.set(renderDataVersionRef.value);
							this.mergeRenderSourcesAndWriteToFile(renderSource, renderMetaFile, newRenderSource);
						}
						catch (Throwable e)
						{
							LOGGER.error("Exception when writing render data to file: ", e);
						}
					}
					else if (!LodUtil.isInterruptOrReject(ex))
					{
						LOGGER.error("Exception when updating render file using data source: ", ex);
					}
					
					debugBox.close();
					return null;
				});
		
		synchronized (this.taskTracker)
		{
			this.taskTracker.put(transformFuture, ETaskType.UPDATE);
		}
		
		
		return transformFuture;
	}
	
	
	
	
	private void mergeRenderSourcesAndWriteToFile(ColumnRenderSource currentRenderSource, RenderMetaDataFile metaFile, ColumnRenderSource newRenderSource)
	{
		if (currentRenderSource == null || newRenderSource == null)
		{
			return;
		}
		
		currentRenderSource.updateFromRenderSource(newRenderSource);
		
		metaFile.baseMetaData.dataLevel = currentRenderSource.getDataDetail();
		metaFile.baseMetaData.dataTypeId = RENDER_SOURCE_TYPE_ID;
		metaFile.baseMetaData.binaryDataFormatVersion = currentRenderSource.getRenderDataFormatVersion();
		metaFile.save(currentRenderSource);
	}
	
	
	
	//=========//
	// F3 menu //
	//=========//
	
	/** Returns what should be displayed in Minecraft's F3 debug menu */
	private String[] f3Log()
	{
		ArrayList<String> lines = new ArrayList<>();
		lines.add("Render Source File Handler [" + this.level.getClientLevelWrapper().getDimensionType().getDimensionName() + "]");
		lines.add("  Loaded files: " + this.metaFileBySectionPos.size() + " / " + (this.unloadedFileBySectionPos.size() + this.metaFileBySectionPos.size()));
		lines.add("  Thread pool tasks: " + this.fileHandlerThreadPool.getQueue().size() + " (completed: " + this.fileHandlerThreadPool.getCompletedTaskCount() + ")");
		
		int totalFutures = this.taskTracker.size();
		EnumMap<ETaskType, Integer> tasksOutstanding = new EnumMap<>(ETaskType.class);
		EnumMap<ETaskType, Integer> tasksCompleted = new EnumMap<>(ETaskType.class);
		for (ETaskType type : ETaskType.values())
		{
			tasksOutstanding.put(type, 0);
			tasksCompleted.put(type, 0);
		}
		
		synchronized (this.taskTracker)
		{
			for (Map.Entry<CompletableFuture<?>, ETaskType> entry : this.taskTracker.entrySet())
			{
				if (entry.getKey().isDone())
				{
					tasksCompleted.put(entry.getValue(), tasksCompleted.get(entry.getValue()) + 1);
				}
				else
				{
					tasksOutstanding.put(entry.getValue(), tasksOutstanding.get(entry.getValue()) + 1);
				}
			}
		}
		int totalOutstanding = tasksOutstanding.values().stream().mapToInt(Integer::intValue).sum();
		lines.add("  Futures: " + totalFutures + " (outstanding: " + totalOutstanding + ")");
		for (ETaskType type : ETaskType.values())
		{
			lines.add("    " + type + ": " + tasksOutstanding.get(type) + " / " + (tasksOutstanding.get(type) + tasksCompleted.get(type)));
		}
		return lines.toArray(new String[0]);
	}
	
	
	
	//=====================//
	// clearing / shutdown //
	//=====================//
	
	@Override
	public void close()
	{
		LOGGER.info("Closing " + this.getClass().getSimpleName() + " with [" + this.metaFileBySectionPos.size() + "] files...");
		this.fileHandlerThreadPool.shutdown();
		this.threadPoolMsg.close();
	}
	
	public void deleteRenderCache()
	{
		// delete each file in the cache directory
		File[] renderFiles = this.saveDir.listFiles();
		if (renderFiles != null)
		{
			for (File renderFile : renderFiles)
			{
				if (!renderFile.delete())
				{
					LOGGER.warn("Unable to delete render file: " + renderFile.getPath());
				}
			}
		}
		
		// clear the cached files
		this.metaFileBySectionPos.clear();
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	public File computeRenderFilePath(DhSectionPos pos) { return new File(this.saveDir, pos.serialize() + RENDER_FILE_POSTFIX); }
	
	@Nullable
	public DhSectionPos decodePositionFromFileName(File file)
	{
		String fileName = file.getName();
		if (!fileName.endsWith(RENDER_FILE_POSTFIX)) return null;
		fileName = fileName.substring(0, fileName.length() - RENDER_FILE_POSTFIX.length());
		return DhSectionPos.deserialize(fileName);
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	/**
	 * READ <br>
	 * UPDATE_READ_DATA <br>
	 * UPDATE <br>
	 * ON_LOADED <br>
	 */
	private enum ETaskType 
	{
		READ, UPDATE_READ_DATA, UPDATE, ON_LOADED, 
	}
	
}
