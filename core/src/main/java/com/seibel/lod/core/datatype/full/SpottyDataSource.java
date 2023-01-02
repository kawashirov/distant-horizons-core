package com.seibel.lod.core.datatype.full;

import com.seibel.lod.core.datatype.IIncompleteDataSource;
import com.seibel.lod.core.datatype.ILodDataSource;
import com.seibel.lod.core.datatype.full.accessor.FullArrayView;
import com.seibel.lod.core.datatype.full.accessor.SingleFullArrayView;
import com.seibel.lod.core.file.datafile.DataMetaFile;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.objects.UnclosableInputStream;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.BitSet;

/**
 * 1 chunk
 */
public class SpottyDataSource extends FullArrayView implements IIncompleteDataSource
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final byte SECTION_SIZE_OFFSET = 6;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
    public static final byte LATEST_VERSION = 0;
    public static final long TYPE_ID = "SpottyDataSource".hashCode();
    private final DhSectionPos sectionPos;
    private boolean isEmpty = true;
    private final BitSet isColumnNotEmpty;

    protected SpottyDataSource(DhSectionPos sectionPos)
	{
        super(new FullDataPointIdMap(), new long[SECTION_SIZE*SECTION_SIZE][0], SECTION_SIZE);
        LodUtil.assertTrue(sectionPos.sectionDetail > SparseDataSource.MAX_SECTION_DETAIL);
        this.sectionPos = sectionPos;
		this.isColumnNotEmpty = new BitSet(SECTION_SIZE*SECTION_SIZE);
    }

    @Override
    public DhSectionPos getSectionPos() { return this.sectionPos;  }
    @Override
    public byte getDataDetail() { return (byte) (this.sectionPos.sectionDetail-SECTION_SIZE_OFFSET); }

    @Override
    public byte getDataVersion() { return LATEST_VERSION;  }

    @Override
    public void update(ChunkSizedData data)
	{
        LodUtil.assertTrue(this.sectionPos.getSectionBBoxPos().overlaps(data.getBBoxLodPos()));

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
    public boolean isEmpty() { return this.isEmpty; }

    public void markNotEmpty() { this.isEmpty = false;  }

    @Override
    public void saveData(IDhLevel level, DataMetaFile file, OutputStream dataStream) throws IOException
	{
        DataOutputStream dos = new DataOutputStream(dataStream); // DO NOT CLOSE
        {
            dos.writeInt(this.getDataDetail());
            dos.writeInt(this.size);
            dos.writeInt(level.getMinY());
            if (this.isEmpty)
			{
                dos.writeInt(0x00000001);
                return;
            }

            // Is column not empty
            dos.writeInt(0xFFFFFFFF);
            byte[] bytes = this.isColumnNotEmpty.toByteArray();
            dos.writeInt(bytes.length);
            dos.write(bytes);

            // Data array content
            dos.writeInt(0xFFFFFFFF);
            for (int i = this.isColumnNotEmpty.nextSetBit(0); i >= 0; i = this.isColumnNotEmpty.nextSetBit(i + 1))
            {
                dos.writeByte(this.dataArrays[i].length);
                if (this.dataArrays[i].length == 0) 
					continue;
                for (long l : this.dataArrays[i]) {
                    dos.writeLong(l);
                }
            }

            // Id mapping
            dos.writeInt(0xFFFFFFFF);
			this.mapping.serialize(dos);
            dos.writeInt(0xFFFFFFFF);
        }
    }


    public static SpottyDataSource loadData(DataMetaFile dataFile, InputStream dataStream, IDhLevel level) throws IOException
	{
        DataInputStream dos = new DataInputStream(dataStream); // DO NOT CLOSE
        {
            int dataDetail = dos.readInt();
            if(dataDetail != dataFile.metaData.dataLevel)
                throw new IOException(LodUtil.formatLog("Data level mismatch: {} != {}", dataDetail, dataFile.metaData.dataLevel));
			
            int size = dos.readInt();
            if (size != SECTION_SIZE)
                throw new IOException(LodUtil.formatLog(
                        "Section size mismatch: {} != {} (Currently only 1 section size is supported)", size, SECTION_SIZE));
			
            int minY = dos.readInt();
            if (minY != level.getMinY())
                LOGGER.warn("Data minY mismatch: {} != {}. Will ignore data's y level", minY, level.getMinY());
			
            int end = dos.readInt();
            // Data array length
            if (end == 0x00000001)
			{
                // Section is empty
                return new SpottyDataSource(dataFile.pos);
            }

            // Is column not empty
            if (end != 0xFFFFFFFF) 
				throw new IOException("invalid header end guard");
            int length = dos.readInt();

            if (length < 0 || length > (SECTION_SIZE*SECTION_SIZE/8+64)*2)
			{
				throw new IOException(LodUtil.formatLog("Spotty Flag BitSet size outside reasonable range: {} (expects {} to {})",
						length, 1, SECTION_SIZE * SECTION_SIZE / 8 + 63));
			}
            byte[] bytes = dos.readNBytes(length);
            BitSet isColumnNotEmpty = BitSet.valueOf(bytes);

            // Data array content
            long[][] data = new long[SECTION_SIZE*SECTION_SIZE][];
            end = dos.readInt();
            if (end != 0xFFFFFFFF) 
				throw new IOException("invalid spotty flag end guard");
			
            for (int i = isColumnNotEmpty.nextSetBit(0); i >= 0; i = isColumnNotEmpty.nextSetBit(i + 1))
            {
                long[] array = new long[dos.readByte()];
                for (int j = 0; j < array.length; j++)
				{
                    array[j] = dos.readLong();
                }
                data[i] = array;
            }

            // Id mapping
            end = dos.readInt();
            if (end != 0xFFFFFFFF) 
				throw new IOException("invalid data content end guard");
			
            FullDataPointIdMap mapping = FullDataPointIdMap.deserialize(new UnclosableInputStream(dos));
            end = dos.readInt();
            if (end != 0xFFFFFFFF)
				throw new IOException("invalid id mapping end guard");
			
            return new SpottyDataSource(dataFile.pos, mapping, isColumnNotEmpty, data);
        }
    }

    private SpottyDataSource(DhSectionPos pos, FullDataPointIdMap mapping, BitSet isColumnNotEmpty, long[][] data)
	{
        super(mapping, data, SECTION_SIZE);
        LodUtil.assertTrue(data.length == SECTION_SIZE*SECTION_SIZE);
        this.sectionPos = pos;
        this.isColumnNotEmpty = isColumnNotEmpty;
		this.isEmpty = false;
    }

    public static SpottyDataSource createEmpty(DhSectionPos pos) { return new SpottyDataSource(pos); }

    public static boolean neededForPosition(DhSectionPos posToWrite, DhSectionPos posToTest)
	{
        if (!posToWrite.overlaps(posToTest)) 
			return false;
        if (posToTest.sectionDetail > posToWrite.sectionDetail) 
			return false;
        if (posToWrite.sectionDetail - posToTest.sectionDetail <= SECTION_SIZE_OFFSET) 
			return true;
        byte sectPerData = (byte) (1 << (posToWrite.sectionDetail - posToTest.sectionDetail - SECTION_SIZE_OFFSET));
        return posToTest.sectionX % sectPerData == 0 && posToTest.sectionZ % sectPerData == 0;
    }

    @Override
    public void sampleFrom(ILodDataSource source)
	{
        DhSectionPos pos = source.getSectionPos();
        LodUtil.assertTrue(pos.sectionDetail < this.sectionPos.sectionDetail);
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

        if (this.getDataDetail() > this.sectionPos.sectionDetail)
		{
            DhLodPos basePos = this.sectionPos.getCorner(this.getDataDetail());
            DhLodPos dataPos = pos.getCorner(this.getDataDetail());
            int offsetX = dataPos.x - basePos.x;
            int offsetZ = dataPos.z - basePos.z;
            LodUtil.assertTrue(offsetX >= 0 && offsetX < SECTION_SIZE && offsetZ >= 0 && offsetZ < SECTION_SIZE);
            int chunksPerData = 1 << (this.getDataDetail() - SparseDataSource.SPARSE_UNIT_DETAIL);
            int dataSpan = this.sectionPos.getWidth(this.getDataDetail()).numberOfLodSectionsWide;

            for (int ox = 0; ox < dataSpan; ox++)
			{
                for (int oz = 0; oz < dataSpan; oz++)
				{
                    SingleFullArrayView column = sparseSource.tryGet(
                            ox * chunksPerData * sparseSource.dataPerChunk,
                            oz * chunksPerData * sparseSource.dataPerChunk);
                    if (column != null)
					{
                        column.deepCopyTo(this.get(offsetX + ox, offsetZ + oz));
						this.isColumnNotEmpty.set((offsetX + ox) * SECTION_SIZE + offsetZ + oz, true);
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
		
		if (this.getDataDetail() > this.sectionPos.sectionDetail)
		{
			DhLodPos basePos = this.sectionPos.getCorner(this.getDataDetail());
			DhLodPos dataPos = pos.getCorner(this.getDataDetail());
			int offsetX = dataPos.x - basePos.x;
			int offsetZ = dataPos.z - basePos.z;
			int dataSpan = this.sectionPos.getWidth(this.getDataDetail()).numberOfLodSectionsWide;
			for (int ox = 0; ox < dataSpan; ox++)
			{
				for (int oz = 0; oz < dataSpan; oz++)
				{
					this.isColumnNotEmpty.set((offsetX + ox) * SECTION_SIZE + offsetZ + oz, true);
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
			this.isColumnNotEmpty.set(offsetX * SECTION_SIZE + offsetZ, true);
        }

    }

    @Override
    public ILodDataSource trySelfPromote()
	{
        if (this.isEmpty) 
			return this;
        if (this.isColumnNotEmpty.cardinality() != SECTION_SIZE * SECTION_SIZE) 
			return this;
        return new FullDataSource(this.sectionPos, this.mapping, this.dataArrays);
    }

    @Override
    public SingleFullArrayView tryGet(int x, int z) { return this.isColumnNotEmpty.get(x * SECTION_SIZE + z) ? this.get(x, z) : null; }
	
}
