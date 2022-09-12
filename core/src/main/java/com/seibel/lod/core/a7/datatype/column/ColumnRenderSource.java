package com.seibel.lod.core.a7.datatype.column;

import com.seibel.lod.core.a7.datatype.column.accessor.*;
import com.seibel.lod.core.a7.datatype.column.render.ColumnRenderBuffer;
import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.transform.FullToColumnTransformer;
import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.render.RenderBuffer;
import com.seibel.lod.core.a7.render.a7LodRenderer;
import com.seibel.lod.core.a7.save.io.render.RenderMetaFile;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.a7.render.LodQuadTree;
import com.seibel.lod.core.a7.render.LodRenderSection;
import com.seibel.lod.core.a7.datatype.LodRenderSource;
import com.seibel.lod.core.util.Reference;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class ColumnRenderSource implements LodRenderSource, IColumnDatatype {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    public static final boolean DO_SAFETY_CHECKS = true;
    public static final byte SECTION_SIZE_OFFSET = 6;
    public static final int SECTION_SIZE = 1 << SECTION_SIZE_OFFSET;
    public static final byte LATEST_VERSION = 1;
    public static final long TYPE_ID = "ColumnRenderSource".hashCode();
    public static final int AIR_LODS_SIZE = 16;
    public static final int AIR_SECTION_SIZE = SECTION_SIZE/AIR_LODS_SIZE;

    public final int verticalSize;
    public final DhSectionPos sectionPos;
    public final int yOffset;

    public final long[] dataContainer;
    public final int[] airDataContainer;

    public boolean isEmpty = true;

    /**
     * Constructor of the ColumnDataType
     * @param maxVerticalSize the maximum vertical size of the container
     */
    public ColumnRenderSource(DhSectionPos sectionPos, int maxVerticalSize, int yOffset) {
        verticalSize = maxVerticalSize;
        dataContainer = new long[SECTION_SIZE * SECTION_SIZE * verticalSize];
        airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * verticalSize];
        this.sectionPos = sectionPos;
        this.yOffset = yOffset;
    }

    private long[] loadData(DataInputStream inputData, int version, int verticalSize) throws IOException {
        switch (version) {
            case 1:
                return readDataV1(inputData, verticalSize);
            default:
                throw new IOException("Invalid Data: The version of the data is not supported");
        }
    }
    private long[] readDataV1(DataInputStream inputData, int tempMaxVerticalData) throws IOException {
        int x = SECTION_SIZE * SECTION_SIZE * tempMaxVerticalData;
        short tempMinHeight = Short.reverseBytes(inputData.readShort());
        if (tempMinHeight == Short.MAX_VALUE) { //FIXME: Temp hack flag for marking a empty section
            return new long[x];
        }
        isEmpty = false;
        byte[] data = new byte[x * Long.BYTES];
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        inputData.readFully(data);
        long[] result = new long[x];
        bb.asLongBuffer().get(result);
        if (tempMinHeight != yOffset) {
            for (int i=0; i<result.length; i++) {
                result[i] = ColumnFormat.shiftHeightAndDepth(result[i], (short) (tempMinHeight - yOffset));
            }
        }
        return result;
    }

    // Load from data stream with maxVerticalSize loaded from the data stream
    public ColumnRenderSource(DhSectionPos sectionPos, DataInputStream inputData, int version, ILevel level) throws IOException {
        this.sectionPos = sectionPos;
        yOffset = level.getMinY();
        byte detailLevel = inputData.readByte();
        if (sectionPos.sectionDetail - SECTION_SIZE_OFFSET != detailLevel) {
            throw new IOException("Invalid data: detail level does not match");
        }
        verticalSize = inputData.readByte() & 0b01111111;
        dataContainer = loadData(inputData, version, verticalSize);
        airDataContainer = new int[AIR_SECTION_SIZE * AIR_SECTION_SIZE * verticalSize];
    }

    @Override
    public void clear(int posX, int posZ)
    {
        for (int verticalIndex = 0; verticalIndex < verticalSize; verticalIndex++)
            dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex] =
                    ColumnFormat.EMPTY_DATA;
    }


    @Override
    public boolean addData(long data, int posX, int posZ, int verticalIndex)
    {
        dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex] = data;
        return true;
    }

    @Override
    public boolean copyVerticalData(IColumnDataView data, int posX, int posZ, boolean override) {
        if (DO_SAFETY_CHECKS) {
            if (data.size() != verticalSize)
                throw new IllegalArgumentException("data size not the same as vertical size");
            if (posX < 0 || posX >= SECTION_SIZE)
                throw new IllegalArgumentException("X position is out of bounds");
            if (posZ < 0 || posZ >= SECTION_SIZE)
                throw new IllegalArgumentException("Z position is out of bounds");
        }
        int index = posX * SECTION_SIZE * verticalSize + posZ * verticalSize;
        int compare = ColumnFormat.compareDatapointPriority(data.get(0), dataContainer[index]);
        if (override) {
            if (compare<0) return false;
        } else {
            if (compare<=0) return false;
        }
        data.copyTo(dataContainer, index, data.size());
        return true;
    }

    @Override
    public long getData(int posX, int posZ, int verticalIndex)
    {
        return dataContainer[posX * SECTION_SIZE * verticalSize + posZ * verticalSize + verticalIndex];
    }

    @Override
    public long[] getAllData(int posX, int posZ)
    {
        long[] result = new long[verticalSize];
        int index = posX * SECTION_SIZE * verticalSize + posZ * verticalSize;
        System.arraycopy(dataContainer, index, result, 0, verticalSize);
        return result;
    }

    @Override
    public ColumnArrayView getVerticalDataView(int posX, int posZ) {
        return new ColumnArrayView(dataContainer, verticalSize,
                posX * SECTION_SIZE * verticalSize + posZ * verticalSize, verticalSize);
    }

    @Override
    public ColumnQuadView getDataInQuad(int quadX, int quadZ, int quadXSize, int quadZSize) {
        return new ColumnQuadView(dataContainer, SECTION_SIZE, verticalSize, quadX, quadZ, quadXSize, quadZSize);
    }
    @Override
    public ColumnQuadView getFullQuad() {
        return new ColumnQuadView(dataContainer, SECTION_SIZE, verticalSize, 0, 0, SECTION_SIZE, SECTION_SIZE);
    }

    @Override
    public int getVerticalSize()
    {
        return verticalSize;
    }

    @Override
    public boolean doesItExist(int posX, int posZ)
    {
        return ColumnFormat.doesItExist(getSingleData(posX, posZ));
    }

    @Override
    public void generateData(IColumnDatatype lowerDataContainer, int posX, int posZ)
    {
        ColumnQuadView quadView = lowerDataContainer.getDataInQuad(posX*2, posZ*2, 2,2);
        ColumnArrayView outputView = getVerticalDataView(posX, posZ);
        outputView.mergeMultiDataFrom(quadView);
    }

    boolean writeData(DataOutputStream output) throws IOException {
        output.writeByte(getDataDetail());
        output.writeByte((byte) verticalSize);
        // FIXME: yOffset is a int, but we only are writing a short.
        if (isEmpty) {
            output.writeByte(Short.MAX_VALUE & 0xFF);
            output.writeByte((Short.MAX_VALUE >> 8) & 0xFF);
            return false;
        }
        output.writeByte((byte) (yOffset & 0xFF));
        output.writeByte((byte) ((yOffset >> 8) & 0xFF));
        boolean allGenerated = true;
        int x = SECTION_SIZE * SECTION_SIZE;
        for (int i = 0; i < x; i++)
        {
            for (int j = 0; j < verticalSize; j++)
            {
                long current = dataContainer[i * verticalSize + j];
                if (ColumnFormat.doesItExist(current))
                    current = ColumnFormat.overrideGenerationMode(current, (byte) 1);
                output.writeLong(Long.reverseBytes(current));
            }
            if (!ColumnFormat.doesItExist(dataContainer[i]))
                allGenerated = false;
        }
        return allGenerated;
    }

    public String toString()
    {
        String LINE_DELIMITER = "\n";
        String DATA_DELIMITER = " ";
        String SUBDATA_DELIMITER = ",";
        StringBuilder stringBuilder = new StringBuilder();
        int size = sectionPos.getWidth().value;
        stringBuilder.append(sectionPos);
        stringBuilder.append(LINE_DELIMITER);
        for (int z = 0; z < size; z++)
        {
            for (int x = 0; x < size; x++)
            {
                for (int y = 0; y < verticalSize; y++) {
                    //Converting the dataToHex
                    stringBuilder.append(Long.toHexString(getData(x,z,y)));
                    if (y != verticalSize-1) stringBuilder.append(SUBDATA_DELIMITER);
                }
                if (x != size-1) stringBuilder.append(DATA_DELIMITER);
            }
            if (z != size-1) stringBuilder.append(LINE_DELIMITER);
        }
        return stringBuilder.toString();
    }

    @Override
    public int getMaxNumberOfLods()
    {
        return SECTION_SIZE * SECTION_SIZE * getVerticalSize();
    }

    @Override
    public long getRoughRamUsage()
    {
        return (long) dataContainer.length * Long.BYTES;
    }

    public DhSectionPos getSectionPos() {
        return sectionPos;
    }

    public byte getDataDetail() {
        return (byte) (sectionPos.sectionDetail - SECTION_SIZE_OFFSET);
    }

    @Override
    public byte getDetailOffset() {
        return SECTION_SIZE_OFFSET;
    }

    private CompletableFuture<ColumnRenderBuffer[]> inBuildRenderBuffer = null;
    private Reference<ColumnRenderBuffer> usedBufferOpaque = new Reference<>();
    private Reference<ColumnRenderBuffer> usedBufferTransparent = new Reference<>();


    private void tryBuildBuffer(IClientLevel level, LodQuadTree quadTree) {
        if (inBuildRenderBuffer == null && !ColumnRenderBuffer.isBusy() && !isEmpty) {
            ColumnRenderSource[] data = new ColumnRenderSource[ELodDirection.ADJ_DIRECTIONS.length];
            for (ELodDirection direction : ELodDirection.ADJ_DIRECTIONS) {
                LodRenderSection section = quadTree.getSection(sectionPos.getAdjacent(direction)); //FIXME: Handle traveling through different detail levels
                if (section != null && section.getRenderSource() != null && section.getRenderSource() instanceof ColumnRenderSource) {
                    data[direction.ordinal()-2] = ((ColumnRenderSource) section.getRenderSource());
                }
            }
            inBuildRenderBuffer = ColumnRenderBuffer.build(level, usedBufferOpaque, usedBufferTransparent, this, data);
        }
    }
    private void cancelBuildBuffer() {
        if (inBuildRenderBuffer != null) {
            //LOGGER.info("Cancelling build of render buffer for {}", sectionPos);
            inBuildRenderBuffer.cancel(true);
            inBuildRenderBuffer = null;
        }
    }

    private IClientLevel level = null; //FIXME: hack to pass level into tryBuildBuffer
    @Override
    public void enableRender(IClientLevel level, LodQuadTree quadTree) {
        this.level = level;
        //tryBuildBuffer(level, quadTree);
    }

    @Override
    public void disableRender() {
        cancelBuildBuffer();
    }

    @Override
    public boolean isRenderReady() {
        return inBuildRenderBuffer == null || inBuildRenderBuffer.isDone();
    }

    @Override
    public void dispose() {
        cancelBuildBuffer();
    }

    //FIXME: Temp Hack
    private long lastNs = -1;
    private static final long SWAP_TIMEOUT = /* 10 sec */ 10_000_000_000L;
    private static final long SWAP_BUSY_COLLISION_TIMEOUT = /* 1 sec */ 1_000_000_000L;

    @Override
    public boolean trySwapRenderBuffer(LodQuadTree quadTree, AtomicReference<RenderBuffer> referenceSlotsOpaque, AtomicReference<RenderBuffer> referenceSlotsTransparent) {
        if (lastNs != -1 && System.nanoTime() - lastNs < SWAP_TIMEOUT) {
            return false;
        }
        if (inBuildRenderBuffer != null) {
            if (inBuildRenderBuffer.isDone()) {
                lastNs = System.nanoTime();
                //LOGGER.info("Swapping render buffer for {}", sectionPos);

                RenderBuffer[] newBuffers = inBuildRenderBuffer.join();

                RenderBuffer oldBuffersOpaque = referenceSlotsOpaque.getAndSet(newBuffers[0]);

                ColumnRenderBuffer swapped;


                if (oldBuffersOpaque instanceof ColumnRenderBuffer) {
                    swapped = usedBufferOpaque.swap((ColumnRenderBuffer) oldBuffersOpaque);
                    LodUtil.assertTrue(swapped == null);
                }

                if(a7LodRenderer.transparencyEnabled) {
                    RenderBuffer oldBuffersTransparent = referenceSlotsTransparent.getAndSet(newBuffers[1]);

                    if (a7LodRenderer.transparencyEnabled) {
                        if (oldBuffersTransparent instanceof ColumnRenderBuffer) {
                            swapped = usedBufferTransparent.swap((ColumnRenderBuffer) oldBuffersTransparent);
                            LodUtil.assertTrue(swapped == null);
                        }
                    }
                }
                inBuildRenderBuffer = null;
                return true;
            }
        } else {
            if (!isEmpty) {
                if (ColumnRenderBuffer.isBusy()) {
                    lastNs += (long) (SWAP_BUSY_COLLISION_TIMEOUT * Math.random());
                } else tryBuildBuffer(level, quadTree);
            }
        }
        return false;
    }

    @Override
    public void saveRender(IClientLevel level, RenderMetaFile file, OutputStream dataStream) throws IOException {
         DataOutputStream dos = new DataOutputStream(dataStream); // DO NOT CLOSE
         writeData(dos);
    }

    @Override
    public byte getRenderVersion() {
        return LATEST_VERSION;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void weakWrite(LodRenderSource source) {
        LodUtil.assertTrue(source instanceof ColumnRenderSource);
        ColumnRenderSource src = (ColumnRenderSource) source;

        LodUtil.assertTrue(src.sectionPos.equals(sectionPos));
        LodUtil.assertTrue(src.verticalSize == verticalSize);

        if (src.isEmpty) return;
        isEmpty = false;

        for (int i=0; i<dataContainer.length; i+=verticalSize) {
            int genMode = ColumnFormat.getGenerationMode(dataContainer[i]);
            int srcGenMode = ColumnFormat.getGenerationMode(src.dataContainer[i]);
            if (srcGenMode == 0) continue;
            if (genMode <= srcGenMode) {
                new ColumnArrayView(dataContainer, verticalSize, i, verticalSize).copyFrom(
                    new ColumnArrayView(src.dataContainer, verticalSize, i, verticalSize));
            }
        }
    }

    @Override
    public void fastWrite(ChunkSizedData chunkData, IClientLevel level) {
        FullToColumnTransformer.writeFullDataChunkToColumnData(this, level, chunkData);
    }
}
