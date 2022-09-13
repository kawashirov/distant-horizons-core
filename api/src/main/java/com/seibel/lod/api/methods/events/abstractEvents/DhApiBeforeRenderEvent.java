package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.lod.core.util.math.Mat4f;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class DhApiBeforeRenderEvent
		implements IDhApiEvent<DhApiBeforeRenderEvent.EventParam>
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
	public final boolean fireEvent(EventParam input) { return beforeRender(input); }
	
	@Override
	public final boolean getCancelable() { return true; }
	
	
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