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

package com.seibel.lod.core.wrapperAdapters.world;

import com.seibel.lod.wrappers.block.BlockPosWrapper;
import com.seibel.lod.wrappers.world.WorldWrapper;


/**
 * 
 * @author Cola?
 * @version 11-15-2021
 */
public interface IBiomeColorWrapperSingleton
{
	public IBiomeColorWrapperSingleton getInstance(); 
	
	public int getGrassColor(WorldWrapper world, BlockPosWrapper blockPos);
	public int getWaterColor(WorldWrapper world, BlockPosWrapper blockPos);
	public int getFoliageColor(WorldWrapper world, BlockPosWrapper blockPos);
}
