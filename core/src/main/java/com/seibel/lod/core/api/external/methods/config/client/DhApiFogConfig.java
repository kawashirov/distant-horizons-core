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
import com.seibel.lod.api.interfaces.config.client.IDhApiHeightFogConfig;
import com.seibel.lod.api.objects.config.DhApiConfigValue;
import com.seibel.lod.core.config.Config.Client.Advanced.Graphics.Fog;

public class DhApiFogConfig implements IDhApiFogConfig
{
	public static DhApiFogConfig INSTANCE = new DhApiFogConfig();
	
	private DhApiFogConfig() { }
	
	
	
	//===============//
	// inner configs //
	//===============//
	
	public IDhApiFarFogConfig farFog() { return DhApiFarFogConfig.INSTANCE; }
	public IDhApiHeightFogConfig heightFog() { return DhApiHeightFogConfig.INSTANCE; }
	
	
	
	//====================//
	// basic fog settings //
	//====================//
	
	@Override
	public IDhApiConfigValue<EFogDistance> distance()
	{ return new DhApiConfigValue<>(Fog.distance); }
	
	@Override
	public IDhApiConfigValue<EFogDrawMode> drawMode()
	{ return new DhApiConfigValue<>(Fog.drawMode); }
	
	@Override
	public IDhApiConfigValue<EFogColorMode> color()
	{ return new DhApiConfigValue<>(Fog.colorMode); }
	
	@Override
	public IDhApiConfigValue<Boolean> disableVanillaFog()
	{ return new DhApiConfigValue<>(Fog.disableVanillaFog); }
	
}
