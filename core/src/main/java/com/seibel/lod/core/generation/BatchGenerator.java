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

import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.api.enums.config.EDistanceGenerationMode;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper.Steps;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BatchGenerator implements IChunkGenerator
{
	public static final boolean ENABLE_GENERATOR_STATS_LOGGING = false;

	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IWrapperFactory FACTORY = SingletonInjector.INSTANCE.get(IWrapperFactory.class);
	public AbstractBatchGenerationEnvionmentWrapper generationGroup;
	public IDhLevel targetLodLevel;
	public static final int generationGroupSize = 4;
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

	public BatchGenerator(IDhLevel targetLodLevel) {
		this.targetLodLevel = targetLodLevel;
		generationGroup = FACTORY.createBatchGenerator(targetLodLevel);
		LOGGER.info("Batch Chunk Generator initialized");
	}

	public void stop(boolean blocking) {
		LOGGER.info("1.18 Experimental Chunk Generator shutting down...");
		generationGroup.stop(blocking);
	}

	@Override
	public boolean isBusy() {
		return generationGroup.getEventCount() > Math.max(Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get().intValue(), 1) *1.5;
	}

	@Override
	public CompletableFuture<Void> generateChunks(DhChunkPos chunkPosMin, byte granularity, byte targetDataDetail, Consumer<IChunkWrapper> resultConsumer) {
		EDistanceGenerationMode mode = Config.Client.WorldGenerator.distanceGenerationMode.get();
		Steps targetStep = null;
		switch (mode) {
			case NONE:
				targetStep = Steps.Empty; // NOTE: Only load in existing chunks. No new chunk generation
				break;
			case BIOME_ONLY:
				targetStep = Steps.Biomes; // NOTE: No block. Require fake height in LodBuilder
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
		};

		int chunkXMin = chunkPosMin.x;
		int chunkZMin = chunkPosMin.z;
		int genChunkSize = 1 << (granularity - 4); // minus 4 for chunk size as its equal to div by 16
		double runTimeRatio = Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get()>1 ? 1.0
				: Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get();
		return generationGroup.generateChunks(chunkXMin, chunkZMin, genChunkSize, targetStep, runTimeRatio, resultConsumer);
	}

	@Override
	public byte getMinDataDetail() {
		return 0;
	}

	@Override
	public byte getMaxDataDetail() {
		return 0;
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public byte getMinGenerationGranularity() {
		return 4;
	}

	@Override
	public byte getMaxGenerationGranularity() {
		return 6;
	}

	@Override
	public void close() {
		stop(true);
	}

	public void update() {
		generationGroup.updateAllFutures();
	}
}
