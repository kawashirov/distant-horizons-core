package com.seibel.distanthorizons.api.methods.events.abstractEvents;

import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.distanthorizons.api.objects.events.DhApiEventDefinition;
import com.seibel.distanthorizons.coreapi.events.ApiEventDefinitionHandler;

/**
 * @author James Seibel
 * @version 2022-11-21
 */
public abstract class DhApiLevelLoadEvent implements IDhApiEvent<DhApiLevelLoadEvent.EventParam>
{
	/** Fired after Distant Horizons loads a new level. */
	public abstract void onLevelLoad(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(EventParam input)
	{
		this.onLevelLoad(input);
		return false;
	}
	
	/**
	 * Note: when creating new events, make sure to bind this definition in {@link ApiEventDefinitionHandler}
	 * Otherwise a bunch of runtime errors will be thrown.
	 */
	public final static DhApiEventDefinition EVENT_DEFINITION = new DhApiEventDefinition(false, false);
	@Override
	public final DhApiEventDefinition getEventDefinition() { return EVENT_DEFINITION; }
	
	
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