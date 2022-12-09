package com.seibel.lod.core.generation;

import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.datatype.full.ChunkSizedData;
import com.seibel.lod.core.datatype.transform.LodDataBuilder;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Most logic is in {@link IDhApiWorldGenerator}, this mainly contains default
 * methods that are useful for Core.
 * 
 * @author James Seibel 
 * @author Leetom
 * @version 2022-12-5
 */
public interface IWorldGenerator extends IDhApiWorldGenerator
{
	/**
	 * Start a generation event
	 * (Note that the chunkPos is always aligned to the granularity)
	 * (For example, if the granularity is 4 (chunk sized) with a data detail level of 0 (block sized), the chunkPos will be aligned to 16x16 blocks)
	 */
	default CompletableFuture<Void> generate(DhChunkPos chunkPosMin,
			byte granularity, byte targetDataDetail,
			Consumer<ChunkSizedData> resultConsumer)
	{
		return this.generateChunks(chunkPosMin.x, chunkPosMin.z, granularity, targetDataDetail, (objectArray) ->
		{ 
			try
			{
				IChunkWrapper chunk = SingletonInjector.INSTANCE.get(IWrapperFactory.class).createChunkWrapper(objectArray);
				resultConsumer.accept(LodDataBuilder.createChunkData(chunk));
			}
			catch (ClassCastException e)
			{
				DhLoggerBuilder.getLogger().error("World generator return type incorrect. Error: [" + e.getMessage() + "].", e);
				Config.Client.WorldGenerator.enableDistantGeneration.set(false);
			}
		});
	}
	
}
