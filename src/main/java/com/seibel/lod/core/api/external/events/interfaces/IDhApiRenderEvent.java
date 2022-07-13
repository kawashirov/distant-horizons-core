package com.seibel.lod.core.api.external.events.interfaces;

import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

/**
 * Event handler for when Distant Horizons
 * starts and finishes rendering
 *
 * @author James Seibel
 * @version 2022-7-13
 */
public interface IDhApiRenderEvent extends IDhApiEvent
{
	// TODO should we allow editing the levelWrapper MVM matrix, etc.?
	// TODO make sure to document it either way.
	
	/**
	 * Called before Distant Horizons starts rendering. <Br>
	 * If this methods returns false DH's rendering will be skipped for that frame.
	 */
	public boolean beforeRender(ILevelWrapper levelWrapper, Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks);
	
	/**
	 * Called after Distant Horizons finishes rendering.
	 * If DH has rendering disabled or beforeRender //TODO Link
	 * canceled the rendering this event will not fire.
	 */
	public void afterRender(ILevelWrapper levelWrapper, Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks);
	
}