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

package com.seibel.lod.core.api.external.items.enums.config;

/**
 * BLOCK <Br>
 * TWO_BLOCKS <Br>
 * FOUR_BLOCKS <br>
 * HALF_CHUNK <Br>
 * CHUNK <br>
 * 
 * @author James Seibel
 * @author Leonardo Amato
 * @version 2022-7-5
 */
public enum EDhApiHorizontalResolution
{
	/** render 256 LODs for each chunk */
	BLOCK,
	
	/** render 64 LODs for each chunk */
	TWO_BLOCKS,
	
	/** render 16 LODs for each chunk */
	FOUR_BLOCKS,
	
	/** render 4 LODs for each chunk */
	HALF_CHUNK,
	
	/** render 1 LOD for each chunk */
	CHUNK;
	
}
