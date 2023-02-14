package com.seibel.lod.core.datatype;

public interface IIncompleteFullDataSource extends IFullDataSource
{
	void sampleFrom(IFullDataSource source);
	
	IFullDataSource trySelfPromote();
	
}
