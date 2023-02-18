package com.seibel.lod.core.datatype.full;

public interface IIncompleteFullDataSource extends IFullDataSource
{
	void sampleFrom(IFullDataSource source);
	
	IFullDataSource trySelfPromote();
	
}
