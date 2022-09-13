package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class DhApiAfterDhInitEvent
	implements IDhApiEvent<Void>
{
	/** Fired after Distant Horizons finishes its initial setup on Minecraft startup. */
	public abstract void afterDistantHorizonsInit();
	
}