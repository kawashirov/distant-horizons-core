package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.external.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-8-21
 */
public abstract class DhApiBeforeRenderEvent implements IDhApiEvent<DhApiBeforeRenderEvent.EventParam>
{
	/**
	 * Fired before Distant Horizons renders fake chunks.
	 *
	 * @return whether the event should be canceled or not.
	 */
	public abstract boolean beforeRender(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean onEvent(EventParam input)
	{
		return beforeRender(input);
	}
	
	@Override
	public final boolean getCancelable() { return true; }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam extends DhApiRenderParam
	{
		public EventParam(DhApiRenderParam dhApiRenderParam)
		{
			super(dhApiRenderParam.mcProjectionMatrix, dhApiRenderParam.mcModelViewMatrix,
					dhApiRenderParam.dhProjectionMatrix, dhApiRenderParam.dhModelViewMatrix,
					dhApiRenderParam.partialTicks);
		}
	}
	
}