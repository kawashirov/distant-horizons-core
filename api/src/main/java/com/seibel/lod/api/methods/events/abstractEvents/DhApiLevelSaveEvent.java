package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-9-10
 */
public abstract class DhApiLevelSaveEvent 
	implements IDhApiEvent<DhApiLevelSaveEvent.EventParam>
{
	/** Fired after Distant Horizons saves LOD data for the server. */
	public abstract void onLevelSave(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(EventParam input)
	{
		onLevelSave(input);
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam
	{
		/** The saved level. */
		public final IDhApiLevelWrapper levelWrapper;
		
		
		public EventParam(IDhApiLevelWrapper newLevelWrapper) { this.levelWrapper = newLevelWrapper; }
	}
	
}