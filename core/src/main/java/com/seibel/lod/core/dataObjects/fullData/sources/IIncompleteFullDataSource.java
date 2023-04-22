package com.seibel.lod.core.dataObjects.fullData.sources;

public interface IIncompleteFullDataSource extends IFullDataSource
{
	void sampleFrom(IFullDataSource fullDataSource);
	
	IFullDataSource trySelfPromote();
	
}
