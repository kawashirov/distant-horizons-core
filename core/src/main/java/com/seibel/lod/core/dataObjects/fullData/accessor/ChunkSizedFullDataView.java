package com.seibel.lod.core.dataObjects.fullData.accessor;

import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.util.LodUtil;

/**
 * Contains the Full Data for a single chunk.
 */
public class ChunkSizedFullDataView extends FullDataArrayView
{
	public final DhChunkPos pos;
	// TODO replace this var with LodUtil.BLOCK_DETAIL_LEVEL 
	public final byte detailLevel = LodUtil.BLOCK_DETAIL_LEVEL;
	
	
	
	public ChunkSizedFullDataView(DhChunkPos pos)
	{
		super(new FullDataPointIdMap(), 
				new long[LodUtil.CHUNK_WIDTH * LodUtil.CHUNK_WIDTH][0], 
				LodUtil.CHUNK_WIDTH);
		
		this.pos = pos;
	}
	
	
	
	public void setSingleColumn(long[] data, int x, int z)
	{
		dataArrays[x * LodUtil.CHUNK_WIDTH + z] = data;
	}
	
	public long nonEmptyCount()
	{
		long count = 0;
		for (long[] data : dataArrays)
		{
			if (data.length != 0)
			{
				count += 1;
			}
		}
		return count;
	}
	
	public long emptyCount() { return LodUtil.CHUNK_WIDTH * LodUtil.CHUNK_WIDTH - nonEmptyCount(); }
	
	public DhLodPos getLodPos() { return new DhLodPos(LodUtil.CHUNK_DETAIL_LEVEL, pos.x, pos.z); }
	
}