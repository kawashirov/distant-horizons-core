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

package com.seibel.lod.api.interfaces.config.client;

import com.seibel.lod.api.enums.config.*;
import com.seibel.lod.api.enums.rendering.ERendererMode;
import com.seibel.lod.api.interfaces.config.IDhApiConfigValue;
import com.seibel.lod.api.interfaces.config.IDhApiConfigGroup;

/**
 * Distant Horizons' graphics/rendering configuration.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public interface IDhApiGraphicsConfig extends IDhApiConfigGroup
{
	
	//========================//
	// basic graphic settings //
	//========================//
	
	/** The distance is the radius measured in chunks. */
	IDhApiConfigValue<Integer> getChunkRenderDistance();
	
	/**
	 * Simplified version of {@link IDhApiGraphicsConfig#getRenderingMode()}
	 * that only enables/disables the fake chunk rendering. <br><br>
	 *
	 * Changing this config also changes {@link IDhApiGraphicsConfig#getRenderingMode()}'s value.
	 */
	IDhApiConfigValue<Boolean> getRenderingEnabled();
	
	/**
	 * Can be used to enable/disable fake chunk rendering or enable the debug renderer. <br><br>
	 *
	 * The debug renderer is used to confirm rendering is working at and will draw
	 * a single multicolor rhombus on the screen in skybox space (AKA behind MC's rendering). <br><br>
	 *
	 * Changing this config also changes {@link IDhApiGraphicsConfig#getRenderingEnabled()}'s value.
	 */
	IDhApiConfigValue<ERendererMode> getRenderingMode();
	
	
	
	//==================//
	// graphic settings //
	//==================//
	
	/** Defines how detailed fake chunks are in the horizontal direction */
	IDhApiConfigValue<EHorizontalResolution> getMaxDetailLevel();
	
	/** Defines how detailed fake chunks are in the vertical direction */
	IDhApiConfigValue<EVerticalQuality> getVerticalQuality();
	
	/** Modifies the quadratic function fake chunks use for horizontal quality drop-off. */
	IDhApiConfigValue<EHorizontalQuality> getHorizontalQualityDropoff();

	/**
	 * The same as vanilla Minecraft's biome blending. <br><br>
	 *
	 * 0 = blending of 1x1 aka off	<br>
	 * 1 = blending of 3x3			<br>
	 * 2 = blending of 5x5			<br>
	 * ...							<br>
	 */
//	IDhApiConfigValue<Integer> getBiomeBlending();
	
	
	
	//===========================//
	// advanced graphic settings //
	//===========================//
	
	/**
	 * If enabled the near clip plane is extended to reduce
	 * overdraw and improve Z-fighting at extreme render distances. <br>
	 * Disabling this reduces holes in the world due to the near clip plane
	 * being too close to the camera and the terrain not being covered by vanilla terrain.
	 */
	IDhApiConfigValue<Boolean> getUseExtendedNearClipPlane();
	
	/**
	 * Modifies how bright fake chunks are. <br>
	 * This is done when generating the vertex data and is applied before any shaders.
	 */
	IDhApiConfigValue<Double> getBrightnessMultiplier();
	
	/**
	 * Modifies how saturated fake chunks are. <br>
	 * This is done when generating the vertex data and is applied before any shaders.
	 */
	IDhApiConfigValue<Double> getSaturationMultiplier();
	
	/** Defines if Distant Horizons should attempt to cull fake chunk cave geometry. */
	IDhApiConfigValue<Boolean> getCaveCullingEnabled();
	
	/** Defines what height cave culling should be used below if enabled. */
	IDhApiConfigValue<Integer> getCaveCullingHeight();
	
	/** This ratio is relative to Earth's real world curvature. */
	IDhApiConfigValue<Integer> getEarthCurvatureRatio();
	
	/** If enabled vanilla chunk rendering is disabled and only fake chunks are rendered. */
	IDhApiConfigValue<Boolean> getEnableLodOnlyMode();
	
	
	
}
