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

package com.seibel.distanthorizons.api.interfaces.world;

import com.seibel.distanthorizons.api.interfaces.IDhApiUnsafeWrapper;

/**
 * @author James Seibel
 * @version 2022-7-14
 */
public interface IDhApiChunkWrapper extends IDhApiUnsafeWrapper
{
	/** Returns the absolute Y coordinate of the highest block for the given relative X and Z coordinates. */
	int getMaxY(int relativeX, int relativeZ);
	/** Returns the maximum absolute block position in the X direction. */
	int getMaxX();
	/** Returns the maximum absolute block position in the Z direction. */
	int getMaxZ();
	
	/** Returns the absolute Y coordinate of the lowest block for the given relative X and Z coordinates. */
	int getMinY(int relativeX, int relativeZ);
	/** Returns the minimum absolute block position in the X direction. */
	int getMinX();
	/** Returns the minimum absolute block position in the Z direction. */
	int getMinZ();
	
	/**
	 * Returns true if this chunk's lighting has been built. <br>
	 * Note: for some versions of Minecraft this value may be unreliable.
	 */
	boolean isLightCorrect();
	
	/** TODO what side of the block should this return the light for? */
	default int getBlockLight(int x, int y, int z) {return -1;}
	/** TODO what side of the block should this return the light for? */
	default int getSkyLight(int x, int y, int z) {return -1;}
	
	/**
	 * Returns true if chunks exist in all 4 cardinal and 4 ordinal directions
	 * relative to this chunk. <br>
	 * IE: returns true if there are chunks to the North, South, East, West, NE, SE, SW, and NW
	 * of this chunk.
	 */
	boolean doNearbyChunksExist();
	
	
	// TODO these will probably need replacing once 1.7's ID system is done
	//IBlockStateWrapper getBlockState(int x, int y, int z);
	//IBiomeWrapper getBiome(int x, int y, int z);
	
}
