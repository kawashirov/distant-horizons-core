package com.seibel.lod.core.dataObjects.fullData.accessor;

import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.util.FullDataPointUtil;

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
	
	
	
	public boolean doesItExist() { return this.dataArrays[this.offset].length != 0; }
	
	@Override
	public FullDataPointIdMap getMapping() { return this.mapping; }
	
	@Override
	public SingleFullArrayView get(int index)
	{
		if (index != 0)
		{
			throw new IllegalArgumentException("Only contains 1 column of full data!");
		}
		
		return this;
	}
	
	@Override
	public SingleFullArrayView get(int x, int z)
	{
		if (x != 0 || z != 0)
		{
			throw new IllegalArgumentException("Only contains 1 column of full data!");
		}
		
		return this;
	}
	
	public long[] getRaw() { return this.dataArrays[this.offset]; }
	
	public long getSingle(int yIndex) { return this.dataArrays[this.offset][yIndex]; }
	public void setSingle(int yIndex, long value) { this.dataArrays[this.offset][yIndex] = value; }
	
	public void setNew(long[] newArray) { this.dataArrays[this.offset] = newArray; }
	
	/** @return how many data points are in this column */
	public int getSingleLength() { return this.dataArrays[this.offset].length; }
	
	@Override
	public int width() { return 1; }
	
	@Override
	public IFullDataView subView(int size, int ox, int oz)
	{
		if (size != 1 || ox != 1 || oz != 1)
		{
			throw new IllegalArgumentException("Getting invalid range of subView from SingleFullArrayView!");
		}
		
		return this;
	}
	
	/** WARNING: It may potentially share the underlying array object! */
	public void shadowCopyTo(SingleFullArrayView target)
	{
		if (target.mapping.equals(this.mapping))
		{
			target.dataArrays[target.offset] = this.dataArrays[this.offset];
		}
		else
		{
			int[] remappedEntryIds = target.mapping.mergeAndReturnRemappedEntityIds(this.mapping);
			long[] sourceData = this.dataArrays[this.offset];
			long[] newData = new long[sourceData.length];
			for (int i = 0; i < newData.length; i++)
			{
				newData[i] = FullDataPointUtil.remap(remappedEntryIds, sourceData[i]);
			}
			target.dataArrays[target.offset] = newData;
		}
	}
	
	public void deepCopyTo(SingleFullArrayView target)
	{
		if (target.mapping.equals(this.mapping))
		{
			target.dataArrays[target.offset] = this.dataArrays[this.offset].clone();
		}
		else
		{
			int[] remappedEntryIds = target.mapping.mergeAndReturnRemappedEntityIds(this.mapping);
			long[] sourceData = this.dataArrays[this.offset];
			long[] newData = new long[sourceData.length];
			for (int i = 0; i < newData.length; i++)
			{
				newData[i] = FullDataPointUtil.remap(remappedEntryIds, sourceData[i]);
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
