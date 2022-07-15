package com.seibel.lod.core.api.external.items.interfaces.events;

import com.seibel.lod.core.api.external.items.objects.events.DhApiAfterRenderEvent;

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