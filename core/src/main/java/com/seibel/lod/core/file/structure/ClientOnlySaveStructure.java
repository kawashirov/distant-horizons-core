package com.seibel.lod.core.file.structure;

import com.google.common.net.PercentEscaper;
import com.seibel.lod.core.file.subDimMatching.SubDimensionLevelMatcher;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.api.enums.config.EServerFolderNameMode;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.util.objects.ParsedIp;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Designed for the Client_Only environment.
 * 
 * @version 12-17-2022
 */
public class ClientOnlySaveStructure extends AbstractSaveStructure
{
	final File folder;
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	public static final String INVALID_FILE_CHARACTERS_REGEX = "[\\\\/:*?\"<>|]";
	
	SubDimensionLevelMatcher fileMatcher = null;
	final HashMap<ILevelWrapper, File> levelToFileMap = new HashMap<>();
	
	
	
	public ClientOnlySaveStructure()
	{
		this.folder = new File(MC_CLIENT.getGameDirectory().getPath() +
				File.separatorChar + "Distant_Horizons_server_data" + File.separatorChar + getServerFolderName());
		
		if (!this.folder.exists())
		{
			if (!this.folder.mkdirs())
			{
				LOGGER.warn("Unable to create folder ["  + this.folder.getPath() + "]");
				//TODO: Deal with errors
			}
		}
	}
	
	
	
	//================//
	// folder methods //
	//================//
	
	@Override
	public File getLevelFolder(ILevelWrapper level)
	{
		return this.levelToFileMap.computeIfAbsent(level, (newLevel) ->
		{
			if (Config.Client.Advanced.Multiplayer.multiDimensionRequiredSimilarity.get() == 0)
			{
				if (this.fileMatcher != null)
				{
					this.fileMatcher.close();
					this.fileMatcher = null;
				}
				return this.getLevelFolderWithoutSimilarityMatching(newLevel);
			}
			
			if (this.fileMatcher == null || !this.fileMatcher.isFindingLevel(newLevel))
			{
				LOGGER.info("Loading level for world " + newLevel.getDimensionType().getDimensionName());
				this.fileMatcher = new SubDimensionLevelMatcher(newLevel, this.folder,
						this.getMatchingLevelFolders(newLevel).toArray(new File[0] /* surprisingly we don't need to create an array of any specific size for this to work */));
			}
			
			File levelFile = this.fileMatcher.tryGetLevel();
			if (levelFile != null)
			{
				this.fileMatcher.close();
				this.fileMatcher = null;
			}
			return levelFile;
		});
	}
	
	private File getLevelFolderWithoutSimilarityMatching(ILevelWrapper level)
	{
		List<File> folders = this.getMatchingLevelFolders(level);
		if (folders.size() > 0 && folders.get(0) == null)
		{
			LOGGER.info("Default Sub Dimension set to: [" + LodUtil.shortenString(folders.get(0).getName(), 8) + "...]");
			return folders.get(0);
		}
		else
		{ 
			// if no valid sub dimension was found, create a new one
			LOGGER.info("Default Sub Dimension not found. Creating: [" + level.getDimensionType().getDimensionName() + "]");
			return new File(this.folder, level.getDimensionType().getDimensionName());
		}
	}
	
	public List<File> getMatchingLevelFolders(@Nullable ILevelWrapper level)
	{
		File[] folders = this.folder.listFiles();
		if (folders == null)
		{
			return new ArrayList<>(0);
		}
		
		Stream<File> fileStream = Arrays.stream(folders).filter(
				(folder) -> 
				{
					if (!isValidLevelFolder(folder))
					{
						return false;
					}
					else
					{
						return level == null || folder.getName().equalsIgnoreCase(level.getDimensionType().getDimensionName());
					}
				}
		).sorted();
		
		return fileStream.collect(Collectors.toList());
	}
	
	@Override
	public File getRenderCacheFolder(ILevelWrapper level)
	{
		File levelFolder = this.levelToFileMap.get(level);
		if (levelFolder == null)
		{
			return null;
		}
		
		return new File(levelFolder, RENDER_CACHE_FOLDER);
	}
	
	@Override
	public File getFullDataFolder(ILevelWrapper level)
	{
		File levelFolder = this.levelToFileMap.get(level);
		if (levelFolder == null)
		{
			return null;
		}
		
		return new File(levelFolder, DATA_FOLDER);
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	/** Returns true if the given folder holds valid Lod Dimension data */
	private static boolean isValidLevelFolder(File potentialFolder)
	{
		if (!potentialFolder.isDirectory())
		{
			// a valid level folder needs to be a folder
			return false;
		}
		
		// filter out any non-DH folders
		File[] files = potentialFolder.listFiles((file) ->
				file.isDirectory() &&
						(file.getName().equalsIgnoreCase(RENDER_CACHE_FOLDER) || file.getName().equalsIgnoreCase(DATA_FOLDER)));
		
		// a valid level folder needs to have DH specific folders in it
		return files != null && files.length != 0;
	}
	
	/** Generated from the server the client is currently connected to. */
	private static String getServerFolderName()
	{
		// parse the current server's IP
		ParsedIp parsedIp = new ParsedIp(MC_CLIENT.getCurrentServerIp());
		String serverIpCleaned = parsedIp.ip.replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
		String serverPortCleaned = parsedIp.port != null ? parsedIp.port.replaceAll(INVALID_FILE_CHARACTERS_REGEX, "") : "";
		
		
		// determine the auto folder name format
		EServerFolderNameMode folderNameMode = Config.Client.Advanced.Multiplayer.serverFolderNameMode.get();
		String serverName = MC_CLIENT.getCurrentServerName().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
		String serverMcVersion = MC_CLIENT.getCurrentServerVersion().replaceAll(INVALID_FILE_CHARACTERS_REGEX, "");
		
		
		// generate the folder name
		String folderName;
		switch (folderNameMode)
		{
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
		
		// PercentEscaper makes the characters all part of the standard alphameric character set
		// This fixes some issues when the server is named something in other languages
		return new PercentEscaper("", true).escape(folderName);
	}
	
	
	
	//==================//
	// override methods //
	//==================//
	
	@Override
	public void close() { this.fileMatcher.close(); }
	
	@Override
	public String toString() { return "[" + this.getClass().getSimpleName() + "@" + this.folder.getName() + "]"; }
	
}
