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

package com.seibel.lod.api.config.client;

import com.seibel.lod.core.api.external.items.enums.config.EDhApiServerFolderNameMode;
import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Multiplayer;
import com.seibel.lod.core.enums.config.EServerFolderNameMode;

/**
 * Distant Horizons' client-side multiplayer configuration.
 *
 * @author James Seibel
 * @version 2022-7-5
 */
public class DhApiMultiplayer
{
	
	/**
	 * Defines how multiplayer server folders are named. <br>
	 * Note: Changing this while connected to a multiplayer world will cause undefined behavior!
	 */
	public static IDhApiConfig<EDhApiServerFolderNameMode> getFolderSavingModeConfig()
	{ return new DhApiConfig<EServerFolderNameMode, EDhApiServerFolderNameMode>(Multiplayer.serverFolderNameMode, new GenericEnumConverter<>(EServerFolderNameMode.class, EDhApiServerFolderNameMode.class)); }
	
	/**
	 * Defines the necessary similarity (as a percent) that two potential levels
	 * need in order to be considered the same. <br> <br>
	 *
	 * Setting this to zero causes every level of a specific dimension type to be consider
	 * the same level. <br>
	 * Setting this to a non-zero value allows for usage in servers that user Multiverse
	 * or similar mods.
	 */
	public static IDhApiConfig<Double> getMultiverseSimilarityRequirementConfig()
	{ return new DhApiConfig<Double, Double>(Multiplayer.multiDimensionRequiredSimilarity); }
	
	
}
