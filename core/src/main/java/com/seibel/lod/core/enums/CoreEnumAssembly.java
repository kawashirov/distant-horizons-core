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

package com.seibel.lod.core.enums;

import com.seibel.lod.core.enums.config.CoreConfigEnumAssembly;
import com.seibel.lod.core.enums.rendering.CoreRenderingEnumAssembly;
import com.seibel.lod.core.enums.worldGeneration.CoreWorldGenerationEnumAssembly;

/**
 * Assembly classes are used to reference the package they are in.
 *
 * @author James Seibel
 * @version 2022-9-6
 */
public class CoreEnumAssembly
{
	// These variables are added in order to load each package into the JVM's class loader.
	// This is done so they can be found via reflection.
	private static final CoreRenderingEnumAssembly renderingAssembly = new CoreRenderingEnumAssembly();
	private static final CoreConfigEnumAssembly configAssembly = new CoreConfigEnumAssembly();
	private static final CoreWorldGenerationEnumAssembly worldGenAssembly = new CoreWorldGenerationEnumAssembly();
	
	/** All enums should have this prefix */
	public static final String ENUM_PREFIX = "E";
}
