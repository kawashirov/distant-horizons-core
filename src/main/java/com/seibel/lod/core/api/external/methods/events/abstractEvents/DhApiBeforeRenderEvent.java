package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.external.items.objects.math.DhApiMat4f;
import com.seibel.lod.core.api.external.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-7-17
 */
public abstract class DhApiBeforeRenderEvent implements IDhApiEvent<DhApiBeforeRenderEvent.EventParam>
{
	/**
	 * Fired before Distant Horizons finishes rendering fake chunks.
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
		public EventParam(
				DhApiMat4f newMinecraftProjectionMatrix, DhApiMat4f newMinecraftModelViewMatrix,
				DhApiMat4f newDistantHorizonsProjectionMatrix, DhApiMat4f newDistantHorizonsModelViewMatrix,
				float newPartialTicks)
		{
			super(newMinecraftProjectionMatrix, newMinecraftModelViewMatrix,
					newDistantHorizonsProjectionMatrix, newDistantHorizonsModelViewMatrix,
					newPartialTicks);
		}
	}
	
}