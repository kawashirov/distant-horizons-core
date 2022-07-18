package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.external.methods.events.parameterObjects.DhApiBeforeRenderParam;
import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-7-17
 */
public abstract class DhApiBeforeRenderEvent implements IDhApiEvent<DhApiBeforeRenderParam>
{
	/**
	 * Fired before Distant Horizons finishes rendering fake chunks.
	 *
	 * @return whether the event should be canceled or not.
	 */
	public abstract boolean beforeRender(DhApiBeforeRenderParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean onEvent(DhApiBeforeRenderParam input)
	{
		return beforeRender(input);
	}
	
	@Override
	public final boolean getCancelable() { return true; }
}