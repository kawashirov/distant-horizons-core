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

package com.seibel.lod.core.wrapperInterfaces.world;

import com.seibel.lod.core.dependencyInjection.IBindable;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;

/**
 * Can be either a Server world or a Client world.
 * 
 * @author James Seibel
 * @version 2022-9-10
 */
public interface ILevelWrapper extends IBindable
{
	IDimensionTypeWrapper getDimensionType();
	
	int getBlockLight(int x, int y, int z);
	
	int getSkyLight(int x, int y, int z);
	
	boolean hasCeiling();
	
	boolean hasSkyLight();
	
	int getHeight();
	
	default short getMinHeight() { return 0; }

	default IChunkWrapper tryGetChunk(DhChunkPos pos) { return null; }

    boolean hasChunkLoaded(int chunkX, int chunkZ);

	IBlockStateWrapper getBlockState(DhBlockPos pos);
	
	IBiomeWrapper getBiome(DhBlockPos pos);
	
	Object unwrapLevel();
	
}
