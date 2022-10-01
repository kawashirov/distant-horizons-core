package com.seibel.lod.core.datatype.full.accessor;

import com.seibel.lod.core.datatype.full.FullDataPoint;
import com.seibel.lod.core.datatype.full.FullDataPointIdMap;

public class SingleFullArrayView implements IFullDataView
{
	private final long[][] dataArrays;
	private final int offset;
	private final FullDataPointIdMap mapping;
	
	public SingleFullArrayView(FullDataPointIdMap mapping, long[][] dataArrays, int offset)
	{
		this.dataArrays = dataArrays;
		this.offset = offset;
		this.mapping = mapping;
	}
	
	public boolean doesItExist() { return dataArrays[offset].length != 0; }
	
	@Override
	public FullDataPointIdMap getMapping() { return mapping; }
	
	@Override
	public SingleFullArrayView get(int index)
	{
		if (index != 0)
			throw new IllegalArgumentException("Only contains 1 column of full data!");
		
		return this;
	}
	
	@Override
	public SingleFullArrayView get(int x, int z)
	{
		if (x != 0 || z != 0)
			throw new IllegalArgumentException("Only contains 1 column of full data!");
		
		return this;
	}
	
	public long[] getRaw() { return dataArrays[offset]; }
	
	public long getSingle(int yIndex) { return dataArrays[offset][yIndex]; }
	public void setSingle(int yIndex, long value) { dataArrays[offset][yIndex] = value; }
	
	public void setNew(long[] newArray) { dataArrays[offset] = newArray; }
	
	public int getSingleLength() { return dataArrays[offset].length; }
	
	@Override
	public int width() { return 1; }
	
	@Override
	public IFullDataView subView(int size, int ox, int oz)
	{
		if (size != 1 || ox != 1 || oz != 1)
			throw new IllegalArgumentException("Getting invalid range of subView from SingleFullArrayView!");
		return this;
	}
	
	/** WARNING: It may potentially share the underlying array object! */
	public void shadowCopyTo(SingleFullArrayView target)
	{
		if (target.mapping.equals(mapping))
		{
			target.dataArrays[target.offset] = dataArrays[offset];
		}
		else
		{
			int[] remappedEntryIds = target.mapping.mergeAndReturnRemappedEntityIds(mapping);
			long[] sourceData = dataArrays[offset];
			long[] newData = new long[sourceData.length];
			for (int i = 0; i < newData.length; i++)
			{
				newData[i] = FullDataPoint.remap(remappedEntryIds, sourceData[i]);
			}
			target.dataArrays[target.offset] = newData;
		}
	}
	
	public void deepCopyTo(SingleFullArrayView target)
	{
		if (target.mapping.equals(mapping))
		{
			target.dataArrays[target.offset] = dataArrays[offset].clone();
		}
		else
		{
			int[] remappedEntryIds = target.mapping.mergeAndReturnRemappedEntityIds(mapping);
			long[] sourceData = dataArrays[offset];
			long[] newData = new long[sourceData.length];
			for (int i = 0; i < newData.length; i++)
			{
				newData[i] = FullDataPoint.remap(remappedEntryIds, sourceData[i]);
			}
			target.dataArrays[target.offset] = newData;
		}
	}
	
	public void downsampleFrom(IFullDataView source)
	{
		//TODO: Temp downsample method
		SingleFullArrayView firstColumn = source.get(0);
		firstColumn.deepCopyTo(this);
	}
	
}
