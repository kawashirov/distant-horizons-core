package com.seibel.lod.core.api.external.events.interfaces;

import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

/**
 * @author James Seibel
 * @version 2022-7-13
 */
public interface IDhApiBeforeRenderEvent extends IDhApiEvent
{
	// TODO should we allow editing the levelWrapper MVM matrix, etc.?
	// TODO make sure to document it either way.
	
	/**
	 * Called before Distant Horizons starts rendering. <Br>
	 * If this methods returns false, DH's rendering will be skipped for that frame.
	 */
	boolean beforeRender(ILevelWrapper levelWrapper, Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks);
	
}