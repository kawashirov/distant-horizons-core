package com.seibel.lod.core.dataObjects.fullData.sources;

import com.seibel.lod.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.lod.core.dataObjects.fullData.FullDataPointIdMap;
import com.seibel.lod.core.dataObjects.fullData.accessor.FullArrayView;
import com.seibel.lod.core.dataObjects.fullData.accessor.SingleFullArrayView;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.BitSet;

/**
 * 1 chunk of full data (formerly SpottyDataSource)
 */
public class SingleChunkFullDataSource extends FullArrayView implements IIncompleteFullDataSource
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final byte SECTION_SIZE_OFFSET = 6;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
    public static final byte LATEST_VERSION = 0;
    public static final long TYPE_ID = "SingleChunkFullDataSource".hashCode();
	
    private final DhSectionPos sectionPos;
	private final BitSet isColumnNotEmpty;
	
    private boolean isEmpty = true;
	public EDhApiWorldGenerationStep worldGenStep = EDhApiWorldGenerationStep.EMPTY;
	
	
	
	//==============//
	// constructors //
	//==============//
	
    protected SingleChunkFullDataSource(DhSectionPos sectionPos)
	{
        super(new FullDataPointIdMap(), new long[SECTION_SIZE*SECTION_SIZE][0], SECTION_SIZE);
        LodUtil.assertTrue(sectionPos.sectionDetailLevel > SparseFullDataSource.MAX_SECTION_DETAIL);
		
        this.sectionPos = sectionPos;
		this.isColumnNotEmpty = new BitSet(SECTION_SIZE*SECTION_SIZE);
		this.worldGenStep = EDhApiWorldGenerationStep.EMPTY;
    }
	
	private SingleChunkFullDataSource(DhSectionPos pos, FullDataPointIdMap mapping, EDhApiWorldGenerationStep worldGenStep, BitSet isColumnNotEmpty, long[][] data)
	{
		super(mapping, data, SECTION_SIZE);
		LodUtil.assertTrue(data.length == SECTION_SIZE*SECTION_SIZE);
		
		this.sectionPos = pos;
		this.isColumnNotEmpty = isColumnNotEmpty;
		this.worldGenStep = EDhApiWorldGenerationStep.EMPTY;
		this.isEmpty = false;
	}
	
	public static SingleChunkFullDataSource createEmpty(DhSectionPos pos) { return new SingleChunkFullDataSource(pos); }
	
	
	
	//===============//
	// file handling //
	//===============//
	
	public static SingleChunkFullDataSource loadData(FullDataMetaFile dataFile, BufferedInputStream bufferedInputStream, IDhLevel level) throws IOException, InterruptedException
	{
		DataInputStream dataInputStream = new DataInputStream(bufferedInputStream); // DO NOT CLOSE
		
		int dataDetail = dataInputStream.readInt();
		if(dataDetail != dataFile.metaData.dataLevel)
		{
			throw new IOException(LodUtil.formatLog("Data level mismatch: {} != {}", dataDetail, dataFile.metaData.dataLevel));
		}
		int size = dataInputStream.readInt();
		if (size != SECTION_SIZE)
		{
			throw new IOException(LodUtil.formatLog("Section size mismatch: {} != {} (Currently only 1 section size is supported)", size, SECTION_SIZE));
		}
		int minY = dataInputStream.readInt();
		if (minY != level.getMinY())
		{
			LOGGER.warn("Data minY mismatch: {} != {}. Will ignore data's y level", minY, level.getMinY());
		}
		// Data array length
		int end = dataInputStream.readInt();
		if (end == IFullDataSource.NO_DATA_FLAG_BYTE)
		{
			// Section is empty
			return new SingleChunkFullDataSource(dataFile.pos);
		}
		
		
		
		// Is column not empty
		if (end != IFullDataSource.DATA_GUARD_BYTE)
		{
			throw new IOException("invalid header end guard");
		}
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
		long[][] data = new long[SECTION_SIZE*SECTION_SIZE][];
		end = dataInputStream.readInt();
		if (end != IFullDataSource.DATA_GUARD_BYTE)
		{
			throw new IOException("invalid spotty flag end guard");
		}
		
		for (int i = isColumnNotEmpty.nextSetBit(0); i >= 0; i = isColumnNotEmpty.nextSetBit(i + 1))
		{
			long[] array = new long[dataInputStream.readByte()];
			for (int j = 0; j < array.length; j++)
			{
				array[j] = dataInputStream.readLong();
			}
			data[i] = array;
		}
		
		
		
		// Id mapping
		end = dataInputStream.readInt();
		if (end != IFullDataSource.DATA_GUARD_BYTE)
		{
			throw new IOException("invalid ID mapping end guard");
		}
		FullDataPointIdMap mapping = FullDataPointIdMap.deserialize(bufferedInputStream);
		
		
		
		// world gen step
		end = dataInputStream.readInt();
		if (end != IFullDataSource.DATA_GUARD_BYTE)
		{
			throw new IOException("invalid world gen step end guard");
		}
		EDhApiWorldGenerationStep worldGenStep = EDhApiWorldGenerationStep.fromValue(dataInputStream.readByte());
		if (worldGenStep == null)
		{
			LOGGER.warn("Missing WorldGenStep, defaulting to: "+EDhApiWorldGenerationStep.SURFACE.name());
			worldGenStep = EDhApiWorldGenerationStep.SURFACE;
		}
		
		
		
		return new SingleChunkFullDataSource(dataFile.pos, mapping, worldGenStep, isColumnNotEmpty, data);
	}
	
	@Override
	public void writeToStream(IDhLevel level, FullDataMetaFile file, BufferedOutputStream bufferedOutputStream) throws IOException
	{
		DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream); // DO NOT CLOSE
		
		
		dataOutputStream.writeInt(this.getDataDetail());
		dataOutputStream.writeInt(this.size);
		dataOutputStream.writeInt(level.getMinY());
		if (this.isEmpty)
		{
			dataOutputStream.writeInt(IFullDataSource.NO_DATA_FLAG_BYTE);
			return;
		}
		
		// Is column not empty bits
		dataOutputStream.writeInt(IFullDataSource.DATA_GUARD_BYTE);
		byte[] bytes = this.isColumnNotEmpty.toByteArray();
		dataOutputStream.writeInt(bytes.length);
		dataOutputStream.write(bytes);
		
		// Data array content
		dataOutputStream.writeInt(IFullDataSource.DATA_GUARD_BYTE);
		for (int i = this.isColumnNotEmpty.nextSetBit(0); i >= 0; i = this.isColumnNotEmpty.nextSetBit(i + 1))
		{
			dataOutputStream.writeByte(this.dataArrays[i].length);
			if (this.dataArrays[i].length != 0)
			{
				for (long dataPoint : this.dataArrays[i])
				{
					dataOutputStream.writeLong(dataPoint);
				}
			}
		}
		
		// Id mapping
		dataOutputStream.writeInt(IFullDataSource.DATA_GUARD_BYTE);
		this.mapping.serialize(bufferedOutputStream);
		
		
		// world Gen step
		dataOutputStream.writeInt(IFullDataSource.DATA_GUARD_BYTE);
		dataOutputStream.writeByte(this.worldGenStep.value);
		
	}
	
	
	
	//===============//
	// Data updating //
	//===============//
	
	@Override
	public void update(ChunkSizedFullDataSource data)
	{
		LodUtil.assertTrue(this.sectionPos.getSectionBBoxPos().overlapsExactly(data.getBBoxLodPos()));
		
		if (data.dataDetail == 0 && this.getDataDetail() >= 4)
		{
			//FIXME: TEMPORARY
			int chunkPerFull = 1 << (this.getDataDetail() - 4);
			if (data.x % chunkPerFull != 0 || data.z % chunkPerFull != 0)
				return;
			DhLodPos baseOffset = this.sectionPos.getCorner(this.getDataDetail());
			DhLodPos dataOffset = data.getBBoxLodPos().convertToDetailLevel(this.getDataDetail());
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
    public void sampleFrom(IFullDataSource source)
	{
        DhSectionPos pos = source.getSectionPos();
        LodUtil.assertTrue(pos.sectionDetailLevel < this.sectionPos.sectionDetailLevel);
        LodUtil.assertTrue(pos.overlaps(this.sectionPos));
        if (source.isEmpty()) 
			return;
		
        if (source instanceof SparseFullDataSource)
		{
			this.sampleFrom((SparseFullDataSource) source);
        }
		else if (source instanceof FullDataSource)
		{
			this.sampleFrom((FullDataSource) source);
        }
		else
		{
            LodUtil.assertNotReach();
        }
    }
	
    private void sampleFrom(SparseFullDataSource sparseSource)
	{
        DhSectionPos pos = sparseSource.getSectionPos();
		this.isEmpty = false;

        if (this.getDataDetail() > this.sectionPos.sectionDetailLevel)
		{
            DhLodPos basePos = this.sectionPos.getCorner(this.getDataDetail());
            DhLodPos dataPos = pos.getCorner(this.getDataDetail());
            int offsetX = dataPos.x - basePos.x;
            int offsetZ = dataPos.z - basePos.z;
            LodUtil.assertTrue(offsetX >= 0 && offsetX < SECTION_SIZE && offsetZ >= 0 && offsetZ < SECTION_SIZE);
            int chunksPerData = 1 << (this.getDataDetail() - SparseFullDataSource.SPARSE_UNIT_DETAIL);
            int dataSpan = this.sectionPos.getWidth(this.getDataDetail()).numberOfLodSectionsWide;

            for (int xOffset = 0; xOffset < dataSpan; xOffset++)
			{
                for (int zOffset = 0; zOffset < dataSpan; zOffset++)
				{
                    SingleFullArrayView column = sparseSource.tryGet(
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
            DhLodPos dataPos = pos.getSectionBBoxPos();
            int lowerSectionsPerData = this.sectionPos.getWidth(dataPos.detailLevel).numberOfLodSectionsWide;
            if (dataPos.x % lowerSectionsPerData != 0 || dataPos.z % lowerSectionsPerData != 0) return;

            DhLodPos basePos = this.sectionPos.getCorner(this.getDataDetail());
            dataPos = dataPos.convertToDetailLevel(this.getDataDetail());
            int offsetX = dataPos.x - basePos.x;
            int offsetZ = dataPos.z - basePos.z;
            SingleFullArrayView column = sparseSource.tryGet(0, 0);
            if (column != null) {
                column.deepCopyTo(this.get(offsetX, offsetZ));
				this.isColumnNotEmpty.set(offsetX * SECTION_SIZE + offsetZ, true);
            }
        }
    }
	
    private void sampleFrom(FullDataSource fullSource)
	{
        DhSectionPos pos = fullSource.getSectionPos();
		this.isEmpty = false;
		this.downsampleFrom(fullSource);
		
		if (this.getDataDetail() > this.sectionPos.sectionDetailLevel)
		{
			DhLodPos basePos = this.sectionPos.getCorner(this.getDataDetail());
			DhLodPos dataPos = pos.getCorner(this.getDataDetail());
			int offsetX = dataPos.x - basePos.x;
			int offsetZ = dataPos.z - basePos.z;
			int dataSpan = this.sectionPos.getWidth(this.getDataDetail()).numberOfLodSectionsWide;
			
			for (int xOffset = 0; xOffset < dataSpan; xOffset++)
			{
				for (int zOffset = 0; zOffset < dataSpan; zOffset++)
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
			
            DhLodPos basePos = this.sectionPos.getCorner(this.getDataDetail());
            dataPos = dataPos.convertToDetailLevel(this.getDataDetail());
            int offsetX = dataPos.x - basePos.x;
            int offsetZ = dataPos.z - basePos.z;
			this.isColumnNotEmpty.set(offsetX * SECTION_SIZE + offsetZ, true);
        }

    }
	
    @Override
    public IFullDataSource trySelfPromote()
	{
        if (this.isEmpty)
		{
			return this;
		}
		
        if (this.isColumnNotEmpty.cardinality() != SECTION_SIZE * SECTION_SIZE)
		{
			return this;
		}
		
        return new FullDataSource(this.sectionPos, this.mapping, this.dataArrays);
    }
	
	
	
	//
	// data 
	//
	
    @Override
    public SingleFullArrayView tryGet(int x, int z) { return this.isColumnNotEmpty.get(x * SECTION_SIZE + z) ? this.get(x, z) : null; }
	
	
	
	//=====================//
	// getters and setters //
	//=====================//
	
	@Override
	public DhSectionPos getSectionPos() { return this.sectionPos; }
	@Override
	public byte getDataDetail() { return (byte) (this.sectionPos.sectionDetailLevel -SECTION_SIZE_OFFSET); }
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
