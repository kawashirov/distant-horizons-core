package com.seibel.distanthorizons.api.objects.data;

import com.seibel.distanthorizons.api.objects.math.DhApiVec3i;
import com.seibel.distanthorizons.coreapi.util.math.Vec3i;

/**
 * Holds a single datapoint of terrain data
 * and the block position from the raycast.
 * 
 * @author James Seibel
 * @version 2022-11-19
 */
public class DhApiRaycastResult
{
	/** 
	 * LOD position of this raycast. <br><br>
	 * 
	 * <strong>Note: </strong>
	 * This will NOT be the exact block position if the LOD the ray
	 * hits is more than one block tall. In that case this will
	 * be the bottom block position for that LOD.
	 */
	public final DhApiVec3i pos;
	
	/** The LOD data at this position. */
	public final DhApiTerrainDataPoint dataPoint;
	
	
	
	public DhApiRaycastResult(DhApiTerrainDataPoint dataPoint, Vec3i blockPos)
	{
		this.dataPoint = dataPoint;
		this.pos = blockPos;
	}
	
}
