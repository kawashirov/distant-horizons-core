package com.seibel.lod.core.file.metaData;

import com.seibel.lod.core.pos.DhSectionPos;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Contains the MetaData used by DataSources. <br><br>
 * 
 * See {@link AbstractMetaDataFile} for a byte map inorder to see the currently used bytes
 */
public class MetaData
{
	public DhSectionPos pos;
	public int checksum;
	public AtomicLong dataVersion;
	public byte dataLevel;
	// Loader stuff
	public long dataTypeId;
	public byte loaderVersion;
	
	public MetaData(DhSectionPos pos, int checksum, long dataVersion, byte dataLevel, long dataTypeId, byte loaderVersion)
	{
		this.pos = pos;
		this.checksum = checksum;
		this.dataVersion = new AtomicLong(dataVersion);
		this.dataLevel = dataLevel;
		this.dataTypeId = dataTypeId;
		this.loaderVersion = loaderVersion;
	}
	
}
