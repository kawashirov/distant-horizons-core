package com.seibel.lod.api;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.datatype.full.FullDataSource;

/**
 * This holds API methods related to version numbers and other unchanging endpoints.
 * This shouldn't change between API versions.
 *
 * @author James Seibel
 * @version 2022-4-27
 */
public class DhApiMain
{
	/** This version should only be updated when breaking changes are introduced to the DH API */
	public static int getApiMajorVersion()
	{
		return ModInfo.API_MAJOR_VERSION;
	}
	/** This version should be updated whenever new methods are added to the DH API */
	public static int getApiMinorVersion()
	{
		return ModInfo.API_MINOR_VERSION;
	}
	
	/** Returns the mod's version number in the format: Major.Minor.Patch */
	public static String getModVersion()
	{
		return ModInfo.VERSION;
	}
	/** Returns true if the mod is a development version, false if it is a release version. */
	public static boolean getIsDevVersion()
	{
		return ModInfo.IS_DEV_BUILD;
	}
	
	/** Returns the network protocol version. */
	public static int getNetworkProtocolVersion()
	{
		return ModInfo.PROTOCOL_VERSION;
	}
	/** Returns the LOD file version. */
	public static int getLodFileFormatVersion()
	{
		return FullDataSource.LATEST_VERSION;
	}
	
}
