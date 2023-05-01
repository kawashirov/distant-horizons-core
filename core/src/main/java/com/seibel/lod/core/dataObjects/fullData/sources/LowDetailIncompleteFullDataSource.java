package com.seibel.lod.core.dataObjects.fullData.sources;

import com.seibel.lod.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.lod.core.dataObjects.fullData.accessor.FullDataArrayAccessor;
import com.seibel.lod.core.dataObjects.fullData.accessor.SingleColumnFullDataAccessor;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.FullDataPointUtil;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.BitSet;

/**
 * Used for large incomplete LOD blocks. <Br>
 * Handles incomplete full data with a detail level higher than 
 * {@link HighDetailIncompleteFullDataSource#MAX_SECTION_DETAIL}. <br><br>
 *  
 * Formerly "SpottyFullDataSource".
 * 
 * @see HighDetailIncompleteFullDataSource
 * @see CompleteFullDataSource
 * @see FullDataPointUtil
 */
public class LowDetailIncompleteFullDataSource extends FullDataArrayAccessor implements IIncompleteFullDataSource, IStreamableFullDataSource<IStreamableFullDataSource.FullDataSourceSummaryData, long[][]>
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final byte SECTION_SIZE_OFFSET = 6;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET; // TODO this is width not size
    public static final byte LATEST_VERSION = 0; // TODO rename to data format version
    public static final long TYPE_ID = "LowDetailIncompleteFullDataSource".hashCode();
	
    private final DhSectionPos sectionPos;
	private final BitSet isColumnNotEmpty;
	
    private boolean isEmpty = true;
	public EDhApiWorldGenerationStep worldGenStep = EDhApiWorldGenerationStep.EMPTY;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public static LowDetailIncompleteFullDataSource createEmpty(DhSectionPos pos) { return new LowDetailIncompleteFullDataSource(pos); }
    private LowDetailIncompleteFullDataSource(DhSectionPos sectionPos)
	{
        super(new FullDataPointIdMap(), new long[SECTION_SIZE*SECTION_SIZE][0], SECTION_SIZE);
        LodUtil.assertTrue(sectionPos.sectionDetailLevel > HighDetailIncompleteFullDataSource.MAX_SECTION_DETAIL);
		
        this.sectionPos = sectionPos;
		this.isColumnNotEmpty = new BitSet(SECTION_SIZE*SECTION_SIZE);
		this.worldGenStep = EDhApiWorldGenerationStep.EMPTY;
    }
	
	private LowDetailIncompleteFullDataSource(DhSectionPos pos, FullDataPointIdMap mapping, EDhApiWorldGenerationStep worldGenStep, BitSet isColumnNotEmpty, long[][] data)
	{
		super(mapping, data, SECTION_SIZE);
		LodUtil.assertTrue(data.length == SECTION_SIZE*SECTION_SIZE);
		
		this.sectionPos = pos;
		this.isColumnNotEmpty = isColumnNotEmpty;
		this.worldGenStep = worldGenStep;
		this.isEmpty = false;
	}
	
	
	
	//=================//
	// stream handling //
	//=================//
	
	
	@Override
	public void writeSourceSummaryInfo(IDhLevel level, BufferedOutputStream bufferedOutputStream) throws IOException
	{
		DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream); // Don't close, this stream is handled outside this method
		
		
		dataOutputStream.writeInt(this.getDataDetailLevel());
		dataOutputStream.writeInt(this.width);
		dataOutputStream.writeInt(level.getMinY());
		dataOutputStream.writeByte(this.worldGenStep.value);
		
	}
	@Override
	public FullDataSourceSummaryData readSourceSummaryInfo(FullDataMetaFile dataFile, BufferedInputStream bufferedInputStream, IDhLevel level) throws IOException
	{
		DataInputStream dataInputStream = new DataInputStream(bufferedInputStream); // DO NOT CLOSE
		
		
		int dataDetail = dataInputStream.readInt();
		if(dataDetail != dataFile.metaData.dataLevel)
		{
			throw new IOException(LodUtil.formatLog("Data level mismatch: "+dataDetail+" != "+dataFile.metaData.dataLevel));
		}
		
		int width = dataInputStream.readInt();
		if (width != SECTION_SIZE)
		{
			throw new IOException(LodUtil.formatLog("Section size mismatch: "+width+" != "+SECTION_SIZE+" (Currently only 1 section size is supported)"));
		}
		
		int minY = dataInputStream.readInt();
		if (minY != level.getMinY())
		{
			LOGGER.warn("Data minY mismatch: "+minY+" != "+level.getMinY()+". Will ignore data's y level");
		}
		
		EDhApiWorldGenerationStep worldGenStep = EDhApiWorldGenerationStep.fromValue(dataInputStream.readByte());
		if (worldGenStep == null)
		{
			worldGenStep = EDhApiWorldGenerationStep.SURFACE;
			LOGGER.warn("Missing WorldGenStep, defaulting to: "+worldGenStep.name());
		}
		
		
		return new FullDataSourceSummaryData(this.width, worldGenStep);
	}
	public void setSourceSummaryData(FullDataSourceSummaryData summaryData)
	{
		this.worldGenStep = summaryData.worldGenStep;
	}
	
	
	@Override
	public boolean writeDataPoints(BufferedOutputStream bufferedOutputStream) throws IOException
	{
		DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream); // Don't close, this stream is handled outside this method
		
		
		if (this.isEmpty)
		{
			dataOutputStream.writeInt(IFullDataSource.NO_DATA_FLAG_BYTE);
			return false;
		}
		dataOutputStream.writeInt(IFullDataSource.DATA_GUARD_BYTE);
		
		
		// data column presence
		byte[] bytes = this.isColumnNotEmpty.toByteArray();
		dataOutputStream.writeInt(bytes.length);
		dataOutputStream.write(bytes);
		
		
		// Data content
		dataOutputStream.writeInt(IFullDataSource.DATA_GUARD_BYTE);
		for (int i = this.isColumnNotEmpty.nextSetBit(0); i >= 0; i = this.isColumnNotEmpty.nextSetBit(i + 1))
		{
			dataOutputStream.writeByte(this.dataArrays[i].length);
			for (long dataPoint : this.dataArrays[i])
			{
				dataOutputStream.writeLong(dataPoint);
			}
		}
		
		
		return true;
	}
	@Override
	public long[][] readDataPoints(FullDataMetaFile dataFile, int width, BufferedInputStream bufferedInputStream) throws IOException
	{
		DataInputStream dataInputStream = new DataInputStream(bufferedInputStream); // DO NOT CLOSE
		
		
		
		// is source empty flag
		int dataPresentFlag = dataInputStream.readInt();
		if (dataPresentFlag == IFullDataSource.NO_DATA_FLAG_BYTE)
		{
			// Section is empty
			return null;
		}
		else if (dataPresentFlag != IFullDataSource.DATA_GUARD_BYTE)
		{
			throw new IOException("Invalid file format. Data Points guard byte expected: (no data) ["+IFullDataSource.NO_DATA_FLAG_BYTE+"] or (data present) ["+IFullDataSource.DATA_GUARD_BYTE+"], but found ["+dataPresentFlag+"].");
		}
		
		
		// data column presence
		int length = dataInputStream.readInt();
		if (length < 0 || length > (SECTION_SIZE*SECTION_SIZE/8+64)*2) // TODO replace magic numbers or comment what they mean
		{
			throw new IOException(LodUtil.formatLog("Spotty Flag BitSet size outside reasonable range: {} (expects {} to {})",
					length, 1, SECTION_SIZE * SECTION_SIZE / 8 + 63));
		}
		
		byte[] bytes = new byte[length];
		dataInputStream.readFully(bytes, 0, length);
		BitSet isColumnNotEmpty = BitSet.valueOf(bytes);
		
		
		
		// Data array content
		long[][] dataPointArray = new long[SECTION_SIZE*SECTION_SIZE][];
		dataPresentFlag = dataInputStream.readInt();
		if (dataPresentFlag != IFullDataSource.DATA_GUARD_BYTE)
		{
			throw new IOException("invalid spotty flag end guard");
		}
		
		for (int xz = isColumnNotEmpty.nextSetBit(0); xz >= 0; xz = isColumnNotEmpty.nextSetBit(xz + 1))
		{
			long[] array = new long[dataInputStream.readByte()];
			for (int y = 0; y < array.length; y++)
			{
				array[y] = dataInputStream.readLong();
			}
			dataPointArray[xz] = array;
		}
		
		
		return dataPointArray;
	}
	@Override
	public void setDataPoints(long[][] dataPoints)
	{
		LodUtil.assertTrue(this.dataArrays.length == dataPoints.length, "Data point array length mismatch.");
		
		this.isEmpty = false;
		System.arraycopy(dataPoints, 0, this.dataArrays, 0, dataPoints.length);
	}
	
	
	@Override
	public void writeIdMappings(BufferedOutputStream bufferedOutputStream) throws IOException
	{
		DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream); // Don't close, this stream is handled outside this method
		
		
		dataOutputStream.writeInt(IFullDataSource.DATA_GUARD_BYTE);
		this.mapping.serialize(bufferedOutputStream);
		
	}
	@Override
	public FullDataPointIdMap readIdMappings(long[][] dataPoints, BufferedInputStream bufferedInputStream) throws IOException, InterruptedException
	{
		DataInputStream dataInputStream = new DataInputStream(bufferedInputStream); // Don't close, this stream is handled outside this method
		
		
		// Id mapping
		int dataPresentFlag = dataInputStream.readInt();
		if (dataPresentFlag != IFullDataSource.DATA_GUARD_BYTE)
		{
			throw new IOException("invalid ID mapping end guard");
		}
		return FullDataPointIdMap.deserialize(bufferedInputStream);
	}
	@Override
	public void setIdMapping(FullDataPointIdMap mappings) { this.mapping.mergeAndReturnRemappedEntityIds(mappings); }
	
	
	
	//===============//
	// Data updating //
	//===============//
	
	@Override
	public void update(ChunkSizedFullDataAccessor data)
	{
		LodUtil.assertTrue(this.sectionPos.getSectionBBoxPos().overlapsExactly(data.getLodPos()));
		
		if (this.getDataDetailLevel() >= 4)
		{
			//FIXME: TEMPORARY
			int chunkPerFull = 1 << (this.getDataDetailLevel() - 4);
			if (data.pos.x % chunkPerFull != 0 || data.pos.z % chunkPerFull != 0)
			{
				return;
			}
			
			DhLodPos baseOffset = this.sectionPos.getCorner(this.getDataDetailLevel());
			DhLodPos dataOffset = data.getLodPos().convertToDetailLevel(this.getDataDetailLevel());
			int offsetX = dataOffset.x - baseOffset.x;
			int offsetZ = dataOffset.z - baseOffset.z;
			LodUtil.assertTrue(offsetX >= 0 && offsetX < SECTION_SIZE && offsetZ >= 0 && offsetZ < SECTION_SIZE);
			this.isEmpty = false;
			data.get(0,0).deepCopyTo(this.get(offsetX, offsetZ));
		}
		else
		{
			LodUtil.assertNotReach();
			//TODO;
		}
		
	}
	
	@Override
    public void sampleFrom(IFullDataSource fullDataSource)
	{
        DhSectionPos pos = fullDataSource.getSectionPos();
        LodUtil.assertTrue(pos.sectionDetailLevel < this.sectionPos.sectionDetailLevel);
        LodUtil.assertTrue(pos.overlaps(this.sectionPos));
		
        if (fullDataSource.isEmpty())
		{
			return;
		}
		
		
        if (fullDataSource instanceof HighDetailIncompleteFullDataSource)
		{
			this.sampleFrom((HighDetailIncompleteFullDataSource) fullDataSource);
        }
		else if (fullDataSource instanceof CompleteFullDataSource)
		{
			this.sampleFrom((CompleteFullDataSource) fullDataSource);
        }
		else
		{
			LodUtil.assertNotReach("SampleFrom not implemented for ["+IFullDataSource.class.getSimpleName()+"] with class ["+fullDataSource.getClass().getSimpleName()+"].");
        }
    }
	
    private void sampleFrom(HighDetailIncompleteFullDataSource sparseSource)
	{
		DhLodPos thisLodPos = this.sectionPos.getCorner(this.getDataDetailLevel());
        DhSectionPos pos = sparseSource.getSectionPos();
		
		this.isEmpty = false;
		
        if (this.getDataDetailLevel() > this.sectionPos.sectionDetailLevel)
		{
            DhLodPos dataLodPos = pos.getCorner(this.getDataDetailLevel());
            
			int offsetX = dataLodPos.x - thisLodPos.x;
            int offsetZ = dataLodPos.z - thisLodPos.z;
            LodUtil.assertTrue(offsetX >= 0 && offsetX < SECTION_SIZE && offsetZ >= 0 && offsetZ < SECTION_SIZE);
            
			int chunksPerData = 1 << (this.getDataDetailLevel() - HighDetailIncompleteFullDataSource.SPARSE_UNIT_DETAIL);
            int dataSpan = this.sectionPos.getWidth(this.getDataDetailLevel()).numberOfLodSectionsWide;

            for (int xOffset = 0; xOffset < dataSpan; xOffset++)
			{
                for (int zOffset = 0; zOffset < dataSpan; zOffset++)
				{
                    SingleColumnFullDataAccessor column = sparseSource.tryGet(
                            xOffset * chunksPerData * sparseSource.dataPerChunk,
                            zOffset * chunksPerData * sparseSource.dataPerChunk);
					
                    if (column != null)
					{
                        column.deepCopyTo(this.get(offsetX + xOffset, offsetZ + zOffset));
						this.isColumnNotEmpty.set((offsetX + xOffset) * SECTION_SIZE + offsetZ + zOffset, true);
                    }
                }
            }
        }
		else
		{
            DhLodPos dataLodPos = pos.getSectionBBoxPos();
            int lowerSectionsPerData = this.sectionPos.getWidth(dataLodPos.detailLevel).numberOfLodSectionsWide;
            if (dataLodPos.x % lowerSectionsPerData != 0 || dataLodPos.z % lowerSectionsPerData != 0)
			{
				return;
			}
			
			
            dataLodPos = dataLodPos.convertToDetailLevel(this.getDataDetailLevel());
            int offsetX = dataLodPos.x - thisLodPos.x;
            int offsetZ = dataLodPos.z - thisLodPos.z;
            
			SingleColumnFullDataAccessor column = sparseSource.tryGet(0, 0);
            if (column != null)
			{
                column.deepCopyTo(this.get(offsetX, offsetZ));
				this.isColumnNotEmpty.set(offsetX * SECTION_SIZE + offsetZ, true);
            }
        }
    }
	
    private void sampleFrom(CompleteFullDataSource completeSource)
	{
        DhSectionPos pos = completeSource.getSectionPos();
		this.isEmpty = false;
		this.downsampleFrom(completeSource);
		
		if (this.getDataDetailLevel() > this.sectionPos.sectionDetailLevel) // TODO what does this mean?
		{
			DhLodPos thisLodPos = this.sectionPos.getCorner(this.getDataDetailLevel());
			DhLodPos dataLodPos = pos.getCorner(this.getDataDetailLevel());
			
			int offsetX = dataLodPos.x - thisLodPos.x;
			int offsetZ = dataLodPos.z - thisLodPos.z;
			int dataWidth = this.sectionPos.getWidth(this.getDataDetailLevel()).numberOfLodSectionsWide;
			
			for (int xOffset = 0; xOffset < dataWidth; xOffset++)
			{
				for (int zOffset = 0; zOffset < dataWidth; zOffset++)
				{
					this.isColumnNotEmpty.set((offsetX + xOffset) * SECTION_SIZE + offsetZ + zOffset, true);
				}
			}
		}
		else
		{
            DhLodPos dataPos = pos.getSectionBBoxPos();
            int lowerSectionsPerData = this.sectionPos.getWidth(dataPos.detailLevel).numberOfLodSectionsWide;
            if (dataPos.x % lowerSectionsPerData != 0 || dataPos.z % lowerSectionsPerData != 0)
			{
				return;
			}
			
			
            DhLodPos basePos = this.sectionPos.getCorner(this.getDataDetailLevel());
            dataPos = dataPos.convertToDetailLevel(this.getDataDetailLevel());
            int offsetX = dataPos.x - basePos.x;
            int offsetZ = dataPos.z - basePos.z;
			this.isColumnNotEmpty.set(offsetX * SECTION_SIZE + offsetZ, true);
        }

    }
	
    @Override
    public IFullDataSource tryPromotingToCompleteDataSource()
	{
		// promotion can only be completed if every column has data
        if (this.isEmpty)
		{
			return this;
		}
        else if (this.isColumnNotEmpty.cardinality() != SECTION_SIZE * SECTION_SIZE)
		{
			return this;
		}
		
        return new CompleteFullDataSource(this.sectionPos, this.mapping, this.dataArrays);
    }
	
	
	
	//======//
	// data //
	//======//
	
    @Override
    public SingleColumnFullDataAccessor tryGet(int relativeX, int relativeZ) { return this.isColumnNotEmpty.get(relativeX * SECTION_SIZE + relativeZ) ? this.get(relativeX, relativeZ) : null; }
	
	
	
	//=====================//
	// getters and setters //
	//=====================//
	
	@Override
	public DhSectionPos getSectionPos() { return this.sectionPos; }
	@Override
	public byte getDataDetailLevel() { return (byte) (this.sectionPos.sectionDetailLevel - SECTION_SIZE_OFFSET); }
	@Override
	public byte getDataVersion() { return LATEST_VERSION;  }
	
	@Override
	public EDhApiWorldGenerationStep getWorldGenStep() { return this.worldGenStep; }
	
	@Override
	public boolean isEmpty() { return this.isEmpty; }
	public void markNotEmpty() { this.isEmpty = false;  }
	
	
	
	//========//
	// unused //
	//========//
	
	public static boolean neededForPosition(DhSectionPos posToWrite, DhSectionPos posToTest)
	{
		if (!posToWrite.overlaps(posToTest))
			return false;
		if (posToTest.sectionDetailLevel > posToWrite.sectionDetailLevel)
			return false;
		if (posToWrite.sectionDetailLevel - posToTest.sectionDetailLevel <= SECTION_SIZE_OFFSET)
			return true;
		byte sectPerData = (byte) (1 << (posToWrite.sectionDetailLevel - posToTest.sectionDetailLevel - SECTION_SIZE_OFFSET));
		return posToTest.sectionX % sectPerData == 0 && posToTest.sectionZ % sectPerData == 0;
	}
	
	
}
