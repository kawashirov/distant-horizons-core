/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.core.api.external.methods.config.client;

import com.seibel.distanthorizons.api.enums.rendering.EFogColorMode;
import com.seibel.distanthorizons.api.enums.rendering.EFogDistance;
import com.seibel.distanthorizons.api.enums.rendering.EFogDrawMode;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.api.interfaces.config.client.IDhApiFarFogConfig;
import com.seibel.distanthorizons.api.interfaces.config.client.IDhApiFogConfig;
import com.seibel.distanthorizons.api.interfaces.config.client.IDhApiHeightFogConfig;
import com.seibel.distanthorizons.api.objects.config.DhApiConfigValue;
import com.seibel.distanthorizons.core.config.Config;

public class DhApiFogConfig implements IDhApiFogConfig
{
	public static DhApiFogConfig INSTANCE = new DhApiFogConfig();
	
	private DhApiFogConfig() { }
	
	
	
	//===============//
	// inner configs //
	//===============//
	
	@Override
	public IDhApiFarFogConfig farFog() { return DhApiFarFogConfig.INSTANCE; }
	@Override
	public IDhApiHeightFogConfig heightFog() { return DhApiHeightFogConfig.INSTANCE; }
	
	
	
	//====================//
	// basic fog settings //
	//====================//
	
	@Override
	public IDhApiConfigValue<EFogDistance> distance()
	{ return new DhApiConfigValue<>(Config.Client.Advanced.Graphics.Fog.distance); }
	
	@Override
	public IDhApiConfigValue<EFogDrawMode> drawMode()
	{ return new DhApiConfigValue<>(Config.Client.Advanced.Graphics.Fog.drawMode); }
	
	@Override
	public IDhApiConfigValue<EFogColorMode> color()
	{ return new DhApiConfigValue<>(Config.Client.Advanced.Graphics.Fog.colorMode); }
	
	@Override
	public IDhApiConfigValue<Boolean> disableVanillaFog()
	{ return new DhApiConfigValue<>(Config.Client.Advanced.Graphics.Fog.disableVanillaFog); }
	
}
