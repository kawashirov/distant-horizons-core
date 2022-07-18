package com.seibel.lod.core.api.external.methods.events.parameterObjects;

import com.seibel.lod.core.api.external.items.objects.math.DhApiMat4f;

/**
 * Parameter passed into Render events.
 *
 * @author James Seibel
 * @version 7-17-2022
 */
class DhApiRenderParam // default visibility so this class isn't visible outside the package
{
	/** The projection matrix Minecraft is using to render this frame. */
	public final DhApiMat4f MinecraftProjectionMatrix;
	/** The model view matrix Minecraft is using to render this frame. */
	public final DhApiMat4f MinecraftModelViewMatrix;
	
	/** The projection matrix Distant Horizons is using to render this frame. */
	public final DhApiMat4f DistantHorizonsProjectionMatrix;
	/** The model view matrix Distant Horizons is using to render this frame. */
	public final DhApiMat4f DistantHorizonsModelViewMatrix;
	
	/** Indicates how far into this tick the frame is. */
	public final float partialTicks;
	
	
	protected DhApiRenderParam(
			DhApiMat4f newMinecraftProjectionMatrix, DhApiMat4f newMinecraftModelViewMatrix,
			DhApiMat4f newDistantHorizonsProjectionMatrix, DhApiMat4f newDistantHorizonsModelViewMatrix,
			float newPartialTicks)
	{
		this.MinecraftProjectionMatrix = newMinecraftProjectionMatrix;
		this.MinecraftModelViewMatrix = newMinecraftModelViewMatrix;
		
		this.DistantHorizonsProjectionMatrix = newDistantHorizonsProjectionMatrix;
		this.DistantHorizonsModelViewMatrix = newDistantHorizonsModelViewMatrix;
		
		this.partialTicks = newPartialTicks;
	}
	
}
