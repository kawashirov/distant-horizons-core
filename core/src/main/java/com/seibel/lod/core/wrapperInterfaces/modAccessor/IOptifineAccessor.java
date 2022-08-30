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
 
package com.seibel.lod.core.wrapperInterfaces.modAccessor;

import com.seibel.lod.core.objects.DHChunkPos;

import java.util.HashSet;

public interface IOptifineAccessor extends IModAccessor 
{
	/** Can be null */
	HashSet<DHChunkPos> getNormalRenderedChunks();
	
	/**
	 * Returns the percentage multiplier of the screen's current resolution. <br>
	 * 1.0 = 100% <br>
	 * 1.5 = 150% <br>
	 */
	default double getRenderResolutionMultiplier()
	{
		return 1.0;
	}
	
}
