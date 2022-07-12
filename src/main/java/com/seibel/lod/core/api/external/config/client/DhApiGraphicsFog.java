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

package com.seibel.lod.core.api.external.config.client;

import com.seibel.lod.core.api.external.apiObjects.enums.*;
import com.seibel.lod.core.api.external.apiObjects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
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
	public static IDhApiConfig<EDhApiFogDistance> getFogDistanceConfig()
	{ return new DhApiConfig<EFogDistance, EDhApiFogDistance>(FogQuality.fogDistance, new GenericEnumConverter<>(EFogDistance.class, EDhApiFogDistance.class)); }
	
	/** Should be used to enable/disable fog rendering. */
	public static IDhApiConfig<EDhApiFogDrawMode> getFogRenderConfig()
	{ return new DhApiConfig<EFogDrawMode, EDhApiFogDrawMode>(FogQuality.fogDrawMode, new GenericEnumConverter<>(EFogDrawMode.class, EDhApiFogDrawMode.class)); }
	
	/** Can be used to enable support with mods that change vanilla MC's fog color. */
	public static IDhApiConfig<EDhApiFogColorMode> getFogColorConfig()
	{ return new DhApiConfig<EFogColorMode, EDhApiFogColorMode>(FogQuality.fogColorMode, new GenericEnumConverter<>(EFogColorMode.class, EDhApiFogColorMode.class)); }
	
	/**
	 * If enabled attempts to disable vanilla MC's fog on real chunks. <br>
	 * May not play nice with other fog editing mods.
	 */
	public static IDhApiConfig<Boolean> getDisableVanillaFogConfig()
	{ return new DhApiConfig<Boolean, Boolean>(FogQuality.disableVanillaFog); }
	
	
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
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.farFogStart); }
	
	/**
	 * Defines where the fog ends as a percent of the radius
	 * of the fake chunks render distance. <br>
	 * Can be less than the fog start distance to invert the fog direction. <br> <br>
	 *
	 * 0.0 = fog ends at the camera <br>
	 * 1.0 = fog ends at the edge of the fake chunk render distance <br>
	 */
	public static IDhApiConfig<Double> getFogEndDistanceConfig()
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.farFogEnd); }
	
	/** Defines how opaque the fog is at its thinnest point. */
	public static IDhApiConfig<Double> getFogMinThicknessConfig()
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.farFogMin); }
	
	/** Defines how opaque the fog is at its thickest point. */
	public static IDhApiConfig<Double> getFogMaxThicknessConfig()
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.farFogMax); }
	
	/** Defines how the fog changes in thickness. */
	public static IDhApiConfig<EDhApiFogFalloff> getFogFalloffConfig()
	{ return new DhApiConfig<EFogFalloff, EDhApiFogFalloff>(FogQuality.AdvancedFog.farFogType, new GenericEnumConverter<>(EFogFalloff.class, EDhApiFogFalloff.class)); }
	
	/** Defines the fog density. */
	public static IDhApiConfig<Double> getFogDensityConfig()
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.farFogDensity); }
	
	
	//=====================//
	// height fog settings //
	//=====================//
	
	/** Defines how the height fog mixes. */
	public static IDhApiConfig<EDhApiHeightFogMixMode> getHeightFogMixModeConfig()
	{ return new DhApiConfig<EHeightFogMixMode, EDhApiHeightFogMixMode>(FogQuality.AdvancedFog.HeightFog.heightFogMixMode, new GenericEnumConverter<>(EHeightFogMixMode.class, EDhApiHeightFogMixMode.class)); }
	
	/** Defines how the height fog is drawn relative to the camera or world. */
	public static IDhApiConfig<EDhApiHeightFogMode> getHeightFogModeConfig()
	{ return new DhApiConfig<EHeightFogMode, EDhApiHeightFogMode>(FogQuality.AdvancedFog.HeightFog.heightFogMode, new GenericEnumConverter<>(EHeightFogMode.class, EDhApiHeightFogMode.class)); }
	
	/**
	 * Defines the height fog's base height if {@link DhApiGraphicsFog#getHeightFogModeConfig()}
	 * is set to use a specific height.
	 */
	public static IDhApiConfig<Double> getHeightFogBaseHeightConfig()
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.HeightFog.heightFogHeight); }
	
	/** Defines the height fog's starting height as a percent of the world height. */
	public static IDhApiConfig<Double> getHeightFogStartingHeightPercentConfig()
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.HeightFog.heightFogStart); }
	
	/** Defines the height fog's ending height as a percent of the world height. */
	public static IDhApiConfig<Double> getHeightFogEndingHeightPercentConfig()
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.HeightFog.heightFogEnd); }
	
	/** Defines how opaque the height fog is at its thinnest point. */
	public static IDhApiConfig<Double> getHeightFogMinThicknessConfig()
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.HeightFog.heightFogMin); }
	
	/** Defines how opaque the height fog is at its thickest point. */
	public static IDhApiConfig<Double> getHeightFogMaxThicknessConfig()
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.HeightFog.heightFogMax); }
	
	/** Defines how the height fog changes in thickness. */
	public static IDhApiConfig<EDhApiFogFalloff> getHeightFogFalloffConfig()
	{ return new DhApiConfig<EFogFalloff, EDhApiFogFalloff>(FogQuality.AdvancedFog.HeightFog.heightFogType, new GenericEnumConverter<>(EFogFalloff.class, EDhApiFogFalloff.class)); }
	
	/** Defines the height fog's density. */
	public static IDhApiConfig<Double> getHeightFogDensityConfig()
	{ return new DhApiConfig<Double, Double>(FogQuality.AdvancedFog.HeightFog.heightFogDensity); }
	
	
}
