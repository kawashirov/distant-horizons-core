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

package com.seibel.lod.core.a7.datatype.column.render;

import com.seibel.lod.core.a7.datatype.column.accessor.ColumnArrayView;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.LodQuadBuilder;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.DataPointUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

public class ColumnBox
{
	private static boolean transparencyEnabled = true;
	private static boolean fakeOceanFloor = false;
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	public static void addBoxQuadsToBuilder(LodQuadBuilder builder, short xSize, short ySize, short zSize, short x,
											short y, short z, int color, byte skyLight, byte blockLight, long topData, long botData, ColumnArrayView[][] adjData)
	{
		short maxX = (short) (x + xSize);
		short maxY = (short) (y + ySize);
		short maxZ = (short) (z + zSize);
		byte skyLightTop = skyLight;
		byte skyLightBot = DataPointUtil.doesItExist(botData) ? DataPointUtil.getLightSky(botData) : 0;

		boolean isTransparent = ColorUtil.getAlpha(color)<255 && transparencyEnabled;
		boolean isTopTransparent = DataPointUtil.getAlpha(topData)<255 && transparencyEnabled;
		boolean isBotTransparent = DataPointUtil.getAlpha(botData)<255 && transparencyEnabled;


		// Up direction case
		boolean skipTop = DataPointUtil.doesItExist(topData) && (
				(isTransparent && (DataPointUtil.getDepth(topData) == maxY)) ||
						(!isTransparent && (DataPointUtil.getDepth(topData) == maxY) && !isTopTransparent));
		boolean skipBot = DataPointUtil.doesItExist(botData) && (
				(isTransparent && (DataPointUtil.getDepth(botData) == maxY)) ||
						(!isTransparent && (DataPointUtil.getDepth(botData) == maxY) && !isBotTransparent));

		if(fakeOceanFloor && transparencyEnabled)
		{
			if(!isTransparent && isTopTransparent)
			{
				ySize = (short) (DataPointUtil.getHeight(topData) - y - 1);
			}
			else if(isTransparent && !isBotTransparent)
			{
				y = (short) (y + ySize - 1);
				ySize = 1;
			}
		}else if(!transparencyEnabled)
		{
			color = ColorUtil.rgbToInt(ColorUtil.getRed(color),ColorUtil.getGreen(color),ColorUtil.getBlue(color));
		}

		maxY = (short) (y + ySize);
		if (!skipTop)
			builder.addQuadUp(x, maxY, z, xSize, zSize, ColorUtil.applyShade(color, MC.getShade(ELodDirection.UP)), skyLightTop, blockLight);
		if (!skipBot)
			builder.addQuadDown(x, y, z, xSize, zSize, ColorUtil.applyShade(color, MC.getShade(ELodDirection.DOWN)), skyLightBot, blockLight);
		
		//If the adj pos is at the same level we cull the faces normally, otherwise we divide the face in two and cull the two part separately
		
		//NORTH face vertex creation
		{
			ColumnArrayView[] adjDataNorth = adjData[ELodDirection.NORTH.ordinal() - 2];
			int adjOverlapNorth = ColorUtil.TRANSPARENT;
			if (adjDataNorth == null)
			{
				builder.addQuadAdj(ELodDirection.NORTH, x, y, z, xSize, ySize, color, (byte) 15, blockLight);
			}
			else if (adjDataNorth.length == 1)
			{
				makeAdjQuads(builder, adjDataNorth[0], ELodDirection.NORTH, x, y, z, xSize, ySize,
						color, adjOverlapNorth, skyLightTop, blockLight);
			}
			else
			{
				makeAdjQuads(builder, adjDataNorth[0], ELodDirection.NORTH, x, y, z, (short) (xSize / 2), ySize,
						color, adjOverlapNorth, skyLightTop, blockLight);
				makeAdjQuads(builder, adjDataNorth[1], ELodDirection.NORTH, (short) (x + xSize / 2), y, z, (short) (xSize / 2), ySize,
						color, adjOverlapNorth, skyLightTop, blockLight);
			}
		}

		//SOUTH face vertex creation
		{
			ColumnArrayView[] adjDataSouth = adjData[ELodDirection.SOUTH.ordinal() - 2];
			int adjOverlapSouth = ColorUtil.TRANSPARENT;
			if (adjDataSouth == null)
			{
				builder.addQuadAdj(ELodDirection.SOUTH, x, y, maxZ, xSize, ySize, color, (byte) 15, blockLight);
			}
			else if (adjDataSouth.length == 1)
			{
				makeAdjQuads(builder, adjDataSouth[0], ELodDirection.SOUTH, x, y, maxZ, xSize, ySize,
						color, adjOverlapSouth, skyLightTop, blockLight);
			}
			else
			{
				makeAdjQuads(builder, adjDataSouth[0], ELodDirection.SOUTH, x, y, maxZ, (short) (xSize / 2), ySize,
						color, adjOverlapSouth, skyLightTop, blockLight);
				
				makeAdjQuads(builder, adjDataSouth[1], ELodDirection.SOUTH, (short) (x + xSize / 2), y, maxZ, (short) (xSize / 2), ySize,
						color, adjOverlapSouth, skyLightTop, blockLight);
			}
		}
		
		//WEST face vertex creation
		{
			ColumnArrayView[] adjDataWest = adjData[ELodDirection.WEST.ordinal() - 2];
			int adjOverlapWest = ColorUtil.TRANSPARENT;
			if (adjDataWest == null)
			{
				builder.addQuadAdj(ELodDirection.WEST, x, y, z, zSize, ySize, color, (byte) 15, blockLight);
			}
			else if (adjDataWest.length == 1)
			{
				makeAdjQuads(builder, adjDataWest[0], ELodDirection.WEST, x, y, z, zSize, ySize,
						color, adjOverlapWest, skyLightTop, blockLight);
			}
			else
			{
				makeAdjQuads(builder, adjDataWest[0], ELodDirection.WEST, x, y, z, (short) (zSize / 2), ySize,
						color, adjOverlapWest, skyLightTop, blockLight);
				makeAdjQuads(builder, adjDataWest[1], ELodDirection.WEST, x, y, (short) (z + zSize / 2), (short) (zSize / 2), ySize,
						color, adjOverlapWest, skyLightTop, blockLight);
			}
		}
		
		//EAST face vertex creation
		{
			ColumnArrayView[] adjDataEast = adjData[ELodDirection.EAST.ordinal() - 2];
			int adjOverlapEast = ColorUtil.TRANSPARENT;
			if (adjData[ELodDirection.EAST.ordinal() - 2] == null)
			{
				builder.addQuadAdj(ELodDirection.EAST, maxX, y, z, zSize, ySize, color, (byte) 15, blockLight);
			}
			else if (adjDataEast.length == 1)
			{
				makeAdjQuads(builder, adjDataEast[0], ELodDirection.EAST, maxX, y, z, zSize, ySize,
						color, adjOverlapEast, skyLightTop, blockLight);
			}
			else
			{
				makeAdjQuads(builder, adjDataEast[0], ELodDirection.EAST, maxX, y, z, (short) (zSize / 2), ySize,
						color, adjOverlapEast, skyLightTop, blockLight);
				makeAdjQuads(builder, adjDataEast[1], ELodDirection.EAST, maxX, y, (short) (z + zSize / 2), (short) (zSize / 2), ySize,
						color, adjOverlapEast, skyLightTop, blockLight);
			}
		}
	}
	
	private static void makeAdjQuads(LodQuadBuilder builder, ColumnArrayView adjData, ELodDirection direction, short x, short y,
									 short z, short w0, short wy, int color, int overlapColor, byte upSkyLight, byte blockLight)
	{
		color = ColorUtil.applyShade(color, MC.getShade(direction));
		ColumnArrayView dataPoint = adjData;
		if (dataPoint == null || DataPointUtil.isVoid(dataPoint.get(0)))
		{
			builder.addQuadAdj(direction, x, y, z, w0, wy, color, (byte) 15, blockLight);
			return;
		}
		
		int i;
		boolean firstFace = true;
		boolean allAbove = true;
		short previousDepth = -1;
		byte nextSkyLight = upSkyLight;
		boolean isTransparent = ColorUtil.getAlpha(color) < 255  && transparencyEnabled;;
		boolean lastWasTransparent = false;
		for (i = 0; i < dataPoint.size() && DataPointUtil.doesItExist(adjData.get(i))
				&& !DataPointUtil.isVoid(adjData.get(i)); i++)
		{
			long adjPoint = adjData.get(i);

			boolean isAdjTransparent = DataPointUtil.getAlpha(adjPoint) < 255 && transparencyEnabled;

			if (!isTransparent && isAdjTransparent && transparencyEnabled)
				continue;

			
			short height = DataPointUtil.getHeight(adjPoint);
			short depth = DataPointUtil.getDepth(adjPoint);

			if(fakeOceanFloor && transparencyEnabled)
			{

				if(lastWasTransparent && !isAdjTransparent)
				{
					height = (short) (DataPointUtil.getHeight(adjData.get(i-1)) - 1);
				}
				else if(isAdjTransparent && (i + 1) < adjData.size())
				{
					if (DataPointUtil.getAlpha(adjData.get(i+1)) == 255)
					{
						depth = (short) (height - 1);
					}
				}
			}

			// If the depth of said block is higher than our max Y, continue
			// Basically: y < maxY <= _____ height
			// _______&&: y < maxY <= depth
			if (y + wy <= depth)
				continue;
			// Now: depth < maxY
			allAbove = false;
			
			if (height < y)
			{
				// Basically: _____ height < y < maxY
				// _______&&: depth ______ < y < maxY
				if (firstFace)
				{
					builder.addQuadAdj(direction, x, y, z, w0, wy, color, DataPointUtil.getLightSky(adjPoint),
							blockLight);
				}
				else
				{
					// Now: depth < height < y < previousDepth < maxY
					if (previousDepth == -1)
						throw new RuntimeException("Loop error");
					builder.addQuadAdj(direction, x, y, z, w0, (short) (previousDepth - y), color,
							DataPointUtil.getLightSky(adjPoint), blockLight);
					previousDepth = -1;
				}
				break;
			}
			
			if (depth <= y)
			{ // AND y <= height
				if (y + wy <= height)
				{
					// Basically: ________ y < maxY <= height
					// _______&&: depth <= y < maxY
					// The face is inside adj face completely.
					if (overlapColor != 0)
					{
						builder.addQuadAdj(direction, x, y, z, w0, wy, overlapColor, (byte) 15, (byte) 15);
					}
					break;
				}
				// Otherwise: ________ y <= Height < maxY
				// _______&&: depth <= y _________ < maxY
				// the adj data intersects the lower part of the current data
				if (height > y && overlapColor != 0)
				{
					builder.addQuadAdj(direction, x, y, z, w0, (short) (height - y), overlapColor, (byte) 15, (byte) 15);
				}
				// if this is the only face, use the maxY and break,
				// if there was another face we finish the last one and break
				if (firstFace)
				{
					builder.addQuadAdj(direction, x, height, z, w0, (short) (y + wy - height), color,
							DataPointUtil.getLightSky(adjPoint), blockLight);
				}
				else
				{
					// Now: depth <= y <= height <= previousDepth < maxY
					if (previousDepth == -1)
						throw new RuntimeException("Loop error");
					if (previousDepth > height)
					{
						builder.addQuadAdj(direction, x, height, z, w0, (short) (previousDepth - height), color,
								DataPointUtil.getLightSky(adjPoint), blockLight);
					}
					previousDepth = -1;
				}
				break;
			}
			
			// In here always true: y < depth < maxY
			// _________________&&: y < _____ (height and maxY)
			
			if (y + wy <= height)
			{
				// Basically: y _______ < maxY <= height
				// _______&&: y < depth < maxY
				// the adj data intersects the higher part of the current data
				if (overlapColor != 0)
				{
					builder.addQuadAdj(direction, x, depth, z, w0, (short) (y + wy - depth), overlapColor, (byte) 15, (byte) 15);
				}
				// we start the creation of a new face
			}
			else
			{
				// Otherwise: y < _____ height < maxY
				// _______&&: y < depth ______ < maxY
				if (overlapColor != 0)
				{
					builder.addQuadAdj(direction, x, depth, z, w0, (short) (height - depth), overlapColor, (byte) 15, (byte) 15);
				}
				if (firstFace)
				{
					builder.addQuadAdj(direction, x, height, z, w0, (short) (y + wy - height), color,
							DataPointUtil.getLightSky(adjPoint), blockLight);
				}
				else
				{
					// Now: y < depth < height <= previousDepth < maxY
					if (previousDepth == -1)
						throw new RuntimeException("Loop error");
					if (previousDepth > height)
					{
						builder.addQuadAdj(direction, x, height, z, w0, (short) (previousDepth - height), color,
								DataPointUtil.getLightSky(adjPoint), blockLight);
					}
					previousDepth = -1;
				}
			}
			// set next top as current depth
			previousDepth = depth;
			firstFace = false;
			nextSkyLight = upSkyLight;
			if (i + 1 < adjData.size() && DataPointUtil.doesItExist(adjData.get(i + 1)))
				nextSkyLight = DataPointUtil.getLightSky(adjData.get(i + 1));
			lastWasTransparent = isAdjTransparent;
		}
		
		if (allAbove)
		{
			builder.addQuadAdj(direction, x, y, z, w0, wy, color, upSkyLight, blockLight);
		}
		else if (previousDepth != -1)
		{
			// We need to finish the last quad.
			builder.addQuadAdj(direction, x, y, z, w0, (short) (previousDepth - y), color, nextSkyLight,
					blockLight);
		}
	}
}
