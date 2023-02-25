/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2021 Tom Lee (TomTheFurry) & James Seibel (Original code)
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.generation;

import com.seibel.lod.api.enums.worldGeneration.EDhApiWorldGenThreadMode;
import com.seibel.lod.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.lod.core.interfaces.dependencyInjection.IOverrideInjector;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.BitShiftUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper.Steps;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Leetom
 * @version 2022-12-10
 */
public class BatchGenerator implements IDhApiWorldGenerator
{
	private static final IWrapperFactory FACTORY = SingletonInjector.INSTANCE.get(IWrapperFactory.class);
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	public AbstractBatchGenerationEnvionmentWrapper generationGroup;
	public IDhLevel targetDhLevel;
	
	
	
	
	public BatchGenerator(IDhLevel targetDhLevel)
	{
		this.targetDhLevel = targetDhLevel;
		this.generationGroup = FACTORY.createBatchGenerator(targetDhLevel);
		LOGGER.info("Batch Chunk Generator initialized");
	}
	
	
	
	//=====================//
	// override parameters // 
	//=====================//
	
	@Override
	public int getPriority() { return IOverrideInjector.CORE_PRIORITY; }
	
	
	
	//======================//
	// generator parameters //
	//======================//
	
	@Override 
	public EDhApiWorldGenThreadMode getThreadingMode() { return EDhApiWorldGenThreadMode.MULTI_THREADED; }
	
	@Override
	public byte getMinDataDetailLevel() { return LodUtil.BLOCK_DETAIL_LEVEL; }
	@Override
	public byte getMaxDataDetailLevel() { return LodUtil.BLOCK_DETAIL_LEVEL; }
	
	@Override
	public byte getMinGenerationGranularity() { return LodUtil.CHUNK_DETAIL_LEVEL; }
	@Override
	public byte getMaxGenerationGranularity() { return LodUtil.CHUNK_DETAIL_LEVEL + 2; }
	
	
	
	
	//===================//
	// generator methods //
	//===================//
	
	@Override
	public CompletableFuture<Void> generateChunks(int chunkPosMinX, int chunkPosMinZ, byte granularity, byte targetDataDetail, EDhApiDistantGeneratorMode generatorMode, Consumer<Object[]> resultConsumer)
	{
		Steps targetStep = null;
		switch (generatorMode)
		{
			case PRE_EXISTING_ONLY:
				targetStep = Steps.Empty; // NOTE: Only load in existing chunks. No new chunk generation
				break;
			case BIOME_ONLY:
				targetStep = Steps.Biomes; // NOTE: No blocks. Require fake height in LodBuilder
				break;
			case BIOME_ONLY_SIMULATE_HEIGHT:
				targetStep = Steps.Noise; // NOTE: Stone only. Require fake surface
				break;
			case SURFACE:
				targetStep = Steps.Surface; // Carvers or Surface???
				break;
			case FEATURES:
			case FULL:
				targetStep = Steps.Features;
				break;
		}
		
		int genChunkSize = BitShiftUtil.powerOfTwo(granularity - 4); // minus 4 is equal to dividing by 16 to convert to chunk scale
		double runTimeRatio = Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get() > 1 ? 
				1.0 :
				Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get();
		
		// the consumer needs to be wrapped like this because the API can't use DH core objects (and IChunkWrapper can't be easily put into the API project)
		Consumer<IChunkWrapper> consumer = (chunkWrapper) -> resultConsumer.accept(new Object[]{ chunkWrapper });
		
		return this.generationGroup.generateChunks(chunkPosMinX, chunkPosMinZ, genChunkSize, targetStep, runTimeRatio, consumer);
	}
	
	@Override
	public void preGeneratorTaskStart() { this.generationGroup.updateAllFutures(); }
	
	@Override
	public boolean isBusy()
	{
		return this.generationGroup.getEventCount() > Math.max(Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get().intValue(), 1) * 1.5;
	}
	
	
	
	//=========//
	// cleanup //
	//=========//
	
	@Override
	public void close() { this.stop(true); }
	public void stop(boolean blocking)
	{
		LOGGER.info(BatchGenerator.class.getSimpleName()+" shutting down...");
		this.generationGroup.stop(blocking);
	}
	
	
}
