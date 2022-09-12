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

package com.seibel.lod.core;

import com.seibel.lod.core.enums.rendering.EFogDrawMode;
import com.seibel.lod.core.dependencyInjection.IBindable;

/**
 * A singleton used to get variables from methods
 * where they are private or potentially absent. 
 * Specifically the fog setting used by Optifine or the
 * presence/absence of other mods.
 * <p>
 * This interface doesn't necessarily have to exist, but
 * it makes using the singleton handler more uniform (always
 * passing in interfaces), and it may be needed in the future if
 * we find that reflection handlers need to be different for
 * different MC versions.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public interface IReflectionHandler extends IBindable
{
	/** @return Whether Optifine is set to render fog or not. */
	EFogDrawMode getFogDrawMode();
	
	/** @return if Vivecraft is present. Attempts to find the "VRRenderer" class. */
	boolean vivecraftPresent();
	
	/** @return if Sodium (or a sodium like) mod is present. Attempts to find the "SodiumWorldRenderer" class. */
	boolean sodiumPresent();

	boolean optifinePresent();
}
