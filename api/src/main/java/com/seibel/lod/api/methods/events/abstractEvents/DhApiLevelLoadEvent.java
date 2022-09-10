package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.items.objects.wrappers.DhApiLevelWrapper;
import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiLevelLoadEvent;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

/**
 * @author James Seibel
 * @version 2022-9-10
 */
public abstract class DhApiLevelLoadEvent 
	extends CoreDhApiLevelLoadEvent
	implements IDhApiEvent<DhApiLevelLoadEvent.EventParam, CoreDhApiLevelLoadEvent.CoreEventParam>
{
	/** Fired after Distant Horizons loads a new level. */
	public abstract void onLevelLoad(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(CoreEventParam input)
	{
		onLevelLoad(new EventParam(input.levelWrapper));
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam
	{
		/** The newly loaded level. */
		public final IDhApiLevelWrapper levelWrapper;
		
		
		public EventParam(ILevelWrapper newLevelWrapper) { this.levelWrapper = new DhApiLevelWrapper(newLevelWrapper); }
	}
	
}