package com.seibel.lod.core.dataObjects.fullData.accessor;

public interface IFullDataType {
    byte getDetailOffset();
    default int getDataSize() {
        return 1 << getDetailOffset();
    }
    long getRoughRamUsage();
    int getVerticalSize(int posX, int posZ);
    boolean doesItExist(int posX, int posZ);
    byte getGenModeAtChunk(int chunkX, int chunkZ);
    SingleFullArrayView getDataAtColumn(int posX, int posZ);
}
