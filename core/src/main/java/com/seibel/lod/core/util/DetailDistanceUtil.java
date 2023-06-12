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

package com.seibel.lod.core.util;

import com.seibel.lod.api.enums.config.EHorizontalQuality;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.coreapi.util.MathUtil;

/**
 * 
 * @author Leonardo Amato
 * @version ??
 */
@Deprecated
public class DetailDistanceUtil
{
	/** smallest numerical detail level */
	private static byte maxDetailLevel = Config.Client.Advanced.Graphics.Quality.drawResolution.get().detailLevel;
	
	/** largest numerical detail level */
	private static final byte minDetailLevel =  Byte.MAX_VALUE;
	private static final double minDistance = 0;
	
	// TODO merge with updateSettings() below
	private static double distanceUnit = Config.Client.Advanced.Graphics.Quality.horizontalQuality.get().distanceUnitInBlocks * LodUtil.CHUNK_WIDTH;
	private static double maxDistance = Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH * 2;
	private static double logBase = Math.log(Config.Client.Advanced.Graphics.Quality.horizontalQuality.get().quadraticBase);
	
	
	
	public static void updateSettings()
	{
		maxDetailLevel = Config.Client.Advanced.Graphics.Quality.drawResolution.get().detailLevel;
		
		distanceUnit = Config.Client.Advanced.Graphics.Quality.horizontalQuality.get().distanceUnitInBlocks * LodUtil.CHUNK_WIDTH;
		maxDistance = Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH * 2;
		logBase = Math.log(Config.Client.Advanced.Graphics.Quality.horizontalQuality.get().quadraticBase);
	}
	
	public static double baseDistanceFunction(int detail)
	{
		if (detail <= maxDetailLevel)
		{
			return minDistance;
		}
		else if (detail >= minDetailLevel)
		{
			return maxDistance;
		}
		
		
		double base = Config.Client.Advanced.Graphics.Quality.horizontalQuality.get().quadraticBase;
		return Math.pow(base, detail) * distanceUnit;
	}
	
	public static double getDrawDistanceFromDetail(int detail) { return baseDistanceFunction(detail); }
	
	public static byte baseInverseFunction(double distance)
	{
		// special case, never drop the quality
		if (Config.Client.Advanced.Graphics.Quality.horizontalQuality.get() == EHorizontalQuality.UNLIMITED)
		{
			return maxDetailLevel;
		}
		
		
		double maxDetailDistance = getDrawDistanceFromDetail(minDetailLevel -1);
		if (distance > maxDetailDistance)
		{
			return minDetailLevel - 1;
		}
		
		
		int detailLevel = (int) (Math.log(distance / distanceUnit) / logBase);
		return (byte) MathUtil.clamp(maxDetailLevel, detailLevel, minDetailLevel - 1);
	}
	
	public static byte getDetailLevelFromDistance(double distance) { return baseInverseFunction(distance); }
	
}
