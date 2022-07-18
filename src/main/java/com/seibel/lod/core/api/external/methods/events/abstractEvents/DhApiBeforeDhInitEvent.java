package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.external.methods.events.parameterObjects.DhApiAfterRenderParam;
import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-7-17
 */
public abstract class DhApiBeforeDhInitEvent implements IDhApiEvent<Void>
{
	/** Fired before Distant Horizons starts its initial setup on Minecraft startup. */
	public abstract void beforeDistantHorizonsInit();
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean onEvent(Void ignoredParam)
	{
		beforeDistantHorizonsInit();
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
}