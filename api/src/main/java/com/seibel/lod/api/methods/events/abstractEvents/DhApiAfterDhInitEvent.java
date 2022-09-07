package com.seibel.lod.api.methods.events.abstractEvents;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiAfterDhInitEvent;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class DhApiAfterDhInitEvent
	extends CoreDhApiAfterDhInitEvent
	implements IDhApiEvent<Void, Void>
{
	/** Fired after Distant Horizons finishes its initial setup on Minecraft startup. */
	public abstract void afterDistantHorizonsInit();
	
}