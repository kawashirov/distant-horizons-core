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

import com.seibel.lod.api.items.enums.rendering.*;
import com.seibel.lod.api.items.interfaces.config.IDhApiConfigValue;
import com.seibel.lod.api.items.interfaces.config.client.IDhApiGraphicsFogConfig;
import com.seibel.lod.api.items.objects.config.DhApiConfigValue;
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
	public static DhApiGraphicsFogConfig INSTANCE = new DhApiGraphicsFogConfig();
	
	private DhApiGraphicsFogConfig() { }
	
	
	
	//====================//
	// basic fog settings //
	//====================//
	
	@Override
	public IDhApiConfigValue<EFogDistance> getFogDistance()
	{ return new DhApiConfigValue<>(FogQuality.fogDistance); }
	
	@Override
	public IDhApiConfigValue<EFogDrawMode> getFogRender()
	{ return new DhApiConfigValue<>(FogQuality.fogDrawMode); }
	
	@Override
	public IDhApiConfigValue<EFogColorMode> getFogColor()
	{ return new DhApiConfigValue<>(FogQuality.fogColorMode); }
	
	@Override
	public IDhApiConfigValue<Boolean> getDisableVanillaFog()
	{ return new DhApiConfigValue<>(FogQuality.disableVanillaFog); }
	
	
	//=======================//
	// advanced fog settings //
	//=======================//
	
	@Override
	public IDhApiConfigValue<Double> getFogStartDistance()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.farFogStart); }
	
	@Override
	public IDhApiConfigValue<Double> getFogEndDistance()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.farFogEnd); }
	
	@Override
	public IDhApiConfigValue<Double> getFogMinThickness()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.farFogMin); }
	
	@Override
	public IDhApiConfigValue<Double> getFogMaxThickness()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.farFogMax); }
	
	@Override
	public IDhApiConfigValue<EFogFalloff> getFogFalloff()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.farFogType); }
	
	@Override
	public IDhApiConfigValue<Double> getFogDensity()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.farFogDensity); }
	
	
	//=====================//
	// height fog settings //
	//=====================//
	
	@Override
	public IDhApiConfigValue<EHeightFogMixMode> getHeightFogMixMode()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.HeightFog.heightFogMixMode); }
	
	@Override
	public IDhApiConfigValue<EHeightFogMode> getHeightFogMode()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.HeightFog.heightFogMode); }
	
	@Override
	public IDhApiConfigValue<Double> getHeightFogBaseHeight()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.HeightFog.heightFogHeight); }
	
	@Override
	public IDhApiConfigValue<Double> getHeightFogStartingHeightPercent()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.HeightFog.heightFogStart); }
	
	@Override
	public IDhApiConfigValue<Double> getHeightFogEndingHeightPercent()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.HeightFog.heightFogEnd); }
	
	@Override
	public IDhApiConfigValue<Double> getHeightFogMinThickness()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.HeightFog.heightFogMin); }
	
	@Override
	public IDhApiConfigValue<Double> getHeightFogMaxThickness()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.HeightFog.heightFogMax); }
	
	@Override
	public IDhApiConfigValue<EFogFalloff> getHeightFogFalloff()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.HeightFog.heightFogType); }
	
	@Override
	public IDhApiConfigValue<Double> getHeightFogDensity()
	{ return new DhApiConfigValue<>(FogQuality.AdvancedFog.HeightFog.heightFogDensity); }
	
}
