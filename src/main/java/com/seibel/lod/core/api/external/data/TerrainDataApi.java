package com.seibel.lod.core.api.external.data;

import com.seibel.lod.core.api.external.data.objects.TerrainDataPoint;
import com.seibel.lod.core.api.external.sharedObjects.DhApiResult;


/**
 * Allows getting and setting any data Distant Horizons has stored.
 *
 * TODO once 1.7's data refactor is complete ask Leetom and/or Leonardo for help on setting these up
 *
 * @author James Seibel
 * @version 2022-7-11
 */
public class TerrainDataApi
{
	/**
	 * Returns the terrain data at the given block position.
	 * Null if the position hasn't been generated.
	 */
	public static TerrainDataPoint getDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ)
	{
		throw new UnsupportedOperationException();
	}
	/** Sets the terrain data at the given block position. */
	public static DhApiResult setDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ, TerrainDataPoint newData)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns the average color for the chunk at the given chunk position.
	 * Returns null if the position hasn't been generated.
	 */
	public static TerrainDataPoint getDataAtChunkPos(int chunkPosX, int chunkPosZ)
	{
		throw new UnsupportedOperationException();
	}
	/** Sets the terrain data at the given chunk position. */
	public static DhApiResult setDataAtChunkPos(int chunkPosX, int chunkPosZ, TerrainDataPoint newData)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns the average color for the chunk at the given chunk position.
	 * May return inaccurate data if the whole region hasn't been generated yet.
	 * Returns null if the position hasn't been generated.
	 */
	public static TerrainDataPoint getDataAtRegionPos(int regionPosX, int regionPosZ)
	{
		throw new UnsupportedOperationException();
	}
	/** Sets the terrain data at the given chunk position. */
	public static DhApiResult setDataAtRegionPos(int regionPosX, int regionPosZ, TerrainDataPoint newData)
	{
		throw new UnsupportedOperationException();
	}
	
	
	/**
	 * Returns the average color for the chunk at the given chunk position.
	 * May return inaccurate data if the whole region hasn't been generated yet.
	 * Returns null if the position hasn't been generated.
	 */
	public static TerrainDataPoint getDataAtDetailLevelAndPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ)
	{
		throw new UnsupportedOperationException();
	}
	/** Sets the terrain data at the given chunk position. */
	public static DhApiResult setDataAtRegionPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ, TerrainDataPoint newData)
	{
		throw new UnsupportedOperationException();
	}
	
}
