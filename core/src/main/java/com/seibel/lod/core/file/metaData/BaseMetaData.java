package com.seibel.lod.core.file.metaData;

import com.seibel.lod.core.dataObjects.fullData.IFullDataSource;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;

/** 
 * Contains and represents the meta information ({@link DhSectionPos}, {@link BaseMetaData#dataLevel}, etc.) 
 * stored at the beginning of files that use the {@link AbstractMetaDataContainerFile}. <Br>
 * Which, as of the time of writing, includes: {@link IFullDataSource} and {@link ColumnRenderSource} files.
 */
public class BaseMetaData
{
	public DhSectionPos pos;
	public int checksum;
//	public AtomicLong dataVersion; // currently broken
	public byte dataLevel;
	
	// Loader stuff //
	/** indicates what data is held in this file, is generally a hash of the data's name */
	public long dataTypeId;
	public byte loaderVersion;
	
	
	
	public BaseMetaData(DhSectionPos pos, int checksum, byte dataLevel, long dataTypeId, byte loaderVersion)
	{
		this.pos = pos;
		this.checksum = checksum;
//		this.dataVersion = new AtomicLong(dataVersion);
		this.dataLevel = dataLevel;
		this.dataTypeId = dataTypeId;
		this.loaderVersion = loaderVersion;
	}
	
}
