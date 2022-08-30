package com.seibel.lod.core.a7.save.structure;

import com.seibel.lod.core.a7.save.io.LevelToFileMatcher;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.EServerFolderNameMode;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.objects.ParsedIp;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

public class ClientOnlySaveStructure extends SaveStructure {
    final File folder;
    private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
    public static final String INVALID_FILE_CHARACTERS_REGEX = "[\\\\/:*?\"<>|]";
    private static String getServerFolderName()
    {
        // parse the current server's IP
        ParsedIp parsedIp = new ParsedIp(MC_CLIENT.getCurrentServerIp());
        String serverIpCleaned = parsedIp.ip.replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
        String serverPortCleaned = parsedIp.port != null ? parsedIp.port.replaceAll(INVALID_FILE_CHARACTERS_REGEX, "") : "";

        // determine the format of the folder name
        EServerFolderNameMode folderNameMode = Config.Client.Multiplayer.serverFolderNameMode.get();
        if (folderNameMode == EServerFolderNameMode.AUTO)
        {
            if (parsedIp.isLan())
            {
                // LAN
                folderNameMode = EServerFolderNameMode.NAME_IP;
            }
            else
            {
                // normal multiplayer
                folderNameMode = EServerFolderNameMode.NAME_IP_PORT;
            }
        }
        String serverName = MC_CLIENT.getCurrentServerName().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
        String serverMcVersion = MC_CLIENT.getCurrentServerVersion().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
        // generate the folder name
        String folderName = "";
        switch (folderNameMode)
        {
            // default and auto shouldn't be used
            // and are just here to make the compiler happy
            default:
            case NAME_ONLY:
                folderName = serverName;
                break;

            case NAME_IP:
                folderName = serverName + ", IP " + serverIpCleaned;
                break;
            case NAME_IP_PORT:
                folderName = serverName + ", IP " + serverIpCleaned + (serverPortCleaned.length() != 0 ? ("-" + serverPortCleaned) : "");
                break;
            case NAME_IP_PORT_MC_VERSION:
                folderName = serverName + ", IP " + serverIpCleaned + (serverPortCleaned.length() != 0 ? ("-" + serverPortCleaned) : "") + ", GameVersion " + serverMcVersion;
                break;
        }
        return folderName;
    }

    LevelToFileMatcher fileMatcher = null;
    final HashMap<ILevelWrapper, File> levelToFileMap = new HashMap<>();

    // Fit for Client_Only environment
    public ClientOnlySaveStructure() {
        folder = new File(MC_CLIENT.getGameDirectory().getPath() +
                File.separatorChar + "Distant_Horizons_server_data" + File.separatorChar + getServerFolderName());
        if (!folder.exists()) folder.mkdirs(); //TODO: Deal with errors
    }

    @Override
    public File tryGetLevelFolder(ILevelWrapper level) {
        return levelToFileMap.computeIfAbsent(level, (l) -> {
            if (Config.Client.Multiplayer.multiDimensionRequiredSimilarity.get() == 0) {
                if (fileMatcher != null) {
                    fileMatcher.close();
                    fileMatcher = null;
                }
                return getLevelFolderWithoutSimilarityMatching(l);
            }
            if (fileMatcher == null || !fileMatcher.isFindingLevel(l)) {
                LOGGER.info("Loading level for world " + l.getDimensionType().getDimensionName());
                fileMatcher = new LevelToFileMatcher(l, folder,
                        (File[]) getMatchingLevelFolders(l).toArray());
            }
            File levelFile = fileMatcher.tryGetLevel();
            if (levelFile != null) {
                fileMatcher.close();
                fileMatcher = null;
            }
            return levelFile;
        });
    }

    private File getLevelFolderWithoutSimilarityMatching(ILevelWrapper level)
    {
        Stream<File> folders = getMatchingLevelFolders(level);
        Optional<File> first = folders.findFirst();
        if (first.isPresent())
        {
            LOGGER.info("Default Sub Dimension set to: [" +  LodUtil.shortenString(first.get().getName(), 8) + "...]");
            return first.get();
        } else { // if no valid sub dimension was found, create a new one
            LOGGER.info("Default Sub Dimension not found. Creating: [" +  level.getDimensionType().getDimensionName() + "]");
            return new File(folder, level.getDimensionType().getDimensionName());
        }
    }

    public Stream<File> getMatchingLevelFolders(@Nullable ILevelWrapper level) {
        File[] folders = folder.listFiles();
        if (folders==null) return Stream.empty();
        return Arrays.stream(folders).filter(
                (f) -> {
                    if (!isValidLevelFolder(f)) return false;
                    return level==null || f.getName().equalsIgnoreCase(level.getDimensionType().getDimensionName());
                }
        ).sorted();
    }

    /** Returns true if the given folder holds valid Lod Dimension data */
    private static boolean isValidLevelFolder(File potentialFolder)
    {
        if (!potentialFolder.isDirectory())
            // it needs to be a folder
            return false;

        File[] files = potentialFolder.listFiles((f) -> f.isDirectory() &&
                (f.getName().equalsIgnoreCase(RENDER_CACHE_FOLDER) || f.getName().equalsIgnoreCase(DATA_FOLDER)));
        // it needs to have folders with specified names in it
        return files != null && files.length != 0;
    }


    @Override
    public File getRenderCacheFolder(ILevelWrapper level) {
        File levelFolder = levelToFileMap.get(level);
        if (levelFolder == null) return null;
        return new File(levelFolder, RENDER_CACHE_FOLDER);
    }

    @Override
    public File getDataFolder(ILevelWrapper level) {
        File levelFolder = levelToFileMap.get(level);
        if (levelFolder == null) return null;
        return new File(levelFolder, DATA_FOLDER);
    }

    @Override
    public void close() {
        fileMatcher.close();
    }

    @Override
    public String toString() {
        return "[ClientOnlySave@"+folder.getName()+"]";
    }
}
