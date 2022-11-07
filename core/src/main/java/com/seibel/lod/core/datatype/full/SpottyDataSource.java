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

public class SpottyDataSource extends FullArrayView implements IIncompleteDataSource { // 1 chunk
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final byte SECTION_SIZE_OFFSET = 6;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
    public static final byte LATEST_VERSION = 0;
    public static final long TYPE_ID = "SpottyDataSource".hashCode();
    private final DhSectionPos sectionPos;
    private boolean isEmpty = true;
    private final BitSet isColumnNotEmpty;

    protected SpottyDataSource(DhSectionPos sectionPos) {
        super(new FullDataPointIdMap(), new long[SECTION_SIZE*SECTION_SIZE][0], SECTION_SIZE);
        LodUtil.assertTrue(sectionPos.sectionDetail > SparseDataSource.MAX_SECTION_DETAIL);
        this.sectionPos = sectionPos;
        isColumnNotEmpty = new BitSet(SECTION_SIZE*SECTION_SIZE);
    }

    @Override
    public DhSectionPos getSectionPos() {
        return sectionPos;
    }
    @Override
    public byte getDataDetail() {
        return (byte) (sectionPos.sectionDetail-SECTION_SIZE_OFFSET);
    }

    @Override
    public byte getDataVersion() {
        return LATEST_VERSION;
    }

    @Override
    public void update(ChunkSizedData data) {
        LodUtil.assertTrue(sectionPos.getSectionBBoxPos().overlaps(data.getBBoxLodPos()));

        if (data.dataDetail == 0 && getDataDetail() >= 4) {
            //FIXME: TEMPORARY
            int chunkPerFull = 1 << (getDataDetail() - 4);
            if (data.x % chunkPerFull != 0 || data.z % chunkPerFull != 0) return;
            DhLodPos baseOffset = sectionPos.getCorner(getDataDetail());
            DhLodPos dataOffset = data.getBBoxLodPos().convertUpwardsTo(getDataDetail());
            int offsetX = dataOffset.x - baseOffset.x;
            int offsetZ = dataOffset.z - baseOffset.z;
            LodUtil.assertTrue(offsetX >= 0 && offsetX < SECTION_SIZE && offsetZ >= 0 && offsetZ < SECTION_SIZE);
            isEmpty = false;
            data.get(0,0).deepCopyTo(get(offsetX, offsetZ));
        } else {
            LodUtil.assertNotReach();
            //TODO;
        }

    }

    @Override
    public boolean isEmpty() {
        return isEmpty;
    }

    public void markNotEmpty() {
        isEmpty = false;
    }

    @Override
    public void saveData(IDhLevel level, DataMetaFile file, OutputStream dataStream) throws IOException {
        DataOutputStream dos = new DataOutputStream(dataStream); // DO NOT CLOSE
        {
            dos.writeInt(getDataDetail());
            dos.writeInt(size);
            dos.writeInt(level.getMinY());
            if (isEmpty) {
                dos.writeInt(0x00000001);
                return;
            }

            // Is column not empty
            dos.writeInt(0xFFFFFFFF);
            byte[] bytes = isColumnNotEmpty.toByteArray();
            dos.writeInt(bytes.length);
            dos.write(bytes);

            // Data array content
            dos.writeInt(0xFFFFFFFF);
            for (int i = isColumnNotEmpty.nextSetBit(0); i >= 0; i = isColumnNotEmpty.nextSetBit(i + 1))
            {
                dos.writeByte(dataArrays[i].length);
                if (dataArrays[i].length == 0) continue;
                for (long l : dataArrays[i]) {
                    dos.writeLong(l);
                }
            }

            // Id mapping
            dos.writeInt(0xFFFFFFFF);
            mapping.serialize(dos);
            dos.writeInt(0xFFFFFFFF);
        }
    }


    public static SpottyDataSource loadData(DataMetaFile dataFile, InputStream dataStream, IDhLevel level) throws IOException {
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
            if (end == 0x00000001) {
                // Section is empty
                return new SpottyDataSource(dataFile.pos);
            }

            // Is column not empty
            if (end != 0xFFFFFFFF) throw new IOException("invalid header end guard");
            int length = dos.readInt();

            if (length < 0 || length > (SECTION_SIZE*SECTION_SIZE/8+64)*2)
                throw new IOException(LodUtil.formatLog("Spotty Flag BitSet size outside reasonable range: {} (expects {} to {})",
                        length, 1, SECTION_SIZE*SECTION_SIZE/8+63));
            byte[] bytes = dos.readNBytes(length);
            BitSet isColumnNotEmpty = BitSet.valueOf(bytes);

            // Data array content
            long[][] data = new long[SECTION_SIZE*SECTION_SIZE][];
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid spotty flag end guard");
            for (int i = isColumnNotEmpty.nextSetBit(0); i >= 0; i = isColumnNotEmpty.nextSetBit(i + 1))
            {
                long[] array = new long[dos.readByte()];
                for (int j = 0; j < array.length; j++) {
                    array[j] = dos.readLong();
                }
                data[i] = array;
            }

            // Id mapping
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid data content end guard");
            FullDataPointIdMap mapping = FullDataPointIdMap.deserialize(new UnclosableInputStream(dos));
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid id mapping end guard");
            return new SpottyDataSource(dataFile.pos, mapping, isColumnNotEmpty, data);
        }
    }

    private SpottyDataSource(DhSectionPos pos, FullDataPointIdMap mapping, BitSet isColumnNotEmpty, long[][] data) {
        super(mapping, data, SECTION_SIZE);
        LodUtil.assertTrue(data.length == SECTION_SIZE*SECTION_SIZE);
        this.sectionPos = pos;
        this.isColumnNotEmpty = isColumnNotEmpty;
        isEmpty = false;
    }

    public static SpottyDataSource createEmpty(DhSectionPos pos) {
        return new SpottyDataSource(pos);
    }

    public static boolean neededForPosition(DhSectionPos posToWrite, DhSectionPos posToTest) {
        if (!posToWrite.overlaps(posToTest)) return false;
        if (posToTest.sectionDetail > posToWrite.sectionDetail) return false;
        if (posToWrite.sectionDetail - posToTest.sectionDetail <= SECTION_SIZE_OFFSET) return true;
        byte sectPerData = (byte) (1 << (posToWrite.sectionDetail - posToTest.sectionDetail - SECTION_SIZE_OFFSET));
        return posToTest.sectionX % sectPerData == 0 && posToTest.sectionZ % sectPerData == 0;
    }

    @Override
    public void sampleFrom(ILodDataSource source) {
        DhSectionPos pos = source.getSectionPos();
        LodUtil.assertTrue(pos.sectionDetail < sectionPos.sectionDetail);
        LodUtil.assertTrue(pos.overlaps(sectionPos));
        if (source.isEmpty()) return;
        if (source instanceof SparseDataSource) {
            sampleFrom((SparseDataSource) source);
        } else if (source instanceof FullDataSource) {
            sampleFrom((FullDataSource) source);
        } else {
            LodUtil.assertNotReach();
        }
    }

    private void sampleFrom(SparseDataSource sparseSource) {
        DhSectionPos pos = sparseSource.getSectionPos();
        isEmpty = false;

        if (getDataDetail() > sectionPos.sectionDetail) {
            DhLodPos basePos = sectionPos.getCorner(getDataDetail());
            DhLodPos dataPos = pos.getCorner(getDataDetail());
            int offsetX = dataPos.x - basePos.x;
            int offsetZ = dataPos.z - basePos.z;
            LodUtil.assertTrue(offsetX >= 0 && offsetX < SECTION_SIZE && offsetZ >= 0 && offsetZ < SECTION_SIZE);
            int chunksPerData = 1 << (getDataDetail() - SparseDataSource.SPARSE_UNIT_DETAIL);
            int dataSpan = sectionPos.getWidth(getDataDetail()).numberOfLodSectionsWide;

            for (int ox = 0; ox < dataSpan; ox++) {
                for (int oz = 0; oz < dataSpan; oz++) {
                    SingleFullArrayView column = sparseSource.tryGet(
                            ox * chunksPerData * sparseSource.dataPerChunk,
                            oz * chunksPerData * sparseSource.dataPerChunk);
                    if (column != null) {
                        column.deepCopyTo(get(offsetX + ox, offsetZ + oz));
                        isColumnNotEmpty.set((offsetX + ox) * SECTION_SIZE + offsetZ + oz, true);
                    }
                }
            }
        } else {
            DhLodPos dataPos = pos.getSectionBBoxPos();
            int lowerSectionsPerData = sectionPos.getWidth(dataPos.detailLevel).numberOfLodSectionsWide;
            if (dataPos.x % lowerSectionsPerData != 0 || dataPos.z % lowerSectionsPerData != 0) return;

            DhLodPos basePos = sectionPos.getCorner(getDataDetail());
            dataPos = dataPos.convertUpwardsTo(getDataDetail());
            int offsetX = dataPos.x - basePos.x;
            int offsetZ = dataPos.z - basePos.z;
            SingleFullArrayView column = sparseSource.tryGet(0, 0);
            if (column != null) {
                column.deepCopyTo(get(offsetX, offsetZ));
                isColumnNotEmpty.set(offsetX * SECTION_SIZE + offsetZ, true);
            }
        }
    }

    private void sampleFrom(FullDataSource fullSource) {
        DhSectionPos pos = fullSource.getSectionPos();
        isEmpty = false;
        downsampleFrom(fullSource);

        if (getDataDetail() > sectionPos.sectionDetail) {
            DhLodPos basePos = sectionPos.getCorner(getDataDetail());
            DhLodPos dataPos = pos.getCorner(getDataDetail());
            int offsetX = dataPos.x - basePos.x;
            int offsetZ = dataPos.z - basePos.z;
            int dataSpan = sectionPos.getWidth(getDataDetail()).numberOfLodSectionsWide;
            for (int ox = 0; ox < dataSpan; ox++) {
                for (int oz = 0; oz < dataSpan; oz++) {
                    isColumnNotEmpty.set((offsetX + ox) * SECTION_SIZE + offsetZ + oz, true);
                }
            }
        } else {
            DhLodPos dataPos = pos.getSectionBBoxPos();
            int lowerSectionsPerData = sectionPos.getWidth(dataPos.detailLevel).numberOfLodSectionsWide;
            if (dataPos.x % lowerSectionsPerData != 0 || dataPos.z % lowerSectionsPerData != 0) return;
            DhLodPos basePos = sectionPos.getCorner(getDataDetail());
            dataPos = dataPos.convertUpwardsTo(getDataDetail());
            int offsetX = dataPos.x - basePos.x;
            int offsetZ = dataPos.z - basePos.z;
            isColumnNotEmpty.set(offsetX * SECTION_SIZE + offsetZ, true);
        }

    }

    @Override
    public ILodDataSource trySelfPromote() {
        if (isEmpty) return this;
        if (isColumnNotEmpty.cardinality() != SECTION_SIZE * SECTION_SIZE) return this;
        return new FullDataSource(sectionPos, mapping, dataArrays);
    }

    @Override
    public SingleFullArrayView tryGet(int x, int z) {
        return isColumnNotEmpty.get(x * SECTION_SIZE + z) ? get(x, z) : null;
    }
}
