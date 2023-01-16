package com.seibel.lod.core.datatype.full;

import com.seibel.lod.core.datatype.IIncompleteDataSource;
import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.full.accessor.FullArrayView;
import com.seibel.lod.core.datatype.full.accessor.SingleFullArrayView;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.file.datafile.DataMetaFile;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.BitSet;

public class SparseDataSource implements IIncompleteDataSource
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final byte SPARSE_UNIT_DETAIL = 4;
    public static final byte SPARSE_UNIT_SIZE = 1 << SPARSE_UNIT_DETAIL;

    public static final byte SECTION_SIZE_OFFSET = 6;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
    public static final byte MAX_SECTION_DETAIL = SECTION_SIZE_OFFSET + SPARSE_UNIT_DETAIL;
    public static final byte LATEST_VERSION = 0;
    public static final long TYPE_ID = "SparseDataSource".hashCode();
    protected final FullDataPointIdMap mapping;
    private final DhSectionPos sectionPos;
    private final FullArrayView[] sparseData;
    final int chunks;
    final int dataPerChunk;
    private final DhLodPos chunkPos;
    public boolean isEmpty = true;
	
	
	
    public static SparseDataSource createEmpty(DhSectionPos pos) { return new SparseDataSource(pos); }

    protected SparseDataSource(DhSectionPos sectionPos)
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
    protected SparseDataSource(DhSectionPos sectionPos, FullDataPointIdMap mapping, FullArrayView[] data)
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
	
    private int calculateOffset(int cx, int cz)
	{
        int ox = cx - this.chunkPos.x;
        int oz = cz - this.chunkPos.z;
        LodUtil.assertTrue(ox >= 0 && oz >= 0 && ox < this.chunks && oz < this.chunks);
        return ox * this.chunks + oz;
    }
	
	
    @Override
    public void update(ChunkSizedData data)
	{
		if (data.dataDetail != 0)
		{
			//TODO: Disable the throw and instead just ignore the data.
			throw new IllegalArgumentException("SparseDataSource only supports dataDetail 0!");
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
	public void sampleFrom(ILodDataSource source)
	{
		DhSectionPos pos = source.getSectionPos();
		LodUtil.assertTrue(pos.sectionDetailLevel < this.sectionPos.sectionDetailLevel);
		LodUtil.assertTrue(pos.overlaps(this.sectionPos));
		if (source.isEmpty())
			return;
		
		if (source instanceof SparseDataSource)
		{
			this.sampleFrom((SparseDataSource) source);
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

    private void sampleFrom(SparseDataSource sparseSource)
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

    @Override
    public void saveData(IDhLevel level, DataMetaFile file, OutputStream dataStream) throws IOException
	{
        try (DataOutputStream dos = new DataOutputStream(dataStream))
		{
            dos.writeShort(this.getDataDetail());
            dos.writeShort(SPARSE_UNIT_DETAIL);
            dos.writeInt(SECTION_SIZE);
            dos.writeInt(level.getMinY());
            if (this.isEmpty)
			{
                dos.writeInt(0x00000001);
                return;
            }
			
            dos.writeInt(0xFFFFFFFF);
            // sparse array existence bitset
            BitSet set = new BitSet(this.sparseData.length);
            for (int i = 0; i < this.sparseData.length; i++) set.set(i, this.sparseData[i] != null);
            byte[] bytes = set.toByteArray();
            dos.writeInt(bytes.length);
            dos.write(bytes);

            // Data array content (only on non-empty stuff)
            dos.writeInt(0xFFFFFFFF);
            for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1))
			{
                FullArrayView array = this.sparseData[i];
                LodUtil.assertTrue(array != null);
				for (int x = 0; x < array.width(); x++)
				{
					for (int z = 0; z < array.width(); z++)
					{
						dos.writeByte(array.get(x, z).getSingleLength());
					}
				}
				
				for (int x = 0; x < array.width(); x++)
				{
					for (int z = 0; z < array.width(); z++)
					{
						SingleFullArrayView column = array.get(x, z);
						LodUtil.assertTrue(column.getMapping() == this.mapping); //MUST be exact equal!
						if (!column.doesItExist())
							continue;
						long[] raw = column.getRaw();
						for (long l : raw)
						{
							dos.writeLong(l);
						}
					}
				}
            }
			
            // Id mapping
            dos.writeInt(0xFFFFFFFF);
			this.mapping.serialize(dos);
            dos.writeInt(0xFFFFFFFF);
        }
    }

    public static SparseDataSource loadData(DataMetaFile dataFile, InputStream dataStream, IDhLevel level) throws IOException
	{
        LodUtil.assertTrue(dataFile.pos.sectionDetailLevel > SPARSE_UNIT_DETAIL);
        LodUtil.assertTrue(dataFile.pos.sectionDetailLevel <= MAX_SECTION_DETAIL);
		
        DataInputStream dos = new DataInputStream(dataStream); // DO NOT CLOSE! It would close all related streams
        {
            int dataDetail = dos.readShort();
            if(dataDetail != dataFile.metaData.dataLevel)
                throw new IOException(LodUtil.formatLog("Data level mismatch: {} != {}", dataDetail, dataFile.metaData.dataLevel));
			
            int sparseDetail = dos.readShort();
            if (sparseDetail != SPARSE_UNIT_DETAIL)
			{
				throw new IOException((LodUtil.formatLog("Unexpected sparse detail level: {} != {}",
						sparseDetail, SPARSE_UNIT_DETAIL)));
			}
			
            int size = dos.readInt();
            if (size != SECTION_SIZE)
			{
				throw new IOException(LodUtil.formatLog(
						"Section size mismatch: {} != {} (Currently only 1 section size is supported)", size, SECTION_SIZE));
			}
			
            int chunks = 1 << (byte) (dataFile.pos.sectionDetailLevel - sparseDetail);
            int dataPerChunk = size / chunks;
			
            int minY = dos.readInt();
            if (minY != level.getMinY())
                LOGGER.warn("Data minY mismatch: {} != {}. Will ignore data's y level", minY, level.getMinY());
            int end = dos.readInt();
            // Data array length
            if (end == 0x00000001)
			{
                // Section is empty
                return createEmpty(dataFile.pos);
            }

            // Non-empty section
            if (end != 0xFFFFFFFF) 
				throw new IOException("invalid header end guard");
            int length = dos.readInt();
	
			if (length < 0 || length > (chunks * chunks / 8 + 64) * 2)
                throw new IOException(LodUtil.formatLog("Sparse Flag BitSet size outside reasonable range: {} (expects {} to {})",
                        length, 1, chunks*chunks/8+63));
            byte[] bytes = dos.readNBytes(length);
            BitSet set = BitSet.valueOf(bytes);

            long[][][] dataChunks = new long[chunks*chunks][][];

            // Data array content (only on non-empty columns)
            end = dos.readInt();
            if (end != 0xFFFFFFFF)
				throw new IOException("invalid data length end guard");
			
			for (int i = set.nextSetBit(0); i >= 0 && i < dataChunks.length; i = set.nextSetBit(i + 1))
			{
				long[][] dataColumns = new long[dataPerChunk * dataPerChunk][];
				dataChunks[i] = dataColumns;
				for (int i2 = 0; i2 < dataColumns.length; i2++)
				{
					dataColumns[i2] = new long[dos.readByte()];
				}
				for (int k = 0; k < dataColumns.length; k++)
				{
					if (dataColumns[k].length == 0)
						continue;
					for (int o = 0; o < dataColumns[k].length; o++)
					{
						dataColumns[k][o] = dos.readLong();
					}
				}
			}

            // Id mapping
            end = dos.readInt();
            if (end != 0xFFFFFFFF) 
				throw new IOException("invalid data content end guard");
			
            FullDataPointIdMap mapping = FullDataPointIdMap.deserialize(dos);
            end = dos.readInt();
            if (end != 0xFFFFFFFF) 
				throw new IOException("invalid id mapping end guard");

            FullArrayView[] objectChunks = new FullArrayView[chunks*chunks];
			for (int i = 0; i < dataChunks.length; i++)
			{
				if (dataChunks[i] == null)
					continue;
				objectChunks[i] = new FullArrayView(mapping, dataChunks[i], dataPerChunk);
			}

            return new SparseDataSource(dataFile.pos, mapping, objectChunks);
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

    public ILodDataSource trySelfPromote()
	{
        if (this.isEmpty) 
			return this;
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
			return null;
        return chunk.get(x % this.dataPerChunk, z % this.dataPerChunk);
    }
	
}
