package com.seibel.lod.core.generation;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.datatype.transform.LodDataBuilder;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Leetom
 * @version 2022-11-25
 */
public interface IChunkGenerator extends Closeable
{
	//============//
	// parameters //
	//============//
	
	/**
	 * What is the detail/resolution of the data? (This will offset the generation granularity)
	 * (minimum detail is 0, maximum detail is 255) (though that high isn't really... realistic)
	 * (0 = 1x1 block per data, 1 = 2x2 block per data, 2 = 4x4 block per data, etc. This measured in the same units as LOD Detail Level.)
	 * TODO: System currently only supports 1x1 block per data.
	 */
	byte getMinDataDetailLevel();
	byte getMaxDataDetailLevel();
	
	/**
	 * What is the min batch size of a single generation call?
	 * (minimum return value is 4 since that's the MC chunk size)
	 * (4 -> 16x16 data per call, 5 -> 32x32 data per call, 6 -> 64x64 data per call, etc. This measured in the same units as LOD Detail Level.)
	 */
	byte getMinGenerationGranularity();
	
	/**
	 * What is the max batch size of a single generation call? 
	 * The system will try to group tasks to the max batch size if possible
	 * (minimum return value is 4 since that's the MC chunk size)
	 * (4 -> 16x16 data per call, 5 -> 32x32 data per call, 6 -> 64x64 data per call, etc. This measured in the same units as LOD Detail Level.)
	 */
	byte getMaxGenerationGranularity();
	
	/** Returns whether the generator is unable to accept new generation requests. */
	boolean isBusy();
	
	
	
	//=================//
	// world generator //
	//=================//
	
	CompletableFuture<Void> generateChunks(DhChunkPos chunkPosMin,
			byte granularity, byte targetDataDetail,
			Consumer<IChunkWrapper> resultConsumer);
	
	
	/**
	 * Start a generation event
	 * (Note that the chunkPos is always aligned to the granularity)
	 * (For example, if the granularity is 4 (chunk sized) with a data detail level of 0 (block sized), the chunkPos will be aligned to 16x16 blocks)
	 */
	default CompletableFuture<Void> generate(DhChunkPos chunkPosMin,
			byte granularity, byte targetDataDetail,
			Consumer<ChunkSizedData> resultConsumer)
	{
		return this.generateChunks(chunkPosMin, granularity, targetDataDetail, (chunk) ->
		{
			resultConsumer.accept(LodDataBuilder.createChunkData(chunk));
		});
	}
	
	
	//===============//
	// event methods //
	//===============//
	
	/** 
	 * Called before a new generator task is started. <br>
	 * This can be used to run cleanup on existing tasks before new tasks are started.
	 */
	void preGeneratorTaskStart();
	
	
	
	//===========//
	// overrides //
	//===========//
	
	// This is overridden to remove the "throws IOException" 
	// that is present in the default Closeable.close() method 
	@Override
	void close();
	
}
