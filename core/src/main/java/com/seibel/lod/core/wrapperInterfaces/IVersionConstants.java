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
 
package com.seibel.lod.core.wrapperInterfaces;

import com.seibel.lod.core.dependencyInjection.IBindable;

/**
 * A singleton that contains variables specific to each version of Minecraft
 * which can be used to change how DH-Core runs. For example: After MC 1.17
 * blocks can be negative, which changes how we generate LODs.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public interface IVersionConstants extends IBindable
{
	/** @return the minimum height blocks can be generated */
	@Deprecated // This changes per world!
	int getMinimumWorldHeight();

	/** @return the number of generations call per thread. */
	@Deprecated // No longer used
	default int getWorldGenerationCountPerThread() {
		return 8;
	}
	
	boolean isVanillaRenderedChunkSquare();

	String getMinecraftVersion();
}
