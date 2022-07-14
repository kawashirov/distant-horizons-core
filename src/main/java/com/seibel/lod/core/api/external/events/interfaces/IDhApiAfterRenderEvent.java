package com.seibel.lod.core.api.external.events.interfaces;

import com.seibel.lod.core.api.external.shared.interfaces.IDhApiLevelWrapper;
import com.seibel.lod.core.api.external.shared.objects.math.DhApiMat4f;

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
	void afterRender(IDhApiLevelWrapper levelWrapper,
			DhApiMat4f mcModelViewMatrix, DhApiMat4f mcProjectionMatrix, // the matrices received from Minecraft
			DhApiMat4f dhModelViewMatrix, DhApiMat4f dhProjectionMatrix, // the matrices used by Distant Horizons
			float partialTicks, boolean renderingEnabled);
	
}