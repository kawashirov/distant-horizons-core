package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.datatype.full.accessor.FullArrayView;
import com.seibel.lod.core.a7.datatype.full.accessor.SingleFullArrayView;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.pos.DhLodPos;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.BitSet;

public class SparseDataSource implements LodDataSource {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final byte SPARSE_UNIT_DETAIL = 4;
    public static final byte SPARSE_UNIT_SIZE = 1 << SPARSE_UNIT_DETAIL;

    public static final byte SECTION_SIZE_OFFSET = 6;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
    public static final byte MAX_SECTION_DETAIL = SECTION_SIZE_OFFSET + SPARSE_UNIT_DETAIL;
    public static final byte LATEST_VERSION = 0;
    public static final long TYPE_ID = "SparseDataSource".hashCode();
    protected final IdBiomeBlockStateMap mapping;
    private final DhSectionPos sectionPos;
    private final FullArrayView[] sparseData;
    private final int chunks;
    private final int dataPerChunk;
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
        mapping = new IdBiomeBlockStateMap();
    }
    protected SparseDataSource(DhSectionPos sectionPos, IdBiomeBlockStateMap mapping, FullArrayView[] data) {
        LodUtil.assertTrue(sectionPos.sectionDetail > SPARSE_UNIT_DETAIL);
        LodUtil.assertTrue(sectionPos.sectionDetail <= MAX_SECTION_DETAIL);
        this.sectionPos = sectionPos;
        chunks = 1 << (byte) (sectionPos.sectionDetail - SPARSE_UNIT_DETAIL);
        dataPerChunk = SECTION_SIZE / chunks;
        LodUtil.assertTrue(chunks*chunks == data.length);
        sparseData = data;
        chunkPos = sectionPos.getCorner(SPARSE_UNIT_DETAIL);
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
    public void setLocalVersion(int localVer) {
        //TODO: implement
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
        sparseData[arrayOffset] = newArray;
    }

    @Override
    public void saveData(ILevel level, DataMetaFile file, OutputStream dataStream) throws IOException {
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

    public static SparseDataSource loadData(DataMetaFile dataFile, InputStream dataStream, ILevel level) throws IOException {
        LodUtil.assertTrue(dataFile.pos.sectionDetail > SPARSE_UNIT_DETAIL);
        LodUtil.assertTrue(dataFile.pos.sectionDetail <= MAX_SECTION_DETAIL);
        try (DataInputStream dos = new DataInputStream(dataStream)) {
            int dataDetail = dos.readShort();
            if(dataDetail != dataFile.dataLevel)
                throw new IOException(LodUtil.formatLog("Data level mismatch: {} != {}", dataDetail, dataFile.dataLevel));
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

            if (length <= 0 || length > chunks*chunks/8+64)
                throw new IOException(LodUtil.formatLog("Sparse Flag BitSet size outside reasonable range: {} (expects {} to {})",
                        length, 1, chunks*chunks/8+63));
            byte[] bytes = dos.readNBytes(length);
            BitSet set = BitSet.valueOf(bytes);
            if (set.size() < chunks*chunks)
                throw new IOException((LodUtil.formatLog("Sparse Flag BitSet too small: {} != {}*{}",
                        set.size(), chunks, chunks)));

            long[][][] dataChunks = new long[chunks*chunks][][];

            // Data array content (only on non-empty columns)
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid data length end guard");
            for (int i = set.nextSetBit(0); i >= 0 && i < dataChunks.length; i = set.nextSetBit(i + 1)) {
                long[][] dataColumns = new long[dataPerChunk*dataPerChunk][];
                dataChunks[i] = dataColumns;
                for (int j = 0; j < dataColumns.length; j++) {
                    dataColumns[i] = new long[dos.readByte()];
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
            IdBiomeBlockStateMap mapping = IdBiomeBlockStateMap.deserialize(dos);
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid id mapping end guard");

            FullArrayView[] objectChunks = new FullArrayView[chunks*chunks];
            for (int i=0; i<dataChunks.length; i++) {
                if (dataChunks[i] == null) continue;
                objectChunks[i] = new FullArrayView(mapping, new long[dataPerChunk * dataPerChunk][], dataPerChunk);
            }

            return new SparseDataSource(dataFile.pos, mapping, objectChunks);
        }
    }

    public void applyToFullDataSource(FullDataSource dataSource) {
        LodUtil.assertTrue(dataSource.getSectionPos().equals(sectionPos));
        LodUtil.assertTrue(dataSource.getDataDetail() == getDataDetail());
        for (int x = 0; x<chunks; x++) {
            for (int z = 0; z<chunks; z++) {
                FullArrayView array = sparseData[x*chunks+z];
                if (array == null) continue;
                // Otherwise, apply data to dataSource
                FullArrayView view = dataSource.subView(dataPerChunk, x*dataPerChunk, z*dataPerChunk);
                array.shadowCopyTo(view);
            }
        }
    }
}