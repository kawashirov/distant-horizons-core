package com.seibel.lod.core.datatype;

import com.seibel.lod.core.datatype.full.accessor.SingleFullArrayView;

public interface IIncompleteDataSource extends ILodDataSource
{
	void sampleFrom(ILodDataSource source);
	
	ILodDataSource trySelfPromote();
	
	/** @return null if the data doesn't exist */
	SingleFullArrayView tryGet(int x, int z);
	
}
