package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-7-17
 */
public abstract class DhApiServerSaveEvent implements IDhApiEvent<Void>
{
	/** Fired after Distant Horizons saves LOD data for the server. */
	public abstract void save();
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean onEvent(Void ignoredParam)
	{
		save();
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
	
}