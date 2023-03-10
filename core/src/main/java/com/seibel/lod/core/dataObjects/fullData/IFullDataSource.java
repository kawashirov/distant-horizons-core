package com.seibel.lod.core.dataObjects.fullData;

import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.accessor.SingleFullArrayView;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.BufferedOutputStream;
import java.io.IOException;

public interface IFullDataSource
{
	DhSectionPos getSectionPos();
	
	byte getDataDetail();
	
	byte getDataVersion();
	
	void update(ChunkSizedFullDataSource data);
	
	boolean isEmpty();
	
	void saveData(IDhLevel level, FullDataMetaFile file, BufferedOutputStream bufferedOutputStream) throws IOException;
	
	/** 
	 * Attempts to get the data column for the given relative x and z position.
	 * @return null if the data doesn't exist
	 */
	SingleFullArrayView tryGet(int x, int z);

	FullDataPointIdMap getMapping();
	
}
