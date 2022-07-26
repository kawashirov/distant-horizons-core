package com.seibel.lod.core.a7.datatype.transform;

import com.seibel.lod.core.a7.datatype.full.ChunkSizedData;
import com.seibel.lod.core.a7.datatype.full.FullFormat;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class LodDataBuilder {
    private static final IBlockStateWrapper AIR = SingletonInjector.INSTANCE.get(IWrapperFactory.class).getAirBlockStateWrapper();
    public static ChunkSizedData createChunkData(IChunkWrapper chunk) {
        if (!canGenerateLodFromChunk(chunk)) return null;

        ChunkSizedData chunkData = new ChunkSizedData();

        for (int x=0; x<16; x++) {
            for (int z=0; z<16; z++) {
                LongArrayList longs = new LongArrayList(chunk.getHeight()/4);
                int lastY = chunk.getMaxBuildHeight();
                IBiomeWrapper biome = chunk.getBiome(x, lastY, z);
                IBlockStateWrapper blockState = AIR;
                int mappedId = chunkData.getMapping().setAndGetId(biome, blockState);
                byte light = (byte) (chunk.getBlockLight(x,lastY,z) << 4 + chunk.getSkyLight(x,lastY,z));
                int y=chunk.getMaxY(x, z);

                for (; y>=chunk.getMinBuildHeight(); y--) {
                    IBiomeWrapper newBiome = chunk.getBiome(x, y, z);
                    IBlockStateWrapper newBlockState = chunk.getBlockState(x, y, z);
                    byte newLight = (byte) (chunk.getBlockLight(x,y,z) << 4 + chunk.getSkyLight(x,y,z));

                    if (!newBiome.equals(biome) || !newBlockState.equals(blockState)) {
                        longs.add(FullFormat.encode(mappedId, lastY-y+1, y+1, light));
                        biome = newBiome;
                        blockState = newBlockState;
                        mappedId = chunkData.getMapping().setAndGetId(biome, blockState);
                        light = newLight;
                        lastY = y;
                    } else if (newLight != light) {
                        longs.add(FullFormat.encode(mappedId, lastY-y+1, y+1, light));
                        light = newLight;
                        lastY = y;
                    }
                }
                longs.add(FullFormat.encode(mappedId, lastY-y+1, y+1, light));

                chunkData.setSingleColumn(longs.toArray(new long[0]), x, z);
            }
        }

        return chunkData;
    }

    public static boolean canGenerateLodFromChunk(IChunkWrapper chunk)
    {
        return chunk != null &&
                chunk.isLightCorrect();
    }
}
