package com.seibel.lod.api;

import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.core.DependencyInjection.DhApiEventInjector;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.interfaces.dependencyInjection.IDhApiEventInjector;

/**
 * This holds API methods related to version numbers and other unchanging endpoints.
 * This shouldn't change between API versions.
 *
 * @author James Seibel
 * @version 2022-9-16
 */
public class DhApiMain
{
	// only available after core initialization //
	
	/**
	 * <strong>WARNING:</strong> will be null until after DH initializes for the first time. <br><br>
	 *
	 * Use a {@link com.seibel.lod.api.methods.events.abstractEvents.DhApiAfterDhInitEvent DhApiAfterDhInitEvent}
	 * along with the {@link DhApiMain#events ApiCoreInjectors.events} to be notified when this can
	 * be safely used.
	 */
	public static IDhApiConfig configs;
	
	
	// always available //
	
	/** Used to bind/unbind DH Api events. */
	public static final IDhApiEventInjector events = DhApiEventInjector.INSTANCE;
	
	
	/** This version should only be updated when breaking changes are introduced to the DH API */
	public static int getApiMajorVersion() { return ModInfo.API_MAJOR_VERSION; }
	/** This version should be updated whenever new methods are added to the DH API */
	public static int getApiMinorVersion() { return ModInfo.API_MINOR_VERSION; }
	
	/** Returns the mod's version number in the format: Major.Minor.Patch */
	public static String getModVersion() { return ModInfo.VERSION; }
	/** Returns true if the mod is a development version, false if it is a release version. */
	public static boolean getIsDevVersion() { return ModInfo.IS_DEV_BUILD; }
	
	/** Returns the network protocol version. */
	public static int getNetworkProtocolVersion() { return ModInfo.PROTOCOL_VERSION; }
	
}
