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
				makeAdjVerticalQuad(builder, adjDataNorth[0], ELodDirection.NORTH, x, y, z, xSize, ySize,
						color, adjOverlapNorth, skyLightTop, blockLight,
						topData, bottomData);
			}
			else
			{
				makeAdjVerticalQuad(builder, adjDataNorth[0], ELodDirection.NORTH, x, y, z, (short) (xSize / 2), ySize,
						color, adjOverlapNorth, skyLightTop, blockLight,
						topData, bottomData);
				makeAdjVerticalQuad(builder, adjDataNorth[1], ELodDirection.NORTH, (short) (x + xSize / 2), y, z, (short) (xSize / 2), ySize,
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
				makeAdjVerticalQuad(builder, adjDataSouth[0], ELodDirection.SOUTH, x, y, maxZ, xSize, ySize,
						color, adjOverlapSouth, skyLightTop, blockLight,
						topData, bottomData);
			}
			else
			{
				makeAdjVerticalQuad(builder, adjDataSouth[0], ELodDirection.SOUTH, x, y, maxZ, (short) (xSize / 2), ySize,
						color, adjOverlapSouth, skyLightTop, blockLight,
						topData, bottomData);
		
				makeAdjVerticalQuad(builder, adjDataSouth[1], ELodDirection.SOUTH, (short) (x + xSize / 2), y, maxZ, (short) (xSize / 2), ySize,
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
				makeAdjVerticalQuad(builder, adjDataWest[0], ELodDirection.WEST, x, y, z, zSize, ySize,
						color, adjOverlapWest, skyLightTop, blockLight,
						topData, bottomData);
			}
			else
			{
				makeAdjVerticalQuad(builder, adjDataWest[0], ELodDirection.WEST, x, y, z, (short) (zSize / 2), ySize,
						color, adjOverlapWest, skyLightTop, blockLight,
						topData, bottomData);
				makeAdjVerticalQuad(builder, adjDataWest[1], ELodDirection.WEST, x, y, (short) (z + zSize / 2), (short) (zSize / 2), ySize,
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
				makeAdjVerticalQuad(builder, adjDataEast[0], ELodDirection.EAST, maxX, y, z, zSize, ySize,
						color, adjOverlapEast, skyLightTop, blockLight,
						topData, bottomData);
			}
			else
			{
				makeAdjVerticalQuad(builder, adjDataEast[0], ELodDirection.EAST, maxX, y, z, (short) (zSize / 2), ySize,
						color, adjOverlapEast, skyLightTop, blockLight,
						topData, bottomData);
				makeAdjVerticalQuad(builder, adjDataEast[1], ELodDirection.EAST, maxX, y, (short) (z + zSize / 2), (short) (zSize / 2), ySize,
						color, adjOverlapEast, skyLightTop, blockLight,
						topData, bottomData);
			}
        }
    }
	
	// the overlap color can be used to see faces that shouldn't be rendered
    private static void makeAdjVerticalQuad(
			LodQuadBuilder builder, ColumnArrayView adjColumnView, ELodDirection direction, 
			short x, short y, short z, short horizontalWidth, short upDownWidth, 
			int color, int debugOverlapColor, byte skyLightTop, byte blockLight,
			long topData, long bottomData)
	{
		color = ColorUtil.applyShade(color, MC.getShade(direction));
		
		if (adjColumnView == null || adjColumnView.size == 0 || RenderDataPointUtil.isVoid(adjColumnView.get(0)))
		{
			// there isn't any data adjacent to this LOD, add the vertical quad
			builder.addQuadAdj(direction, x, y, z, horizontalWidth, upDownWidth, color, (byte) 15, blockLight);
			return;
		}
		
		
		int inputMaxHeight = y + upDownWidth;
		
        int adjIndex;
        boolean firstFace = true;
        boolean inputAboveAdjLods = true;
        short previousAdjDepth = -1;
        byte nextTopSkyLight = skyLightTop;
        boolean isTransparent = ColorUtil.getAlpha(color) < 255 && LodRenderer.transparencyEnabled;
        boolean lastAdjWasTransparent = false;
		
		
		
		if (!RenderDataPointUtil.doesDataPointExist(bottomData))
		{
			// there isn't anything under this LOD,
			// to prevent seeing through the world, make it opaque
			color = ColorUtil.setAlpha(color, 255);
		}
		
		
		// Add adjacent faces if this LOD is surrounded by transparent LODs
		// (prevents invisible sides underwater)
		int adjCount = adjColumnView.size();
		for (adjIndex = 0;
				adjIndex < adjCount 
				&& RenderDataPointUtil.doesDataPointExist(adjColumnView.get(adjIndex))
				&& !RenderDataPointUtil.isVoid(adjColumnView.get(adjIndex));
			 adjIndex++)
		{
			long adjPoint = adjColumnView.get(adjIndex);
			boolean isAdjTransparent = RenderDataPointUtil.getAlpha(adjPoint) < 255 && LodRenderer.transparencyEnabled;
			
			
			// continue if this data point is transparent or the adjacent point is not 
			if (isTransparent || !isAdjTransparent) // TODO isTransparent may be unnecessary
			{
				short adjDepth = RenderDataPointUtil.getDepth(adjPoint);
				short adjHeight = RenderDataPointUtil.getHeight(adjPoint);
				
				
				// if fake transparency is enabled, allow for 1 block of transparency,
				// everything under that should be opaque
				if (LodRenderer.transparencyEnabled && LodRenderer.fakeOceanFloor)
				{
					if (lastAdjWasTransparent && !isAdjTransparent)
					{
						adjHeight = (short) (RenderDataPointUtil.getHeight(adjColumnView.get(adjIndex - 1)) - 1);
					}
					else if (isAdjTransparent && (adjIndex+1) < adjCount)
					{
						if (RenderDataPointUtil.getAlpha(adjColumnView.get(adjIndex + 1)) == 255)
						{
							adjDepth = (short) (adjHeight - 1);
						}
					}
				}
				
				
				if (inputMaxHeight <= adjDepth)
				{
					// the adjacent LOD is above the input LOD and won't affect its rendering,
					// skip to the next adjacent
					continue;
				}
				inputAboveAdjLods = false;
				
				
				if (adjHeight < y) // TODO why not adjMaxHeight?
				{
					// the adjacent LOD is below the input LOD
					
					// FIXME both of these methods cause black LODs when next to deep/dark water
					if (firstFace)
					{
						builder.addQuadAdj(direction, x, y, z, horizontalWidth, upDownWidth, color, RenderDataPointUtil.getLightSky(adjPoint),
								blockLight);
					}
					else
					{
						// Now: adjMaxHeight < y < previousAdjDepth < inputMaxHeight
						if (previousAdjDepth == -1)
						{
							// TODO why is this an error?
							throw new RuntimeException("Loop error");
						}
						
						builder.addQuadAdj(direction, x, y, z, horizontalWidth, (short) (previousAdjDepth - y), color,
								RenderDataPointUtil.getLightSky(adjPoint), blockLight);
						
						previousAdjDepth = -1;
					}
					
					
					// TODO why break here?
					break;
				}
				
				
				if (adjDepth <= y)
				{
					// the adjacent LOD's base is at or below the input's base
					
					if (inputMaxHeight <= adjHeight)
					{
						// The input face is completely inside the adj's face, don't render it
						if (debugOverlapColor != 0)
						{
							builder.addQuadAdj(direction, x, y, z, horizontalWidth, upDownWidth, debugOverlapColor, (byte) 15, (byte) 15);
						}
					}
					else
					{
						// the adj data intersects the lower part of the input data, don't render below the intersection
						
						if (adjHeight > y && debugOverlapColor != 0)
						{
							builder.addQuadAdj(direction, x, y, z, horizontalWidth, (short) (adjHeight - y), debugOverlapColor, (byte) 15, (byte) 15);
						}
						
						// if this is the only face, use the inputMaxHeight and break,
						// if there was another face finish the last one and then break
						if (firstFace)
						{
							builder.addQuadAdj(direction, x, adjHeight, z, horizontalWidth, (short) (inputMaxHeight - adjHeight), color,
									RenderDataPointUtil.getLightSky(adjPoint), blockLight);
						}
						else
						{
							// Now: depth <= y <= height <= previousAdjDepth < inputMaxHeight
							if (previousAdjDepth == -1)
							{
								// TODO why is this an error?
								throw new RuntimeException("Loop error");
							}
							
							if (previousAdjDepth > adjHeight)
							{
								builder.addQuadAdj(direction, x, adjHeight, z, horizontalWidth, (short) (previousAdjDepth - adjHeight), color,
										RenderDataPointUtil.getLightSky(adjPoint), blockLight);
							}
							previousAdjDepth = -1;
						}
					}
					
					
					// we don't need to check any other adjacent LODs 
					// since this one completely covers the input
					break;
				}
				
				
				
				// In here always true: y < adjDepth < inputMaxHeight
				// _________________&&: y < ________ (height and inputMaxHeight)
				
				if (inputMaxHeight <= adjHeight)
				{
					// Basically: y _______ < inputMaxHeight <= height
					// _______&&: y < depth < inputMaxHeight
					// the adj data intersects the higher part of the current data
					if (debugOverlapColor != 0)
					{
						builder.addQuadAdj(direction, x, adjDepth, z, horizontalWidth, (short) (inputMaxHeight - adjDepth), debugOverlapColor, (byte) 15, (byte) 15);
					}
					
					// we start the creation of a new face
				}
				else
				{
					// Otherwise: y < _____ height < inputMaxHeight
					// _______&&: y < depth ______ < inputMaxHeight
					if (debugOverlapColor != 0)
					{
						builder.addQuadAdj(direction, x, adjDepth, z, horizontalWidth, (short) (adjHeight - adjDepth), debugOverlapColor, (byte) 15, (byte) 15);
					}
					
					if (firstFace)
					{
						builder.addQuadAdj(direction, x, adjHeight, z, horizontalWidth, (short) (inputMaxHeight - adjHeight), color,
								RenderDataPointUtil.getLightSky(adjPoint), blockLight);
					}
					else
					{
						// Now: y < depth < height <= previousAdjDepth < inputMaxHeight
						if (previousAdjDepth == -1)
							throw new RuntimeException("Loop error");
						if (previousAdjDepth > adjHeight)
						{
							builder.addQuadAdj(direction, x, adjHeight, z, horizontalWidth, (short) (previousAdjDepth - adjHeight), color,
									RenderDataPointUtil.getLightSky(adjPoint), blockLight);
						}
						previousAdjDepth = -1;
					}
				}
				
				
				// set next top as current depth
				previousAdjDepth = adjDepth;
				firstFace = false;
				nextTopSkyLight = skyLightTop;
				
				if (adjIndex + 1 < adjColumnView.size() && RenderDataPointUtil.doesDataPointExist(adjColumnView.get(adjIndex + 1)))
				{
					nextTopSkyLight = RenderDataPointUtil.getLightSky(adjColumnView.get(adjIndex + 1));
				}
				
				lastAdjWasTransparent = isAdjTransparent;
			}
		}
		
		
		
		if (inputAboveAdjLods)
		{
			// the input LOD is above all adjacent LODs and won't be affected
			// by them, add the vertical quad using the input's lighting and height
			builder.addQuadAdj(direction, x, y, z, horizontalWidth, upDownWidth, color, skyLightTop, blockLight);
		}
		else if (previousAdjDepth != -1)
		{
			// We need to finish the last quad.
			builder.addQuadAdj(direction, x, y, z, horizontalWidth, (short) (previousAdjDepth - y), color, nextTopSkyLight, blockLight);
		}
    }
}
