/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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
 
package com.seibel.lod.core.wrapperInterfaces.worldGeneration;

import com.seibel.lod.core.a7.level.ILevel;
import com.seibel.lod.core.util.gridList.ArrayGridList;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractBatchGenerationEnvionmentWrapper {
	public enum Steps {
		Empty, StructureStart, StructureReference, Biomes, Noise, Surface, Carvers, LiquidCarvers, Features, Light,
	}

	public AbstractBatchGenerationEnvionmentWrapper(ILevel level) {
	}

	public abstract void resizeThreadPool(int newThreadCount);

	@Deprecated
	public abstract void updateAllFutures();

	public abstract int getEventCount();

	@Deprecated
	public abstract boolean tryAddPoint(int chunkX, int chunkZ, int genSize, Steps targetStep, boolean genAllDetails, double runTimeRatio);

	public abstract void stop(boolean blocking);

	public abstract CompletableFuture<ArrayGridList<IChunkWrapper>> generateChunks(int minX, int minZ, int genSize, Steps targetStep, double runTimeRatio);
}
