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

package com.seibel.distanthorizons.core.util;

import com.seibel.distanthorizons.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.distanthorizons.core.file.renderfile.ILodRenderSourceProvider;
import com.seibel.distanthorizons.core.file.renderfile.RenderSourceFileHandler;
import com.seibel.distanthorizons.core.file.structure.AbstractSaveStructure;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Used to pull in the initial files used by both {@link IFullDataSourceProvider} and {@link ILodRenderSourceProvider}s. */
public class FileScanUtil
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	public static final int MAX_SCAN_DEPTH = 5;
	public static final String LOD_FILE_POSTFIX = ".lod";
	public static final String RENDER_FILE_POSTFIX = ".rlod";
	
	
	
	public static void scanFullDataFiles(AbstractSaveStructure saveStructure, ILevelWrapper levelWrapper, IFullDataSourceProvider dataSourceProvider)
	{
		try (Stream<Path> pathStream = Files.walk(saveStructure.getFullDataFolder(levelWrapper).toPath(), MAX_SCAN_DEPTH))
		{
			List<File> files = pathStream.filter(
					path -> path.toFile().getName().endsWith(LOD_FILE_POSTFIX) && path.toFile().isFile()
			).map(Path::toFile).collect(Collectors.toList());
			LOGGER.info("Found " + files.size() + " full data files for " + levelWrapper + " in " + saveStructure);
			dataSourceProvider.addScannedFile(files);
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to scan and collect full data files for " + levelWrapper + " in " + saveStructure, e);
		}
	}
	
	public static void scanRenderFiles(AbstractSaveStructure saveStructure, ILevelWrapper levelWrapper, ILodRenderSourceProvider renderSourceProvider)
	{
		try (Stream<Path> pathStream = Files.walk(saveStructure.getRenderCacheFolder(levelWrapper).toPath(), MAX_SCAN_DEPTH))
		{
			List<File> files = pathStream.filter(
					path -> path.toFile().getName().endsWith(RENDER_FILE_POSTFIX) && path.toFile().isFile()
			).map(Path::toFile).collect(Collectors.toList());
			LOGGER.info("Found " + files.size() + " render cache files for " + levelWrapper + " in " + saveStructure);
			renderSourceProvider.addScannedFile(files);
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to scan and collect cache files for " + levelWrapper + " in " + saveStructure, e);
		}
	}
	
}
