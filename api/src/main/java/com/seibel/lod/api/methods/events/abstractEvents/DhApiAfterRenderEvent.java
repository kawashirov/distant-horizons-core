package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.lod.api.objects.events.DhApiEventDefinition;
import com.seibel.lod.core.events.ApiEventDefinitionHandler;

/**
 * @author James Seibel
 * @version 2022-11-21
 */
public abstract class DhApiAfterRenderEvent implements IDhApiEvent<DhApiAfterRenderEvent.EventParam>
{
	/** Fired after Distant Horizons finishes rendering fake chunks. */
	public abstract void afterRender(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(EventParam input)
	{
		this.afterRender(input);
		return false;
	}
	
	private static boolean firstTimeSetupComplete = false;
	public DhApiAfterRenderEvent()
	{
		if (!firstTimeSetupComplete)
		{
			firstTimeSetupComplete = true;
			ApiEventDefinitionHandler.setEventDefinition(DhApiAfterRenderEvent.class, new DhApiEventDefinition(false, false));
		}
	}
	@Override
	public final DhApiEventDefinition getEventDefinition() { return ApiEventDefinitionHandler.getEventDefinition(DhApiAfterRenderEvent.class); }
	
	
	//==================//
	// parameter object //
	//==================//
	
	public static class EventParam extends DhApiRenderParam
	{
		public EventParam(DhApiRenderParam parent)
		{
			super(parent.mcProjectionMatrix, parent.mcModelViewMatrix, parent.dhProjectionMatrix, parent.dhModelViewMatrix, parent.partialTicks);
		}
	}
	
}