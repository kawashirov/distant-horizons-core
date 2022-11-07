package com.seibel.lod.core.datatype.column;

import com.seibel.lod.core.datatype.column.accessor.*;
import com.seibel.lod.core.datatype.column.render.ColumnRenderBuffer;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.datatype.transform.FullToColumnTransformer;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.RenderBuffer;
import com.seibel.lod.core.file.renderfile.RenderMetaFile;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.render.LodRenderSection;
import com.seibel.lod.core.datatype.ILodRenderSource;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.objects.Reference;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Leetom
 * @version 2022-10-5
 */
public class ColumnRenderSource implements ILodRenderSource, IColumnDatatype
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	public static final boolean DO_SAFETY_CHECKS = true; // TODO: this could potentially be replaced with "ModInfo.IS_DEV_BUILD"
	public static final byte SECTION_SIZE_OFFSET = 6;
	public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
	public static final byte LATEST_VERSION = 1;
	public static final long TYPE_ID = "ColumnRenderSource".hashCode();
	public static final int AIR_LODS_SIZE = 16;
	public static final int AIR_SECTION_SIZE = SECTION_SIZE / AIR_LODS_SIZE;
	
	public final int verticalSize;
	public final DhSectionPos sectionPos;
	public final int yOffset;
	
	public final long[] dataContainer;
	public final int[] airDataContainer;
	
	public final DebugSourceFlag[] debugSourceFlags;
	
	private boolean isEmpty = true;
	
	private IDhClientLevel level = null; //FIXME: hack to pass level into tryBuildBuffer
	
	//FIXME: Temp Hack
	private long lastNs = -1;
	/** 10 sec */
	private static final long SWAP_TIMEOUT = 10_000_000_000L;
	/** 1 sec */
	private static final long SWAP_BUSY_COLLISION_TIMEOUT = 1_000_000_000L;
	
	private CompletableFuture<ColumnRenderBuffer> inBuildRenderBuffer = null;
	private final Reference<ColumnRenderBuffer> usedBuffer = new Reference<>();
	
	
	
	
	/**
	 * Creates an empty ColumnRenderSource.
	 * 
	 * @param sectionPos the relative position of the container
	 * @param maxVerticalSize the maximum vertical size of the container
	 */
	public ColumnRenderSource(DhSectionPos sectionPos, int maxVerticalSize, int yOffset)
	{
		verticalSize = maxVerticalSize;
		dataContainer = new long[SECTION_SIZE * SECTION_SIZE * verticalSize];
		airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * verticalSize];
		debugSourceFlags = new DebugSourceFlag[SECTION_SIZE * SECTION_SIZE];
		this.sectionPos = sectionPos;
		this.yOffset = yOffset;
	}
	
	/**
	 * Creates a new ColumnRenderSource with data from the given DataInputStream.
	 * 
	 * @param inputData Expected format: 1st byte: detail level, 2nd byte: vertical size, 3rd byte on: column data
	 * @throws IOException if the DataInputStream's detail level isn't what was expected
	 */
	public ColumnRenderSource(DhSectionPos sectionPos, DataInputStream inputData, int version, IDhLevel level) throws IOException
	{
		byte detailLevel = inputData.readByte();
		if (sectionPos.sectionDetail - SECTION_SIZE_OFFSET != detailLevel)
		{
			throw new IOException("Invalid data: detail level does not match");
		}
		
		this.sectionPos = sectionPos;
		this.yOffset = level.getMinY();
		this.verticalSize = inputData.readByte() & 0b01111111;
		this.dataContainer = this.loadData(inputData, version, this.verticalSize);
		this.airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * this.verticalSize];
		
		this.debugSourceFlags = new DebugSourceFlag[SECTION_SIZE * SECTION_SIZE];
		this.fillDebugFlag(0, 0, SECTION_SIZE, SECTION_SIZE, DebugSourceFlag.FILE);
	}
	
	
	
	//========================//
	// datapoint manipulation //
	//========================//
	
	/** 
	 * Attempts to parse and load the given DataInputStream.
	 * 
	 * @throws IOException if the version isn't supported
	 */
	private long[] loadData(DataInputStream inputData, int version, int verticalSize) throws IOException
	{
		switch (version)
		{
		case 1:
			return readDataV1(inputData, verticalSize);
		default:
			throw new IOException("Invalid Data: The data version [" + version + "] is not supported");
		}
	}
	
	private long[] readDataV1(DataInputStream inputData, int tempMaxVerticalData) throws IOException
	{
		int maxNumberOfDataPoints = SECTION_SIZE * SECTION_SIZE * tempMaxVerticalData;
		
		short tempMinHeight = Short.reverseBytes(inputData.readShort());
		if (tempMinHeight == Short.MAX_VALUE)
		{ //FIXME: Temp hack flag for marking a empty section
			return new long[maxNumberOfDataPoints];
		}
		
		this.isEmpty = false;
		byte[] data = new byte[maxNumberOfDataPoints * Long.BYTES];
		ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		inputData.readFully(data);
		
		long[] result = new long[maxNumberOfDataPoints];
		byteBuffer.asLongBuffer().get(result);
		if (tempMinHeight != this.yOffset)
		{
			for (int i = 0; i < result.length; i++)
			{
				result[i] = ColumnFormat.shiftHeightAndDepth(result[i], (short) (tempMinHeight - this.yOffset));
			}
		}
		return result;
	}
	
	@Override
	public void clearDataPoint(int posX, int posZ)
	{
		for (int verticalIndex = 0; verticalIndex < this.verticalSize; verticalIndex++)
		{
			this.dataContainer[posX * SECTION_SIZE * this.verticalSize + posZ * this.verticalSize + verticalIndex] = ColumnFormat.EMPTY_DATA;
		}
	}
	
	@Override
	public boolean setDataPoint(long data, int posX, int posZ, int verticalIndex)
	{
		this.dataContainer[posX * SECTION_SIZE * this.verticalSize + posZ * this.verticalSize + verticalIndex] = data;
		return true;
	}
	
	@Override
	public boolean copyVerticalData(IColumnDataView newData, int posX, int posZ, boolean overwriteDataWithSameGenerationMode)
	{
		if (DO_SAFETY_CHECKS)
		{
			if (newData.size() != this.verticalSize)
				throw new IllegalArgumentException("newData size not the same as this column's vertical size");
			if (posX < 0 || posX >= SECTION_SIZE)
				throw new IllegalArgumentException("X position is out of bounds");
			if (posZ < 0 || posZ >= SECTION_SIZE)
				throw new IllegalArgumentException("Z position is out of bounds");
		}
		
		int dataOffset = posX * SECTION_SIZE * this.verticalSize + posZ * this.verticalSize;
		int compare = ColumnFormat.compareDatapointPriority(newData.get(0), this.dataContainer[dataOffset]);
		if (overwriteDataWithSameGenerationMode)
		{
			if (compare < 0)
				return false;
		}
		else
		{
			if (compare <= 0)
				return false;
		}
		
		// copy the newData into this column's data
		newData.copyTo(this.dataContainer, dataOffset, newData.size());
		return true;
	}
	
	
	
	// TODO:
	
	@Override
	public long getDataPoint(int posX, int posZ, int verticalIndex) { return this.dataContainer[posX * SECTION_SIZE * this.verticalSize + posZ * this.verticalSize + verticalIndex]; }
	
	@Override
	public long[] getVerticalDataPointArray(int posX, int posZ)
	{
		long[] result = new long[this.verticalSize];
		int index = posX * SECTION_SIZE * this.verticalSize + posZ * this.verticalSize;
		System.arraycopy(this.dataContainer, index, result, 0, this.verticalSize);
		return result;
	}
	
	@Override
	public ColumnArrayView getVerticalDataPointView(int posX, int posZ)
	{
		return new ColumnArrayView(this.dataContainer, this.verticalSize,
				posX * SECTION_SIZE * this.verticalSize + posZ * this.verticalSize,
				this.verticalSize);
	}
	
	@Override
	public ColumnQuadView getFullQuadView() { return getQuadViewOverRange(0, 0, SECTION_SIZE, SECTION_SIZE); }
	@Override
	public ColumnQuadView getQuadViewOverRange(int quadX, int quadZ, int quadXSize, int quadZSize) { return new ColumnQuadView(this.dataContainer, SECTION_SIZE, this.verticalSize, quadX, quadZ, quadXSize, quadZSize); }
	
	@Override
	public int getVerticalSize() { return this.verticalSize; }
	
	
	
	//========================//
	// data update and output //
	//========================//
	
	/** @return true if this object had data written in every column */
	boolean writeData(DataOutputStream outputStream) throws IOException
	{
		outputStream.writeByte(getDataDetail());
		outputStream.writeByte((byte) this.verticalSize);
		
		if (isEmpty)
		{
			outputStream.writeByte(Short.MAX_VALUE & 0xFF);
			outputStream.writeByte((Short.MAX_VALUE >> 8) & 0xFF);
			
			return false;
		}
		else
		{
			// FIXME: yOffset is a int, but we only are writing a short.
			outputStream.writeByte((byte) (this.yOffset & 0xFF));
			outputStream.writeByte((byte) ((this.yOffset >> 8) & 0xFF));
			
			// write the data for each column
			boolean allGenerated = true;
			for (int x = 0; x < SECTION_SIZE * SECTION_SIZE; x++)
			{
				for (int z = 0; z < verticalSize; z++)
				{
					long currentDatapoint = dataContainer[x * verticalSize + z];
					if (ColumnFormat.doesDataPointExist(currentDatapoint))
					{
						// TODO: the "1" is a placeholder debug line
						currentDatapoint = ColumnFormat.overrideGenerationMode(currentDatapoint, (byte) 1);
					}
					outputStream.writeLong(Long.reverseBytes(currentDatapoint));
				}
				
				if (!ColumnFormat.doesDataPointExist(dataContainer[x]))
				{
					allGenerated = false;	
				}
			}
			
			return allGenerated;
		}
	}
	
	@Override
	public void updateFromRenderSource(ILodRenderSource source)
	{
		// TODO if we can only write this one type of data isn't it dangerous to have it in the interface?
		LodUtil.assertTrue(source instanceof ColumnRenderSource);
		ColumnRenderSource src = (ColumnRenderSource) source;
		
		// validate we are writing for the same location
		LodUtil.assertTrue(src.sectionPos.equals(this.sectionPos));
		// validate both objects have the same number of dataPoints
		LodUtil.assertTrue(src.verticalSize == this.verticalSize);
		
		
		if (src.isEmpty)
			// the source is empty, don't attempt to update anything
			return;
		// the source isn't empty, this object won't be empty after the method finishes
		this.isEmpty = false;
		
		
		for (int i = 0; i < this.dataContainer.length; i += this.verticalSize)
		{
			int thisGenMode = ColumnFormat.getGenerationMode(this.dataContainer[i]);
			int srcGenMode = ColumnFormat.getGenerationMode(src.dataContainer[i]);
			
			if (srcGenMode == 0)
				// the source hasn't been generated, don't write it
				continue;
			
			// this object's column is older than the source's column, update it
			if (thisGenMode <= srcGenMode)
			{
				ColumnArrayView thisColumnArrayView = new ColumnArrayView(this.dataContainer, this.verticalSize, i, this.verticalSize);
				ColumnArrayView srcColumnArrayView = new ColumnArrayView(src.dataContainer, src.verticalSize, i, src.verticalSize);
				thisColumnArrayView.copyFrom(srcColumnArrayView);
				
				this.debugSourceFlags[i / this.verticalSize] = src.debugSourceFlags[i / this.verticalSize];
			}
		}
	}
	
	@Override
	public void fastWrite(ChunkSizedData chunkData, IDhClientLevel level) { FullToColumnTransformer.writeFullDataChunkToColumnData(this, level, chunkData); }
	
	
	
	//=====================//
	// data helper methods //
	//=====================//
	
	@Override
	public boolean doesDataPointExist(int posX, int posZ) { return ColumnFormat.doesDataPointExist(this.getFirstDataPoint(posX, posZ)); }
	
	@Override
	public void generateData(IColumnDatatype lowerDataContainer, int posX, int posZ)
	{
		ColumnArrayView outputView = this.getVerticalDataPointView(posX, posZ);
		ColumnQuadView quadView = lowerDataContainer.getQuadViewOverRange(posX * 2, posZ * 2, 2, 2);
		outputView.mergeMultiDataFrom(quadView);
	}
	
	@Override
	public int getMaxLodCount() { return SECTION_SIZE * SECTION_SIZE * getVerticalSize(); }
	
	@Override
	public long getRoughRamUsageInBytes() { return (long) this.dataContainer.length * Long.BYTES; }
	
	public DhSectionPos getSectionPos() { return this.sectionPos; }
	
	public byte getDataDetail() { return (byte) (this.sectionPos.sectionDetail - SECTION_SIZE_OFFSET); }
	
	@Override
	public byte getDetailOffset() { return SECTION_SIZE_OFFSET; }
	
	
	
	//================//
	// Render Methods //
	//================//
	
	private void tryBuildBuffer(IDhClientLevel level, LodQuadTree quadTree)
	{
		if (this.inBuildRenderBuffer == null && !ColumnRenderBuffer.isBusy() && !this.isEmpty)
		{
			ColumnRenderSource[] data = new ColumnRenderSource[ELodDirection.ADJ_DIRECTIONS.length];
			for (ELodDirection direction : ELodDirection.ADJ_DIRECTIONS)
			{
				LodRenderSection section = quadTree.getSection(this.sectionPos.getAdjacentPos(direction)); //FIXME: Handle traveling through different detail levels
				if (section != null && section.getRenderSource() != null && section.getRenderSource() instanceof ColumnRenderSource)
				{
					data[direction.ordinal() - 2] = ((ColumnRenderSource) section.getRenderSource());
				}
			}
			this.inBuildRenderBuffer = ColumnRenderBuffer.build(level, this.usedBuffer, this, data);
		}
	}
	
	private void cancelBuildBuffer()
	{
		if (this.inBuildRenderBuffer != null)
		{
			//LOGGER.info("Cancelling build of render buffer for {}", sectionPos);
			this.inBuildRenderBuffer.cancel(true);
			this.inBuildRenderBuffer = null;
		}
	}
	
	@Override
	public void enableRender(IDhClientLevel level, LodQuadTree quadTree)
	{
		this.level = level;
		//tryBuildBuffer(level, quadTree);
	}
	
	@Override
	public void disableRender() { cancelBuildBuffer(); }
	
	@Override
	public void dispose() { cancelBuildBuffer(); }
	
	@Override
	public boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlot)
	{
		if (this.lastNs != -1 && System.nanoTime() - this.lastNs < SWAP_TIMEOUT)
		{
			return false;
		}
		
		if (this.inBuildRenderBuffer != null)
		{
			if (this.inBuildRenderBuffer.isDone())
			{
				this.lastNs = System.nanoTime();
				//LOGGER.info("Swapping render buffer for {}", sectionPos);
				RenderBuffer newBuffer = this.inBuildRenderBuffer.join();
				RenderBuffer oldBuffer = referenceSlot.getAndSet(newBuffer);
				if (oldBuffer instanceof ColumnRenderBuffer)
				{
					ColumnRenderBuffer swapped = this.usedBuffer.swap((ColumnRenderBuffer) oldBuffer);
					LodUtil.assertTrue(swapped == null);
				}
				this.inBuildRenderBuffer = null;
				return true;
			}
		}
		else
		{
			if (!this.isEmpty)
			{
				if (ColumnRenderBuffer.isBusy())
				{
					this.lastNs += (long) (SWAP_BUSY_COLLISION_TIMEOUT * Math.random());
				}
				else
				{
					this.tryBuildBuffer(this.level, quadTree);
				}
			}
		}
		return false;
	}
	
	@Override
	public void saveRender(IDhClientLevel level, RenderMetaFile file, OutputStream dataStream) throws IOException
	{
		DataOutputStream dos = new DataOutputStream(dataStream); // DO NOT CLOSE
		writeData(dos);
	}
	
	@Override
	public byte getRenderVersion() { return LATEST_VERSION; }
	
	@Override
	public boolean isValid() { return true; }
	
	@Override
	public boolean isEmpty() { return this.isEmpty; }
	public void markNotEmpty() { this.isEmpty = false; }
	
	
	
	//=======//
	// debug //
	//=======//
	
	/** Sets the debug flag for the given area */
	public void fillDebugFlag(int startX, int startZ, int width, int height, DebugSourceFlag flag)
	{
		for (int x = startX; x < startX + width; x++)
		{
			for (int z = startZ; z < startZ + height; z++)
			{
				debugSourceFlags[x * SECTION_SIZE + z] = flag;
			}
		}
	}
	
	public DebugSourceFlag debugGetFlag(int ox, int oz) { return debugSourceFlags[ox * SECTION_SIZE + oz]; }
	
	
	
	//==============//
	// base methods //
	//==============//
	
	@Override
	public String toString()
	{
		String LINE_DELIMITER = "\n";
		String DATA_DELIMITER = " ";
		String SUBDATA_DELIMITER = ",";
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append(sectionPos);
		stringBuilder.append(LINE_DELIMITER);
		
		int size = sectionPos.getWidth().numberOfLodSectionsWide;
		for (int z = 0; z < size; z++)
		{
			for (int x = 0; x < size; x++)
			{
				for (int y = 0; y < verticalSize; y++)
				{
					//Converting the dataToHex
					stringBuilder.append(Long.toHexString(getDataPoint(x, z, y)));
					if (y != verticalSize - 1)
						stringBuilder.append(SUBDATA_DELIMITER);
				}
				
				if (x != size - 1)
					stringBuilder.append(DATA_DELIMITER);
			}
			
			if (z != size - 1)
				stringBuilder.append(LINE_DELIMITER);
		}
		return stringBuilder.toString();
	}
	
	
	
	//==============//
	// helper enums //
	//==============//
	
	public enum DebugSourceFlag
	{
		FULL(ColorUtil.BLUE),
		DIRECT(ColorUtil.WHITE),
		SPARSE(ColorUtil.YELLOW),
		FILE(ColorUtil.BROWN);
		
		public final int color;
		
		DebugSourceFlag(int color) { this.color = color; }
	}
	
}
