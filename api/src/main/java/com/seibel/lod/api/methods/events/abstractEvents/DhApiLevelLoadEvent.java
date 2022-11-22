package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.api.objects.events.DhApiEventDefinition;
import com.seibel.lod.core.events.ApiEventDefinitionHandler;

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
	
	private static boolean firstTimeSetupComplete = false;
	public DhApiLevelLoadEvent()
	{
		if (!firstTimeSetupComplete)
		{
			firstTimeSetupComplete = true;
			ApiEventDefinitionHandler.setEventDefinition(DhApiLevelLoadEvent.class, new DhApiEventDefinition(false, false));
		}
	}
	@Override
	public final DhApiEventDefinition getEventDefinition() { return ApiEventDefinitionHandler.getEventDefinition(DhApiLevelLoadEvent.class); }
	
	
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