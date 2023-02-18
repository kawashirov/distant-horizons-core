package com.seibel.lod.core.datatype;

import com.seibel.lod.core.datatype.full.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.datatype.full.FullDataPointIdMap;
import com.seibel.lod.core.datatype.full.accessor.SingleFullArrayView;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.IOException;
import java.io.OutputStream;

public interface IFullDataSource
{
	DhSectionPos getSectionPos();
	
	byte getDataDetail();
	
	byte getDataVersion();
	
	void update(ChunkSizedFullDataSource data);
	
	boolean isEmpty();
	
	void saveData(IDhLevel level, FullDataMetaFile file, OutputStream dataStream) throws IOException;
	
	/** 
	 * Attempts to get the data column for the given relative x and z position.
	 * @return null if the data doesn't exist
	 */
	SingleFullArrayView tryGet(int x, int z);

	FullDataPointIdMap getMapping();
	
}
