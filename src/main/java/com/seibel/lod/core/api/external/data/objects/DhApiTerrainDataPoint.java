package com.seibel.lod.core.api.external.data.objects;

import java.awt.Color;

/**
 * Holds a single datapoint of terrain data.
 *
 * // TODO what additional data should this hold?
 *
 * @author James Seibel
 * @version 2022-7-12
 */
public class DhApiTerrainDataPoint
{
	/**
	 * The average color for the given data point.
	 * Invisible if the position is air.
	 * Null if the position is invalid.
	 */
	public Color color;
	
	/**
	 * TODO is this data type correct?
	 * TODO create an API enum that contains useful values (block, chunk, region, etc.)
	 * 0 = block
	 */
	public short detailLevel;
	
}
