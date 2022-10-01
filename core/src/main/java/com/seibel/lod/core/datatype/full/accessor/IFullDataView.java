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
	
	/** Returns an iterator that  */
	default Iterator<SingleFullArrayView> iterator()
	{
		return new Iterator<SingleFullArrayView>()
		{
			private int index = 0;
			private final int size = width() * width();
			
			@Override
			public boolean hasNext()
			{
				return index < size;
			}
			
			@Override
			public SingleFullArrayView next()
			{
				LodUtil.assertTrue(hasNext(), "No more data to iterate!");
				return get(index++);
			}
		};
	}
	
	IFullDataView subView(int size, int ox, int oz);
	
}
