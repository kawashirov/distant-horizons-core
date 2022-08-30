package com.seibel.lod.core.api.external.methods.events.sharedParameterObjects;

import com.seibel.lod.core.api.external.items.objects.math.DhApiMat4f;
import com.seibel.lod.core.objects.math.Mat4f;

/**
 * Parameter passed into Render events.
 *
 * @author James Seibel
 * @version 2022-8-21
 */
public class DhApiRenderParam
{
	/** The projection matrix Minecraft is using to render this frame. */
	public final DhApiMat4f mcProjectionMatrix;
	/** The model view matrix Minecraft is using to render this frame. */
	public final DhApiMat4f mcModelViewMatrix;
	
	/** The projection matrix Distant Horizons is using to render this frame. */
	public final DhApiMat4f dhProjectionMatrix;
	/** The model view matrix Distant Horizons is using to render this frame. */
	public final DhApiMat4f dhModelViewMatrix;
	
	/** Indicates how far into this tick the frame is. */
	public final float partialTicks;
	
	
	
	public DhApiRenderParam(
			Mat4f newMcProjectionMatrix, Mat4f newMcModelViewMatrix,
			Mat4f newDhProjectionMatrix, Mat4f newDhModelViewMatrix,
			float newPartialTicks)
	{
		this(newMcProjectionMatrix.createApiObject(), newMcModelViewMatrix.createApiObject(),
				newDhProjectionMatrix.createApiObject(), newDhModelViewMatrix.createApiObject(),
				newPartialTicks);
	}
	
	public DhApiRenderParam(
			DhApiMat4f newMcProjectionMatrix, DhApiMat4f newMcModelViewMatrix,
			DhApiMat4f newDhProjectionMatrix, DhApiMat4f newDhModelViewMatrix,
			float newPartialTicks)
	{
		this.mcProjectionMatrix = newMcProjectionMatrix;
		this.mcModelViewMatrix = newMcModelViewMatrix;
		
		this.dhProjectionMatrix = newDhProjectionMatrix;
		this.dhModelViewMatrix = newDhModelViewMatrix;
		
		this.partialTicks = newPartialTicks;
	}
	
	
}
