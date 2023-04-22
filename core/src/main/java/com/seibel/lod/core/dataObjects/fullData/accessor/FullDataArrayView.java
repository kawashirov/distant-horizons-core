package com.seibel.lod.core.dataObjects.fullData.accessor;

import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.util.FullDataPointUtil;
import com.seibel.lod.core.util.LodUtil;

/**
 * 
 */
public class FullDataArrayView implements IFullDataView
{
	protected final FullDataPointIdMap mapping;
	protected final long[][] dataArrays;
	
	/** measured in data points */
	protected final int width;
	/** measured in data points */
	protected final int dataWidth;
	
	protected final int offset;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public FullDataArrayView(FullDataPointIdMap mapping, long[][] dataArrays, int width)
	{
		if (dataArrays.length != width * width)
		{
			throw new IllegalArgumentException("tried constructing dataArrayView with invalid input!");
		}
		
		this.dataArrays = dataArrays;
		this.width = width;
		this.dataWidth = width;
		this.mapping = mapping;
		this.offset = 0;
	}
	
	public FullDataArrayView(FullDataArrayView source, int width, int offsetX, int offsetZ)
	{
		if (source.width < width || source.width < width + offsetX || source.width < width + offsetZ)
		{
			throw new IllegalArgumentException("tried constructing dataArrayView subview with invalid input!");
		}
		
		this.dataArrays = source.dataArrays;
		this.width = width;
		this.dataWidth = source.dataWidth;
		this.mapping = source.mapping;
		this.offset = source.offset + offsetX * this.dataWidth + offsetZ;
	}
	
	
	
	//=========//
	// methods //
	//=========//
	
	@Override
	public FullDataArrayView subView(int size, int xOffset, int zOffset) { return new FullDataArrayView(this, size, xOffset, zOffset); }
	
	/** WARNING: This will potentially share the underlying array object! */
	public void shadowCopyTo(FullDataArrayView target)
	{
		if (target.width != this.width)
		{
			throw new IllegalArgumentException("Target view must have same size as this view");
		}
		
		
		if (target.mapping.equals(this.mapping))
		{
			for (int x = 0; x < this.width; x++)
			{
				System.arraycopy(this.dataArrays, this.offset + x * this.dataWidth,
						target.dataArrays, target.offset + x * target.dataWidth, this.width);
			}
		}
		else
		{
			int[] remappedIds = target.mapping.mergeAndReturnRemappedEntityIds(this.mapping);
			for (int x = 0; x < this.width; x++)
			{
				for (int z = 0; z < this.width; z++)
				{
					long[] sourceData = this.dataArrays[this.offset + x * this.dataWidth + z];
					long[] newData = new long[sourceData.length];
					for (int dataPointIndex = 0; dataPointIndex < newData.length; dataPointIndex++)
					{
						newData[dataPointIndex] = FullDataPointUtil.remap(remappedIds, sourceData[dataPointIndex]);
					}
					
					target.dataArrays[target.offset + x * target.dataWidth + z] = newData;
				}
			}
		}
	}
	
	public void downsampleFrom(FullDataArrayView arrayView)
	{
		LodUtil.assertTrue(arrayView.width > this.width && arrayView.width % this.width == 0);
		
		int dataPerUnit = arrayView.width / this.width;
		for (int xOffset = 0; xOffset < this.width; xOffset++)
		{
			for (int zOffset = 0; zOffset < this.width; zOffset++)
			{
				SingleFullArrayView column = this.get(xOffset, zOffset);
				column.downsampleFrom(arrayView.subView(dataPerUnit, xOffset * dataPerUnit, zOffset * dataPerUnit));
			}
		}
	}
	
	
	
	//=========//
	// getters //
	//=========//
	
	@Override
	public FullDataPointIdMap getMapping() { return this.mapping; }
	
	@Override
	public SingleFullArrayView get(int index) { return this.get(index / this.width, index % this.width); }
	@Override
	public SingleFullArrayView get(int relativeX, int relativeZ) { return new SingleFullArrayView(this.mapping, this.dataArrays, relativeX * this.width + relativeZ + this.offset); }
	
	@Override
	public int width() { return this.width; }
	
}
