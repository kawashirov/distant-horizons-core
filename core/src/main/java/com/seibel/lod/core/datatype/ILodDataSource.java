package com.seibel.lod.core.datatype;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.file.datafile.DataMetaFile;

import java.io.IOException;
import java.io.OutputStream;

public interface ILodDataSource
{
	DhSectionPos getSectionPos();
	
	byte getDataDetail();
	
	byte getDataVersion();
	
	void update(ChunkSizedData data);
	
	boolean isEmpty();
	
	// Saving related
	void saveData(IDhLevel level, DataMetaFile file, OutputStream dataStream) throws IOException;
}
