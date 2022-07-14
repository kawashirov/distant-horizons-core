package com.seibel.lod.core.api.external.events.interfaces;

import com.seibel.lod.core.api.external.events.objects.DhApiAfterRenderEvent;

/**
 * @author James Seibel
 * @version 2022-7-13
 */
public interface IDhApiAfterRenderEventHandler extends IDhApiEventHandler<DhApiAfterRenderEvent>
{
	/**
	 * Called after Distant Horizons' rendering pipeline finishes.
	 */
	void afterRender(DhApiAfterRenderEvent event);
	
}