package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiLevelUnloadEvent;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class DhApiLevelUnloadEvent 
	extends CoreDhApiLevelUnloadEvent
	implements IDhApiEvent<DhApiLevelUnloadEvent.EventParam, CoreDhApiLevelUnloadEvent.CoreEventParam>
{
	/** Fired before Distant Horizons unloads a level. */
	public abstract void onLevelUnload(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(CoreDhApiLevelUnloadEvent.CoreEventParam input)
	{
		onLevelUnload(new EventParam());
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam
	{
		/** The recently unloaded level. */
//		public final IDhApiLevelWrapper levelWrapper;
		
		
		// TODO
//		public EventParam(IDhApiLevelWrapper newLevelWrapper) { this.levelWrapper = newLevelWrapper; }
	}
	
}