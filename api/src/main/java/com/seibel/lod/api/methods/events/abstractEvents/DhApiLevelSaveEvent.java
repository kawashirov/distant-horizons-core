package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiLevelSaveEvent;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class DhApiLevelSaveEvent 
	extends CoreDhApiLevelSaveEvent
	implements IDhApiEvent<DhApiLevelSaveEvent.EventParam, CoreDhApiLevelSaveEvent.CoreEventParam>
{
	/** Fired after Distant Horizons saves LOD data for the server. */
	public abstract void onLevelSave(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(CoreDhApiLevelSaveEvent.CoreEventParam input)
	{
		onLevelSave(new EventParam());
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
		//public final IDhApiLevelWrapper levelWrapper;
		
		// TODO
		//public EventParam(IDhApiLevelWrapper newLevelWrapper) { this.levelWrapper = newLevelWrapper; }
	}
	
}