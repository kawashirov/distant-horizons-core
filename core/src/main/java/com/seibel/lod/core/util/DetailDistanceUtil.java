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

import com.seibel.lod.core.config.Config;
import com.seibel.lod.api.enums.config.EHorizontalQuality;
import com.seibel.lod.coreapi.util.MathUtil;

/**
 * 
 * @author Leonardo Amato
 * @version ??
 */
@Deprecated
public class DetailDistanceUtil
{
	public static byte minDetail = Config.Client.Advanced.Graphics.Quality.drawResolution.get().detailLevel;
	
	private static final byte maxDetail =  Byte.MAX_VALUE;
	private static final double minDistance = 0;
	private static double distanceUnit = 16 * Config.Client.Advanced.Graphics.Quality.horizontalScale.get();
	private static double maxDistance = Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistance.get() * 16 * 2;
	private static double logBase = Math.log(Config.Client.Advanced.Graphics.Quality.horizontalQuality.get().quadraticBase);
	
	
	public static void updateSettings()
	{
		distanceUnit = 16 * Config.Client.Advanced.Graphics.Quality.horizontalScale.get();
		minDetail = Config.Client.Advanced.Graphics.Quality.drawResolution.get().detailLevel;
		maxDistance = Config.Client.Advanced.Graphics.Quality.lodChunkRenderDistance.get() * 16 * 8;
		logBase = Math.log(Config.Client.Advanced.Graphics.Quality.horizontalQuality.get().quadraticBase);
	}
	
	public static double baseDistanceFunction(int detail)
	{
		if (detail <= minDetail)
			return minDistance;
		if (detail >= maxDetail)
			return maxDistance;
		
		detail-=minDetail;
		
		if (Config.Client.Advanced.Graphics.Quality.horizontalQuality.get() == EHorizontalQuality.LOWEST)
			return ((double)detail * distanceUnit);
		else
		{
			double base = Config.Client.Advanced.Graphics.Quality.horizontalQuality.get().quadraticBase;
			return Math.pow(base, detail) * distanceUnit;
		}
	}
	
	public static double getDrawDistanceFromDetail(int detail)
	{
		return baseDistanceFunction(detail);
	}
	
	public static byte baseInverseFunction(double distance)
	{
		double maxDetailDistance = getDrawDistanceFromDetail(maxDetail-1);
		if (distance > maxDetailDistance) {
			//ApiShared.LOGGER.info("DEBUG: Scale as max: {}", distance);
			return maxDetail-1;
		}
		
		int detail;
		
		if (Config.Client.Advanced.Graphics.Quality.horizontalQuality.get() == EHorizontalQuality.LOWEST)
			detail = (int) (distance/distanceUnit);
		else
			detail = (int) (Math.log(distance/distanceUnit) / logBase);
		
		return (byte) MathUtil.clamp(minDetail, detail+minDetail, maxDetail - 1);
	}
	
	public static byte getDetailLevelFromDistance(double distance)
	{
		return baseInverseFunction(distance);
	}
	
}
