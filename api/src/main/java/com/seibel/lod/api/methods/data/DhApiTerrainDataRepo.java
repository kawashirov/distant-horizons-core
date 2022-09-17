package com.seibel.lod.api.methods.data;

import com.seibel.lod.api.objects.DhApiResult;
import com.seibel.lod.api.objects.data.DhApiTerrainDataPoint;
import com.seibel.lod.api.interfaces.data.IDhApiTerrainDataRepo;


/**
 * Allows getting and setting any terrain data Distant Horizons has stored.
 *
 * TODO once 1.7's data refactor is complete ask Leetom and/or Leonardo for help on setting these up
 *
 * @author James Seibel
 * @version 2022-9-16
 */
public class DhApiTerrainDataRepo implements IDhApiTerrainDataRepo
{
	public static DhApiTerrainDataRepo INSTANCE = new DhApiTerrainDataRepo();
	
	private DhApiTerrainDataRepo() {  }
	
	
	
	@Override
	public DhApiTerrainDataPoint getDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ) { throw new UnsupportedOperationException(); }
	@Override
	public DhApiResult setDataAtBlockPos(int blockPosX, int blockPosY, int blockPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	@Override
	public DhApiTerrainDataPoint getDataAtChunkPos(int chunkPosX, int chunkPosZ) { throw new UnsupportedOperationException(); }
	@Override
	public DhApiResult setDataAtChunkPos(int chunkPosX, int chunkPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	@Override
	public DhApiTerrainDataPoint getDataAtRegionPos(int regionPosX, int regionPosZ) { throw new UnsupportedOperationException(); }
	@Override
	public DhApiResult setDataAtRegionPos(int regionPosX, int regionPosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
	
	@Override
	public DhApiTerrainDataPoint getDataAtDetailLevelAndPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ) { throw new UnsupportedOperationException(); }
	@Override
	public DhApiResult setDataAtRegionPos(short detailLevel, int relativePosX, int relativePosY, int relativePosZ, DhApiTerrainDataPoint newData) { throw new UnsupportedOperationException(); }
	
}
