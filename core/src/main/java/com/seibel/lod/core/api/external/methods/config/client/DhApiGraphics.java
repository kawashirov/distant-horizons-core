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

package com.seibel.lod.core.api.external.methods.config.client;

import com.seibel.lod.core.api.external.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.core.api.external.items.enums.config.*;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.objects.RenderModeEnabledConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.ERendererMode;
import com.seibel.lod.core.config.Config.Client.Graphics.Quality;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;
import com.seibel.lod.core.config.Config.Client.Graphics.AdvancedGraphics;

/**
 * Distant Horizons' graphics/rendering configuration.
 *
 * @author James Seibel
 * @version 2022-7-11
 */
public class DhApiGraphics
{
	
	//========================//
	// basic graphic settings //
	//========================//
	
	/** The distance is the radius measured in chunks. */
	public static IDhApiConfig<Integer> getChunkRenderDistanceConfig()
	{ return new DhApiConfig<Integer, Integer>(Quality.lodChunkRenderDistance); }
	
	/**
	 * Simplified version of {@link DhApiGraphics#getRenderingModeConfig()}
	 * that only enables/disables the fake chunk rendering. <br><br>
	 *
	 * Changing this config also changes {@link DhApiGraphics#getRenderingModeConfig()}'s value.
	 */
	public static IDhApiConfig<Boolean> getRenderingEnabledConfig()
	{ return new DhApiConfig<ERendererMode, Boolean>(Debugging.rendererMode, new RenderModeEnabledConverter()); }
	
	/**
	 * Can be used to enable/disable fake chunk rendering or enable the debug renderer. <br><br>
	 *
	 * The debug renderer is used to confirm rendering is working at and will draw
	 * a single multicolor rhombus on the screen in skybox space (AKA behind MC's rendering). <br><br>
	 *
	 * Changing this config also changes {@link DhApiGraphics#getRenderingEnabledConfig()}'s value.
	 */
	public static IDhApiConfig<EDhApiRendererMode> getRenderingModeConfig()
	{ return new DhApiConfig<ERendererMode, EDhApiRendererMode>(Debugging.rendererMode, new GenericEnumConverter<>(ERendererMode.class, EDhApiRendererMode.class)); }
	
	
	
	//==================//
	// graphic settings //
	//==================//
	
	/** Defines how detailed fake chunks are in the horizontal direction */
	public static IDhApiConfig<EDhApiHorizontalResolution> getMaxDetailLevelConfig()
	{ return new DhApiConfig<EHorizontalResolution, EDhApiHorizontalResolution>(Quality.drawResolution, new GenericEnumConverter<>(EHorizontalResolution.class, EDhApiHorizontalResolution.class)); }
	
	/** Defines how detailed fake chunks are in the vertical direction */
	public static IDhApiConfig<EDhApiVerticalQuality> getVerticalQualityConfig()
	{ return new DhApiConfig<EVerticalQuality, EDhApiVerticalQuality>(Quality.verticalQuality, new GenericEnumConverter<>(EVerticalQuality.class, EDhApiVerticalQuality.class)); }
	
	/** Modifies the quadratic function fake chunks use for horizontal quality drop-off. */
	public static IDhApiConfig<EDhApiHorizontalQuality> getHorizontalQualityDropoffConfig()
	{ return new DhApiConfig<EHorizontalQuality, EDhApiHorizontalQuality>(Quality.horizontalQuality, new GenericEnumConverter<>(EHorizontalQuality.class, EDhApiHorizontalQuality.class)); }
	
	/**
	 * Defines how fake chunks drop off in quality. <br><br>
	 *
	 * Higher quality settings require fake chunk geometry data to be
	 * rebuilt more often when moving but make quality drop-off less noticeable.
	 * */
	public static IDhApiConfig<EDhApiDropoffQuality> getHorizontalQualityDropoffMethodConfig()
	{ return new DhApiConfig<EDropoffQuality, EDhApiDropoffQuality>(Quality.dropoffQuality, new GenericEnumConverter<>(EDropoffQuality.class, EDhApiDropoffQuality.class)); }
	
	/**
	 * The same as vanilla Minecraft's biome blending. <br><br>
	 *
	 * 0 = blending of 1x1 aka off	<br>
	 * 1 = blending of 3x3			<br>
	 * 2 = blending of 5x5			<br>
	 * ...							<br>
	 */
	public static IDhApiConfig<Integer> getBiomeBlendingConfig()
	{ return new DhApiConfig<Integer, Integer>(Quality.lodBiomeBlending); }
	
	
	
	//===========================//
	// advanced graphic settings //
	//===========================//
	
	/** If directional culling is disabled fake chunks will be rendered behind the camera. */
	public static IDhApiConfig<Boolean> getDisableDirectionalCullingConfig()
	{ return new DhApiConfig<Boolean, Boolean>(AdvancedGraphics.disableDirectionalCulling); }
	
	/** Determines how fake chunks are rendered in comparison to vanilla MC's chunks. */
	public static IDhApiConfig<EDhApiVanillaOverdraw> getVanillaOverdrawConfig()
	{ return new DhApiConfig<EVanillaOverdraw, EDhApiVanillaOverdraw>(AdvancedGraphics.vanillaOverdraw, new GenericEnumConverter<>(EVanillaOverdraw.class, EDhApiVanillaOverdraw.class)); }
	
	/** Modifies how far the vanilla overdraw is rendered in chunks. */
	public static IDhApiConfig<Integer> getVanillaOverdrawOffsetConfig()
	{ return new DhApiConfig<Integer, Integer>(AdvancedGraphics.overdrawOffset); }
	
	/**
	 * If enabled the near clip plane is extended to reduce
	 * overdraw and improve Z-fighting at extreme render distances. <br>
	 * Disabling this reduces holes in the world due to the near clip plane
	 * being too close to the camera and the terrain not being covered by vanilla terrain.
	 */
	public static IDhApiConfig<Boolean> getUseExtendedNearClipPlaneConfig()
	{ return new DhApiConfig<Boolean, Boolean>(AdvancedGraphics.useExtendedNearClipPlane); }
	
	/**
	 * Modifies how bright fake chunks are. <br>
	 * This is done when generating the vertex data and is applied before any shaders.
	 */
	public static IDhApiConfig<Double> getBrightnessMultiplierConfig()
	{ return new DhApiConfig<Double, Double>(AdvancedGraphics.brightnessMultiplier); }
	
	/**
	 * Modifies how saturated fake chunks are. <br>
	 * This is done when generating the vertex data and is applied before any shaders.
	 */
	public static IDhApiConfig<Double> getSaturationMultiplierConfig()
	{ return new DhApiConfig<Double, Double>(AdvancedGraphics.saturationMultiplier); }
	
	/** Defines if Distant Horizons should attempt to cull fake chunk cave geometry. */
	public static IDhApiConfig<Boolean> getCaveCullingEnabledConfig()
	{ return new DhApiConfig<Boolean, Boolean>(AdvancedGraphics.enableCaveCulling); }
	
	/** Defines what height cave culling should be used below if enabled. */
	public static IDhApiConfig<Integer> getCaveCullingHeightConfig()
	{ return new DhApiConfig<Integer, Integer>(AdvancedGraphics.caveCullingHeight); }
	
	/** This ratio is relative to Earth's real world curvature. */
	public static IDhApiConfig<Integer> getEarthCurvatureRatioConfig()
	{ return new DhApiConfig<Integer, Integer>(AdvancedGraphics.earthCurveRatio); }
	
	/** If enabled vanilla chunk rendering is disabled and only fake chunks are rendered. */
	public static IDhApiConfig<Boolean> getEnableLodOnlyModeConfig()
	{ return new DhApiConfig<Boolean, Boolean>(Config.Client.Advanced.lodOnlyMode); }
	
	/** Defines how often the geometry should be rebuilt when the player moves. */
	public static IDhApiConfig<EDhApiBufferRebuildTimes> getGeometryRebuildFrequencyConfig()
	{ return new DhApiConfig<EBufferRebuildTimes, EDhApiBufferRebuildTimes>(Config.Client.Advanced.Buffers.rebuildTimes, new GenericEnumConverter<>(EBufferRebuildTimes.class, EDhApiBufferRebuildTimes.class)); }
	
	
	
}
