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

import com.seibel.lod.api.items.enums.rendering.EDebugMode;
import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;

/**
 * Distant Horizons' debug configuration.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public interface IDhApiDebuggingConfig
{
	/** Can be used to debug the standard fake chunk rendering. */
	IDhApiConfig<EDebugMode> getDebugRenderModeConfig();
	
	/** If enabled debug keybindings can be used. */
	IDhApiConfig<Boolean> getEnableDebugKeybindingsConfig();
	
	
}
