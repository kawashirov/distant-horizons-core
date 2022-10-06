package com.seibel.lod.core.datatype.column.accessor;

public interface IColumnDatatype
{
	byte getDetailOffset();
	
	default int getDataSize()
	{
		return 1 << getDetailOffset();
	}
	
	/** Returns how many LODs could be contained by this object. */
	int getMaxLodCount();
	
	long getRoughRamUsageInBytes();
	
	int getVerticalSize();
	
	boolean doesDataPointExist(int posX, int posZ);
	
	/** Returns the datapoint for the given relative coordinates and vertical index */
	long getDataPoint(int posX, int posZ, int verticalIndex);
	/** 
	 * Returns the top datapoint for the given relative coordinates <br>
	 * Returns the empty datapoint if no data is present.
	 */
	default long getFirstDataPoint(int posX, int posZ) { return getDataPoint(posX, posZ, 0); }
	
	/** Returns every datapoint in the vertical slice at the given position as an array */
	long[] getVerticalDataPointArray(int posX, int posZ);
	/** Returns every datapoint in the vertical slice at the given position as a ColumnArrayView */
	ColumnArrayView getVerticalDataPointView(int posX, int posZ);
	
	/** Returns a QuadView that covers this whole object */
	ColumnQuadView getFullQuadView();
	/** Returns a QuadView over the give coordinate range */
	ColumnQuadView getQuadViewOverRange(int quadX, int quadZ, int quadXSize, int quadZSize);
	
	/** clears the datapoint stored at the relative position */
	void clearDataPoint(int posX, int posZ);
	
	/** 
	 * adds/sets the given datapoint at the relative position and vertical index 
	 * @return true if the datapoint was added/set
	 */
	boolean setDataPoint(long data, int posX, int posZ, int verticalIndex);
	
	/**
	 * This methods will add the data in the given position if certain condition are satisfied
	 * @param overwriteDataWithSameGenerationMode if false old data will only be overwritten if it was generated with a lower priority than the newData
	 * @return true if the newData was successfully added, false otherwise
	 */
	boolean copyVerticalData(IColumnDataView newData, int posX, int posZ, boolean overwriteDataWithSameGenerationMode);
	
	void generateData(IColumnDatatype lowerDataContainer, int posX, int posZ);
	
}
