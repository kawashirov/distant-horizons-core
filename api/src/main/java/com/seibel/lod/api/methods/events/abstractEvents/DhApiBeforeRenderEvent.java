package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.CoreDhApiRenderParam;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiAfterRenderEvent;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiBeforeRenderEvent;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class DhApiBeforeRenderEvent
		extends CoreDhApiBeforeRenderEvent
		implements IDhApiEvent<DhApiBeforeRenderEvent.EventParam, CoreDhApiBeforeRenderEvent.CoreEventParam>
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
	public final boolean fireEvent(CoreEventParam input) { return beforeRender(new EventParam(input)); }
	
	@Override
	public final boolean getCancelable() { return true; }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam extends DhApiRenderParam
	{
		public EventParam(CoreEventParam dhApiRenderParam) { super(dhApiRenderParam); }
	}
	
}