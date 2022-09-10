package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.items.objects.wrappers.DhApiLevelWrapper;
import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiLevelUnloadEvent;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

/**
 * @author James Seibel
 * @version 2022-9-10
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
		onLevelUnload(new EventParam(input.levelWrapper));
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
		public final IDhApiLevelWrapper levelWrapper;
		
		
		public EventParam(ILevelWrapper newLevelWrapper) { this.levelWrapper = new DhApiLevelWrapper(newLevelWrapper); }
	}
	
}