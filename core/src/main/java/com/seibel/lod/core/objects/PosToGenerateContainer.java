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

package com.seibel.lod.core.objects;

import com.seibel.lod.core.util.LevelPosUtil;

/**
 * Holds the levelPos that need to be generated.
 * 
 * @author Leonardo Amato
 * @version 9-27-2021
 */
public class PosToGenerateContainer
{
	private final int playerPosX;
	private final int playerPosZ;
	private int nearSize;
	private int farSize;
	
	// TODO what is the format of these two arrays? [detailLevel][4-children]?
	private final int[][] nearPosToGenerate;
	private final int[][] farPosToGenerate;
	
	public PosToGenerateContainer(int maxDataToGenerate, int playerPosX, int playerPosZ)
	{
		this.playerPosX = playerPosX;
		this.playerPosZ = playerPosZ;
		nearSize = 0;
		farSize = 0;
		nearPosToGenerate = new int[maxDataToGenerate][4];
		farPosToGenerate = new int[maxDataToGenerate][4];
	}
	
	
	
	// TODO what is going on in this method?
	public void addNearPosToGenerate(byte detailLevel, int posX, int posZ, boolean sort)
	{
		// FIXME: This is a cast from double to int!!! OVERFLOW MAY HAPPEN!
		int distance = (int)LevelPosUtil.minDistance(detailLevel, posX, posZ, playerPosX, playerPosZ);
		int index;
		
		//We are introducing a position in the near array

		index = nearSize;
		if (index == nearPosToGenerate.length) {
			if (Integer.compare(distance, nearPosToGenerate[index - 1][3]) > 0) {
				return;
			}
			index--;
		} else nearSize++;
		
		if (sort) {
			while (index > 0 && Integer.compare(distance, nearPosToGenerate[index - 1][3]) <= 0)
			{
				nearPosToGenerate[index][0] = nearPosToGenerate[index - 1][0];
				nearPosToGenerate[index][1] = nearPosToGenerate[index - 1][1];
				nearPosToGenerate[index][2] = nearPosToGenerate[index - 1][2];
				nearPosToGenerate[index][3] = nearPosToGenerate[index - 1][3];
				index--;
			}
		}
		nearPosToGenerate[index][0] = detailLevel + 1;
		nearPosToGenerate[index][1] = posX;
		nearPosToGenerate[index][2] = posZ;
		nearPosToGenerate[index][3] = distance;
	}

	// TODO what is going on in this method?
	public void addFarPosToGenerate(byte detailLevel, int posX, int posZ, boolean sort)
	{
		// FIXME: This is a cast from double to int!!! OVERFLOW MAY HAPPEN!
		int distance = (int)LevelPosUtil.minDistance(detailLevel, posX, posZ, playerPosX, playerPosZ);
		int index;
	
		// We are introducing a position in the far array
		
		index = farSize;
		if (index == farPosToGenerate.length) {
			if (Integer.compare(distance, farPosToGenerate[index - 1][3]) > 0) {
				return;
			}
			index--;
		} else farSize++;
		
		if (sort) {
			while (index > 0 && Integer.compare(distance, farPosToGenerate[index - 1][3]) <= 0)
			{
				farPosToGenerate[index][0] = farPosToGenerate[index - 1][0];
				farPosToGenerate[index][1] = farPosToGenerate[index - 1][1];
				farPosToGenerate[index][2] = farPosToGenerate[index - 1][2];
				farPosToGenerate[index][3] = farPosToGenerate[index - 1][3];
				index--;
			}
		}
		farPosToGenerate[index][0] = detailLevel + 1;
		farPosToGenerate[index][1] = posX;
		farPosToGenerate[index][2] = posZ;
		farPosToGenerate[index][3] = distance;
	}
	
	public boolean isFull() {
		return nearSize == nearPosToGenerate.length && farSize == farPosToGenerate.length;
	}
	
	
	
	public int getNumberOfPos()
	{
		return nearSize + farSize;
	}
	
	public int getNumberOfNearPos()
	{
		return nearSize;
	}
	
	public int getNumberOfFarPos()
	{
		return farSize;
	}
	
	public int getMaxNumberOfNearPos()
	{
		return nearPosToGenerate.length;
	}
	
	public int getMaxNumberOfFarPos()
	{
		return farPosToGenerate.length;
	}
	
	// TODO what does getNth mean? could the name be more descriptive or is it just a index?
	public int getNthDetail(int n, boolean near)
	{
		if (near)
			return nearPosToGenerate[n][0];
		else
			return farPosToGenerate[n][0];
	}
	
	public int getNthPosX(int n, boolean near)
	{
		if (near)
			return nearPosToGenerate[n][1];
		else
			return farPosToGenerate[n][1];
	}
	
	public int getNthPosZ(int n, boolean near)
	{
		if (near)
			return nearPosToGenerate[n][2];
		else
			return farPosToGenerate[n][2];
	}
	
	public int getNthGeneration(int n, boolean near)
	{
		if (near)
			return nearPosToGenerate[n][3];
		else
			return farPosToGenerate[n][3];
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('\n');
		builder.append('\n');
		builder.append('\n');
		builder.append("near pos to generate");
		builder.append('\n');
		for (int[] ints : nearPosToGenerate)
		{
			if (ints[0] == 0)
				break;
			builder.append(ints[0] - 1);
			builder.append(" ");
			builder.append(ints[1]);
			builder.append(" ");
			builder.append(ints[2]);
			builder.append(" ");
			builder.append(ints[3]);
			builder.append('\n');
		}
		builder.append('\n');
		
		builder.append("far pos to generate");
		builder.append('\n');
		for (int[] ints : farPosToGenerate)
		{
			if (ints[0] == 0)
				break;
			builder.append(ints[0] - 1);
			builder.append(" ");
			builder.append(ints[1]);
			builder.append(" ");
			builder.append(ints[2]);
			builder.append(" ");
			builder.append(ints[3]);
			builder.append('\n');
		}
		return builder.toString();
	}
}
