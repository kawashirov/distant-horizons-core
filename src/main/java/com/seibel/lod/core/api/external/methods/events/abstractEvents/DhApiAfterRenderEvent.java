package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.external.methods.events.parameterObjects.DhApiAfterRenderParam;
import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-7-17
 */
public abstract class DhApiAfterRenderEvent implements IDhApiEvent<DhApiAfterRenderParam>
{
	/** Fired after Distant Horizons finishes rendering fake chunks. */
	public abstract void afterRender(DhApiAfterRenderParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean onEvent(DhApiAfterRenderParam input)
	{
		afterRender(input);
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
}