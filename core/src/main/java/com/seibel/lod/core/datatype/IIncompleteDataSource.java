package com.seibel.lod.core.datatype;

import com.seibel.lod.core.datatype.full.accessor.SingleFullArrayView;

public interface IIncompleteDataSource extends ILodDataSource
{
	void sampleFrom(ILodDataSource source);
	ILodDataSource trySelfPromote();
	// Return null if doesn't exist
	SingleFullArrayView tryGet(int x, int z);
}
