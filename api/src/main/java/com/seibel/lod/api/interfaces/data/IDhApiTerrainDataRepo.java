package com.seibel.lod.api.interfaces.data;

import com.seibel.lod.api.objects.DhApiResult;
import com.seibel.lod.api.objects.data.DhApiTerrainDataPoint;

/**
 * @author James Seibel
 * @version 2022-9-16
 */
public interface IDhApiTerrainDataRepo
{
	
	/**
	 * Returns the terrain data at the given block position.
	 * Null if the position hasn't been generated.
	 */
	DhApiTerrainDataPoint getDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ);
	/** Sets the terrain data at the given block position. */
	DhApiResult setDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ, DhApiTerrainDataPoint newData);
	
	/**
	 * Returns the average color for the chunk at the given chunk position.
	 * Returns null if the position hasn't been generated.
	 */
	DhApiTerrainDataPoint getDataAtChunkPos(int chunkPosX, int chunkPosZ);
	/** Sets the terrain data at the given chunk position. */
	DhApiResult setDataAtChunkPos(int chunkPosX, int chunkPosZ, DhApiTerrainDataPoint newData);
	
	/**
	 * Returns the average color for the chunk at the given chunk position.
	 * May return inaccurate data if the whole region hasn't been generated yet.
	 * Returns null if the position hasn't been generated.
	 */
	DhApiTerrainDataPoint getDataAtRegionPos(int regionPosX, int regionPosZ);
	/** Sets the terrain data at the given chunk position. */
	DhApiResult setDataAtRegionPos(int regionPosX, int regionPosZ, DhApiTerrainDataPoint newData);
	
	
	/**
	 * Returns the average color for the chunk at the given chunk position.
	 * May return inaccurate data if the whole region hasn't been generated yet.
	 * Returns null if the position hasn't been generated.
	 */
	DhApiTerrainDataPoint getDataAtDetailLevelAndPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ);
	/** Sets the terrain data at the given chunk position. */
	DhApiResult setDataAtRegionPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ, DhApiTerrainDataPoint newData);
	
}
