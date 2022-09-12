package com.seibel.lod.core.util;

import com.seibel.lod.core.io.datafile.IDataSourceProvider;
import com.seibel.lod.core.io.renderfile.IRenderSourceProvider;
import com.seibel.lod.core.io.structure.SaveStructure;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Static util class??
public class FileScanUtil {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final int MAX_SCAN_DEPTH = 5;
    public static final String LOD_FILE_POSTFIX = ".lod";
    public static void scanFile(SaveStructure save, ILevelWrapper level,
                                      @Nullable IDataSourceProvider dataSource,
                                      @Nullable IRenderSourceProvider renderSource) {
        if (dataSource != null) {
            try (Stream<Path> pathStream = Files.walk(save.getDataFolder(level).toPath(), MAX_SCAN_DEPTH)) {
                dataSource.addScannedFile(pathStream.filter(
                        path -> path.toFile().getName().endsWith(LOD_FILE_POSTFIX) && path.toFile().isFile()
                    ).map(Path::toFile).collect(Collectors.toList())
                );
            } catch (Exception e) {
                LOGGER.error("Failed to scan and collect data files for {} in {}", level, save, e);
            }
        }
        if (renderSource != null) {
            try (Stream<Path> pathStream = Files.walk(save.getRenderCacheFolder(level).toPath(), MAX_SCAN_DEPTH)) {
                renderSource.addScannedFile(pathStream.filter((
                                path -> path.toFile().getName().endsWith(LOD_FILE_POSTFIX) && path.toFile().isFile())
                        ).map(Path::toFile).collect(Collectors.toList())
                );
            } catch (Exception e) {
                LOGGER.error("Failed to scan and collect data files for {} in {}", level, save, e);
            }
        }
    }
}
