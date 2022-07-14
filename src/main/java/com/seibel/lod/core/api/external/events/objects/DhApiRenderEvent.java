package com.seibel.lod.core.api.external.events.objects;

import com.seibel.lod.core.api.external.shared.interfaces.IDhApiLevelWrapper;
import com.seibel.lod.core.api.external.shared.objects.math.DhApiMat4f;

/**
 * @author James Seibel
 * @version 2022-7-14
 */
public class DhApiRenderEvent
{
	public IDhApiLevelWrapper levelWrapper;
	
	// the matrices received from Minecraft
	public DhApiMat4f mcModelViewMatrix;
	public DhApiMat4f mcProjectionMatrix;
	
	// the matrices used by Distant Horizons
	public DhApiMat4f dhModelViewMatrix;
	public DhApiMat4f dhProjectionMatrix;
	
	public float partialTicks;
}