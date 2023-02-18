package com.seibel.lod.core.datatype.full.sources;

import com.seibel.lod.core.datatype.full.IIncompleteFullDataSource;
import com.seibel.lod.core.datatype.full.IFullDataSource;
import com.seibel.lod.core.datatype.full.FullDataPointIdMap;
import com.seibel.lod.core.datatype.full.accessor.FullArrayView;
import com.seibel.lod.core.datatype.full.accessor.SingleFullArrayView;
import com.seibel.lod.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.BitShiftUtil;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.BitSet;

/**
 * Handles full data with the detail level {@link SparseFullDataSource#SPARSE_UNIT_DETAIL}
 */
public class SparseFullDataSource implements IIncompleteFullDataSource
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final byte SPARSE_UNIT_DETAIL = 4;
    public static final byte SPARSE_UNIT_SIZE = (byte) BitShiftUtil.powerOfTwo(SPARSE_UNIT_DETAIL);

    public static final byte SECTION_SIZE_OFFSET = 6;
    public static final int SECTION_SIZE = (byte) BitShiftUtil.powerOfTwo(SECTION_SIZE_OFFSET);
    public static final byte MAX_SECTION_DETAIL = SECTION_SIZE_OFFSET + SPARSE_UNIT_DETAIL;
    public static final byte LATEST_VERSION = 0;
    public static final long TYPE_ID = "SparseFullDataSource".hashCode();
	
	/**
	 * This is the byte put between different sections in the binary save file.
	 * The presence and absence of this byte indicates if the file is correctly formatted.  
	 */
	private static final int DATA_GUARD_BYTE = 0xFFFFFFFF;
	/** indicates the binary save file represents an empty data source */
	private static final int NO_DATA_FLAG_BYTE = 0x00000001;
	
	protected final FullDataPointIdMap mapping;
    private final DhSectionPos sectionPos;
    private final FullArrayView[] sparseData;
    private final DhLodPos chunkPos;
	
	public final int chunks;
	public final int dataPerChunk;
    public boolean isEmpty = true;
	
	
	
	//==============//
	// constructors //
	//==============//
	
    public static SparseFullDataSource createEmpty(DhSectionPos pos) { return new SparseFullDataSource(pos); }

    protected SparseFullDataSource(DhSectionPos sectionPos)
	{
        LodUtil.assertTrue(sectionPos.sectionDetailLevel > SPARSE_UNIT_DETAIL);
        LodUtil.assertTrue(sectionPos.sectionDetailLevel <= MAX_SECTION_DETAIL);
        this.sectionPos = sectionPos;
		this.chunks = 1 << (byte) (sectionPos.sectionDetailLevel - SPARSE_UNIT_DETAIL);
		this.dataPerChunk = SECTION_SIZE / this.chunks;
		this.sparseData = new FullArrayView[this.chunks * this.chunks];
		this.chunkPos = sectionPos.getCorner(SPARSE_UNIT_DETAIL);
		this.mapping = new FullDataPointIdMap();
    }
    protected SparseFullDataSource(DhSectionPos sectionPos, FullDataPointIdMap mapping, FullArrayView[] data)
	{
        LodUtil.assertTrue(sectionPos.sectionDetailLevel > SPARSE_UNIT_DETAIL);
        LodUtil.assertTrue(sectionPos.sectionDetailLevel <= MAX_SECTION_DETAIL);
        this.sectionPos = sectionPos;
		this.chunks = 1 << (byte) (sectionPos.sectionDetailLevel - SPARSE_UNIT_DETAIL);
		this.dataPerChunk = SECTION_SIZE / this.chunks;
		LodUtil.assertTrue(this.chunks * this.chunks == data.length);
		this.sparseData = data;
		this.chunkPos = sectionPos.getCorner(SPARSE_UNIT_DETAIL);
		this.isEmpty = false;
        this.mapping = mapping;
    }
	
	
	
    @Override
    public DhSectionPos getSectionPos() { return this.sectionPos; }
    @Override
    public byte getDataDetail() { return (byte) (this.sectionPos.sectionDetailLevel -SECTION_SIZE_OFFSET); }

    @Override
    public byte getDataVersion() { return LATEST_VERSION; }
	
	@Override
	public FullDataPointIdMap getMapping() { return this.mapping; }
	
    private int calculateOffset(int chunkX, int chunkZ)
	{
        int offsetX = chunkX - this.chunkPos.x;
        int offsetZ = chunkZ - this.chunkPos.z;
        LodUtil.assertTrue(offsetX >= 0 && offsetZ >= 0 && offsetX < this.chunks && offsetZ < this.chunks);
        return offsetX * this.chunks + offsetZ;
    }
	
	
    @Override
    public void update(ChunkSizedFullDataSource data)
	{
		if (data.dataDetail != 0)
		{
			//TODO: Disable the throw and instead just ignore the data.
			throw new IllegalArgumentException("SparseFullDataSource only supports dataDetail 0!");
		}
		
		int arrayOffset = this.calculateOffset(data.x, data.z);
		FullArrayView newArray = new FullArrayView(this.mapping, new long[this.dataPerChunk * this.dataPerChunk][], this.dataPerChunk);
		if (this.getDataDetail() == data.dataDetail)
		{
			data.shadowCopyTo(newArray);
		}
		else
		{
			int count = this.dataPerChunk;
			int dataPerCount = SPARSE_UNIT_SIZE / this.dataPerChunk;
	
			for (int ox = 0; ox < count; ox++)
			{
				for (int oz = 0; oz < count; oz++)
				{
					SingleFullArrayView column = newArray.get(ox, oz);
					column.downsampleFrom(data.subView(dataPerCount, ox * dataPerCount, oz * dataPerCount));
				}
			}
		}
		this.isEmpty = false;
		this.sparseData[arrayOffset] = newArray;
    }
	
    @Override
    public boolean isEmpty() { return this.isEmpty; }
	
	
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
		
        DhLodPos basePos = this.sectionPos.getCorner(SPARSE_UNIT_DETAIL);
        DhLodPos dataPos = pos.getCorner(SPARSE_UNIT_DETAIL);
        int offsetX = dataPos.x-basePos.x;
        int offsetZ = dataPos.z-basePos.z;
		LodUtil.assertTrue(offsetX >= 0 && offsetX < this.chunks && offsetZ >= 0 && offsetZ < this.chunks);
	
		for (int ox = 0; ox < sparseSource.chunks; ox++)
		{
			for (int oz = 0; oz < sparseSource.chunks; oz++)
			{
				FullArrayView sourceChunk = sparseSource.sparseData[ox * sparseSource.chunks + oz];
				if (sourceChunk != null)
				{
					FullArrayView buff = new FullArrayView(this.mapping, new long[this.dataPerChunk * this.dataPerChunk][], this.dataPerChunk);
					buff.downsampleFrom(sourceChunk);
					this.sparseData[(ox + offsetX) * this.chunks + (oz + offsetZ)] = buff;
				}
			}
		}
    }
    private void sampleFrom(FullDataSource fullSource)
	{
        DhSectionPos pos = fullSource.getSectionPos();
		this.isEmpty = false;

        DhLodPos basePos = this.sectionPos.getCorner(SPARSE_UNIT_DETAIL);
        DhLodPos dataPos = pos.getCorner(SPARSE_UNIT_DETAIL);
        int coveredChunks = pos.getWidth(SPARSE_UNIT_DETAIL).numberOfLodSectionsWide;
        int sourceDataPerChunk = SPARSE_UNIT_SIZE >>> fullSource.getDataDetail();
        LodUtil.assertTrue(coveredChunks*sourceDataPerChunk == FullDataSource.SECTION_SIZE);
        int offsetX = dataPos.x-basePos.x;
        int offsetZ = dataPos.z-basePos.z;
        LodUtil.assertTrue(offsetX >=0 && offsetX < this.chunks && offsetZ >=0 && offsetZ < this.chunks);
	
		for (int ox = 0; ox < coveredChunks; ox++)
		{
			for (int oz = 0; oz < coveredChunks; oz++)
			{
				FullArrayView sourceChunk = fullSource.subView(sourceDataPerChunk, ox * sourceDataPerChunk, oz * sourceDataPerChunk);
				FullArrayView buff = new FullArrayView(this.mapping, new long[this.dataPerChunk * this.dataPerChunk][], this.dataPerChunk);
				buff.downsampleFrom(sourceChunk);
				this.sparseData[(ox + offsetX) * this.chunks + (oz + offsetZ)] = buff;
			}
		}
    }
	
	
	
	//===============//
	// file handling //
	//===============//
	
    @Override
    public void saveData(IDhLevel level, FullDataMetaFile file, OutputStream dataStream) throws IOException
	{
        try (DataOutputStream dos = new DataOutputStream(dataStream))
		{
            dos.writeShort(this.getDataDetail());
            dos.writeShort(SPARSE_UNIT_DETAIL);
            dos.writeInt(SECTION_SIZE);
            dos.writeInt(level.getMinY());
            if (this.isEmpty)
			{
                dos.writeInt(NO_DATA_FLAG_BYTE);
                return;
            }
			
            dos.writeInt(DATA_GUARD_BYTE);
            // sparse array existence bitset
            BitSet dataArrayIndexHasData = new BitSet(this.sparseData.length);
            for (int i = 0; i < this.sparseData.length; i++)
			{
				dataArrayIndexHasData.set(i, this.sparseData[i] != null);
			}
            byte[] bytes = dataArrayIndexHasData.toByteArray();
            dos.writeInt(bytes.length);
            dos.write(bytes);

            // Data array content (only on non-empty stuff)
            dos.writeInt(DATA_GUARD_BYTE);
            for (int i = dataArrayIndexHasData.nextSetBit(0); 
				 i >= 0; 
				 i = dataArrayIndexHasData.nextSetBit(i+1))
			{
				// column data length
                FullArrayView array = this.sparseData[i];
                LodUtil.assertTrue(array != null);
				for (int x = 0; x < array.width(); x++)
				{
					for (int z = 0; z < array.width(); z++)
					{
						dos.writeInt(array.get(x, z).getSingleLength());
					}
				}
				
				// column data
				for (int x = 0; x < array.width(); x++)
				{
					for (int z = 0; z < array.width(); z++)
					{
						SingleFullArrayView column = array.get(x, z);
						LodUtil.assertTrue(column.getMapping() == this.mapping); // the mappings must be exactly equal!
						
						if (column.doesItExist())
						{
							long[] raw = column.getRaw();
							for (long l : raw)
							{
								dos.writeLong(l);
							}
						}
					}
				}
            }
			
            // Id mapping
            dos.writeInt(DATA_GUARD_BYTE);
			this.mapping.serialize(dos);
			dos.writeInt(DATA_GUARD_BYTE);
        }
    }

    public static SparseFullDataSource loadData(FullDataMetaFile dataFile, InputStream dataStream, IDhLevel level) throws IOException
	{
        LodUtil.assertTrue(dataFile.pos.sectionDetailLevel > SPARSE_UNIT_DETAIL);
        LodUtil.assertTrue(dataFile.pos.sectionDetailLevel <= MAX_SECTION_DETAIL);
		
        DataInputStream inputStream = new DataInputStream(dataStream); // DO NOT CLOSE! It would close all related streams
        {
			// TODO what is a data detail?
            int dataDetail = inputStream.readShort();
            if(dataDetail != dataFile.metaData.dataLevel)
			{
				throw new IOException(LodUtil.formatLog("Data level mismatch: {} != {}", dataDetail, dataFile.metaData.dataLevel));
			}
			
			// confirm that the detail level is correct
            int sparseDetail = inputStream.readShort();
            if (sparseDetail != SPARSE_UNIT_DETAIL)
			{
				throw new IOException((LodUtil.formatLog("Unexpected sparse detail level: {} != {}",
						sparseDetail, SPARSE_UNIT_DETAIL)));
			}
			
			// confirm the scale of the data points is correct
            int sectionSize = inputStream.readInt();
            if (sectionSize != SECTION_SIZE)
			{
				throw new IOException(LodUtil.formatLog(
						"Section size mismatch: {} != {} (Currently only 1 section size is supported)", sectionSize, SECTION_SIZE));
			}
			
			
			// calculate the number of chunks and dataPoints based on the sparseDetail and sectionSize
			// TODO these values should be constant, should we still be calculating them like this?
			int chunks = BitShiftUtil.powerOfTwo(dataFile.pos.sectionDetailLevel - sparseDetail);
			int dataPointsPerChunk = sectionSize / chunks;
			
			
			// get the data's starting Y-level
			int minY = inputStream.readInt();
			if (minY != level.getMinY())
			{
				LOGGER.warn("Data minY mismatch: {} != {}. Will ignore data's y level", minY, level.getMinY());
			}
			
			
			// check if this file has any data
            int hasDataFlag = inputStream.readInt();
            if (hasDataFlag == NO_DATA_FLAG_BYTE)
			{
                // this file is empty
                return createEmpty(dataFile.pos);
            }
            else if (hasDataFlag != DATA_GUARD_BYTE)
			{
				// the file format is incorrect
				throw new IOException("invalid header end guard");
			}
			else
			{
				// this file has data
				
				
				// get the number of columns (IE the bitSet from before)
				int numberOfDataColumns = inputStream.readInt();
				// validate the number of data columns
				int maxNumberOfDataColumns = (chunks * chunks / 8 + 64) * 2; // TODO what do these values represent?
				if (numberOfDataColumns < 0 || numberOfDataColumns > maxNumberOfDataColumns)
				{
					throw new IOException(LodUtil.formatLog("Sparse Flag BitSet size outside reasonable range: {} (expects {} to {})",
							numberOfDataColumns, 1, maxNumberOfDataColumns));
				}
				
				// read in the presence of each data column
				byte[] bytes = inputStream.readNBytes(numberOfDataColumns);
				BitSet dataArrayIndexHasData = BitSet.valueOf(bytes);
				
				
				
				//====================//
				// Data array content //
				//====================//
				
				//  (only on non-empty columns)
				int dataArrayStartByte = inputStream.readInt();
				// confirm the column data is starting
				if (dataArrayStartByte != DATA_GUARD_BYTE)
				{
					// the file format is incorrect
					throw new IOException("invalid data length end guard");
				}
				
				
				// read in each column that has data written to it
				long[][][] rawFullDataArrays = new long[chunks * chunks][][];
				for (int fullDataIndex = dataArrayIndexHasData.nextSetBit(0);
					 fullDataIndex >= 0 && // TODO why does this happen? 
							 fullDataIndex < rawFullDataArrays.length;
					 fullDataIndex = dataArrayIndexHasData.nextSetBit(fullDataIndex + 1))
				{
					long[][] dataColumn = new long[dataPointsPerChunk * dataPointsPerChunk][];
					
					// get the column data lengths
					rawFullDataArrays[fullDataIndex] = dataColumn;
					for (int x = 0; x < dataColumn.length; x++)
					{
						// this should be zero if the column doesn't have any data
						int dataColumnLength = inputStream.readInt();
						dataColumn[x] = new long[dataColumnLength];
					}
					
					// get the column data
					for (int x = 0; x < dataColumn.length; x++)
					{
						
						if (dataColumn[x].length != 0)
						{
							// read in the data columns
							for (int z = 0; z < dataColumn[x].length; z++)
							{
								dataColumn[x][z] = inputStream.readLong();
							}
						}
					}
				}
				
				
				
				//============//
				// ID mapping //
				//============//
				
				// mark the start of the ID data
				int idMappingStartByte = inputStream.readInt();
				if (idMappingStartByte != DATA_GUARD_BYTE)
				{
					// the file format is incorrect
					throw new IOException("invalid data content end guard");
				}
				
				// deserialize the ID data
				FullDataPointIdMap mapping = FullDataPointIdMap.deserialize(inputStream);
				int idMappingEndByte = inputStream.readInt();
				if (idMappingEndByte != DATA_GUARD_BYTE)
				{
					// the file format is incorrect
					throw new IOException("invalid id mapping end guard");
				}
				
				FullArrayView[] fullDataArrays = new FullArrayView[chunks * chunks];
				for (int i = 0; i < rawFullDataArrays.length; i++)
				{
					if (rawFullDataArrays[i] != null)
					{
						fullDataArrays[i] = new FullArrayView(mapping, rawFullDataArrays[i], dataPointsPerChunk);
					}
				}
				
				return new SparseFullDataSource(dataFile.pos, mapping, fullDataArrays);
			}
        }
    }
	
	
	
    private void applyToFullDataSource(FullDataSource dataSource)
	{
        LodUtil.assertTrue(dataSource.getSectionPos().equals(this.sectionPos));
        LodUtil.assertTrue(dataSource.getDataDetail() == this.getDataDetail());
		for (int x = 0; x < this.chunks; x++)
		{
			for (int z = 0; z < this.chunks; z++)
			{
				FullArrayView array = this.sparseData[x * this.chunks + z];
				if (array == null)
					continue;
				
				// Otherwise, apply data to dataSource
				dataSource.markNotEmpty();
				FullArrayView view = dataSource.subView(this.dataPerChunk, x * this.dataPerChunk, z * this.dataPerChunk);
				array.shadowCopyTo(view);
			}
		}
    }

    public IFullDataSource trySelfPromote()
	{
        if (this.isEmpty)
		{
			return this;
		}
		
        for (FullArrayView array : this.sparseData)
		{
            if (array == null) return this;
        }
        FullDataSource newSource = FullDataSource.createEmpty(this.sectionPos);
		this.applyToFullDataSource(newSource);
        return newSource;
    }
	
    public SingleFullArrayView tryGet(int x, int z)
	{
        LodUtil.assertTrue(x>=0 && x<SECTION_SIZE && z>=0 && z<SECTION_SIZE);
        int chunkX = x / this.dataPerChunk;
        int chunkZ = z / this.dataPerChunk;
        FullArrayView chunk = this.sparseData[chunkX * this.chunks + chunkZ];
        if (chunk == null)
		{
			return null;
		}
        return chunk.get(x % this.dataPerChunk, z % this.dataPerChunk);
    }
	
}
