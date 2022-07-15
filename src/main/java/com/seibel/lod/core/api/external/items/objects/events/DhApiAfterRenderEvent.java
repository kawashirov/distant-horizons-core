package com.seibel.lod.core.api.external.items.objects.events;

/**
 * @author James Seibel
 * @version 2022-7-14
 */
public class DhApiAfterRenderEvent extends DhApiRenderEvent
{
	/** False if DH rendering was disabled or canceled for this frame. */
	public boolean renderingEnabled;
}