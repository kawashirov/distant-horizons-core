package com.seibel.lod.core.dataObjects.fullData.sources;

import com.seibel.lod.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataView;
import com.seibel.lod.core.dataObjects.fullData.accessor.SingleFullArrayView;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.BufferedOutputStream;
import java.io.IOException;

// TODO make into an abstract class so the read(stream) method can be used as a constructor
// TODO validate how we know which file to use when (probably, TYPE_ID)
// TODO merge with FullDataArrayView and IFullDataView
public interface IFullDataSource
{
	/**
	 * This is the byte put between different sections in the binary save file.
	 * The presence and absence of this byte indicates if the file is correctly formatted.  
	 */
	public static final int DATA_GUARD_BYTE = 0xFFFFFFFF;
	/** indicates the binary save file represents an empty data source */
	public static final int NO_DATA_FLAG_BYTE = 0x00000001;
	
	
	
	public abstract DhSectionPos getSectionPos();
	
	public abstract byte getDataDetailLevel();
	public abstract byte getDataVersion();
	public abstract EDhApiWorldGenerationStep getWorldGenStep();
	
	public abstract void update(ChunkSizedFullDataView data);
	
	public abstract boolean isEmpty();
	
	
	
	//===============//
	// file handling // 
	//===============//
	
	void writeToStream(IDhLevel level, FullDataMetaFile file, BufferedOutputStream bufferedOutputStream) throws IOException;
	
	// TODO replace above with this
//	public default void writeToStream(IDhLevel level, FullDataMetaFile file, BufferedOutputStream bufferedOutputStream) throws IOException
//	{
//		this.saveSourceInfo(level, file, bufferedOutputStream);
//		
//		if (this.isEmpty())
//		{
//			bufferedOutputStream.write(IFullDataSource.NO_DATA_FLAG_BYTE);
//			return;
//		}
//		this.saveDataPoints(level, file, bufferedOutputStream);
//		
//		this.saveIdMappings(level, file, bufferedOutputStream);
//	}
//	/** includes information about the source file that doesn't need to be saved in each data point. Like the source's size and y-level. */
//	public abstract void saveSourceInfo(IDhLevel level, FullDataMetaFile file, BufferedOutputStream bufferedOutputStream) throws IOException;
//	public abstract void saveDataPoints(IDhLevel level, FullDataMetaFile file, BufferedOutputStream bufferedOutputStream) throws IOException;
//	public abstract void saveIdMappings(IDhLevel level, FullDataMetaFile file, BufferedOutputStream bufferedOutputStream) throws IOException;
	
	
	
	/** 
	 * Attempts to get the data column for the given relative x and z position.
	 * @return null if the data doesn't exist
	 */
	public abstract SingleFullArrayView tryGet(int relativeX, int relativeZ);
	
	public abstract FullDataPointIdMap getMapping();
	
}
