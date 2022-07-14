package com.seibel.lod.core.api.external.events.interfaces;

import com.seibel.lod.core.api.external.shared.interfaces.IDhApiLevelWrapper;
import com.seibel.lod.core.api.external.shared.objects.math.DhApiMat4f;

/**
 * @author James Seibel
 * @version 2022-7-13
 */
public interface IDhApiBeforeRenderEvent extends IDhApiEvent
{
	/**
	 * Called before Distant Horizons starts rendering. <Br>
	 * If this method returns false; DH's rendering will be skipped for that frame. <Br> <Br>
	 *
	 * The Matrices received are not passed on to the renderer and can be safely
	 * edited without modifying Minecraft or Distant Horizons' rendering.
	 */
	boolean beforeRender(IDhApiLevelWrapper levelWrapper,
			DhApiMat4f mcModelViewMatrix, DhApiMat4f mcProjectionMatrix, // the matrices received from Minecraft
			DhApiMat4f dhModelViewMatrix, DhApiMat4f dhProjectionMatrix, // the matrices used by Distant Horizons
			float partialTicks);
	
}