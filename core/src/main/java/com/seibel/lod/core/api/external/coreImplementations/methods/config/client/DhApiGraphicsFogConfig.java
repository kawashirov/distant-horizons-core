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

import com.seibel.lod.api.items.enums.rendering.*;
import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.api.items.interfaces.config.client.IDhApiGraphicsFogConfig;
import com.seibel.lod.api.items.objects.config.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Graphics.FogQuality;

/**
 * Distant Horizons' fog configuration. <br><br>
 *
 * Note: unless an option explicitly states that it modifies
 * Minecraft's vanilla rendering (like DisableVanillaFog)
 * these settings will only affect Distant horizons' fog.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public class DhApiGraphicsFogConfig implements IDhApiGraphicsFogConfig
{
	
	//====================//
	// basic fog settings //
	//====================//
	
	@Override
	public IDhApiConfig<EFogDistance> getFogDistanceConfig()
	{ return new DhApiConfig<>(FogQuality.fogDistance); }
	
	@Override
	public IDhApiConfig<EFogDrawMode> getFogRenderConfig()
	{ return new DhApiConfig<>(FogQuality.fogDrawMode); }
	
	@Override
	public IDhApiConfig<EFogColorMode> getFogColorConfig()
	{ return new DhApiConfig<>(FogQuality.fogColorMode); }
	
	@Override
	public IDhApiConfig<Boolean> getDisableVanillaFogConfig()
	{ return new DhApiConfig<>(FogQuality.disableVanillaFog); }
	
	
	//=======================//
	// advanced fog settings //
	//=======================//
	
	@Override
	public IDhApiConfig<Double> getFogStartDistanceConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogStart); }
	
	@Override
	public IDhApiConfig<Double> getFogEndDistanceConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogEnd); }
	
	@Override
	public IDhApiConfig<Double> getFogMinThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogMin); }
	
	@Override
	public IDhApiConfig<Double> getFogMaxThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogMax); }
	
	@Override
	public IDhApiConfig<EFogFalloff> getFogFalloffConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogType); }
	
	@Override
	public IDhApiConfig<Double> getFogDensityConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.farFogDensity); }
	
	
	//=====================//
	// height fog settings //
	//=====================//
	
	@Override
	public IDhApiConfig<EHeightFogMixMode> getHeightFogMixModeConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMixMode); }
	
	@Override
	public IDhApiConfig<EHeightFogMode> getHeightFogModeConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMode); }
	
	@Override
	public IDhApiConfig<Double> getHeightFogBaseHeightConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogHeight); }
	
	@Override
	public IDhApiConfig<Double> getHeightFogStartingHeightPercentConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogStart); }
	
	@Override
	public IDhApiConfig<Double> getHeightFogEndingHeightPercentConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogEnd); }
	
	@Override
	public IDhApiConfig<Double> getHeightFogMinThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMin); }
	
	@Override
	public IDhApiConfig<Double> getHeightFogMaxThicknessConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogMax); }
	
	@Override
	public IDhApiConfig<EFogFalloff> getHeightFogFalloffConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogType); }
	
	@Override
	public IDhApiConfig<Double> getHeightFogDensityConfig()
	{ return new DhApiConfig<>(FogQuality.AdvancedFog.HeightFog.heightFogDensity); }
	
}
