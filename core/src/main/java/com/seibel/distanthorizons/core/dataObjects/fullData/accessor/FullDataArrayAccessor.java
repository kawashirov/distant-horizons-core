package com.seibel.distanthorizons.core.dataObjects.fullData.accessor;

import com.seibel.distanthorizons.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.distanthorizons.core.util.FullDataPointUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.CompleteFullDataSource;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.LowDetailIncompleteFullDataSource;

/**
 * Contains Full Data points and basic methods for getting and setting them. <br>
 * Can be used standalone or as the base for Full data sources.
 * 
 * @see CompleteFullDataSource
 * @see LowDetailIncompleteFullDataSource
 */
public class FullDataArrayAccessor implements IFullDataAccessor
{
	protected final FullDataPointIdMap mapping;
	
	/** 
	 * A flattened 2D array (for the X and Z directions) containing an array for the Y direction.  
	 * TODO the flattened array is probably to reduce garbage collection overhead, but is doing it this way worth while? Having a 3D array would be much easier to understand
	 */
	protected final long[][] dataArrays;
	
	/** measured in data points */
	protected final int width;
	/** measured in data points */
	protected final int dataWidth;
	
	/** index offset used when getting/setting data in {@link FullDataArrayAccessor#dataArrays}. */
	protected final int offset;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public FullDataArrayAccessor(FullDataPointIdMap mapping, long[][] dataArrays, int width)
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
	
	public FullDataArrayAccessor(FullDataArrayAccessor source, int width, int offsetX, int offsetZ)
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
	public FullDataArrayAccessor subView(int width, int xOffset, int zOffset) { return new FullDataArrayAccessor(this, width, xOffset, zOffset); }
	
	/** WARNING: This will potentially share the underlying array object! */
	public void shadowCopyTo(FullDataArrayAccessor target)
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
	
	/**
	 * Takes a higher detail {@link FullDataArrayAccessor}'s and converts the data to a lower detail level.
	 * @param fullDataAccessor must be larger than this {@link FullDataArrayAccessor} and its width must a power of two larger (example: this.width = 4, other.width = 8)
	 */
	public void downsampleFrom(FullDataArrayAccessor fullDataAccessor)
	{
		// validate that the incoming data isn't smaller than this accessor
		LodUtil.assertTrue(fullDataAccessor.width >= this.width && fullDataAccessor.width % this.width == 0);
		
		int dataPointsPerWidthUnit = fullDataAccessor.width / this.width;
		for (int xOffset = 0; xOffset < this.width; xOffset++)
		{
			for (int zOffset = 0; zOffset < this.width; zOffset++)
			{
				SingleColumnFullDataAccessor column = this.get(xOffset, zOffset);
				column.downsampleFrom(fullDataAccessor.subView(dataPointsPerWidthUnit, xOffset * dataPointsPerWidthUnit, zOffset * dataPointsPerWidthUnit));
			}
		}
	}
	
	
	
	//=========//
	// getters //
	//=========//
	
	@Override
	public FullDataPointIdMap getMapping() { return this.mapping; }
	
	@Override
	public SingleColumnFullDataAccessor get(int index) { return this.get(index / this.width, index % this.width); }
	@Override
	public SingleColumnFullDataAccessor get(int relativeX, int relativeZ) 
	{ 
		int dataArrayIndex = (relativeX * this.width) + relativeZ + this.offset;
		if (dataArrayIndex >= this.dataArrays.length)
		{
			LodUtil.assertNotReach(
					"FullDataArrayAccessor.get() called with a relative position that is outside the data source. \n" + 
					"source width: ["+this.width+"] source offset: ["+this.offset+"]\n" +
					"given relative pos X: ["+relativeX+"] Z: ["+relativeZ+"]\n" +
					"dataArrays.length: ["+this.dataArrays.length+"] dataArrayIndex: ["+dataArrayIndex+"].");
		}
		
		return new SingleColumnFullDataAccessor(this.mapping, this.dataArrays, dataArrayIndex); 
	}
	
	@Override
	public int width() { return this.width; }
	
}
