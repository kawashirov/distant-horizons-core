package com.seibel.lod.core.file.metaData;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.objects.UnclosableOutputStream;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;

/**
 * See {@link MetaDataFile} for a byte map inorder to see the currently used bytes
 */
public class MetaData
{
	public DhSectionPos pos;
	public int checksum;
	public AtomicLong dataVersion;
	public byte dataLevel;
	//Loader stuff
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
