package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.CoreDhApiRenderParam;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiAfterRenderEvent;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class DhApiAfterRenderEvent 
		extends CoreDhApiAfterRenderEvent 
		implements IDhApiEvent<DhApiAfterRenderEvent.EventParam, CoreDhApiAfterRenderEvent.CoreEventParam>
{
	/** Fired after Distant Horizons finishes rendering fake chunks. */
	public abstract void afterRender(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(CoreEventParam input)
	{
		afterRender(new EventParam(input));
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam extends DhApiRenderParam
	{
		public EventParam(CoreEventParam dhApiRenderParam) { super(dhApiRenderParam); }
	}
	
}