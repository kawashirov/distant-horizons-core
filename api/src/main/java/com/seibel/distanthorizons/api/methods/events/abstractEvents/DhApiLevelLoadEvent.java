package com.seibel.distanthorizons.api.methods.events.abstractEvents;

import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;

/**
 * Called after Distant Horizons finishes loading a new level. <br>
 * Note: this may be fired before Minecraft has loaded in the player.
 * 
 * @author James Seibel
 * @version 2023-6-23
 */
public abstract class DhApiLevelLoadEvent implements IDhApiEvent<DhApiLevelLoadEvent.EventParam>
{
	/** Fired after Distant Horizons loads a new level. */
	public abstract void onLevelLoad(DhApiEventParam<EventParam> input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final void fireEvent(DhApiEventParam<EventParam> input) { this.onLevelLoad(input); }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam
	{
		/** The newly loaded level. */
		public final IDhApiLevelWrapper levelWrapper;
		
		public EventParam(IDhApiLevelWrapper newLevelWrapper) { this.levelWrapper = newLevelWrapper; }
	}
	
}