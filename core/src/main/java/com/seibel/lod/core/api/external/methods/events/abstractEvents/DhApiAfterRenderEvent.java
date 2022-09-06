package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.external.coreImplementations.objects.events.CoreDhApiRenderParam;
import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-8-21
 */
public abstract class DhApiAfterRenderEvent implements IDhApiEvent<DhApiAfterRenderEvent.EventParam>
{
	/** Fired after Distant Horizons finishes rendering fake chunks. */
	public abstract void afterRender(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean onEvent(EventParam input)
	{
		afterRender(input);
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam extends CoreDhApiRenderParam
	{
		public EventParam(CoreDhApiRenderParam dhApiRenderParam)
		{
			super(dhApiRenderParam.mcProjectionMatrix, dhApiRenderParam.mcModelViewMatrix,
					dhApiRenderParam.dhProjectionMatrix, dhApiRenderParam.dhModelViewMatrix,
					dhApiRenderParam.partialTicks);
		}
	}
	
}