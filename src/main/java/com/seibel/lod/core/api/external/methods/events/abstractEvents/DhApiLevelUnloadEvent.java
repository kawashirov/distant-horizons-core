package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-7-17
 */
public abstract class DhApiLevelUnloadEvent implements IDhApiEvent<DhApiLevelUnloadEvent.EventParam>
{
	/** Fired after Distant Horizons loads a new client level. */
	public abstract void levelUnload();
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean onEvent(EventParam ignoredParam)
	{
		levelUnload();
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
		
		
		public EventParam(IDhApiLevelWrapper newLevelWrapper)
		{
			this.levelWrapper = newLevelWrapper;
		}
	}
	
}