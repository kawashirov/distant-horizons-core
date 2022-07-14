package com.seibel.lod.core.api.external.events.objects;

import com.seibel.lod.core.api.external.shared.interfaces.IDhApiLevelWrapper;
import com.seibel.lod.core.api.external.shared.objects.math.DhApiMat4f;

/**
 * @author James Seibel
 * @version 2022-7-14
 */
public class DhApiAfterRenderEvent extends DhApiRenderEvent
{
	/** False if DH rendering was disabled or canceled for this frame. */
	public boolean renderingEnabled;
}