package com.seibel.lod.core.generation;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.pos.DhChunkPos;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @version 2022-11-25
 */
public interface IGenerator extends AutoCloseable
{
	/**
	 * What is the detail/resolution of the data? (This will offset the generation granularity)
	 * (minimum detail is 0, maximum detail is 255) (though that high isn't really... realistic)
	 * (0 = 1x1 block per data, 1 = 2x2 block per data, 2 = 4x4 block per data... etc.)
	 * TODO: System currently only supports 1x1 block per data.
	 */
    byte getMinDataDetail();
    byte getMaxDataDetail();
    int getPriority();
	
	/**
	 * What is the min batch size of a single generation?
	 * (minimum return value is 4 since that's the MC chunk size)
	 * (4 -> 16x16 data per call, 5 -> 32x32 data per call, 6 -> 64x64 data per call... etc.)
	 */
    byte getMinGenerationGranularity();
	
	/**
	 * What is the max batch size of a single generation? The system will try to group tasks to the max batch size if possible
	 * (minimum return value is 4 since that's the MC chunk size)
	 * (4 -> 16x16 data per call, 5 -> 32x32 data per call, 6 -> 64x64 data per call... etc.)
	 */
    byte getMaxGenerationGranularity();
	
	/**
	 * Start a generation event
	 * (Note that the chunkPos is always aligned to the granularity)
	 * (For example, if the granularity is 4, data detail is 0, the chunkPos will be aligned to 16x16 blocks)
	 */
    CompletableFuture<Void> generate(DhChunkPos chunkPosMin, byte granularity, byte targetDataDetail, Consumer<ChunkSizedData> resultConsumer);

    /** Returns whether the generator is unable to accept new generation requests. */
    boolean isBusy();
	
}
