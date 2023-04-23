package com.seibel.lod.core.dataObjects.fullData.sources;

public interface IIncompleteFullDataSource extends IFullDataSource
{
	/**
	 * Overwrites data in this object with non-null data from the input {@link IFullDataSource}. <br><br>
	 * 
	 * This can be used to either merge same sized data sources or downsample to 
	 */
	void sampleFrom(IFullDataSource fullDataSource);
	
	/** 
	 * Attempts to convert this {@link IIncompleteFullDataSource} into a {@link CompleteFullDataSource}. 
	 * 
	 * @return this if the promotion failed, a new {@link CompleteFullDataSource} if successful.
	 */
	IFullDataSource tryPromotingToCompleteDataSource(); // TODO make this return CompleteFullDataSource instead, if it fails just return null
	
}
