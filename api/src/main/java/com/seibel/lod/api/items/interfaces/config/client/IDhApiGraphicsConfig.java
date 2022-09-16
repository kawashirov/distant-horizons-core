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

package com.seibel.lod.api.items.interfaces.config.client;

import com.seibel.lod.api.items.enums.config.*;
import com.seibel.lod.api.items.enums.rendering.ERendererMode;
import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;

/**
 * Distant Horizons' graphics/rendering configuration.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public interface IDhApiGraphicsConfig
{
	
	//========================//
	// basic graphic settings //
	//========================//
	
	/** The distance is the radius measured in chunks. */
	IDhApiConfig<Integer> getChunkRenderDistanceConfig();
	
	/**
	 * Simplified version of {@link IDhApiGraphicsConfig#getRenderingModeConfig()}
	 * that only enables/disables the fake chunk rendering. <br><br>
	 *
	 * Changing this config also changes {@link IDhApiGraphicsConfig#getRenderingModeConfig()}'s value.
	 */
	IDhApiConfig<Boolean> getRenderingEnabledConfig();
	
	/**
	 * Can be used to enable/disable fake chunk rendering or enable the debug renderer. <br><br>
	 *
	 * The debug renderer is used to confirm rendering is working at and will draw
	 * a single multicolor rhombus on the screen in skybox space (AKA behind MC's rendering). <br><br>
	 *
	 * Changing this config also changes {@link IDhApiGraphicsConfig#getRenderingEnabledConfig()}'s value.
	 */
	IDhApiConfig<ERendererMode> getRenderingModeConfig();
	
	
	
	//==================//
	// graphic settings //
	//==================//
	
	/** Defines how detailed fake chunks are in the horizontal direction */
	IDhApiConfig<EHorizontalResolution> getMaxDetailLevelConfig();
	
	/** Defines how detailed fake chunks are in the vertical direction */
	IDhApiConfig<EVerticalQuality> getVerticalQualityConfig();
	
	/** Modifies the quadratic function fake chunks use for horizontal quality drop-off. */
	IDhApiConfig<EHorizontalQuality> getHorizontalQualityDropoffConfig();

	/**
	 * The same as vanilla Minecraft's biome blending. <br><br>
	 *
	 * 0 = blending of 1x1 aka off	<br>
	 * 1 = blending of 3x3			<br>
	 * 2 = blending of 5x5			<br>
	 * ...							<br>
	 */
	IDhApiConfig<Integer> getBiomeBlendingConfig();
	
	
	
	//===========================//
	// advanced graphic settings //
	//===========================//
	
	/** If directional culling is disabled fake chunks will be rendered behind the camera. */
	IDhApiConfig<Boolean> getDisableDirectionalCullingConfig();
	
	/** Determines how fake chunks are rendered in comparison to vanilla MC's chunks. */
	IDhApiConfig<EVanillaOverdraw> getVanillaOverdrawConfig();
	
	/** Modifies how far the vanilla overdraw is rendered in chunks. */
	IDhApiConfig<Integer> getVanillaOverdrawOffsetConfig();
	
	/**
	 * If enabled the near clip plane is extended to reduce
	 * overdraw and improve Z-fighting at extreme render distances. <br>
	 * Disabling this reduces holes in the world due to the near clip plane
	 * being too close to the camera and the terrain not being covered by vanilla terrain.
	 */
	IDhApiConfig<Boolean> getUseExtendedNearClipPlaneConfig();
	
	/**
	 * Modifies how bright fake chunks are. <br>
	 * This is done when generating the vertex data and is applied before any shaders.
	 */
	IDhApiConfig<Double> getBrightnessMultiplierConfig();
	
	/**
	 * Modifies how saturated fake chunks are. <br>
	 * This is done when generating the vertex data and is applied before any shaders.
	 */
	IDhApiConfig<Double> getSaturationMultiplierConfig();
	
	/** Defines if Distant Horizons should attempt to cull fake chunk cave geometry. */
	IDhApiConfig<Boolean> getCaveCullingEnabledConfig();
	
	/** Defines what height cave culling should be used below if enabled. */
	IDhApiConfig<Integer> getCaveCullingHeightConfig();
	
	/** This ratio is relative to Earth's real world curvature. */
	IDhApiConfig<Integer> getEarthCurvatureRatioConfig();
	
	/** If enabled vanilla chunk rendering is disabled and only fake chunks are rendered. */
	IDhApiConfig<Boolean> getEnableLodOnlyModeConfig();
	
	/** Defines how often the geometry should be rebuilt when the player moves. */
	IDhApiConfig<EBufferRebuildTimes> getGeometryRebuildFrequencyConfig();
	
	
	
}
