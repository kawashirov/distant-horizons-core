package com.seibel.lod.core.datatype.column;

import com.seibel.lod.core.datatype.IIncompleteFullDataSource;
import com.seibel.lod.core.datatype.IFullDataSource;
import com.seibel.lod.core.datatype.column.accessor.ColumnFormat;
import com.seibel.lod.core.datatype.full.FullDataSource;
import com.seibel.lod.core.datatype.transform.FullToColumnTransformer;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.datatype.IRenderSource;
import com.seibel.lod.core.datatype.AbstractRenderSourceLoader;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.file.renderfile.RenderMetaDataFile;
import com.seibel.lod.core.util.LodUtil;

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
public class ColumnRenderLoader extends AbstractRenderSourceLoader
{
    public ColumnRenderLoader()
	{
        super(ColumnRenderSource.class, ColumnRenderSource.TYPE_ID, new byte[]{ ColumnRenderSource.LATEST_VERSION }, ColumnRenderSource.SECTION_SIZE_OFFSET);
    }
	
	
	
    @Override
    public IRenderSource loadRenderSource(RenderMetaDataFile dataFile, InputStream data, IDhLevel level) throws IOException
	{
		DataInputStream inputStream = new DataInputStream(data); // DO NOT CLOSE
		int dataFileVersion = dataFile.metaData.loaderVersion;
		
		switch (dataFileVersion)
		{
			case 1:
				return new ColumnRenderSource(dataFile.pos, readDataV1(inputStream, level.getMinY()), level);
			default:
				throw new IOException("Invalid Data: The data version [" + dataFileVersion + "] is not supported");
		}
    }
	
    @Override
    public IRenderSource createRenderSource(IFullDataSource dataSource, IDhClientLevel level)
	{
		if (dataSource instanceof FullDataSource) // TODO replace with Java 7 method
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
	 * @param inputData Expected format: 1st byte: detail level, 2nd byte: vertical size, 3rd byte on: column data
	 * 
	 * @throws IOException if there was an issue reading the stream 
	 */
	private static ParsedColumnData readDataV1(DataInputStream inputData, int yOffset) throws IOException
	{
		byte detailLevel = inputData.readByte();
		int verticalDataCount = inputData.readByte() & 0b01111111;
		
		int maxNumberOfDataPoints = ColumnRenderSource.SECTION_SIZE * ColumnRenderSource.SECTION_SIZE * verticalDataCount;
		
		
		//FIXME: Temp hack flag for marking a empty section
		short tempMinHeight = Short.reverseBytes(inputData.readShort());
		if (tempMinHeight == Short.MAX_VALUE)
		{
			return new ParsedColumnData(detailLevel, verticalDataCount, new long[maxNumberOfDataPoints], true);
		}
		
		
		// isEmpty = false
		byte[] data = new byte[maxNumberOfDataPoints * Long.BYTES];
		ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		inputData.readFully(data);
		
		long[] dataPoints = new long[maxNumberOfDataPoints];
		byteBuffer.asLongBuffer().get(dataPoints);
		if (tempMinHeight != yOffset)
		{
			for (int i = 0; i < dataPoints.length; i++)
			{
				dataPoints[i] = ColumnFormat.shiftHeightAndDepth(dataPoints[i], (short) (tempMinHeight - yOffset));
			}
		}
		
		return new ParsedColumnData(detailLevel, verticalDataCount, dataPoints, false);
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
