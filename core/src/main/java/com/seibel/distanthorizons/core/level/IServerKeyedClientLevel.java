package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;

/** Enhances a {@link IClientLevelWrapper} with server provided level information. */
public interface IServerKeyedClientLevel extends IClientLevelWrapper
{
	/** Returns the level key, which is used to select the correct folder on the client. */
	String getServerLevelKey();
	
}
