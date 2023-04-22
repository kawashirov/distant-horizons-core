package com.seibel.lod.core.dataObjects.fullData.sources;

import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.dataObjects.fullData.accessor.FullArrayView;
import com.seibel.lod.core.pos.DhLodPos;

public class ChunkSizedFullDataSource extends FullArrayView
{
	public final byte dataDetail;
	public final int x;
	public final int z;
	
	
	
	public ChunkSizedFullDataSource(byte dataDetail, int x, int z)
	{
		super(new FullDataPointIdMap(), new long[16 * 16][0], 16);
		this.dataDetail = dataDetail;
		this.x = x;
		this.z = z;
	}
	
	
	
	public void setSingleColumn(long[] data, int x, int z)
	{
		dataArrays[x * 16 + z] = data;
	}
	
	public long nonEmptyCount()
	{
		long count = 0;
		for (long[] data : dataArrays)
		{
			if (data.length != 0)
				count += 1;
		}
		return count;
	}
	
	public long emptyCount() { return 16 * 16 - nonEmptyCount(); }
	
	public DhLodPos getBBoxLodPos() { return new DhLodPos((byte) (dataDetail + 4), x, z); }
	
}