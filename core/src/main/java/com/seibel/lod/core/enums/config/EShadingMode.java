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
 
package com.seibel.lod.core.enums.config;

/**
 * NONE, GAME_SHADING
 * 
 * @author James Seibel
 * @version 7-25-2020
 */
public enum EShadingMode
{
	/**
	 * LODs will have darker sides and bottoms to simulate
	 * Minecraft's fast lighting.
	 */
	GAME_SHADING,
	
	/**
	 * LODs will use ambient occlusion to mimic Minecraft's
	 * Fancy lighting.
	 */
	AMBIENT_OCCLUSION
}