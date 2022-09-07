package com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.events.ICoreDhApiEvent;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class CoreDhApiLevelLoadEvent implements ICoreDhApiEvent<CoreDhApiLevelLoadEvent.CoreEventParam>
{
	
	//==================//
	// parameter object //
	//==================//
	
	public static class CoreEventParam
	{
		/** The newly loaded level. */
		public final ILevelWrapper levelWrapper;
		
		
		public CoreEventParam(ILevelWrapper newLevelWrapper)
		{
			this.levelWrapper = newLevelWrapper;
		}
	}
	
}