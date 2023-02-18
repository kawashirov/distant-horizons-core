package com.seibel.lod.core.dataObjects.fullData;

public interface IIncompleteFullDataSource extends IFullDataSource
{
	void sampleFrom(IFullDataSource source);
	
	IFullDataSource trySelfPromote();
	
}
