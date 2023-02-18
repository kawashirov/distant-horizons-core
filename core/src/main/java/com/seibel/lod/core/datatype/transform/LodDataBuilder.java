package com.seibel.lod.core.datatype.transform;

import com.seibel.lod.core.datatype.full.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.datatype.full.FullDataPoint;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class LodDataBuilder {
    private static final IBlockStateWrapper AIR = SingletonInjector.INSTANCE.get(IWrapperFactory.class).getAirBlockStateWrapper();
    public static ChunkSizedFullDataSource createChunkData(IChunkWrapper chunk) {
        if (!canGenerateLodFromChunk(chunk)) return null;

        ChunkSizedFullDataSource chunkData = new ChunkSizedFullDataSource((byte)0, chunk.getChunkPos().x, chunk.getChunkPos().z);

        for (int x=0; x<16; x++) {
            for (int z=0; z<16; z++) {
                LongArrayList longs = new LongArrayList(chunk.getHeight()/4);
                int lastY = chunk.getMaxBuildHeight();
                IBiomeWrapper biome = chunk.getBiome(x, lastY, z);
                IBlockStateWrapper blockState = AIR;
                int mappedId = chunkData.getMapping().addIfNotPresentAndGetId(biome, blockState);
                // FIXME: The +1 offset to reproduce the old behavior. Remove this when we get per-face lighting
                byte light = (byte) ((chunk.getBlockLight(x,lastY+1,z) << 4) + chunk.getSkyLight(x,lastY+1,z));
                int y=chunk.getMaxY(x, z);

                for (; y>=chunk.getMinBuildHeight(); y--) {
                    IBiomeWrapper newBiome = chunk.getBiome(x, y, z);
                    IBlockStateWrapper newBlockState = chunk.getBlockState(x, y, z);
                    byte newLight = (byte) ((chunk.getBlockLight(x,y+1,z) << 4) + chunk.getSkyLight(x,y+1,z));

                    if (!newBiome.equals(biome) || !newBlockState.equals(blockState)) {
                        longs.add(FullDataPoint.encode(mappedId, lastY-y, y+1 - chunk.getMinBuildHeight(), light));
                        biome = newBiome;
                        blockState = newBlockState;
                        mappedId = chunkData.getMapping().addIfNotPresentAndGetId(biome, blockState);
                        light = newLight;
                        lastY = y;
                    }
//                    else if (newLight != light) {
//                        longs.add(FullFormat.encode(mappedId, lastY-y, y+1 - chunk.getMinBuildHeight(), light));
//                        light = newLight;
//                        lastY = y;
//                    }
                }
                longs.add(FullDataPoint.encode(mappedId, lastY-y, y+1 - chunk.getMinBuildHeight(), light));

                chunkData.setSingleColumn(longs.toArray(new long[0]), x, z);
            }
        }
        LodUtil.assertTrue(chunkData.emptyCount() == 0);
        return chunkData;
    }

    public static boolean canGenerateLodFromChunk(IChunkWrapper chunk)
    {
        //return true;
        return chunk != null &&
                chunk.isLightCorrect();
    }
}
