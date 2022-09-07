package com.seibel.lod.api.methods.events.abstractEvents;



import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiBeforeDhInitEvent;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class DhApiBeforeDhInitEvent
		extends CoreDhApiBeforeDhInitEvent
		implements IDhApiEvent<Void, Void>
{
	/** Fired before Distant Horizons starts its initial setup on Minecraft startup. */
	public abstract void beforeDistantHorizonsInit();
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(Void ignoredParam)
	{
		beforeDistantHorizonsInit();
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
	
}