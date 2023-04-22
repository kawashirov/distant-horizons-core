package com.seibel.lod.core.dataObjects.fullData.accessor;

import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.util.LodUtil;

import java.util.Iterator;

public interface IFullDataView
{
	FullDataPointIdMap getMapping();
	
	/** generally used for iterating through the whole data set */
	SingleFullArrayView get(int index);
	SingleFullArrayView get(int relativeX, int relativeZ);
	
	/** measured in full data points */
	int width();
	
	IFullDataView subView(int size, int xOffset, int zOffset);
	
	
	
	
	/** Returns an iterator that goes over each data column */
	default Iterator<SingleFullArrayView> iterator()
	{
		return new Iterator<SingleFullArrayView>()
		{
			private int index = 0;
			private final int size = width() * width();
			
			@Override
			public boolean hasNext() { return this.index < this.size; }
			
			@Override
			public SingleFullArrayView next()
			{
				LodUtil.assertTrue(this.hasNext(), "No more data to iterate!");
				return get(this.index++);
			}
		};
	}
	
}
