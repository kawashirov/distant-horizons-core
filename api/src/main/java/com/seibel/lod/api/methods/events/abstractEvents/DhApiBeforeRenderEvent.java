package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.lod.api.objects.events.DhApiEventDefinition;
import com.seibel.lod.core.events.ApiEventDefinitionHandler;

/**
 * @author James Seibel
 * @version 2022-11-21
 */
public abstract class DhApiBeforeRenderEvent implements IDhApiEvent<DhApiBeforeRenderEvent.EventParam>
{
	/**
	 * Fired before Distant Horizons renders fake chunks.
	 *
	 * @return whether the event should be canceled or not.
	 */
	public abstract boolean beforeRender(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(EventParam input) { return this.beforeRender(input); }
	
	private static boolean firstTimeSetupComplete = false;
	public DhApiBeforeRenderEvent()
	{
		if (!firstTimeSetupComplete)
		{
			firstTimeSetupComplete = true;
			ApiEventDefinitionHandler.setEventDefinition(DhApiBeforeRenderEvent.class, new DhApiEventDefinition(true, false));
		}
	}
	@Override
	public final DhApiEventDefinition getEventDefinition() { return ApiEventDefinitionHandler.getEventDefinition(DhApiBeforeRenderEvent.class); }
	
	
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