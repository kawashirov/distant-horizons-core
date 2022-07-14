package com.seibel.lod.core.api.external.events.interfaces;

import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

/**
 * @author James Seibel
 * @version 2022-7-13
 */
public interface IDhApiAfterRenderEvent extends IDhApiEvent
{
	/**
	 * Called after Distant Horizons' rendering pipeline finishes.
	 *
	 * @param renderingEnabled Passes in false if DH rendering was disabled or canceled for this frame.
	 */
	void afterRender(ILevelWrapper levelWrapper, Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, boolean renderingEnabled);
	
}