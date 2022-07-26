package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.external.items.objects.math.DhApiMat4f;
import com.seibel.lod.core.api.external.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-7-17
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