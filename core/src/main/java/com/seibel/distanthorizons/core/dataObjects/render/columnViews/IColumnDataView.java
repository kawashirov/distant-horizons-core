package com.seibel.distanthorizons.core.dataObjects.render.columnViews;

import java.util.Iterator;

public interface IColumnDataView
{
	long get(int index);
	
	int size();
	
	default Iterator<Long> iterator()
	{
		return new Iterator<Long>()
		{
			private int index = 0;
			private final int size = size();
			
			@Override
			public boolean hasNext() { return this.index < this.size; }
			
			@Override
			public Long next() { return get(this.index++); }
			
		};
	}
	
	int verticalSize();
	
	int dataCount();
	
	IColumnDataView subView(int dataIndexStart, int dataCount);
	
	void copyTo(long[] target, int offset, int count);
	
}
