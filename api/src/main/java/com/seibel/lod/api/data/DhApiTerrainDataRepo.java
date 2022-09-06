package com.seibel.lod.api.data;

import com.seibel.lod.api.items.objects.DhApiResult;
import com.seibel.lod.api.items.objects.data.DhApiTerrainDataPoint;


/**
 * Allows getting and setting any terrain data Distant Horizons has stored.
 *
 * TODO once 1.7's data refactor is complete ask Leetom and/or Leonardo for help on setting these up
 *
 * @author James Seibel
 * @version 2022-7-12
 */
public class DhApiTerrainDataRepo
{
	/**
	 * Returns the terrain data at the given block position.
	 * Null if the position hasn't been generated.
	 */
	public static DhApiTerrainDataPoint getDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ)
	{
		throw new UnsupportedOperationException();
	}
	/** Sets the terrain data at the given block position. */
	public static DhApiResult setDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ, DhApiTerrainDataPoint newData)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns the average color for the chunk at the given chunk position.
	 * Returns null if the position hasn't been generated.
	 */
	public static DhApiTerrainDataPoint getDataAtChunkPos(int chunkPosX, int chunkPosZ)
	{
		throw new UnsupportedOperationException();
	}
	/** Sets the terrain data at the given chunk position. */
	public static DhApiResult setDataAtChunkPos(int chunkPosX, int chunkPosZ, DhApiTerrainDataPoint newData)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns the average color for the chunk at the given chunk position.
	 * May return inaccurate data if the whole region hasn't been generated yet.
	 * Returns null if the position hasn't been generated.
	 */
	public static DhApiTerrainDataPoint getDataAtRegionPos(int regionPosX, int regionPosZ)
	{
		throw new UnsupportedOperationException();
	}
	/** Sets the terrain data at the given chunk position. */
	public static DhApiResult setDataAtRegionPos(int regionPosX, int regionPosZ, DhApiTerrainDataPoint newData)
	{
		throw new UnsupportedOperationException();
	}
	
	
	/**
	 * Returns the average color for the chunk at the given chunk position.
	 * May return inaccurate data if the whole region hasn't been generated yet.
	 * Returns null if the position hasn't been generated.
	 */
	public static DhApiTerrainDataPoint getDataAtDetailLevelAndPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ)
	{
		throw new UnsupportedOperationException();
	}
	/** Sets the terrain data at the given chunk position. */
	public static DhApiResult setDataAtRegionPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ, DhApiTerrainDataPoint newData)
	{
		throw new UnsupportedOperationException();
	}
	
}
