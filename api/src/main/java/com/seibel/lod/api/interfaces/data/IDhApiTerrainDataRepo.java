package com.seibel.lod.api.interfaces.data;

import com.seibel.lod.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.objects.DhApiResult;
import com.seibel.lod.api.objects.data.DhApiTerrainDataPoint;

/**
 * Used to interface with Distant Horizons' terrain data.
 * 
 * @author James Seibel
 * @version 2022-11-14
 */
public interface IDhApiTerrainDataRepo
{
	
	/** Returns the terrain datapoint at the given block position, at or containing the given Y position. */
	DhApiResult<DhApiTerrainDataPoint> getSingleDataPointAtBlockPos(IDhApiLevelWrapper levelWrapper, int blockPosX, int blockPosY, int blockPosZ);
	/** Returns every datapoint in the column located at the given block X and Z position top to bottom. */
	DhApiResult<DhApiTerrainDataPoint[]> getColumnDataAtBlockPos(IDhApiLevelWrapper levelWrapper, int blockPosX, int blockPosZ);
//	/** Sets the terrain data at the given block position. */
//	DhApiResult setDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ, DhApiTerrainDataPoint newData);
	
	
	/** 
	 * Returns every datapoint in the given chunk's X and Z position. <br><br>
	 * 
	 * The returned array is ordered: [relativeBlockX][relativeBlockZ][columnIndex] <br>
	 * RelativeBlockX/Z are relative to the block position closest to negative infinity in the chunk's position. <br>
	 * The column data is ordered from top to bottom. Note: each column may have a different number of values. <br>
	 */
	DhApiResult<DhApiTerrainDataPoint[][][]> getAllTerrainDataAtChunkPos(IDhApiLevelWrapper levelWrapper, int chunkPosX, int chunkPosZ);
//	/** Sets the terrain data at the given chunk position. */
//	DhApiResult setDataAtChunkPos(int chunkPosX, int chunkPosZ, DhApiTerrainDataPoint newData);
	
	
	/**
	 * Returns every datapoint in the given region's X and Z position. <br><br>
	 *
	 * The returned array is ordered: [relativeBlockX][relativeBlockZ][columnIndex] <br>
	 * RelativeBlockX/Z are relative to the block position closest to negative infinity in the region's position. <br>
	 * The column data is ordered from top to bottom. Note: each column may have a different number of values. <br>
	 */
	DhApiResult<DhApiTerrainDataPoint[][][]> getAllTerrainDataAtRegionPos(IDhApiLevelWrapper levelWrapper, int regionPosX, int regionPosZ);
//	/** Sets the terrain data at the given chunk position. */
//	DhApiResult setDataAtRegionPos(int regionPosX, int regionPosZ, DhApiTerrainDataPoint newData);
	
	
	/**
	 * Returns every datapoint in the column located at the given detail level and X/Z position. <br>
	 * This can be used to return terrain data for non-standard sizes (IE 2x2 blocks or 2x2 chunks).
	 * 
	 * @param detailLevel a positive byte defining the detail level of the returned data. <br>
	 * 					  Every increase doubles the width of the returned area. <br>
	 * 					  Example values: 0 = block, 1 = 2x2 blocks, 2 = 4x4 blocks, ... 4 = chunk (16x16 blocks), ... 9 = region (512x512 blocks)
	 */
	DhApiResult<DhApiTerrainDataPoint[][][]> getAllTerrainDataAtDetailLevelAndPos(IDhApiLevelWrapper levelWrapper, byte detailLevel, int posX, int posZ);
//	/** Sets the terrain data at the given chunk position. */
//	DhApiResult setDataAtRegionPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ, DhApiTerrainDataPoint newData);
	
}
