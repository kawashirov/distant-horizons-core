package com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.events.ICoreDhApiEvent;

/**
 * Only used for unit testing
 * 
 * @author James Seibel
 * @version 2022-9-11
 */
public abstract class CoreDhApiTestEvent implements ICoreDhApiEvent<Boolean>
{
	public abstract boolean getTestValue();
	
}