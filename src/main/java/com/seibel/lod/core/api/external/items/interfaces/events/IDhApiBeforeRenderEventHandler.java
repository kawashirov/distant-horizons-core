package com.seibel.lod.core.api.external.items.interfaces.events;

import com.seibel.lod.core.api.external.items.objects.events.DhApiRenderEvent;

/**
 * @author James Seibel
 * @version 2022-7-14
 */
public interface IDhApiBeforeRenderEventHandler extends IDhApiEventHandler<DhApiRenderEvent>
{
	/**
	 * Called before Distant Horizons starts rendering. <Br>
	 * If this method returns false; DH's rendering will be skipped for that frame. <Br> <Br>
	 *
	 * The Matrices received are not passed on to the renderer and can be safely
	 * edited without modifying Minecraft or Distant Horizons' rendering.
	 */
	boolean beforeRender(DhApiRenderEvent event);
	
}