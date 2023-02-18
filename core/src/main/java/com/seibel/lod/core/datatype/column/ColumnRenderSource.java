package com.seibel.lod.core.datatype.column;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.datatype.column.accessor.*;
import com.seibel.lod.core.datatype.column.render.ColumnRenderBuffer;
import com.seibel.lod.core.datatype.full.ChunkSizedFullData;
import com.seibel.lod.core.datatype.transform.FullToColumnTransformer;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.AbstractRenderBuffer;
import com.seibel.lod.core.file.renderfile.RenderMetaDataFile;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.render.LodRenderSection;
import com.seibel.lod.core.datatype.IRenderSource;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.objects.Reference;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stores the color data used when generating OpenGL buffers.
 * 
 * @author Leetom
 * @version 2022-2-7
 */
public class ColumnRenderSource implements IRenderSource, IColumnDatatype
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	public static final boolean DO_SAFETY_CHECKS = ModInfo.IS_DEV_BUILD;
	public static final byte SECTION_SIZE_OFFSET = 6;
	public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
	public static final byte LATEST_VERSION = 1;
	public static final long TYPE_ID = "ColumnRenderSource".hashCode();
	public static final int AIR_LODS_SIZE = 16;
	public static final int AIR_SECTION_SIZE = SECTION_SIZE / AIR_LODS_SIZE;
	
	/**
	 * This is the byte put between different sections in the binary save file.
	 * The presence and absence of this byte indicates if the file is correctly formatted.  
	 */
	public static final int DATA_GUARD_BYTE = 0xFFFFFFFF;
	/** indicates the binary save file represents an empty data source */
	public static final int NO_DATA_FLAG_BYTE = 0x00000001;
	
	
	
	public int verticalDataCount;
	public final DhSectionPos sectionPos;
	public final int yOffset;
	
	public long[] dataContainer;
	public int[] airDataContainer;
	
	public final DebugSourceFlag[] debugSourceFlags;
	
	private boolean isEmpty = true;
	
	private IDhClientLevel level = null; //FIXME: hack to pass level into tryBuildBuffer
	
	//FIXME: Temp Hack
	private long lastNs = -1;
	/** 10 sec */
	private static final long SWAP_TIMEOUT = 10_000000000L;
	/** 1 sec */
	private static final long SWAP_BUSY_COLLISION_TIMEOUT = 1_000000000L;
	
	private CompletableFuture<ColumnRenderBuffer> buildRenderBufferFuture = null;
	private final Reference<ColumnRenderBuffer> columnRenderBufferRef = new Reference<>();
	
	
	
	
	/**
	 * Creates an empty ColumnRenderSource.
	 * 
	 * @param sectionPos the relative position of the container
	 * @param maxVerticalSize the maximum vertical size of the container
	 */
	public ColumnRenderSource(DhSectionPos sectionPos, int maxVerticalSize, int yOffset)
	{
		this.verticalDataCount = maxVerticalSize;
		this.dataContainer = new long[SECTION_SIZE * SECTION_SIZE * this.verticalDataCount];
		this.airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * this.verticalDataCount];
		this.debugSourceFlags = new DebugSourceFlag[SECTION_SIZE * SECTION_SIZE];
		this.sectionPos = sectionPos;
		this.yOffset = yOffset;
	}
	
	/**
	 * Creates a new ColumnRenderSource from the parsedColumnData.
	 * 
	 * @throws IOException if the DataInputStream's detail level isn't what was expected
	 */
	public ColumnRenderSource(DhSectionPos sectionPos, ColumnRenderLoader.ParsedColumnData parsedColumnData, IDhLevel level) throws IOException
	{
		if (sectionPos.sectionDetailLevel - SECTION_SIZE_OFFSET != parsedColumnData.detailLevel)
		{
			throw new IOException("Invalid data: detail level does not match");
		}
		
		this.sectionPos = sectionPos;
		this.yOffset = level.getMinY();
		this.verticalDataCount = parsedColumnData.verticalSize;
		this.dataContainer = parsedColumnData.dataContainer;
		this.airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * this.verticalDataCount];
		
		this.debugSourceFlags = new DebugSourceFlag[SECTION_SIZE * SECTION_SIZE];
		this.fillDebugFlag(0, 0, SECTION_SIZE, SECTION_SIZE, DebugSourceFlag.FILE);
	}
	
	
	
	//========================//
	// datapoint manipulation //
	//========================//
	
	@Override
	public void clearDataPoint(int posX, int posZ)
	{
		for (int verticalIndex = 0; verticalIndex < this.verticalDataCount; verticalIndex++)
		{
			this.dataContainer[posX * SECTION_SIZE * this.verticalDataCount + posZ * this.verticalDataCount + verticalIndex] = ColumnFormat.EMPTY_DATA;
		}
	}
	
	@Override
	public boolean setDataPoint(long data, int posX, int posZ, int verticalIndex)
	{
		this.dataContainer[posX * SECTION_SIZE * this.verticalDataCount + posZ * this.verticalDataCount + verticalIndex] = data;
		return true;
	}
	
	@Override
	public boolean copyVerticalData(IColumnDataView newData, int posX, int posZ, boolean overwriteDataWithSameGenerationMode)
	{
		if (DO_SAFETY_CHECKS)
		{
			if (newData.size() != this.verticalDataCount)
				throw new IllegalArgumentException("newData size not the same as this column's vertical size");
			if (posX < 0 || posX >= SECTION_SIZE)
				throw new IllegalArgumentException("X position is out of bounds");
			if (posZ < 0 || posZ >= SECTION_SIZE)
				throw new IllegalArgumentException("Z position is out of bounds");
		}
		
		int dataOffset = posX * SECTION_SIZE * this.verticalDataCount + posZ * this.verticalDataCount;
		int compare = ColumnFormat.compareDatapointPriority(newData.get(0), this.dataContainer[dataOffset]);
		if (overwriteDataWithSameGenerationMode)
		{
			if (compare < 0)
			{
				return false;
			}
		}
		else
		{
			if (compare <= 0)
			{
				return false;
			}
		}
		
		// copy the newData into this column's data
		newData.copyTo(this.dataContainer, dataOffset, newData.size());
		return true;
	}
	
	
	
	// TODO:
	
	@Override
	public long getDataPoint(int posX, int posZ, int verticalIndex) { return this.dataContainer[posX * SECTION_SIZE * this.verticalDataCount + posZ * this.verticalDataCount + verticalIndex]; }
	
	@Override
	public long[] getVerticalDataPointArray(int posX, int posZ)
	{
		long[] result = new long[this.verticalDataCount];
		int index = posX * SECTION_SIZE * this.verticalDataCount + posZ * this.verticalDataCount;
		System.arraycopy(this.dataContainer, index, result, 0, this.verticalDataCount);
		return result;
	}
	
	@Override
	public ColumnArrayView getVerticalDataPointView(int posX, int posZ)
	{
		return new ColumnArrayView(this.dataContainer, this.verticalDataCount,
				posX * SECTION_SIZE * this.verticalDataCount + posZ * this.verticalDataCount,
				this.verticalDataCount);
	}
	
	@Override
	public ColumnQuadView getFullQuadView() { return this.getQuadViewOverRange(0, 0, SECTION_SIZE, SECTION_SIZE); }
	@Override
	public ColumnQuadView getQuadViewOverRange(int quadX, int quadZ, int quadXSize, int quadZSize) { return new ColumnQuadView(this.dataContainer, SECTION_SIZE, this.verticalDataCount, quadX, quadZ, quadXSize, quadZSize); }
	
	@Override
	public int getVerticalSize() { return this.verticalDataCount; }
	
	
	
	//========================//
	// data update and output //
	//========================//
	
	void writeData(DataOutputStream outputStream) throws IOException
	{
		outputStream.writeByte(this.getDataDetail());
		outputStream.writeInt(this.verticalDataCount);
		
		if (this.isEmpty)
		{
			// no data is present
			outputStream.writeByte(NO_DATA_FLAG_BYTE);
		}
		else
		{
			// data is present
			outputStream.writeByte(DATA_GUARD_BYTE);
			
			outputStream.writeInt(this.yOffset);
			
			// write the data for each column
			for (int xz = 0; xz < SECTION_SIZE * SECTION_SIZE; xz++)
			{
				for (int y = 0; y < this.verticalDataCount; y++)
				{
					long currentDatapoint = this.dataContainer[xz * this.verticalDataCount + y];
					outputStream.writeLong(Long.reverseBytes(currentDatapoint)); // the reverse bytes is necessary to ensure the data is read in correctly
				}
			}
		}
	}
	
	@Override
	public void updateFromRenderSource(IRenderSource source)
	{
		// TODO if we can only write this one type of data isn't it dangerous to have it in the interface?
		LodUtil.assertTrue(source instanceof ColumnRenderSource);
		ColumnRenderSource src = (ColumnRenderSource) source;
		
		// validate we are writing for the same location
		LodUtil.assertTrue(src.sectionPos.equals(this.sectionPos));
		
		// change the vertical size if necessary (this can happen if the vertical quality was changed in the config) 
		this.clearAndChangeVerticalSize(src.verticalDataCount);
		// validate both objects have the same number of dataPoints
		LodUtil.assertTrue(src.verticalDataCount == this.verticalDataCount);
		
		
		if (src.isEmpty)
		{
			// the source is empty, don't attempt to update anything
			return;
		}
		
		// the source isn't empty, this object won't be empty after the method finishes
		this.isEmpty = false;
		
		
		for (int i = 0; i < this.dataContainer.length; i += this.verticalDataCount)
		{
			int thisGenMode = ColumnFormat.getGenerationMode(this.dataContainer[i]);
			int srcGenMode = ColumnFormat.getGenerationMode(src.dataContainer[i]);
			
			if (srcGenMode == 0)
			{
				// the source hasn't been generated, don't write it
				continue;
			}
			
			// this object's column is older than the source's column, update it
			if (thisGenMode <= srcGenMode)
			{
				ColumnArrayView thisColumnArrayView = new ColumnArrayView(this.dataContainer, this.verticalDataCount, i, this.verticalDataCount);
				ColumnArrayView srcColumnArrayView = new ColumnArrayView(src.dataContainer, src.verticalDataCount, i, src.verticalDataCount);
				thisColumnArrayView.copyFrom(srcColumnArrayView);
				
				this.debugSourceFlags[i / this.verticalDataCount] = src.debugSourceFlags[i / this.verticalDataCount];
			}
		}
	}
	/** 
	 * If the newVerticalSize is different than the current verticalSize,
	 * this will delete any data currently in this object and re-size it. <Br>
	 * Otherwise this method will do nothing.
	 */
	private void clearAndChangeVerticalSize(int newVerticalSize)
	{
		if (newVerticalSize != this.verticalDataCount)
		{
			this.verticalDataCount = newVerticalSize;
			this.dataContainer = new long[SECTION_SIZE * SECTION_SIZE * this.verticalDataCount];
			this.airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * this.verticalDataCount];
		}
	}
	
	@Override
	public void fastWrite(ChunkSizedFullData chunkData, IDhClientLevel level) { FullToColumnTransformer.writeFullDataChunkToColumnData(this, level, chunkData); }
	
	
	
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
	public int getMaxLodCount() { return SECTION_SIZE * SECTION_SIZE * this.getVerticalSize(); }
	
	@Override
	public long getRoughRamUsageInBytes() { return (long) this.dataContainer.length * Long.BYTES; }
	
	public DhSectionPos getSectionPos() { return this.sectionPos; }
	
	public byte getDataDetail() { return (byte) (this.sectionPos.sectionDetailLevel - SECTION_SIZE_OFFSET); }
	
	@Override
	public byte getDetailOffset() { return SECTION_SIZE_OFFSET; }
	
	
	
	//================//
	// Render Methods //
	//================//
	
	private void tryBuildBuffer(IDhClientLevel level, LodQuadTree quadTree)
	{
		if (this.buildRenderBufferFuture == null && !ColumnRenderBuffer.isBusy() && !this.isEmpty)
		{
			ColumnRenderSource[] columnRenderSources = new ColumnRenderSource[ELodDirection.ADJ_DIRECTIONS.length];
			for (ELodDirection direction : ELodDirection.ADJ_DIRECTIONS)
			{
				LodRenderSection renderSection = quadTree.getSection(this.sectionPos.getAdjacentPos(direction)); //FIXME: Handle traveling through different detail levels
				if (renderSection != null && renderSection.getRenderSource() != null && renderSection.getRenderSource() instanceof ColumnRenderSource)
				{
					columnRenderSources[direction.ordinal() - 2] = ((ColumnRenderSource) renderSection.getRenderSource());
					//LOGGER.info("attempting to build buffer for: "+renderSection.pos);
				}
			}
			this.buildRenderBufferFuture = ColumnRenderBuffer.build(level, this.columnRenderBufferRef, this, columnRenderSources);
		}
	}
	
	private void cancelBuildBuffer()
	{
		if (this.buildRenderBufferFuture != null)
		{
			//LOGGER.info("Cancelling build of render buffer for {}", sectionPos);
			this.buildRenderBufferFuture.cancel(true);
			this.buildRenderBufferFuture = null;
		}
	}
	
	@Override
	public void enableRender(IDhClientLevel level, LodQuadTree quadTree)
	{
		this.level = level;
		//this.tryBuildBuffer(level, quadTree); // FIXME why was this commented out?
	}
	
	@Override
	public void disableRender() { this.cancelBuildBuffer(); }
	
	@Override
	public void dispose() { this.cancelBuildBuffer(); }
	
	@Override
	public boolean trySwapRenderBufferAsync(LodQuadTree quadTree, AtomicReference<AbstractRenderBuffer> renderBufferToSwap)
	{
		// prevent swapping the buffer to quickly
		if (this.lastNs != -1 && System.nanoTime() - this.lastNs < SWAP_TIMEOUT)
		{
			return false;
		}
		
		
		if (this.buildRenderBufferFuture != null)
		{
			if (this.buildRenderBufferFuture.isDone())
			{
				this.lastNs = System.nanoTime();
				//LOGGER.info("Swapping render buffer for {}", sectionPos);
				
				AbstractRenderBuffer newBuffer = this.buildRenderBufferFuture.join();
				AbstractRenderBuffer oldBuffer = renderBufferToSwap.getAndSet(newBuffer);
				if (oldBuffer instanceof ColumnRenderBuffer)
				{
					ColumnRenderBuffer swapped = this.columnRenderBufferRef.swap((ColumnRenderBuffer) oldBuffer);
					LodUtil.assertTrue(swapped == null);
				}
				else if (oldBuffer != null)
				{
					throw new UnsupportedOperationException("swap buffer fail, Expected "+AbstractRenderBuffer.class.getSimpleName()+" of type: "+ColumnRenderBuffer.class.getSimpleName()+" class given: "+oldBuffer.getClass().getSimpleName());
				}
				
				this.buildRenderBufferFuture = null;
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
	public void saveRender(IDhClientLevel level, RenderMetaDataFile file, OutputStream dataStream) throws IOException
	{
		DataOutputStream dos = new DataOutputStream(dataStream); // DO NOT CLOSE
		this.writeData(dos);
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
				this.debugSourceFlags[x * SECTION_SIZE + z] = flag;
			}
		}
	}
	
	public DebugSourceFlag debugGetFlag(int ox, int oz) { return this.debugSourceFlags[ox * SECTION_SIZE + oz]; }
	
	
	
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
		
		stringBuilder.append(this.sectionPos);
		stringBuilder.append(LINE_DELIMITER);
		
		int size = this.sectionPos.getWidth().numberOfLodSectionsWide;
		for (int z = 0; z < size; z++)
		{
			for (int x = 0; x < size; x++)
			{
				for (int y = 0; y < this.verticalDataCount; y++)
				{
					//Converting the dataToHex
					stringBuilder.append(Long.toHexString(this.getDataPoint(x, z, y)));
					if (y != this.verticalDataCount - 1)
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
