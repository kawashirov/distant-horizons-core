package com.seibel.lod.core.dataObjects.render;

import com.seibel.lod.core.dataObjects.fullData.IIncompleteFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.IFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.FullDataSource;
import com.seibel.lod.core.dataObjects.transformers.FullToColumnTransformer;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.renderfile.RenderMetaDataFile;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Handles loading and parsing {@link RenderMetaDataFile}s to create {@link ColumnRenderSource}s. <br><br>
 * 
 * Please see the {@link ColumnRenderLoader#loadRenderSource} method to see what
 * file versions this class can handle.
 */
public class ColumnRenderLoader
{
	public static ColumnRenderLoader INSTANCE = new ColumnRenderLoader();
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	
	private ColumnRenderLoader() { }
	
	
	
    public ColumnRenderSource loadRenderSource(RenderMetaDataFile dataFile, InputStream inputStream, IDhLevel level) throws IOException
	{
		DataInputStream inputDataStream = new DataInputStream(inputStream); // DO NOT CLOSE
		int dataFileVersion = dataFile.metaData.loaderVersion;
		
		switch (dataFileVersion)
		{
			case 1:
				LOGGER.info("loading render source "+dataFile.pos);
				
				ParsedColumnData parsedColumnData = readDataV1(inputDataStream, level.getMinY());
				if (parsedColumnData.isEmpty)
				{
					LOGGER.warn("Empty render file "+dataFile.pos);
				}
				
				return new ColumnRenderSource(dataFile.pos, parsedColumnData, level);
			default:
				throw new IOException("Invalid Data: The data version ["+dataFileVersion+"] is not supported");
		}
    }
	
	/** 
	 * @throws InterruptedException see {@link FullToColumnTransformer#transformFullDataToColumnData(IDhClientLevel, FullDataSource) FullToColumnTransformer#transformFullDataToColumnData} for documentation
	 */
    public ColumnRenderSource createRenderSource(IFullDataSource dataSource, IDhClientLevel level) throws InterruptedException
	{
		if (dataSource instanceof FullDataSource)
		{
			return FullToColumnTransformer.transformFullDataToColumnData(level, (FullDataSource) dataSource);
		}
		else if (dataSource instanceof IIncompleteFullDataSource)
		{
			return FullToColumnTransformer.transformIncompleteDataToColumnData(level, (IIncompleteFullDataSource) dataSource);
		}
		
		LodUtil.assertNotReach();
		return null;
    }
	
	
	
	//========================//
	// versioned file parsing //
	//========================//
	
	/**
	 * @param inputStream Expected format: 1st byte: detail level, 2nd byte: vertical size, 3rd byte on: column data
	 * 
	 * @throws IOException if there was an issue reading the stream 
	 */
	private static ParsedColumnData readDataV1(DataInputStream inputStream, int expectedYOffset) throws IOException
	{
		byte detailLevel = inputStream.readByte();
		
		int verticalDataCount = inputStream.readInt();
		if (verticalDataCount <= 0)
		{
			throw new IOException("Invalid data: vertical size must be 0 or greater");
		}
		
		int maxNumberOfDataPoints = ColumnRenderSource.SECTION_SIZE * ColumnRenderSource.SECTION_SIZE * verticalDataCount;
		
		
		byte dataPresentFlag = inputStream.readByte();
		if (dataPresentFlag != ColumnRenderSource.NO_DATA_FLAG_BYTE && dataPresentFlag != ColumnRenderSource.DATA_GUARD_BYTE)
		{
			throw new IOException("Incorrect render file format. Expected either: NO_DATA_FLAG_BYTE ["+ColumnRenderSource.NO_DATA_FLAG_BYTE+"] or DATA_GUARD_BYTE ["+ColumnRenderSource.DATA_GUARD_BYTE+"], Found: ["+dataPresentFlag+"]");
		}
		else if (dataPresentFlag == ColumnRenderSource.NO_DATA_FLAG_BYTE)
		{
			// no data is present
			return new ParsedColumnData(detailLevel, verticalDataCount, new long[maxNumberOfDataPoints], true);
		}
		else
		{
			// data is present
			
			int fileYOffset = inputStream.readInt();
			if (fileYOffset != expectedYOffset)
			{
				throw new IOException("Invalid data: yOffset is incorrect. Expected: ["+expectedYOffset+"], found: ["+fileYOffset+"].");
			}
			
			// read the column data
			byte[] rawByteData = new byte[maxNumberOfDataPoints * Long.BYTES];
			ByteBuffer columnDataByteBuffer = ByteBuffer.wrap(rawByteData).order(ByteOrder.LITTLE_ENDIAN);
			inputStream.readFully(rawByteData);
			
			// parse the column data
			long[] dataPoints = new long[maxNumberOfDataPoints];
			columnDataByteBuffer.asLongBuffer().get(dataPoints);
			
			boolean isEmpty = true;
			for (long dataPoint : dataPoints)
			{
				if (dataPoint != 0)
				{
					isEmpty = false;
					break;
				}
			}
			
			return new ParsedColumnData(detailLevel, verticalDataCount, dataPoints, isEmpty);
		}
	}
	
	public static class ParsedColumnData
	{
		byte detailLevel;
		int verticalSize;
		long[] dataContainer;
		boolean isEmpty;
		
		public ParsedColumnData(byte detailLevel, int verticalSize, long[] dataContainer, boolean isEmpty)
		{
			this.detailLevel = detailLevel;
			this.verticalSize = verticalSize;
			this.dataContainer = dataContainer;
			this.isEmpty = isEmpty;
		}
	}
	
	
}
