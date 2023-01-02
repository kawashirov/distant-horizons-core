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

/**
 * 
 * @author Leonardo Amato
 * @version ??
 */
@Deprecated // Use DhSectionPos & DhLodPos & etc classes methods instead
public class LevelPosUtil
{
	public static int[] convert(int[] levelPos, byte newDetailLevel)
	{
		return convert(getDetailLevel(levelPos), getPosX(levelPos), getPosZ(levelPos), newDetailLevel);
	}
	
	public static int[] convert(byte detailLevel, int posX, int posZ, byte newDetailLevel)
	{
		int width;
		if (newDetailLevel >= detailLevel)
		{
			width = 1 << (newDetailLevel - detailLevel);
			return createLevelPos(
					newDetailLevel,
					Math.floorDiv(posX, width),
					Math.floorDiv(posZ, width));
		}
		else
		{
			width = 1 << (detailLevel - newDetailLevel);
			return createLevelPos(
					newDetailLevel,
					posX * width,
					posZ * width);
		}
	}
	
	public static int[] createLevelPos(byte detailLevel, int posX, int posZ)
	{
		return new int[] { detailLevel, posX, posZ };
	}
	
	public static int getRegion(byte detailLevel, int pos)
	{
		return Math.floorDiv(pos, 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel));
	}
	
	public static int getRegionModule(byte detailLevel, int pos)
	{
		return Math.floorMod(pos, 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel));
	}
	
	public static byte getDetailLevel(int[] levelPos)
	{
		return (byte) levelPos[0];
	}
	
	public static int getPosX(int[] levelPos)
	{
		return levelPos[1];
	}
	
	public static int getPosZ(int[] levelPos)
	{
		return levelPos[2];
	}
	
	public static int getDistance(int[] levelPos)
	{
		return levelPos[3];
	}
	
	public static int[] getRegionModule(int[] levelPos)
	{
		return getRegionModule(getDetailLevel(levelPos), getPosX(levelPos), getPosZ(levelPos));
	}
	
	public static int[] getRegionModule(byte detailLevel, int posX, int posZ)
	{
		int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - detailLevel);
		return createLevelPos(
				detailLevel,
				Math.floorMod(posX, width),
				Math.floorMod(posZ, width));
	}
	
	public static int[] applyOffset(int[] levelPos, int xOffset, int zOffset)
	{
		return createLevelPos(
				getDetailLevel(levelPos),
				getPosX(levelPos) + xOffset,
				getPosZ(levelPos) + zOffset);
	}
	
	public static int[] applyLevelOffset(int[] levelPos, byte detailOffset, int xOffset, int zOffset)
	{
		return createLevelPos(
				getDetailLevel(levelPos),
				getPosX(levelPos) + xOffset * (1 << detailOffset),
				getPosZ(levelPos) + zOffset * (1 << detailOffset));
	}
	
	public static int getRegionPosX(int[] levelPos)
	{
		int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - getDetailLevel(levelPos));
		return Math.floorDiv(getPosX(levelPos), width);
	}
	
	public static int getRegionPosZ(int[] levelPos)
	{
		int width = 1 << (LodUtil.REGION_DETAIL_LEVEL - getDetailLevel(levelPos));
		return Math.floorDiv(getPosZ(levelPos), width);
	}
	
	public static double centerDistance(byte detailLevel, int posX, int posZ, int playerPosX, int playerPosZ) {
		int width = 1 << detailLevel;
		double cPosX = posX * width + width/2.;
		double cPosZ = posZ * width + width/2.;
		cPosX = playerPosX - cPosX;
		cPosZ = playerPosZ - cPosZ;
		return Math.sqrt(MathUtil.pow2(cPosX) + MathUtil.pow2(cPosZ));
	}

	public static double maxDistance(byte detailLevel, int posX, int posZ, int playerPosX, int playerPosZ)
	{
		int width = 1 << detailLevel;
		
		double startPosX = posX * width;
		double startPosZ = posZ * width;
		double endPosX = MathUtil.pow2(playerPosX - startPosX - width);
		double endPosZ = MathUtil.pow2(playerPosZ - startPosZ - width);
		startPosX = MathUtil.pow2(playerPosX - startPosX);
		startPosZ = MathUtil.pow2(playerPosZ - startPosZ);
		
		double maxDistance = Math.sqrt(startPosX + startPosZ);
		maxDistance = Math.max(maxDistance, Math.sqrt(startPosX + endPosZ));
		maxDistance = Math.max(maxDistance, Math.sqrt(endPosX + startPosZ));
		maxDistance = Math.max(maxDistance, Math.sqrt(endPosX + endPosZ));
		
		return maxDistance;
	}
	
	
	public static double minDistance(byte detailLevel, int posX, int posZ, int playerPosX, int playerPosZ)
	{
		int width = 1 << detailLevel;
		
		int startPosX = posX * width;
		int startPosZ = posZ * width;
		int endPosX = startPosX + width;
		int endPosZ = startPosZ + width;
		
		boolean inXArea = playerPosX >= startPosX && playerPosX <= endPosX;
		boolean inZArea = playerPosZ >= startPosZ && playerPosZ <= endPosZ;
		if (inXArea && inZArea)
			return 0;
		else if (inXArea)
		{
			return Math.min(
					Math.abs(playerPosZ - startPosZ),
					Math.abs(playerPosZ - endPosZ)
			);
		}
		else if (inZArea)
		{
			return Math.min(
					Math.abs(playerPosX - startPosX),
					Math.abs(playerPosX - endPosX)
			);
		}
		else
		{
			double startPosX2 = MathUtil.pow2(playerPosX - startPosX);
			double startPosZ2 = MathUtil.pow2(playerPosZ - startPosZ);
			double endPosX2 = MathUtil.pow2(playerPosX - endPosX);
			double endPosZ2 = MathUtil.pow2(playerPosZ - endPosZ);
			
			double minDistance = Math.sqrt(startPosX2 + startPosZ2);
			minDistance = Math.min(minDistance, Math.sqrt(startPosX2 + endPosZ2));
			minDistance = Math.min(minDistance, Math.sqrt(endPosX2 + startPosZ2));
			minDistance = Math.min(minDistance, Math.sqrt(endPosX2 + endPosZ2));
			return minDistance;
		}
	}
	
	public static double compareLevelAndDistance(byte firstDetail, double firstDistance, byte secondDetail, double secondDistance)
	{
		int compareResult = Byte.compare(
				secondDetail,
				firstDetail);
		if (compareResult == 0)
		{
			compareResult = Double.compare(
					firstDistance,
					secondDistance);
		}
		return compareResult;
	}
	
	@SuppressWarnings("unused")
	public static String toString(int[] levelPos)
	{
		return (getDetailLevel(levelPos) + " "
				+ getPosX(levelPos) + " "
				+ getPosZ(levelPos));
	}
	
	public static String toString(byte detailLevel, int posX, int posZ)
	{
		return (detailLevel + " " + posX + " " + posZ);
	}
}
