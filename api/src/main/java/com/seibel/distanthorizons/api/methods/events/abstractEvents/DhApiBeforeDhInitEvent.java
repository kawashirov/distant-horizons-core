package com.seibel.distanthorizons.api.methods.events.abstractEvents;



import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.distanthorizons.api.objects.events.DhApiEventDefinition;
import com.seibel.distanthorizons.coreapi.events.ApiEventDefinitionHandler;

/**
 * @author James Seibel
 * @version 2022-11-21
 */
public abstract class DhApiBeforeDhInitEvent implements IDhApiEvent<Void>
{
	/** Fired before Distant Horizons starts its initial setup on Minecraft startup. */
	public abstract void beforeDistantHorizonsInit();
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(Void ignoredParam)
	{
		this.beforeDistantHorizonsInit();
		return false;
	}
	
	/**
	 * Note: when creating new events, make sure to bind this definition in {@link ApiEventDefinitionHandler}
	 * Otherwise a bunch of runtime errors will be thrown.
	 */
	public final static DhApiEventDefinition EVENT_DEFINITION = new DhApiEventDefinition(false, true);
	@Override
	public final DhApiEventDefinition getEventDefinition() { return EVENT_DEFINITION; }
	
}