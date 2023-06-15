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

import com.seibel.lod.api.interfaces.config.IDhApiConfigValue;
import com.seibel.lod.api.interfaces.config.client.IDhApiDebuggingConfig;
import com.seibel.lod.api.objects.config.DhApiConfigValue;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;
import com.seibel.lod.api.enums.rendering.EDebugRendering;

public class DhApiDebuggingConfig implements IDhApiDebuggingConfig
{
	public static DhApiDebuggingConfig INSTANCE = new DhApiDebuggingConfig();
	
	private DhApiDebuggingConfig() { }
	
	
	
	public IDhApiConfigValue<EDebugRendering> debugRendering()
	{ return new DhApiConfigValue<EDebugRendering, EDebugRendering>(Debugging.debugRendering); }
	
	public IDhApiConfigValue<Boolean> debugKeybindings()
	{ return new DhApiConfigValue<Boolean, Boolean>(Debugging.enableDebugKeybindings); }
	
	public IDhApiConfigValue<Boolean> renderWireframe()
	{ return new DhApiConfigValue<Boolean, Boolean>(Debugging.renderWireframe); }
	
	public IDhApiConfigValue<Boolean> lodOnlyMode()
	{ return new DhApiConfigValue<Boolean, Boolean>(Debugging.lodOnlyMode); }
	
	public IDhApiConfigValue<Boolean> debugWireframeRendering()
	{ return new DhApiConfigValue<Boolean, Boolean>(Debugging.debugWireframeRendering); }
	
}
