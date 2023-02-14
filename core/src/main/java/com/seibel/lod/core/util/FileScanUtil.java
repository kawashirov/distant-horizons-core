package com.seibel.lod.core.util;

import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.file.renderfile.ILodRenderSourceProvider;
import com.seibel.lod.core.file.structure.AbstractSaveStructure;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Static util class??
public class FileScanUtil
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final int MAX_SCAN_DEPTH = 5;
    public static final String LOD_FILE_POSTFIX = ".lod";
	
    public static void scanFiles(AbstractSaveStructure saveStructure, ILevelWrapper levelWrapper,
			@Nullable IFullDataSourceProvider dataSourceProvider,
			@Nullable ILodRenderSourceProvider renderSourceProvider)
	{
		if (dataSourceProvider != null)
		{
			try (Stream<Path> pathStream = Files.walk(saveStructure.getDataFolder(levelWrapper).toPath(), MAX_SCAN_DEPTH))
			{
				dataSourceProvider.addScannedFile(pathStream.filter(
								path -> path.toFile().getName().endsWith(LOD_FILE_POSTFIX) && path.toFile().isFile()
						).map(Path::toFile).collect(Collectors.toList())
				);
			}
			catch (Exception e)
			{
				LOGGER.error("Failed to scan and collect data files for {} in {}", levelWrapper, saveStructure, e);
			}
		}
		
		if (renderSourceProvider != null)
		{
			try (Stream<Path> pathStream = Files.walk(saveStructure.getRenderCacheFolder(levelWrapper).toPath(), MAX_SCAN_DEPTH))
			{
				renderSourceProvider.addScannedFile(pathStream.filter((
								path -> path.toFile().getName().endsWith(LOD_FILE_POSTFIX) && path.toFile().isFile())
						).map(Path::toFile).collect(Collectors.toList())
				);
			}
			catch (Exception e)
			{
				LOGGER.error("Failed to scan and collect data files for {} in {}", levelWrapper, saveStructure, e);
			}
		}
	}
	
}
