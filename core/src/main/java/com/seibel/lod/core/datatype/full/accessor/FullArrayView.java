package com.seibel.lod.core.datatype.full.accessor;

import com.seibel.lod.core.util.FullDataPointUtil;
import com.seibel.lod.core.datatype.full.FullDataPointIdMap;
import com.seibel.lod.core.util.LodUtil;

public class FullArrayView implements IFullDataView
{
	protected final long[][] dataArrays;
	protected final int offset;
	protected final int size;
	protected final int dataSize;
	protected final FullDataPointIdMap mapping;
	
	public FullArrayView(FullDataPointIdMap mapping, long[][] dataArrays, int size)
	{
		if (dataArrays.length != size * size)
			throw new IllegalArgumentException(
					"tried constructing dataArrayView with invalid input!");
		this.dataArrays = dataArrays;
		this.size = size;
		this.dataSize = size;
		this.mapping = mapping;
		this.offset = 0;
	}
	
	public FullArrayView(FullArrayView source, int size, int offsetX, int offsetZ)
	{
		if (source.size < size || source.size < size + offsetX || source.size < size + offsetZ)
		{
			throw new IllegalArgumentException(
					"tried constructing dataArrayView subview with invalid input!");
		}
		
		this.dataArrays = source.dataArrays;
		this.size = size;
		this.dataSize = source.dataSize;
		this.mapping = source.mapping;
		this.offset = source.offset + offsetX * this.dataSize + offsetZ;
	}
	
	@Override
	public FullDataPointIdMap getMapping() { return this.mapping; }
	
	@Override
	public SingleFullArrayView get(int index) { return this.get(index / this.size, index % this.size); }
	
	@Override
	public SingleFullArrayView get(int x, int z) { return new SingleFullArrayView(this.mapping, this.dataArrays, x * this.size + z + this.offset); }
	
	@Override
	public int width() { return this.size; }
	
	@Override
	public FullArrayView subView(int size, int ox, int oz) { return new FullArrayView(this, size, ox, oz); }
	
	/** WARNING: This will potentially share the underlying array object! */
	public void shadowCopyTo(FullArrayView target)
	{
		if (target.size != this.size)
		{
			throw new IllegalArgumentException("Target view must have same size as this view");
		}
		if (target.mapping.equals(this.mapping))
		{
			for (int x = 0; x < this.size; x++)
			{
				System.arraycopy(this.dataArrays, this.offset + x * this.dataSize,
						target.dataArrays, target.offset + x * target.dataSize, this.size);
			}
		}
		else
		{
			int[] remappedIds = target.mapping.mergeAndReturnRemappedEntityIds(this.mapping);
			for (int x = 0; x < this.size; x++)
			{
				for (int o = 0; o < this.size; o++)
				{
					long[] sourceData = this.dataArrays[this.offset + x * this.dataSize + o];
					long[] newData = new long[sourceData.length];
					for (int i = 0; i < newData.length; i++)
					{
						newData[i] = FullDataPointUtil.remap(remappedIds, sourceData[i]);
					}
					target.dataArrays[target.offset + x * target.dataSize + o] = newData;
				}
			}
		}
	}
	
	public void downsampleFrom(FullArrayView source)
	{
		LodUtil.assertTrue(source.size > this.size && source.size % this.size == 0);
		int dataPerUnit = source.size / this.size;
		for (int ox = 0; ox < this.size; ox++)
		{
			for (int oz = 0; oz < this.size; oz++)
			{
				SingleFullArrayView column = this.get(ox, oz);
				column.downsampleFrom(source.subView(dataPerUnit, ox * dataPerUnit, oz * dataPerUnit));
			}
		}
	}
	
}
