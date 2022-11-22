package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.api.objects.events.DhApiEventDefinition;
import com.seibel.lod.core.events.ApiEventDefinitionHandler;

/**
 * @author James Seibel
 * @version 2022-11-21
 */
public abstract class DhApiAfterDhInitEvent implements IDhApiEvent<Void>
{
	/** Fired after Distant Horizons finishes its initial setup on Minecraft startup. */
	public abstract void afterDistantHorizonsInit();
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(Void ignoredParam)
	{
		this.afterDistantHorizonsInit();
		return false;
	}
	
	private static boolean firstTimeSetupComplete = false;
	public DhApiAfterDhInitEvent()
	{
		if (!firstTimeSetupComplete)
		{
			firstTimeSetupComplete = true;
			ApiEventDefinitionHandler.setEventDefinition(DhApiAfterDhInitEvent.class, new DhApiEventDefinition(false, true));
		}
	}
	@Override
	public final DhApiEventDefinition getEventDefinition() { return ApiEventDefinitionHandler.getEventDefinition(DhApiAfterDhInitEvent.class); }
	
}