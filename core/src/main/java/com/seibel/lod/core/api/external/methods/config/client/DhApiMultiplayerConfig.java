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

import com.seibel.lod.api.items.interfaces.config.IDhApiConfigValue;
import com.seibel.lod.api.items.interfaces.config.client.IDhApiMultiplayerConfig;
import com.seibel.lod.api.items.objects.config.DhApiConfigValue;
import com.seibel.lod.core.config.Config.Client.Multiplayer;
import com.seibel.lod.api.items.enums.config.EServerFolderNameMode;

/**
 * Distant Horizons' client-side multiplayer configuration.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public class DhApiMultiplayerConfig implements IDhApiMultiplayerConfig
{
	public static DhApiMultiplayerConfig INSTANCE = new DhApiMultiplayerConfig();
	
	private DhApiMultiplayerConfig() { }
	
	
	
	public IDhApiConfigValue<EServerFolderNameMode> getFolderSavingMode()
	{ return new DhApiConfigValue<>(Multiplayer.serverFolderNameMode); }
	
	public IDhApiConfigValue<Double> getMultiverseSimilarityRequirement()
	{ return new DhApiConfigValue<>(Multiplayer.multiDimensionRequiredSimilarity); }
	
}
