package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.lod.core.util.math.Mat4f;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class DhApiAfterRenderEvent 
		implements IDhApiEvent<DhApiAfterRenderEvent.EventParam>
{
	/** Fired after Distant Horizons finishes rendering fake chunks. */
	public abstract void afterRender(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(EventParam input)
	{
		afterRender(input);
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam extends DhApiRenderParam
	{
		public EventParam(DhApiRenderParam parent)
		{
			super(parent.mcProjectionMatrix, parent.mcModelViewMatrix, parent.dhProjectionMatrix, parent.dhModelViewMatrix, parent.partialTicks);
		}
	}
	
}