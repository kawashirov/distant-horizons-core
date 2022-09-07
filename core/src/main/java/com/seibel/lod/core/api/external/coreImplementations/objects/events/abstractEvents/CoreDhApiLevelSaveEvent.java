package com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents;

import com.seibel.lod.core.api.implementation.interfaces.events.ICoreDhApiEvent;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class CoreDhApiLevelSaveEvent implements ICoreDhApiEvent<CoreDhApiLevelSaveEvent.CoreEventParam>
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