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

import com.seibel.lod.api.enums.rendering.*;
import com.seibel.lod.api.interfaces.config.IDhApiConfigValue;
import com.seibel.lod.api.interfaces.config.client.IDhApiFarFogConfig;
import com.seibel.lod.api.interfaces.config.client.IDhApiFogConfig;
import com.seibel.lod.api.objects.config.DhApiConfigValue;
import com.seibel.lod.core.config.Config.Client.Advanced.Graphics.Fog;

public class DhApiFarFogConfig implements IDhApiFarFogConfig
{
	public static DhApiFarFogConfig INSTANCE = new DhApiFarFogConfig();
	
	private DhApiFarFogConfig() { }
	
	
	
	@Override
	public IDhApiConfigValue<Double> farFogStartDistance()
	{ return new DhApiConfigValue<Double, Double>(Fog.AdvancedFog.farFogStart); }
	
	@Override
	public IDhApiConfigValue<Double> farFogEndDistance()
	{ return new DhApiConfigValue<Double, Double>(Fog.AdvancedFog.farFogEnd); }
	
	@Override
	public IDhApiConfigValue<Double> farFogMinThickness()
	{ return new DhApiConfigValue<Double, Double>(Fog.AdvancedFog.farFogMin); }
	
	@Override
	public IDhApiConfigValue<Double> farFogMaxThickness()
	{ return new DhApiConfigValue<Double, Double>(Fog.AdvancedFog.farFogMax); }
	
	@Override
	public IDhApiConfigValue<EFogFalloff> farFogFalloff()
	{ return new DhApiConfigValue<EFogFalloff, EFogFalloff>(Fog.AdvancedFog.farFogFalloff); }
	
	@Override
	public IDhApiConfigValue<Double> farFogDensity()
	{ return new DhApiConfigValue<Double, Double>(Fog.AdvancedFog.farFogDensity); }
	
}
