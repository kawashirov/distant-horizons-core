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

package com.seibel.lod.core.builders.worldGeneration;

import com.seibel.lod.core.a7.generation.IChunkGenerator;
import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.EDistanceGenerationMode;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.objects.DHChunkPos;
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
	public ILevel targetLodLevel;
	public static final int generationGroupSize = 4;
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	
//	private int estimatedSampleNeeded = 128;
//	private int estimatedPointsToQueue = 1;

	public BatchGenerator(ILevel targetLodLevel) {
		this.targetLodLevel = targetLodLevel;
		generationGroup = FACTORY.createBatchGenerator(targetLodLevel);
		LOGGER.info("Batch Chunk Generator initialized");
	}

//	@SuppressWarnings("unused")
//	public void queueGenerationRequests() {
//		EDistanceGenerationMode mode = Config.Client.WorldGenerator.distanceGenerationMode.get();
//		int newThreadCount = Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get()<1 ? 1 : (int) Math.ceil(Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get());
//		if (newThreadCount != previousThreadCount) {
//			generationGroup.resizeThreadPool(newThreadCount);
//			previousThreadCount = newThreadCount;
//		}
//		if (estimatedPointsToQueue < newThreadCount)
//			estimatedPointsToQueue = newThreadCount;
//
//		EGenerationPriority priority = Config.Client.WorldGenerator.generationPriority.get();
//		if (priority == EGenerationPriority.AUTO)
//			priority = MC.hasSinglePlayerServer() ? EGenerationPriority.FAR_FIRST : EGenerationPriority.NEAR_FIRST;
//
//		generationGroup.updateAllFutures();
//		if (!MC.hasSinglePlayerServer())
//			return;
//		if (!LodUtil.checkRamUsage(0.1, 64)) return;
//
//		int eventsCount = generationGroup.getEventCount();
//		// If we still all jobs running, return.
//		if (eventsCount >= estimatedPointsToQueue) {
//			estimatedPointsToQueue--;
//			if (estimatedPointsToQueue < newThreadCount)
//				estimatedPointsToQueue = newThreadCount;
//			return;
//		}
//
//		final int targetToGenerate = estimatedPointsToQueue - eventsCount;
//		int toGenerate = targetToGenerate;
//		int positionGoneThough = 0;
//
//		// round the player's block position down to the nearest chunk BlockPos
//		int playerPosX = MC.getPlayerBlockPos().getX();
//		int playerPosZ = MC.getPlayerBlockPos().getZ();
//		double runTimeRatio = Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get()>1 ? 1.0 : Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get();
//
//		//FIXME
//		PosToGenerateContainer posToGenerate = null;//lodDim.getPosToGenerate(estimatedSampleNeeded, playerPosX, playerPosZ,
//				//priority, mode);
//
//		if (eventsCount == 0 && posToGenerate.getNumberOfPos() >= estimatedSampleNeeded) {
//			estimatedPointsToQueue++;
//			if (estimatedPointsToQueue > newThreadCount * 10)
//				estimatedPointsToQueue = newThreadCount * 10;
//		}
//
//		// ApiShared.LOGGER.info("PosToGenerate: {}", posToGenerate);
//
//		// Find the max number of iterations we need to go though.
//		// We are checking one FarPos, and one NearPos per iterations.
//		// This ensures we aren't just always picking one or the other.
//		Steps targetStep;
//		switch (mode) {
//		case NONE:
//			targetStep = Steps.Empty; // NOTE: Only load in existing chunks. No new chunk generation
//			break;
//		case BIOME_ONLY:
//			targetStep = Steps.Biomes; // NOTE: No block. Require fake height in LodBuilder
//			break;
//		case BIOME_ONLY_SIMULATE_HEIGHT:
//			targetStep = Steps.Noise; // NOTE: Stone only. Require fake surface
//			break;
//		case SURFACE:
//			targetStep = Steps.Surface; // Carvers or Surface???
//			break;
//		case FEATURES:
//		case FULL:
//			targetStep = Steps.Features;
//			break;
//		default:
//			assert false;
//			return;
//		}
//
//		if (ENABLE_GENERATOR_STATS_LOGGING)
//			LOGGER.info("WorldGen. Near:" + posToGenerate.getNumberOfNearPos() + " Far:"
//					+ posToGenerate.getNumberOfFarPos());
//		if (priority == EGenerationPriority.FAR_FIRST || priority == EGenerationPriority.BALANCED) {
//
//			int nearCount = posToGenerate.getNumberOfNearPos();
//			int farCount = posToGenerate.getNumberOfFarPos();
//			if (ENABLE_GENERATOR_STATS_LOGGING)
//				LOGGER.info("WorldGen. Near:" + nearCount + " Far:" + farCount);
//			int maxIteration = Math.max(nearCount, farCount);
//			for (int i = 0; i < maxIteration; i++) {
//
//				// We have farPos to go though
//				if (i < farCount && posToGenerate.getNthDetail(i, false) != 0) {
//					positionGoneThough++;
//					byte detailLevel = (byte) (posToGenerate.getNthDetail(i, false) - 1);
//					int chunkX = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosX(i, false));
//					int chunkZ = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosZ(i, false));
//					int genSize = detailLevel > LodUtil.CHUNK_DETAIL_LEVEL ? 0 : generationGroupSize;
//					if (generationGroup.tryAddPoint(chunkX, chunkZ, genSize, targetStep, false, runTimeRatio)) {
//						toGenerate--;
//					}
//				}
//				if (toGenerate <= 0)
//					break;
//
//				// We have nearPos to go though
//				if (i < nearCount && posToGenerate.getNthDetail(i, true) != 0) {
//					positionGoneThough++;
//					byte detailLevel = (byte) (posToGenerate.getNthDetail(i, true) - 1);
//					int chunkX = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosX(i, true));
//					int chunkZ = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosZ(i, true));
//					int genSize = detailLevel > LodUtil.CHUNK_DETAIL_LEVEL ? 0 : generationGroupSize;
//					if (generationGroup.tryAddPoint(chunkX, chunkZ, genSize, targetStep, true, runTimeRatio)) {
//						toGenerate--;
//					}
//				}
//
//				if (toGenerate <= 0)
//					break;
//			}
//		} else {
//			int nearCount = posToGenerate.getNumberOfNearPos();
//			for (int i = 0; i < nearCount; i++) {
//
//				// We have nearPos to go though
//				if (posToGenerate.getNthDetail(i, true) != 0) {
//					positionGoneThough++;
//					byte detailLevel = (byte) (posToGenerate.getNthDetail(i, true) - 1);
//					int chunkX = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosX(i, true));
//					int chunkZ = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosZ(i, true));
//					int genSize = detailLevel > LodUtil.CHUNK_DETAIL_LEVEL ? 0 : generationGroupSize;
//					if (generationGroup.tryAddPoint(chunkX, chunkZ, genSize, targetStep, true, runTimeRatio)) {
//						toGenerate--;
//					}
//					if (toGenerate <= 0)
//						break;
//				}
//			}
//			// Only do far gen if toGenerate is non 0 and that we have requested all samples
//			// we can get.
//			if (toGenerate > 0 && estimatedSampleNeeded > posToGenerate.getNumberOfPos()) {
//				int farCount = posToGenerate.getNumberOfFarPos();
//				for (int i = 0; i < farCount; i++) {
//					// We have farPos to go though
//					if (posToGenerate.getNthDetail(i, false) != 0) {
//						positionGoneThough++;
//						byte detailLevel = (byte) (posToGenerate.getNthDetail(i, false) - 1);
//						int chunkX = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosX(i, false));
//						int chunkZ = LevelPosUtil.getChunkPos(detailLevel, posToGenerate.getNthPosZ(i, false));
//						int genSize = detailLevel > LodUtil.CHUNK_DETAIL_LEVEL ? 0 : generationGroupSize;
//						if (generationGroup.tryAddPoint(chunkX, chunkZ, genSize, targetStep, false, runTimeRatio)) {
//							toGenerate--;
//						}
//					}
//					if (toGenerate <= 0)
//						break;
//				}
//			}
//		}
//
//		if (targetToGenerate != toGenerate && ENABLE_GENERATOR_STATS_LOGGING) {
//			if (toGenerate <= 0) {
//				LOGGER.info(
//						"WorldGenerator: Sampled " + posToGenerate.getNumberOfPos() + " out of " + estimatedSampleNeeded
//								+ " points, started all targeted " + targetToGenerate + " generations.");
//			} else {
//				LOGGER.info("WorldGenerator: Sampled " + posToGenerate.getNumberOfPos() + " out of "
//						+ estimatedSampleNeeded + " points, started " + (targetToGenerate - toGenerate)
//						+ " out of targeted " + targetToGenerate + " generations.");
//			}
//		}
//
//		if (toGenerate > 0 && estimatedSampleNeeded <= posToGenerate.getNumberOfPos()) {
//			// We failed to generate enough points from the samples.
//			// Let's increase the estimatedSampleNeeded.
//			estimatedSampleNeeded *= 1.3;
//			// Ensure wee don't go to basically infinity
//			if (estimatedSampleNeeded > 32768)
//				estimatedSampleNeeded = 32768;
//			if (ENABLE_GENERATOR_STATS_LOGGING)
//				LOGGER.info("WorldGenerator: Increasing estimatedSampleNeeded to " + estimatedSampleNeeded);
//
//		} else if (toGenerate <= 0 && positionGoneThough * 1.5 < posToGenerate.getNumberOfPos()) {
//			// We haven't gone through half of them, and it's already enough.
//			// Let's shrink the estimatedSampleNeeded.
//			estimatedSampleNeeded /= 1.2;
//			// Ensure we don't go near zero.
//			if (estimatedSampleNeeded < 4)
//				estimatedSampleNeeded = 4;
//			if (ENABLE_GENERATOR_STATS_LOGGING)
//				LOGGER.info("WorldGenerator: Decreasing estimatedSampleNeeded to " + estimatedSampleNeeded);
//		}
//
//	}

	public void stop(boolean blocking) {
		LOGGER.info("1.18 Experimental Chunk Generator shutting down...");
		generationGroup.stop(blocking);
	}

	@Override
	public boolean isBusy() {
		return generationGroup.getEventCount() > Math.max(Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get().intValue(), 1) *1.5;
	}

	@Override
	public CompletableFuture<Void> generateChunks(DHChunkPos chunkPosMin, byte granularity, byte targetDataDetail, Consumer<IChunkWrapper> resultConsumer) {
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
