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

package com.seibel.distanthorizons.api.interfaces.config.client;

import com.seibel.distanthorizons.api.enums.rendering.EDebugRendering;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigGroup;

/**
 * Distant Horizons' debug configuration.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public interface IDhApiDebuggingConfig extends IDhApiConfigGroup
{
	/** Can be used to debug the standard fake chunk rendering. */
	IDhApiConfigValue<EDebugRendering> debugRendering();
	
	/** If enabled debug keybindings can be used. */
	IDhApiConfigValue<Boolean> debugKeybindings();
	
	/** TODO */
	IDhApiConfigValue<Boolean> renderWireframe();
	
	/** TODO */
	IDhApiConfigValue<Boolean> lodOnlyMode();
	
	/** TODO */
	IDhApiConfigValue<Boolean> debugWireframeRendering();
	
	
}
