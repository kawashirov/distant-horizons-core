package com.seibel.lod.core.api.external.methods.events.abstractEvents;

import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-8-23
 */
public abstract class DhApiLevelSaveEvent implements IDhApiEvent<DhApiLevelSaveEvent.EventParam>
{
	/** Fired after Distant Horizons saves LOD data for the server. */
	public abstract void save(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean onEvent(EventParam input)
	{
		save(input);
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
		
		
		public EventParam(IDhApiLevelWrapper newLevelWrapper)
		{
			this.levelWrapper = newLevelWrapper;
		}
	}
	
}