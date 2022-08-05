package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.a7.datatype.column.ColumnRenderSource;
import com.seibel.lod.core.a7.datatype.full.accessor.FullArrayView;
import com.seibel.lod.core.a7.datatype.full.accessor.SingleFullArrayView;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.pos.DhBlockPos2D;
import com.seibel.lod.core.a7.save.io.file.DataMetaFile;
import com.seibel.lod.core.a7.datatype.LodDataSource;
import com.seibel.lod.core.a7.util.IdMappingUtil;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class FullDataSource extends FullArrayView implements LodDataSource { // 1 chunk
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final byte SECTION_SIZE_OFFSET = ColumnRenderSource.SECTION_SIZE_OFFSET;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
    public static final byte LATEST_VERSION = 0;
    public static final long TYPE_ID = "FullDataSource".hashCode();
    private final DhSectionPos sectionPos;
    private int localVersion = 0;
    protected FullDataSource(DhSectionPos sectionPos) {
        super(new IdBiomeBlockStateMap(), new long[SECTION_SIZE*SECTION_SIZE][0], SECTION_SIZE);
        this.sectionPos = sectionPos;
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
        localVersion = localVer;
    }

    @Override
    public byte getDataVersion() {
        return LATEST_VERSION;
    }

    @Override
    public void update(ChunkSizedData data) {
        if (getDataDetail() == 0 && data.dataDetail == 0) {
            DhBlockPos2D chunkBlockPos = new DhBlockPos2D(data.x * 16, data.z * 16);
            DhBlockPos2D blockOffset = chunkBlockPos.subtract(sectionPos.getCorner().getCorner());
            LodUtil.assertTrue(blockOffset.x >= 0 && blockOffset.x < SECTION_SIZE && blockOffset.z >= 0 && blockOffset.z < SECTION_SIZE,
                    "ChunkWrite of {} outside section {}. (cal offset {} larger than {})",
                    new DHChunkPos(data.x, data.z), sectionPos, blockOffset, SECTION_SIZE);
            data.shadowCopyTo(this.subView(16, blockOffset.x, blockOffset.z));

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    SingleFullArrayView column = this.get(x+ blockOffset.x, z+ blockOffset.z);
                    LodUtil.assertTrue(column.doesItExist());
                }
            }


        } else {
            LodUtil.assertNotReach();
            //TODO;
        }

    }

    @Override
    public void saveData(ILevel level, DataMetaFile file, OutputStream dataStream) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(dataStream)) {
            dos.writeInt(getDataDetail());
            dos.writeInt(size);
            dos.writeInt(level.getMinY());
            dos.writeInt(0xFFFFFFFF);
            // Data array length
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    dos.writeByte(get(x, z).getSingleLength());
                }
            }
            // Data array content (only on non-empty columns)
            dos.writeInt(0xFFFFFFFF);
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    SingleFullArrayView column = get(x, z);
                    if (!column.doesItExist()) continue;
                    long[] raw = column.getRaw();
                    for (long l : raw) {
                        dos.writeLong(l);
                    }
                }
            }
            // Id mapping
            dos.writeInt(0xFFFFFFFF);
            mapping.serialize(dos);
            dos.writeInt(0xFFFFFFFF);
        }
    }


    public static FullDataSource loadData(DataMetaFile dataFile, InputStream dataStream, ILevel level) throws IOException {
        try (DataInputStream dos = new DataInputStream(dataStream)) {
            int dataDetail = dos.readInt();
            if(dataDetail != dataFile.dataLevel)
                throw new IOException(LodUtil.formatLog("Data level mismatch: {} != {}", dataDetail, dataFile.dataLevel));
            int size = dos.readInt();
            if (size != SECTION_SIZE)
                throw new IOException(LodUtil.formatLog(
                        "Section size mismatch: {} != {} (Currently only 1 section size is supported)", size, SECTION_SIZE));
            int minY = dos.readInt();
            if (minY != level.getMinY())
                LOGGER.warn("Data minY mismatch: {} != {}. Will ignore data's y level", minY, level.getMinY());
            int end = dos.readInt();
            // Data array length
            if (end != 0xFFFFFFFF) throw new IOException("invalid header end guard");
            long[][] data = new long[size*size][];
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    data[x*size+z] = new long[dos.readByte()];
                }
            }
            // Data array content (only on non-empty columns)
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid data length end guard");
            for (int i = 0; i < data.length; i++) {
                if (data[i].length == 0) continue;
                for (int j = 0; j < data[i].length; j++) {
                    data[i][j] = dos.readLong();
                }
            }
            // Id mapping
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid data content end guard");
            IdBiomeBlockStateMap mapping = IdBiomeBlockStateMap.deserialize(dos);
            end = dos.readInt();
            if (end != 0xFFFFFFFF) throw new IOException("invalid id mapping end guard");
            return new FullDataSource(dataFile.pos, mapping, data);
        }
    }

    private FullDataSource(DhSectionPos pos, IdBiomeBlockStateMap mapping, long[][] data) {
        super(mapping, data, SECTION_SIZE);
        LodUtil.assertTrue(data.length == SECTION_SIZE*SECTION_SIZE);
        this.sectionPos = pos;
    }

    public static FullDataSource createEmpty(DhSectionPos pos) {
        return new FullDataSource(pos);
    }
}
