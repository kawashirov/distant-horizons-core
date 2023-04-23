package com.seibel.lod.core.dataObjects.fullData.accessor;

import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.dataObjects.fullData.sources.IFullDataSource;
import com.seibel.lod.core.util.FullDataPointUtil;

import java.util.Iterator;

/**
 * Contains raw full data points, which must be interpreted by the {@link FullDataPointUtil}. <br>
 * Often used by {@link IFullDataSource}'s.
 * 
 * @see IFullDataSource
 * @see FullDataArrayAccessor
 * @see FullDataPointUtil
 */
public interface IFullDataAccessor
{
	FullDataPointIdMap getMapping();
	
	/** generally used for iterating through the whole data set */
	SingleColumnFullDataAccessor get(int index);
	SingleColumnFullDataAccessor get(int relativeX, int relativeZ);
	
	/** measured in full data points */
	int width();
	
	/** 
	 * Creates a new {@link IFullDataAccessor} with the given width and starting at the given X and Z offsets. <br> 
	 * The returned object will use the same underlining data structure (IE memory addresses) as the source {@link IFullDataAccessor}.
	 */
	IFullDataAccessor subView(int width, int xOffset, int zOffset);
	
	
	
	
	/** Returns an iterator that goes over each data column */
	default Iterator<SingleColumnFullDataAccessor> iterator()
	{
		return new Iterator<SingleColumnFullDataAccessor>()
		{
			private int index = 0;
			private final int size = width() * width();
			
			@Override
			public boolean hasNext() { return this.index < this.size; }
			
			@Override
			public SingleColumnFullDataAccessor next()
			{
				LodUtil.assertTrue(this.hasNext(), "No more data to iterate!");
				return get(this.index++);
			}
		};
	}
	
}
