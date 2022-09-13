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

package com.seibel.lod.core.api.external.coreImplementations.methods.config.client;

import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.api.methods.config.DhApiConfig;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.config.Config.Client.Graphics.FogQuality;

/**
 * Distant Horizons' fog configuration. <br><br>
 *
 * Note: unless an option explicitly states that it modifies
 * Minecraft's vanilla rendering (like DisableVanillaFog)
 * these settings will only affect Distant horizons' fog.
 *
 * @author James Seibel
 * @version 2022-7-11
 */
public class DhApiGraphicsFog
{
	
	//====================//
	// basic fog settings //
	//====================//
	
	/** Defines at what distance fog is rendered on fake chunks. */
	public static IDhApiConfig<EFogDistance> getFogDistanceConfig()
	{ return new DhApiConfig<>(FogQuality.fogDistance); }
	
	/** Should be used to enable/disable fog rendering. */
	public static IDhApiConfig<EFogDrawMode> getFogRenderConfig()
	{ return new DhApiConfig<>(FogQuality.fogDrawMode); }
	
	/** Can be used to enable support with mods that change vanilla MC's fog color. */
	public static IDhApiConfig<EFogColorMode> getFogColorConfig()
	{ return new DhApiConfig<>(FogQuality.fogColorMode); }
	
	/**
	 * If enabled attempts to disable vanilla MC's fog on real chunks. <br>
	 * May not play nice with other fog editing mods.
	 */
	public static IDhApiConfig<Boolean> getDisableVanillaFogConfig()
	{ return new DhApiConfig<>(FogQuality.disableVanillaFog); }
	
	
	//=======================//
	// advanced fog settings //
	//=======================//
	
	/**
	 * Defines where the fog starts as a percent of the
	 * fake chunks render distance radius. <br>
	 * Can be greater than the fog end distance to invert the fog direction. <br> <br>
	 *
	 * 0.0 = fog starts at the camera <br>
	 * 1.0 = fog starts at the edge of the fake chunk render distance <br>
	 */
	public static IDhApiConfig<Double> getFogStartDistanceConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogStart); }
	
	/**
	 * Defines where the fog ends as a percent of the radius
	 * of the fake chunks render distance. <br>
	 * Can be less than the fog start distance to invert the fog direction. <br> <br>
	 *
	 * 0.0 = fog ends at the camera <br>
	 * 1.0 = fog ends at the edge of the fake chunk render distance <br>
	 */
	public static IDhApiConfig<Double> getFogEndDistanceConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogEnd); }
	
	/** Defines how opaque the fog is at its thinnest point. */
	public static IDhApiConfig<Double> getFogMinThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogMin); }
	
	/** Defines how opaque the fog is at its thickest point. */
	public static IDhApiConfig<Double> getFogMaxThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogMax); }
	
	/** Defines how the fog changes in thickness. */
	public static IDhApiConfig<EFogFalloff> getFogFalloffConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogType); }
	
	/** Defines the fog density. */
	public static IDhApiConfig<Double> getFogDensityConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogDensity); }
	
	
	//=====================//
	// height fog settings //
	//=====================//
	
	/** Defines how the height fog mixes. */
	public static IDhApiConfig<EHeightFogMixMode> getHeightFogMixModeConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMixMode); }
	
	/** Defines how the height fog is drawn relative to the camera or world. */
	public static IDhApiConfig<EHeightFogMode> getHeightFogModeConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMode); }
	
	/**
	 * Defines the height fog's base height if {@link DhApiGraphicsFog#getHeightFogModeConfig()}
	 * is set to use a specific height.
	 */
	public static IDhApiConfig<Double> getHeightFogBaseHeightConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogHeight); }
	
	/** Defines the height fog's starting height as a percent of the world height. */
	public static IDhApiConfig<Double> getHeightFogStartingHeightPercentConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogStart); }
	
	/** Defines the height fog's ending height as a percent of the world height. */
	public static IDhApiConfig<Double> getHeightFogEndingHeightPercentConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogEnd); }
	
	/** Defines how opaque the height fog is at its thinnest point. */
	public static IDhApiConfig<Double> getHeightFogMinThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMin); }
	
	/** Defines how opaque the height fog is at its thickest point. */
	public static IDhApiConfig<Double> getHeightFogMaxThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMax); }
	
	/** Defines how the height fog changes in thickness. */
	public static IDhApiConfig<EFogFalloff> getHeightFogFalloffConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogType); }
	
	/** Defines the height fog's density. */
	public static IDhApiConfig<Double> getHeightFogDensityConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogDensity); }
	
	
}
