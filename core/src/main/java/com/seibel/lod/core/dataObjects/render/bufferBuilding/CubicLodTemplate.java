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

package com.seibel.lod.core.dataObjects.render.bufferBuilding;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.util.RenderDataPointUtil;
import com.seibel.lod.api.enums.rendering.EDebugRendering;
import com.seibel.lod.core.dataObjects.render.columnViews.ColumnArrayView;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.coreapi.util.BitShiftUtil;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.LodUtil;

/**
 * Builds LODs as rectangular prisms.
 * @author James Seibel
 * @version 2022-1-2
 */
public class CubicLodTemplate
{

	public static void addLodToBuffer(
			long data, long topData, long bottomData, ColumnArrayView[][] adjData,
			byte detailLevel, int offsetPosX, int offsetOosZ, LodQuadBuilder quadBuilder, 
			EDebugRendering debugging, ColumnRenderSource.DebugSourceFlag debugSource)
	{
		DhLodPos blockOffsetPos = new DhLodPos(detailLevel, offsetPosX, offsetOosZ).convertToDetailLevel(LodUtil.BLOCK_DETAIL_LEVEL);
		
		short width = (short) BitShiftUtil.powerOfTwo(detailLevel);
		short x = (short) blockOffsetPos.x;
		short y = RenderDataPointUtil.getDepth(data);
		short z = (short) (short) blockOffsetPos.z;
		short yHeight = (short) (RenderDataPointUtil.getHeight(data) - y);
		
		if (yHeight == 0)
		{
			return;
		}
		else if (yHeight < 0)
		{
			throw new IllegalArgumentException("Negative y size for the data! Data: " + RenderDataPointUtil.toString(data));
		}
		
		
		
		int color;
		boolean fullBright = false;
		switch (debugging) {
			case OFF:
			{
				float saturationMultiplier = Config.Client.Advanced.Graphics.AdvancedGraphics.saturationMultiplier.get().floatValue();
				float brightnessMultiplier = Config.Client.Advanced.Graphics.AdvancedGraphics.brightnessMultiplier.get().floatValue();
				if (saturationMultiplier == 1.0 && brightnessMultiplier == 1.0)
				{
					color = RenderDataPointUtil.getColor(data);
				}
				else
				{
					float[] ahsv = ColorUtil.argbToAhsv(RenderDataPointUtil.getColor(data));
					color = ColorUtil.ahsvToArgb(ahsv[0], ahsv[1], ahsv[2] * saturationMultiplier, ahsv[3] * brightnessMultiplier);
					//ApiShared.LOGGER.info("Raw color:[{}], AHSV:{}, Out color:[{}]",
					//		ColorUtil.toString(DataPointUtil.getColor(data)),
					//		ahsv, ColorUtil.toString(color));
				}
				break;
			}
			case SHOW_DETAIL:
			{
				color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[detailLevel];
				fullBright = true;
				break;
			}
			case SHOW_GENMODE:
			{
				color = LodUtil.DEBUG_DETAIL_LEVEL_COLORS[RenderDataPointUtil.getGenerationMode(data)];
				fullBright = true;
				break;
			}
			case SHOW_OVERLAPPING_QUADS:
			{
				color = ColorUtil.WHITE;
				fullBright = true;
				break;
			}
			case SHOW_RENDER_SOURCE_FLAG:
			{
				color = debugSource == null ? ColorUtil.RED : debugSource.color;
				fullBright = true;
				break;
			}
			default:
				throw new IllegalArgumentException("Unknown debug mode: " + debugging);
		}
		
		ColumnBox.addBoxQuadsToBuilder(
				quadBuilder, // buffer
				width, yHeight, width, // setWidth
				x, y, z, // setOffset
				color, // setColor
				RenderDataPointUtil.getLightSky(data), // setSkyLights
				fullBright ? 15 : RenderDataPointUtil.getLightBlock(data), // setBlockLights
				topData, bottomData, adjData); // setAdjData
	}
}
