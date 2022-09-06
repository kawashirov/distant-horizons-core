package com.seibel.lod.api.items.objects.events;

import com.seibel.lod.api.items.objects.math.DhApiMat4f;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.CoreDhApiRenderParam;

/**
 * Parameter passed into Render events.
 *
 * @author James Seibel
 * @version 2022-9-5
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
	
	
	
	public DhApiRenderParam(CoreDhApiRenderParam param)
	{
		this.mcProjectionMatrix = new DhApiMat4f(param.mcProjectionMatrix);
		this.mcModelViewMatrix = new DhApiMat4f(param.mcModelViewMatrix);
		
		this.dhProjectionMatrix = new DhApiMat4f(param.dhProjectionMatrix);
		this.dhModelViewMatrix = new DhApiMat4f(param.dhModelViewMatrix);
		
		this.partialTicks = param.partialTicks;
	}
	
}
