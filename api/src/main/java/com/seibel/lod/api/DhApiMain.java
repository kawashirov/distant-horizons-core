package com.seibel.lod.api;

import com.seibel.lod.api.interfaces.config.IDhApiConfig;
import com.seibel.lod.api.interfaces.override.IDhApiOverrideable;
import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGeneratorOverrideRegister;
import com.seibel.lod.api.methods.data.DhApiTerrainDataRepo;
import com.seibel.lod.api.methods.override.DhApiWorldGeneratorOverrideRegister;
import com.seibel.lod.core.DependencyInjection.DhApiEventInjector;
import com.seibel.lod.core.DependencyInjection.OverrideInjector;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.api.interfaces.data.IDhApiTerrainDataRepo;
import com.seibel.lod.core.interfaces.dependencyInjection.IDhApiEventInjector;
import com.seibel.lod.core.interfaces.dependencyInjection.IOverrideInjector;

/**
 * This is the masthead of the API, almost everything you could want to do
 * can be achieved from here. <br>
 * For example: you can access singletons which handle the config or event binding. <br><br>
 * 
 * <strong>Q:</strong> Why should I use this class instead of just getting the API singleton I need? <br>
 * 
 * <strong>A:</strong> This way there is a lower chance of your code breaking if we change something on our end.
 * For example, if we realized there is a much better way of handling dependency injection we would keep the
 * interface the same so your code doesn't have to change. Whereas if you were directly referencing 
 * the concrete object we replaced, there would be issues.
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
	
	/** Used to bind/unbind DH Api events. */
	public static final IDhApiWorldGeneratorOverrideRegister worldGenOverrides = DhApiWorldGeneratorOverrideRegister.INSTANCE;
	
	/** Used to bind overrides to change DH's core behavior. */
	public static final IOverrideInjector<IDhApiOverrideable> overrides = OverrideInjector.INSTANCE;
	
	/** Used to interact with DH's terrain data. */
	public static final IDhApiTerrainDataRepo terrainRepo = DhApiTerrainDataRepo.INSTANCE;
	
	
	/** This version should only be updated when breaking changes are introduced to the DH API. */
	public static int getApiMajorVersion() { return ModInfo.API_MAJOR_VERSION; }
	/** This version should be updated whenever new methods are added to the DH API. */
	public static int getApiMinorVersion() { return ModInfo.API_MINOR_VERSION; }
	
	/** Returns the mod's version number in the format: Major.Minor.Patch */
	public static String getModVersion() { return ModInfo.VERSION; }
	/** Returns true if the mod is a development version, false if it is a release version. */
	public static boolean getIsDevVersion() { return ModInfo.IS_DEV_BUILD; }
	
	/** Returns the network protocol version. */
	public static int getNetworkProtocolVersion() { return ModInfo.PROTOCOL_VERSION; }
	
}
