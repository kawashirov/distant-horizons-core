/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

package com.seibel.lod.core.util;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.EServerFolderNameMode;
import com.seibel.lod.core.enums.config.EVanillaOverdraw;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.objects.ParsedIp;
import com.seibel.lod.core.objects.Pos2D;
import com.seibel.lod.core.objects.DHRegionPos;
import com.seibel.lod.core.objects.opengl.DefaultLodVertexFormats;
import com.seibel.lod.core.objects.opengl.LodVertexFormat;
import com.seibel.lod.core.util.gridList.EdgeDistanceBooleanGrid;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;

/**
 * This class holds methods and constants that may be used in multiple places.
 * 
 * @author James Seibel
 * @version 2022-3-30
 */
public class LodUtil
{
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	/**
	 * Vanilla render distances less than or equal to this will not allow partial
	 * overdraw. The VanillaOverdraw will either be ALWAYS or NEVER.
	 */
	public static final int MINIMUM_RENDER_DISTANCE_FOR_PARTIAL_OVERDRAW = 4;
	
	/**
	 * Vanilla render distances less than or equal to this will cause the overdraw to
	 * run at a smaller fraction of the vanilla render distance.
	 */
	public static final int MINIMUM_RENDER_DISTANCE_FOR_FAR_OVERDRAW = 11;
	
	
	
	
	/** The maximum number of LODs that can be rendered vertically */
	public static final int MAX_NUMBER_OF_VERTICAL_LODS = 32;
	
	/**
	 * alpha used when drawing chunks in debug mode
	 */
	public static final int DEBUG_ALPHA = 255; // 0 - 25;

	public static final int COLOR_DEBUG_BLACK = ColorUtil.rgbToInt(DEBUG_ALPHA, 0, 0, 0);
	public static final int COLOR_DEBUG_WHITE = ColorUtil.rgbToInt(DEBUG_ALPHA, 255, 255, 255);
	public static final int COLOR_INVISIBLE = ColorUtil.rgbToInt(0, 0, 0, 0);
	
	public static final int CEILED_DIMENSION_MAX_RENDER_DISTANCE = 64; // 0 - 255
	
	//FIXME: WE NEED MORE COLORS!!!!
	/**
	 * In order of nearest to farthest: <br>
	 * Red, Orange, Yellow, Green, Cyan, Blue, Magenta, white, gray, black
	 */
	public static final int[] DEBUG_DETAIL_LEVEL_COLORS = new int[] {
			ColorUtil.rgbToInt(255,0,0), ColorUtil.rgbToInt(255,127,0),
			ColorUtil.rgbToInt(255, 255, 0), ColorUtil.rgbToInt(127, 255, 0),
			ColorUtil.rgbToInt(0, 255, 0), ColorUtil.rgbToInt(0, 255, 127),
			ColorUtil.rgbToInt(0, 255, 255), ColorUtil.rgbToInt(0, 127, 255),
			ColorUtil.rgbToInt(0, 0, 255), ColorUtil.rgbToInt(127, 0, 255),
			ColorUtil.rgbToInt(255, 0, 255), ColorUtil.rgbToInt(255, 127, 255),
			ColorUtil.rgbToInt(255, 255, 255)};
	
	
	public static final byte DETAIL_OPTIONS = 10;
	
	/** 512 blocks wide */
	public static final byte REGION_DETAIL_LEVEL = DETAIL_OPTIONS - 1;
	/** 16 blocks wide */
	public static final byte CHUNK_DETAIL_LEVEL = 4;
	/** 1 block wide */
	public static final byte BLOCK_DETAIL_LEVEL = 0;
	
	//public static final short MAX_VERTICAL_DATA = 4;
	
	/**
	 * measured in Blocks <br>
	 * detail level max - 1
	 * 256 blocks
	 */
	public static final short REGION_WIDTH = 1 << REGION_DETAIL_LEVEL;
	/**
	 * measured in Blocks <br>
	 * detail level 4
	 */
	public static final short CHUNK_WIDTH = 16;
	/**
	 * measured in Blocks <br>
	 * detail level 0
	 */
	public static final short BLOCK_WIDTH = 1;
	
	
	/** number of chunks wide */
	public static final int REGION_WIDTH_IN_CHUNKS = REGION_WIDTH / CHUNK_WIDTH;
	
	
	/**
	 * This regex finds any characters that are invalid for use in a windows
	 * (and by extension mac and linux) file path
	 */
	public static final String INVALID_FILE_CHARACTERS_REGEX = "[\\\\/:*?\"<>|]";
	
	/**
	 * 64 MB by default is the maximum amount of memory that
	 * can be directly allocated. <br><br>
	 * <p>
	 * James knows there are commands to change that amount
	 * (specifically "-XX:MaxDirectMemorySize"), but
	 * He has no idea how to access that amount. <br>
	 * So for now this will be the hard limit. <br><br>
	 * <p>
	 * https://stackoverflow.com/questions/50499238/bytebuffer-allocatedirect-and-xmx
	 */
	public static final int MAX_ALLOCATABLE_DIRECT_MEMORY = 64 * 1024 * 1024;
	
	/** the format of data stored in the GPU buffers */
	public static final LodVertexFormat LOD_VERTEX_FORMAT = DefaultLodVertexFormats.POSITION_COLOR_BLOCK_LIGHT_SKY_LIGHT;
	
	
	
	
	
	/**
	 * Gets the ServerWorld for the relevant dimension.
	 * @return null if there is no ServerWorld for the given dimension
	 */
	public static ILevelWrapper getServerWorldFromDimension(IDimensionTypeWrapper newDimension)
	{
		if(!MC_CLIENT.hasSinglePlayerServer())
			return null;
		
		Iterable<ILevelWrapper> worlds = MC_CLIENT.getAllServerWorlds();
		ILevelWrapper returnWorld = null;
		
		for (ILevelWrapper world : worlds)
		{
			if (world.getDimensionType() == newDimension)
			{
				returnWorld = world;
				break;
			}
		}
		
		return returnWorld;
	}
	
	/** Convert a 2D absolute position into a quad tree relative position. */
	public static DHRegionPos convertGenericPosToRegionPos(int x, int z, int detailLevel)
	{
		int relativePosX = Math.floorDiv(x, 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel));
		int relativePosZ = Math.floorDiv(z, 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel));
		
		return new DHRegionPos(relativePosX, relativePosZ);
	}
	
	
	/**
	 * If on single player this will return the name of the user's
	 * world, if in multiplayer it will return the server name, IP,
	 * and game version.
	 */
	public static String getWorldID(ILevelWrapper world)
	{
		if (MC_CLIENT.hasSinglePlayerServer())
		{
			// chop off the dimension ID as it is not needed/wanted
			String dimId = getDimensionIDFromWorld(world);
			
			// get the world name
			int saveIndex = dimId.indexOf("saves") + 1 + "saves".length();
			int slashIndex = dimId.indexOf(File.separatorChar, saveIndex);
			dimId = dimId.substring(saveIndex, slashIndex);
			return dimId;
		}
		else
		{
			return getServerFolderName();
		}
	}
	
	
	/**
	 * If on single player this will return the name of the user's
	 * world and the dimensional save folder, if in multiplayer
	 * it will return the server name, ip, game version, and dimension.<br>
	 * <br>
	 * This can be used to determine where to save files for a given
	 * dimension.
	 */
	@Deprecated // FIXME: There are soooo many duplicated methods doing the same thing everywhere
	public static String getDimensionIDFromWorld(ILevelWrapper world)
	{
		if (MC_CLIENT.hasSinglePlayerServer())
		{
			// this will return the world save location
			// and the dimension folder
			
			ILevelWrapper serverWorld = LodUtil.getServerWorldFromDimension(world.getDimensionType());
			if (serverWorld == null)
				throw new NullPointerException("getDimensionIDFromWorld wasn't able to get the WorldWrapper for the dimension " + world.getDimensionType().getDimensionName());
			
			return serverWorld.getSaveFolder().toString();
		}
		else
		{
			return getServerFolderName() + File.separatorChar + "dim_" + world.getDimensionType().getDimensionName() + File.separatorChar;
		}
	}
	
	/** returns the server name, IP and game version. */
	@Deprecated // FIXME: There are soooo many duplicated methods doing the same thing everywhere
	// Cloned to a7 package's DHFileHandler
	public static String getServerFolderName()
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
		case AUTO:
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
	
	/**
	 * Clamps the given value between the min and max values.
	 * May behave strangely if min > max.
	 */
	public static int clamp(int min, int value, int max)
	{
		return Math.min(max, Math.max(value, min));
	}
	
	/**
	 * Clamps the given value between the min and max values.
	 * May behave strangely if min > max.
	 */
	public static float clamp(float min, float value, float max)
	{
		return Math.min(max, Math.max(value, min));
	}
	
	/**
	 * Clamps the given value between the min and max values.
	 * May behave strangely if min > max.
	 */
	public static double clamp(double min, double value, double max)
	{
		return Math.min(max, Math.max(value, min));
	}

	/**
	 * Like Math.floorDiv, but reverse in that it is a ceilDiv
	 */
	public static int ceilDiv(int value, int divider) {
		return -Math.floorDiv(-value, divider);
	}

	// Why is this not in the standard library?! Come on Java!
	public static byte min(byte a, byte b) {
		return a < b ? a : b;
	}
	public static byte max(byte a, byte b) {
		return a > b ? a : b;
	}

	public static int computeOverdrawOffset() {
		int chunkRenderDist = MC_RENDER.getRenderDistance() + 1;
		EVanillaOverdraw overdraw = Config.Client.Graphics.AdvancedGraphics.vanillaOverdraw.get();
		if (overdraw == EVanillaOverdraw.ALWAYS) return Integer.MAX_VALUE;
		int offset;
		if (overdraw == EVanillaOverdraw.NEVER) {
			offset = Config.Client.Graphics.AdvancedGraphics.overdrawOffset.get();
		} else {
			if (chunkRenderDist < MINIMUM_RENDER_DISTANCE_FOR_FAR_OVERDRAW) {
				offset = 1;
			} else {
				offset = chunkRenderDist / 5;
			}
		}

		if (chunkRenderDist - offset <= 1) {
			return Integer.MAX_VALUE;
		}
		return offset;
	}

	public static EdgeDistanceBooleanGrid readVanillaRenderedChunks() {
		int offset = computeOverdrawOffset();
		if (offset == Integer.MAX_VALUE) return null;
		int renderDist = MC_RENDER.getRenderDistance() + 1;

		Iterator<DHChunkPos> posIter = MC_RENDER.getVanillaRenderedChunks().iterator();

		return new EdgeDistanceBooleanGrid(new Iterator<Pos2D>() {
					@Override
					public boolean hasNext() {
						return posIter.hasNext();
					}

					@Override
					public Pos2D next() {
						DHChunkPos pos = posIter.next();
						return new Pos2D(pos.getX(), pos.getZ());
					}
				},
				MC_CLIENT.getPlayerChunkPos().getX() - renderDist,
				MC_CLIENT.getPlayerChunkPos().getZ() - renderDist,
				renderDist * 2 + 1);
	}
	
	
	/** This is copied from Minecraft's MathHelper class */
	public static float fastInvSqrt(float numb)
	{
		float half = 0.5F * numb;
		int i = Float.floatToIntBits(numb);
		i = 1597463007 - (i >> 1);
		numb = Float.intBitsToFloat(i);
		return numb * (1.5F - half * numb * numb);
	}
	public static float pow2(float x) {return x*x;}
	public static double pow2(double x) {return x*x;}
	public static int pow2(int x) {return x*x;}

	public static long pow2(long x) {return x*x;}
	
	// True if the requested threshold pass, or false otherwise
	// For details, see:
	// https://stackoverflow.com/questions/3571203/what-are-runtime-getruntime-totalmemory-and-freememory
	public static boolean checkRamUsage(double minFreeMemoryPercent, int minFreeMemoryMB) {
		long freeMem = Runtime.getRuntime().freeMemory() + Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
		if (freeMem < minFreeMemoryMB * 1024L * 1024L) return false;
		long maxMem = Runtime.getRuntime().maxMemory();
		if (freeMem/(double)maxMem < minFreeMemoryPercent) return false;
		return true;
	}
	
	public static void checkInterrupts() throws InterruptedException {
		if (Thread.interrupted()) throw new InterruptedException();
	}

	public static void checkInterruptsUnchecked() {
		if (Thread.interrupted()) throw new RuntimeException(new InterruptedException());
	}

	/**
	 * Format a given string with params using log4j's MessageFormat
	 *
	 * @apiNote <b>This 'format' SHOULD ONLY be used for logging and debugging purposes!
	 * Do not use it for deserialization or naming of objects.</b>
	 * @param str The string to format
	 * @param param The parameters to use in the string
	 * @return A message object. Call .toString() to get the string.
	 * @author leetom
	 */
	public static String formatLog(String str, Object... param) {
		return LOGGER.getMessageFactory().newMessage(str, param).getFormattedMessage();
	}
	
	/**
	 * Returns a shortened version of the given string that is no longer than maxLength. <br>
	 * If null returns the empty string.
	 */
	public static String shortenString(String str, int maxLength)
	{
		if (str == null)
		{
			return "";
		}
		else
		{
			return str.substring(0, Math.min(str.length(), maxLength));
		}
	}

	static class AssertFailureException extends RuntimeException {
		public AssertFailureException(String message) {
			super(message);
			debugBreak();
		}
	}
	private static void debugBreak() {
		int a = 0; // Set breakpoint here for auto pause on assert failure
	}

	public static void assertTrue(boolean condition) {
		if (!condition) {
			throw new AssertFailureException("Assertion failed");
		}
	}
	public static void assertTrue(boolean condition, String message) {
		if (!condition) {
			throw new AssertFailureException("Assertion failed:\n " + message);
		}
	}
	public static void assertTrue(boolean condition, String message, Object... args) {
		if (!condition) {
			throw new AssertFailureException("Assertion failed:\n " + formatLog(message, args));
		}
	}
	public static void assertNotReach() {
		throw new AssertFailureException("Assert Not Reach failed");
	}
	public static void assertNotReach(String message) {
		throw new AssertFailureException("Assert Not Reach failed:\n " + message);
	}
	public static void assertNotReach(String message, Object... args) {
		throw new AssertFailureException("Assert Not Reach failed:\n " + formatLog(message, args));
	}
	public static ExecutorService makeSingleThreadPool(String name, int relativePriority) {
		return Executors.newSingleThreadExecutor(new LodThreadFactory(name, Thread.NORM_PRIORITY+relativePriority));
	}
	public static ExecutorService makeSingleThreadPool(Class<?> clazz, int relativePriority) {
		return makeSingleThreadPool(clazz.getSimpleName(), relativePriority);
	}
	public static ExecutorService makeSingleThreadPool(String name) {
		return makeSingleThreadPool(name, 0);
	}
	public static ExecutorService makeSingleThreadPool(Class<?> clazz) {
		return makeSingleThreadPool(clazz.getSimpleName(), 0);
	}
}
