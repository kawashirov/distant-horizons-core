package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-7-17
 */
public abstract class DhApiAfterDhInitEvent implements IDhApiEvent<Void>
{
	/** Fired after Distant Horizons finishes its initial setup on Minecraft startup. */
	public abstract void afterDistantHorizonsInit();
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean onEvent(Void ignoredParam)
	{
		afterDistantHorizonsInit();
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
}