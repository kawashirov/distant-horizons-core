package com.seibel.lod.core.dataObjects.fullData.sources;

public interface IIncompleteFullDataSource extends IFullDataSource
{
	void sampleFrom(IFullDataSource fullDataSource);
	
	/** 
	 * Attempts to convert this {@link IIncompleteFullDataSource} into a {@link CompleteFullDataSource}. 
	 * 
	 * @return this if the promotion failed, a new {@link CompleteFullDataSource} if successful.
	 */
	IFullDataSource tryPromotingToCompleteDataSource();
	
}
