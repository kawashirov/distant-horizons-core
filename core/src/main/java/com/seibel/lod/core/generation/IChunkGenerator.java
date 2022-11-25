package com.seibel.lod.core.generation;

import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.datatype.transform.LodDataBuilder;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @version 2022-11-25
 */
public interface IChunkGenerator extends IGenerator
{
	CompletableFuture<Void> generateChunks(DhChunkPos chunkPosMin, 
			byte granularity, byte targetDataDetail, 
			Consumer<IChunkWrapper> resultConsumer);
	
	@Override
	default CompletableFuture<Void> generate(DhChunkPos chunkPosMin, 
			byte granularity, byte targetDataDetail, 
			Consumer<ChunkSizedData> resultConsumer)
	{
		return this.generateChunks(chunkPosMin, granularity, targetDataDetail, (chunk) -> 
		{
			resultConsumer.accept(LodDataBuilder.createChunkData(chunk));
		});
	}
	
}
