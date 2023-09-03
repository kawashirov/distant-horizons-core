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

import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IFullDataSourceProvider extends AutoCloseable
{
	void addScannedFile(Collection<File> detectedFiles);
	
	CompletableFuture<IFullDataSource> readAsync(DhSectionPos pos);
	void writeChunkDataToFile(DhSectionPos sectionPos, ChunkSizedFullDataAccessor chunkData);
	CompletableFuture<Void> flushAndSave();
	CompletableFuture<Void> flushAndSave(DhSectionPos sectionPos);
	
	void addOnUpdatedListener(Consumer<IFullDataSource> listener);
	
	//long getCacheVersion(DhSectionPos sectionPos);
	//boolean isCacheVersionValid(DhSectionPos sectionPos, long cacheVersion);
	
	CompletableFuture<IFullDataSource> onCreateDataFile(FullDataMetaFile file);
	CompletableFuture<IFullDataSource> onDataFileUpdate(IFullDataSource source, FullDataMetaFile file, Consumer<IFullDataSource> onUpdated, Function<IFullDataSource, Boolean> updater);
	File computeDataFilePath(DhSectionPos pos);
	ExecutorService getIOExecutor();

	@Nullable
    FullDataMetaFile getFileIfExist(DhSectionPos pos);
}
