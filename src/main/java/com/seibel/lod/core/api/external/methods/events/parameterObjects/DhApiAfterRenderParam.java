package com.seibel.lod.core.api.external.methods.events.parameterObjects;

import com.seibel.lod.core.api.external.items.objects.math.DhApiMat4f;

/**
 * Parameter passed into the After Render event.
 *
 * @author James Seibel
 * @version 7-17-2022
 */
public class DhApiAfterRenderParam extends DhApiRenderParam
{
	
	public DhApiAfterRenderParam(
			DhApiMat4f newMinecraftProjectionMatrix, DhApiMat4f newMinecraftModelViewMatrix,
			DhApiMat4f newDistantHorizonsProjectionMatrix, DhApiMat4f newDistantHorizonsModelViewMatrix,
			float newPartialTicks)
	{
		super(newMinecraftProjectionMatrix, newMinecraftModelViewMatrix,
				newDistantHorizonsProjectionMatrix, newDistantHorizonsModelViewMatrix,
				newPartialTicks);
	}
	
}
