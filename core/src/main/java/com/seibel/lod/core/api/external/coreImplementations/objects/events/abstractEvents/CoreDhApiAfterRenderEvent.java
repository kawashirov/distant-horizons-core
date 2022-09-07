package com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents;

import com.seibel.lod.core.api.external.coreImplementations.objects.events.CoreDhApiRenderParam;
import com.seibel.lod.core.api.implementation.interfaces.events.ICoreDhApiEvent;

/**
 * @author James Seibel
 * @version 2022-9-6
 */
public abstract class CoreDhApiAfterRenderEvent implements ICoreDhApiEvent<CoreDhApiAfterRenderEvent.CoreEventParam>
{
	
	//==================//
	// parameter object //
	//==================//
	
	public static class CoreEventParam extends CoreDhApiRenderParam
	{
		public CoreEventParam(CoreDhApiRenderParam dhApiRenderParam)
		{
			super(dhApiRenderParam.mcProjectionMatrix, dhApiRenderParam.mcModelViewMatrix,
					dhApiRenderParam.dhProjectionMatrix, dhApiRenderParam.dhModelViewMatrix,
					dhApiRenderParam.partialTicks);
		}
	}
	
}