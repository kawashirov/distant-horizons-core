package com.seibel.lod.core.datatype.full.accessor;

import com.seibel.lod.core.datatype.full.FullDataPointIdMap;
import com.seibel.lod.core.util.LodUtil;

import java.util.Iterator;

public interface IFullDataView
{
	FullDataPointIdMap getMapping();
	
	SingleFullArrayView get(int index);
	
	SingleFullArrayView get(int x, int z);
	
	int width();
	
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
	
	IFullDataView subView(int size, int ox, int oz);
	
}
