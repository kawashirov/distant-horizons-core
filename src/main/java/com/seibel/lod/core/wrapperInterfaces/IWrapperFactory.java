/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.wrapperInterfaces;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.objects.lod.LodDimension;
import com.seibel.lod.core.wrapperInterfaces.block.AbstractBlockPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.AbstractChunkPosWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractExperimentalWorldGeneratorWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractWorldGeneratorWrapper;

/**
 * This handles creating abstract wrapper objects.
 * 
 * @author James Seibel
 * @version 12-14-2021
 */
public interface IWrapperFactory
{	
	AbstractBlockPosWrapper createBlockPos();
	AbstractBlockPosWrapper createBlockPos(int x, int y, int z);
	
	
	AbstractChunkPosWrapper createChunkPos();
	public default AbstractChunkPosWrapper createChunkPos(long xAndZPositionCombined)
	{
		int x = (int) (xAndZPositionCombined & Integer.MAX_VALUE);
		int z = (int) (xAndZPositionCombined >> Long.SIZE / 2) & Integer.MAX_VALUE;
		
		return createChunkPos(x, z);
	}
	AbstractChunkPosWrapper createChunkPos(int x, int z);
	AbstractChunkPosWrapper createChunkPos(AbstractChunkPosWrapper newChunkPos);
	AbstractChunkPosWrapper createChunkPos(AbstractBlockPosWrapper blockPos);
	
	
	AbstractWorldGeneratorWrapper createWorldGenerator(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper);
	// Return null to signal that there is no AbstractWorldGenerator
	public default AbstractExperimentalWorldGeneratorWrapper createExperimentalWorldGenerator(LodBuilder newLodBuilder, LodDimension newLodDimension, IWorldWrapper worldWrapper) {
		return null;
	}
}
