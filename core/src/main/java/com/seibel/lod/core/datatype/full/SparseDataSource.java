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

    public static SparseDataSource createEmpty(DhSectionPos pos) {
        return new SparseDataSource(pos);
    }

    protected SparseDataSource(DhSectionPos sectionPos) {
        LodUtil.assertTrue(sectionPos.sectionDetail > SPARSE_UNIT_DETAIL);
        LodUtil.assertTrue(sectionPos.sectionDetail <= MAX_SECTION_DETAIL);
        this.sectionPos = sectionPos;
        chunks = 1 << (byte) (sectionPos.sectionDetail - SPARSE_UNIT_DETAIL);
        dataPerChunk = SECTION_SIZE / chunks;
        sparseData = new FullArrayView[chunks * chunks];
        chunkPos = sectionPos.getCorner(SPARSE_UNIT_DETAIL);
        mapping = new FullDataPointIdMap();
    }
    protected SparseDataSource(DhSectionPos sectionPos, FullDataPointIdMap mapping, FullArrayView[] data) {
        LodUtil.assertTrue(sectionPos.sectionDetail > SPARSE_UNIT_DETAIL);
        LodUtil.assertTrue(sectionPos.sectionDetail <= MAX_SECTION_DETAIL);
        this.sectionPos = sectionPos;
        chunks = 1 << (byte) (sectionPos.sectionDetail - SPARSE_UNIT_DETAIL);
        dataPerChunk = SECTION_SIZE / chunks;
        LodUtil.assertTrue(chunks*chunks == data.length);
        sparseData = data;
        chunkPos = sectionPos.getCorner(SPARSE_UNIT_DETAIL);
        isEmpty = false;
        this.mapping = mapping;
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

    private int calculateOffset(int cx, int cz) {
        int ox = cx - chunkPos.x;
        int oz = cz - chunkPos.z;
        LodUtil.assertTrue(ox >= 0 && oz >= 0 && ox < chunks && oz < chunks);
        return ox * chunks + oz;
    }


    @Override
    public void update(ChunkSizedData data) {
        if (data.dataDetail != 0) {
            //TODO: Disable the throw and instead just ignore the data.
            throw new IllegalArgumentException("SparseDataSource only supports dataDetail 0!");
        }
        int arrayOffset = calculateOffset(data.x, data.z);
        FullArrayView newArray = new FullArrayView(mapping, new long[dataPerChunk * dataPerChunk][], dataPerChunk);
        if (getDataDetail() == data.dataDetail) {
            data.shadowCopyTo(newArray);
        } else {
            int count = dataPerChunk;
            int dataPerCount = SPARSE_UNIT_SIZE / dataPerChunk;

            for (int ox = 0; ox < count; ox++) {
                for (int oz = 0; oz < count; oz++) {
                    SingleFullArrayView column = newArray.get(ox, oz);
                    column.downsampleFrom(data.subView(dataPerCount, ox * dataPerCount, oz * dataPerCount));
                }
            }
        }
        isEmpty = false;
        sparseData[arrayOffset] = newArray;
    }

    @Override
    public boolean isEmpty() {
        return isEmpty;
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

        DhLodPos basePos = sectionPos.getCorner(SPARSE_UNIT_DETAIL);
        DhLodPos dataPos = pos.getCorner(SPARSE_UNIT_DETAIL);
        int offsetX = dataPos.x-basePos.x;
        int offsetZ = dataPos.z-basePos.z;
        LodUtil.assertTrue(offsetX >=0 && offsetX < chunks && offsetZ >=0 && offsetZ < chunks);

        for (int ox = 0; ox < sparseSource.chunks; ox++) {
            for (int oz = 0; oz < sparseSource.chunks; oz++) {
                FullArrayView sourceChunk = sparseSource.sparseData[ox*sparseSource.chunks + oz];
                if (sourceChunk != null) {
                    FullArrayView buff = new FullArrayView(mapping, new long[dataPerChunk * dataPerChunk][], dataPerChunk);
                    buff.downsampleFrom(sourceChunk);
                    sparseData[(ox+offsetX)* chunks + (oz+offsetZ)] = buff;
                }
            }
        }
    }
    private void sampleFrom(FullDataSource fullSource) {
        DhSectionPos pos = fullSource.getSectionPos();
        isEmpty = false;

        DhLodPos basePos = sectionPos.getCorner(SPARSE_UNIT_DETAIL);
        DhLodPos dataPos = pos.getCorner(SPARSE_UNIT_DETAIL);
        int coveredChunks = pos.getWidth(SPARSE_UNIT_DETAIL).numberOfLodSectionsWide;
        int sourceDataPerChunk = SPARSE_UNIT_SIZE >>> fullSource.getDataDetail();
        LodUtil.assertTrue(coveredChunks*sourceDataPerChunk == FullDataSource.SECTION_SIZE);
        int offsetX = dataPos.x-basePos.x;
        int offsetZ = dataPos.z-basePos.z;
        LodUtil.assertTrue(offsetX >=0 && offsetX < chunks && offsetZ >=0 && offsetZ < chunks);

        for (int ox = 0; ox < coveredChunks; ox++) {
            for (int oz = 0; oz < coveredChunks; oz++) {
                FullArrayView sourceChunk = fullSource.subView(sourceDataPerChunk, ox*sourceDataPerChunk, oz*sourceDataPerChunk);
                FullArrayView buff = new FullArrayView(mapping, new long[dataPerChunk * dataPerChunk][], dataPerChunk);
                buff.downsampleFrom(sourceChunk);
                sparseData[(ox+offsetX)* chunks + (oz+offsetZ)] = buff;
            }
        }
    }

    @Override
    public void saveData(IDhLevel level, DataMetaFile file, OutputStream dataStream) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(dataStream)) {
            dos.writeShort(getDataDetail());
            dos.writeShort(SPARSE_UNIT_DETAIL);
            dos.writeInt(SECTION_SIZE);
            dos.writeInt(level.getMinY());
            if (isEmpty) {
                dos.writeInt(0x00000001);
                return;
            }
            dos.writeInt(0xFFFFFFFF);
            // sparse array existence bitset
            BitSet set = new BitSet(sparseData.length);
            for (int i = 0; i < sparseData.length; i++) set.set(i, sparseData[i] != null);
            byte[] bytes = set.toByteArray();
            dos.writeInt(bytes.length);
            dos.write(bytes);

            // Data array content (only on non-empty stuff)
            dos.writeInt(0xFFFFFFFF);
            for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
                FullArrayView array = sparseData[i];
                LodUtil.assertTrue(array != null);
                for (int x = 0; x < array.width(); x++) {
                    for (int z = 0; z < array.width(); z++) {
                        dos.writeByte(array.get(x, z).getSingleLength());
                    }
                }
                for (int x = 0; x < array.width(); x++) {
                    for (int z = 0; z < array.width(); z++) {
                        SingleFullArrayView column = array.get(x, z);
                        LodUtil.assertTrue(column.getMapping() == mapping); //MUST be exact equal!
                        if (!column.doesItExist()) continue;
                        long[] raw = column.getRaw();
                        for (long l : raw) {
                            dos.writeLong(l);
                        }
                    }
                }
            }
            // Id mapping
            dos.writeInt(0xFFFFFFFF);
            mapping.serialize(dos);
            dos.writeInt(0xFFFFFFFF);
        }
    }

    public static SparseDataSource loadData(DataMetaFile dataFile, InputStream dataStream, IDhLevel level) throws IOException {
        LodUtil.assertTrue(dataFile.pos.sectionDetail > SPARSE_UNIT_DETAIL);
        LodUtil.assertTrue(dataFile.pos.sectionDetail <= MAX_SECTION_DETAIL);
        DataInputStream dos = new DataInputStream(dataStream); // DO NOT CLOSE! It would close all related streams
        {
            int dataDetail = dos.readShort();
            if(dataDetail != dataFile.metaData.dataLevel)
                throw new IOException(LodUtil.formatLog("Data level mismatch: {} != {}", dataDetail, dataFile.metaData.dataLevel));
            int sparseDetail = dos.readShort();
            if (sparseDetail != SPARSE_UNIT_DETAIL)
                throw new IOException((LodUtil.formatLog("Unexpected sparse detail level: {} != {}",
                        sparseDetail, SPARSE_UNIT_DETAIL)));
            int size = dos.readInt();
            if (size != SECTION_SIZE)
                throw new IOException(LodUtil.formatLog(
                        "Section size mismatch: {} != {} (Currently only 1 section size is supported)", size, SECTION_SIZE));
            int chunks = 1 << (byte) (dataFile.pos.sectionDetail - sparseDetail);
            int dataPerChunk = size / chunks;

            int minY = dos.readInt();
            if (minY != level.getMinY())
                LOGGER.warn("Data minY mismatch: {} != {}. Will ignore data's y level", minY, level.getMinY());
            int end = dos.readInt();
            // Data array length
            if (end == 0x00000001) {
                // Section is empty
                return createEmpty(dataFile.pos);
            }

            // Non-empty section
            if (end != 0xFFFFFFFF) throw new IOException("invalid header end guard");
            int length = dos.readInt();

            if (length < 0 || length > (chunks*chunks/8+64)*2)
                throw new IOException(LodUtil.formatLog("Sparse Flag BitSet size outside reasonable range: {} (expects {} to {})",
                        length, 1, chunks*chunks/8+63));
            byte[] bytes = dos.readNBytes(length);
            BitSet set = BitSet.valueOf(bytes);

            long[][][] dataChunks = new long[chunks*chunks][][];

            // Data array content (only on non-empty columns)
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid data length end guard");
            for (int i = set.nextSetBit(0); i >= 0 && i < dataChunks.length; i = set.nextSetBit(i + 1)) {
                long[][] dataColumns = new long[dataPerChunk*dataPerChunk][];
                dataChunks[i] = dataColumns;
                for (int i2 = 0; i2 < dataColumns.length; i2++) {
                    dataColumns[i2] = new long[dos.readByte()];
                }
                for (int k = 0; k < dataColumns.length; k++) {
                    if (dataColumns[k].length == 0) continue;
                    for (int o = 0; o < dataColumns[k].length; o++) {
                        dataColumns[k][o] = dos.readLong();
                    }
                }
            }

            // Id mapping
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid data content end guard");
            FullDataPointIdMap mapping = FullDataPointIdMap.deserialize(dos);
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid id mapping end guard");

            FullArrayView[] objectChunks = new FullArrayView[chunks*chunks];
            for (int i=0; i<dataChunks.length; i++) {
                if (dataChunks[i] == null) continue;
                objectChunks[i] = new FullArrayView(mapping, dataChunks[i], dataPerChunk);
            }

            return new SparseDataSource(dataFile.pos, mapping, objectChunks);
        }
    }

    private void applyToFullDataSource(FullDataSource dataSource) {
        LodUtil.assertTrue(dataSource.getSectionPos().equals(sectionPos));
        LodUtil.assertTrue(dataSource.getDataDetail() == getDataDetail());
        for (int x = 0; x<chunks; x++) {
            for (int z = 0; z<chunks; z++) {
                FullArrayView array = sparseData[x*chunks+z];
                if (array == null) continue;
                // Otherwise, apply data to dataSource
                dataSource.markNotEmpty();
                FullArrayView view = dataSource.subView(dataPerChunk, x*dataPerChunk, z*dataPerChunk);
                array.shadowCopyTo(view);
            }
        }
    }

    public ILodDataSource trySelfPromote() {
        if (isEmpty) return this;
        for (FullArrayView array : sparseData) {
            if (array == null) return this;
        }
        FullDataSource newSource = FullDataSource.createEmpty(sectionPos);
        applyToFullDataSource(newSource);
        return newSource;
    }

    // Return null if doesn't exist
    public SingleFullArrayView tryGet(int x, int z) {
        LodUtil.assertTrue(x>=0 && x<SECTION_SIZE && z>=0 && z<SECTION_SIZE);
        int chunkX = x / dataPerChunk;
        int chunkZ = z / dataPerChunk;
        FullArrayView chunk = sparseData[chunkX * chunks + chunkZ];
        if (chunk == null) return null;
        return chunk.get(x % dataPerChunk, z % dataPerChunk);
    }
}
