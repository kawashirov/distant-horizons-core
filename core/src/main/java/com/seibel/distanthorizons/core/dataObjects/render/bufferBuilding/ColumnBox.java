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

package com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding;

import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.util.ColorUtil;
import com.seibel.distanthorizons.core.util.RenderDataPointUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.dataObjects.render.columnViews.ColumnArrayView;
import com.seibel.distanthorizons.core.render.renderer.LodRenderer;
import com.seibel.distanthorizons.core.enums.ELodDirection;
import com.seibel.distanthorizons.coreapi.util.MathUtil;

public class ColumnBox
{
    private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
    public static void addBoxQuadsToBuilder(
			LodQuadBuilder builder, 
			short xSize, short ySize, short zSize, 
			short x, short y, short z, 
			int color, byte skyLight, byte blockLight, 
			long topData, long bottomData, ColumnArrayView[][] adjData)
	{
        short maxX = (short) (x + xSize);
        short maxY = (short) (y + ySize);
        short maxZ = (short) (z + zSize);
        byte skyLightTop = skyLight;
        byte skyLightBot = RenderDataPointUtil.doesDataPointExist(bottomData) ? RenderDataPointUtil.getLightSky(bottomData) : 0;
		
        boolean isTransparent = ColorUtil.getAlpha(color) < 255 && LodRenderer.transparencyEnabled;
		boolean overVoid = !RenderDataPointUtil.doesDataPointExist(bottomData);
		boolean isTopTransparent = RenderDataPointUtil.getAlpha(topData) < 255 && LodRenderer.transparencyEnabled;
		boolean isBottomTransparent = RenderDataPointUtil.getAlpha(bottomData) < 255 && LodRenderer.transparencyEnabled;
		
		// if there isn't any data below this LOD, make this LOD's color opaque to prevent seeing void through transparent blocks
		// Note: this LOD should still be considered transparent for this method's checks, otherwise rendering bugs may occur
		if (!RenderDataPointUtil.doesDataPointExist(bottomData))
		{
			color = ColorUtil.setAlpha(color, 255);
		}
		
		
		// cave culling prevention
		// prevents certain faces from being culled underground that should be allowed
		if (builder.skipQuadsWithZeroSkylight
                && 0 == skyLight
                && builder.skyLightCullingBelow > maxY
                && 
				(
					(RenderDataPointUtil.getAlpha(topData) < 255 && RenderDataPointUtil.getHeight(topData) >= builder.skyLightCullingBelow)
                	|| (RenderDataPointUtil.getDepth(topData) >= builder.skyLightCullingBelow)
                	|| !RenderDataPointUtil.doesDataPointExist(topData)
				)
			) 
		{
            maxY = builder.skyLightCullingBelow;
        }
		
		
		
        // fake ocean transparency
		if (LodRenderer.transparencyEnabled && LodRenderer.fakeOceanFloor)
		{
			if (!isTransparent && isTopTransparent && RenderDataPointUtil.doesDataPointExist(topData))
			{
				skyLightTop = (byte) MathUtil.clamp(0, 15 - (RenderDataPointUtil.getHeight(topData) - y), 15);
				ySize = (short) (RenderDataPointUtil.getHeight(topData) - y - 1);
			}
			else if (isTransparent && !isBottomTransparent && RenderDataPointUtil.doesDataPointExist(bottomData))
			{
				y = (short) (y + ySize - 1);
				ySize = 1;
			}
			
			maxY = (short) (y + ySize);
		}
		
		
		
		// add top and bottom faces if requested //
		
		boolean skipTop = RenderDataPointUtil.doesDataPointExist(topData) && (RenderDataPointUtil.getDepth(topData) == maxY) && !isTopTransparent;
        if (!skipTop)
		{
			builder.addQuadUp(x, maxY, z, xSize, zSize, ColorUtil.applyShade(color, MC.getShade(ELodDirection.UP)), skyLightTop, blockLight);
		}
		
		boolean skipBottom = RenderDataPointUtil.doesDataPointExist(bottomData) && (RenderDataPointUtil.getHeight(bottomData) == y) && !isBottomTransparent;
        if (!skipBottom)
		{
			builder.addQuadDown(x, y, z, xSize, zSize, ColorUtil.applyShade(color, MC.getShade(ELodDirection.DOWN)), skyLightBot, blockLight);
		}
		
		
		// add North, south, east, and west faces if requested //
		
		// TODO merge duplicate code
        //NORTH face vertex creation
        {
			ColumnArrayView[] adjDataNorth = adjData[ELodDirection.NORTH.ordinal() - 2]; // TODO can we use something other than ordinal-2?
			int adjOverlapNorth = ColorUtil.INVISIBLE;
			if (adjDataNorth == null)
			{
				// add an adjacent face if this is opaque face or transparent over the void
				if (!isTransparent || overVoid)
				{
					builder.addQuadAdj(ELodDirection.NORTH, x, y, z, xSize, ySize, color, (byte) 15, blockLight);
				}
			}
			else if (adjDataNorth.length == 1)
			{
				makeAdjQuads(builder, adjDataNorth[0], ELodDirection.NORTH, x, y, z, xSize, ySize,
						color, adjOverlapNorth, skyLightTop, blockLight,
						topData, bottomData);
			}
			else
			{
				makeAdjQuads(builder, adjDataNorth[0], ELodDirection.NORTH, x, y, z, (short) (xSize / 2), ySize,
						color, adjOverlapNorth, skyLightTop, blockLight,
						topData, bottomData);
				makeAdjQuads(builder, adjDataNorth[1], ELodDirection.NORTH, (short) (x + xSize / 2), y, z, (short) (xSize / 2), ySize,
						color, adjOverlapNorth, skyLightTop, blockLight,
						topData, bottomData);
			}
        }

        //SOUTH face vertex creation
        {
            ColumnArrayView[] adjDataSouth = adjData[ELodDirection.SOUTH.ordinal() - 2];
			int adjOverlapSouth = ColorUtil.INVISIBLE;
			if (adjDataSouth == null)
			{
				if (!isTransparent || overVoid)
					builder.addQuadAdj(ELodDirection.SOUTH, x, y, maxZ, xSize, ySize, color, (byte) 15, blockLight);
			}
			else if (adjDataSouth.length == 1)
			{
				makeAdjQuads(builder, adjDataSouth[0], ELodDirection.SOUTH, x, y, maxZ, xSize, ySize,
						color, adjOverlapSouth, skyLightTop, blockLight,
						topData, bottomData);
			}
			else
			{
				makeAdjQuads(builder, adjDataSouth[0], ELodDirection.SOUTH, x, y, maxZ, (short) (xSize / 2), ySize,
						color, adjOverlapSouth, skyLightTop, blockLight,
						topData, bottomData);
		
				makeAdjQuads(builder, adjDataSouth[1], ELodDirection.SOUTH, (short) (x + xSize / 2), y, maxZ, (short) (xSize / 2), ySize,
						color, adjOverlapSouth, skyLightTop, blockLight,
						topData, bottomData);
			}
        }

        //WEST face vertex creation
        {
            ColumnArrayView[] adjDataWest = adjData[ELodDirection.WEST.ordinal() - 2];
			int adjOverlapWest = ColorUtil.INVISIBLE;
			if (adjDataWest == null)
			{
				if (!isTransparent || overVoid)
					builder.addQuadAdj(ELodDirection.WEST, x, y, z, zSize, ySize, color, (byte) 15, blockLight);
			}
			else if (adjDataWest.length == 1)
			{
				makeAdjQuads(builder, adjDataWest[0], ELodDirection.WEST, x, y, z, zSize, ySize,
						color, adjOverlapWest, skyLightTop, blockLight,
						topData, bottomData);
			}
			else
			{
				makeAdjQuads(builder, adjDataWest[0], ELodDirection.WEST, x, y, z, (short) (zSize / 2), ySize,
						color, adjOverlapWest, skyLightTop, blockLight,
						topData, bottomData);
				makeAdjQuads(builder, adjDataWest[1], ELodDirection.WEST, x, y, (short) (z + zSize / 2), (short) (zSize / 2), ySize,
						color, adjOverlapWest, skyLightTop, blockLight,
						topData, bottomData);
			}
        }

        //EAST face vertex creation
        {
            ColumnArrayView[] adjDataEast = adjData[ELodDirection.EAST.ordinal() - 2];
			int adjOverlapEast = ColorUtil.INVISIBLE;
			if (adjData[ELodDirection.EAST.ordinal() - 2] == null)
			{
				if (!isTransparent || overVoid)
					builder.addQuadAdj(ELodDirection.EAST, maxX, y, z, zSize, ySize, color, (byte) 15, blockLight);
			}
			else if (adjDataEast.length == 1)
			{
				makeAdjQuads(builder, adjDataEast[0], ELodDirection.EAST, maxX, y, z, zSize, ySize,
						color, adjOverlapEast, skyLightTop, blockLight,
						topData, bottomData);
			}
			else
			{
				makeAdjQuads(builder, adjDataEast[0], ELodDirection.EAST, maxX, y, z, (short) (zSize / 2), ySize,
						color, adjOverlapEast, skyLightTop, blockLight,
						topData, bottomData);
				makeAdjQuads(builder, adjDataEast[1], ELodDirection.EAST, maxX, y, (short) (z + zSize / 2), (short) (zSize / 2), ySize,
						color, adjOverlapEast, skyLightTop, blockLight,
						topData, bottomData);
			}
        }
    }

    private static void makeAdjQuads(
			LodQuadBuilder builder, ColumnArrayView adjData, ELodDirection direction, short x, short y,
			short z, short w0, short wy, int color, int overlapColor, byte upSkyLight, byte blockLight,
			long topData, long bottomData)
	{
		color = ColorUtil.applyShade(color, MC.getShade(direction));
		if (adjData == null || adjData.size == 0 || RenderDataPointUtil.isVoid(adjData.get(0)))
		{
			builder.addQuadAdj(direction, x, y, z, w0, wy, color, (byte) 15, blockLight);
			return;
		}
		
        int i;
        boolean firstFace = true;
        boolean allAbove = true;
        short previousDepth = -1;
        byte nextSkyLight = upSkyLight;
        boolean isTransparent = ColorUtil.getAlpha(color) < 255 && LodRenderer.transparencyEnabled;
        boolean lastWasTransparent = false;
		
		if (!RenderDataPointUtil.doesDataPointExist(bottomData))
		{
			color = ColorUtil.setAlpha(color, 255);
		}
		
		
		// Add adjacent faces if this LOD is are surrounded by transparent LODs
		// (prevents invisible sides underwater)
		for (i = 0;
			i < adjData.size() && RenderDataPointUtil.doesDataPointExist(adjData.get(i))
				&& !RenderDataPointUtil.isVoid(adjData.get(i));
			 i++)
		{
			long adjPoint = adjData.get(i);
			boolean isAdjTransparent = RenderDataPointUtil.getAlpha(adjPoint) < 255 && LodRenderer.transparencyEnabled;
			
			if (!(!isTransparent && isAdjTransparent))
			{
				short height = RenderDataPointUtil.getHeight(adjPoint);
				short depth = RenderDataPointUtil.getDepth(adjPoint);
				
				if (LodRenderer.transparencyEnabled && LodRenderer.fakeOceanFloor)
				{
					
					if (lastWasTransparent && !isAdjTransparent)
					{
						height = (short) (RenderDataPointUtil.getHeight(adjData.get(i - 1)) - 1);
					}
					else if (isAdjTransparent && (i + 1) < adjData.size())
					{
						if (RenderDataPointUtil.getAlpha(adjData.get(i + 1)) == 255)
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
						builder.addQuadAdj(direction, x, y, z, w0, wy, color, RenderDataPointUtil.getLightSky(adjPoint),
								blockLight);
					}
					else
					{
						// Now: depth < height < y < previousDepth < maxY
						if (previousDepth == -1)
							throw new RuntimeException("Loop error");
						builder.addQuadAdj(direction, x, y, z, w0, (short) (previousDepth - y), color,
								RenderDataPointUtil.getLightSky(adjPoint), blockLight);
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
								RenderDataPointUtil.getLightSky(adjPoint), blockLight);
					}
					else
					{
						// Now: depth <= y <= height <= previousDepth < maxY
						if (previousDepth == -1)
							throw new RuntimeException("Loop error");
						if (previousDepth > height)
						{
							builder.addQuadAdj(direction, x, height, z, w0, (short) (previousDepth - height), color,
									RenderDataPointUtil.getLightSky(adjPoint), blockLight);
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
								RenderDataPointUtil.getLightSky(adjPoint), blockLight);
					}
					else
					{
						// Now: y < depth < height <= previousDepth < maxY
						if (previousDepth == -1)
							throw new RuntimeException("Loop error");
						if (previousDepth > height)
						{
							builder.addQuadAdj(direction, x, height, z, w0, (short) (previousDepth - height), color,
									RenderDataPointUtil.getLightSky(adjPoint), blockLight);
						}
						previousDepth = -1;
					}
				}
				
				
				// set next top as current depth
				previousDepth = depth;
				firstFace = false;
				nextSkyLight = upSkyLight;
				if (i + 1 < adjData.size() && RenderDataPointUtil.doesDataPointExist(adjData.get(i + 1)))
					nextSkyLight = RenderDataPointUtil.getLightSky(adjData.get(i + 1));
				lastWasTransparent = isAdjTransparent;
			}
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