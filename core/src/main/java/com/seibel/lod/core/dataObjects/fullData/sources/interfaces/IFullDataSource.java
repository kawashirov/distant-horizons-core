package com.seibel.lod.core.dataObjects.fullData.sources.interfaces;

import com.seibel.lod.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.lod.core.dataObjects.fullData.accessor.IFullDataAccessor;
import com.seibel.lod.core.dataObjects.fullData.accessor.SingleColumnFullDataAccessor;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhSectionPos;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Base for all Full Data Source objects. <br><br>
 * 
 * Contains full DH data, methods related to file/stream reading/writing, and the data necessary to create {@link ColumnRenderSource}'s. <br>
 * {@link IFullDataSource}'s will either implement or contain {@link IFullDataAccessor}'s.
 * 
 * @see IFullDataAccessor
 * @see IIncompleteFullDataSource
 * @see IStreamableFullDataSource
 */
public interface IFullDataSource
{
	/**
	 * This is the byte put between different sections in the binary save file.
	 * The presence and absence of this byte indicates if the file is correctly formatted.  
	 */
	int DATA_GUARD_BYTE = 0xFFFFFFFF;
	/** indicates the binary save file represents an empty data source */
	int NO_DATA_FLAG_BYTE = 0x00000001;
	
	
	
	DhSectionPos getSectionPos();
	
	byte getDataDetailLevel();
	byte getBinaryDataFormatVersion();
	EDhApiWorldGenerationStep getWorldGenStep();
	
	void update(ChunkSizedFullDataAccessor data);
	
	boolean isEmpty();
	
	
	
	//======//
	// data //
	//======//
	
	/** 
	 * Attempts to get the data column for the given relative x and z position.
	 * @return null if the data doesn't exist
	 */
	SingleColumnFullDataAccessor tryGet(int relativeX, int relativeZ);
	
	FullDataPointIdMap getMapping();
	
	/** @return the list of {@link DhSectionPos} that aren't generated in this data source. */
	ArrayList<DhSectionPos> getUngeneratedPosList();
	
	
	
	//=======================//
	// basic stream handling // 
	//=======================//
	
	/** 
	 * Should only be implemented by {@link IStreamableFullDataSource} to prevent potential stream read/write inconsistencies. 
	 * @see IStreamableFullDataSource#writeToStream(BufferedOutputStream, IDhLevel)
	 */
	void writeToStream(BufferedOutputStream bufferedOutputStream, IDhLevel level) throws IOException;
	
	/** 
	 * Should only be implemented by {@link IStreamableFullDataSource} to prevent potential stream read/write inconsistencies. 
	 * @see IStreamableFullDataSource#populateFromStream(FullDataMetaFile, BufferedInputStream, IDhLevel)
	 */
	void populateFromStream(FullDataMetaFile dataFile, BufferedInputStream bufferedInputStream, IDhLevel level) throws IOException, InterruptedException;
	
}
