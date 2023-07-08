package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;

/**
 * Handles level overrides initiated by servers that
 * support differentiating between different levels.
 */
public interface IKeyedClientLevelManager extends IBindable
{
	/** Called when a client level is wrapped by a ServerEnhancedClientLevel, for integration into mod internals. */
	void setServerKeyedLevel(IServerKeyedClientLevel clientLevel);
	IServerKeyedClientLevel getOverrideWrapper();
	
	/** Returns a new instance of a ServerEnhancedClientLevel. */
	IServerKeyedClientLevel getServerKeyedLevel(ILevelWrapper level, String serverLevelKey);
	
	/** Sets the LOD engine to use the override wrapper, if the server has communication enabled. */
	void setUseOverrideWrapper(boolean useOverrideWrapper);
	boolean getUseOverrideWrapper();
	
}
