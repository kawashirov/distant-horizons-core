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

package com.seibel.lod.core.api.external.methods.config.both;

import com.seibel.lod.api.interfaces.config.IDhApiConfigValue;
import com.seibel.lod.api.interfaces.config.both.IDhApiWorldGenerationConfig;
import com.seibel.lod.api.objects.config.DhApiConfigValue;
import com.seibel.lod.core.config.Config.Client.Advanced.WorldGenerator;
import com.seibel.lod.api.enums.config.EBlocksToAvoid;
import com.seibel.lod.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.lod.api.enums.config.EGenerationPriority;
import com.seibel.lod.api.enums.config.ELightGenerationMode;

/**
 * Distant Horizons' world generation configuration. <br><br>
 *
 * Note: Fake chunks are NOT saved in Minecraft's vanilla save system.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public class DhApiWorldGenerationConfig implements IDhApiWorldGenerationConfig
{
	public static DhApiWorldGenerationConfig INSTANCE = new DhApiWorldGenerationConfig(); 
	
	private DhApiWorldGenerationConfig() { }
	
	
	
	@Override
	public IDhApiConfigValue<Boolean> getEnableDistantWorldGeneration()
	{ return new DhApiConfigValue<>(WorldGenerator.enableDistantGeneration); }
	
	@Override
	public IDhApiConfigValue<EDhApiDistantGeneratorMode> getDistantGeneratorMode()
	{ return new DhApiConfigValue<>(WorldGenerator.distantGeneratorMode); }
	
	@Deprecated
	@Override
	public IDhApiConfigValue<EBlocksToAvoid> getBlocksToAvoid()
	{ return new DhApiConfigValue<>(WorldGenerator.blocksToAvoid); }
	
	@Deprecated
	@Override
	public IDhApiConfigValue<Boolean> getTintWithAvoidedBlocks()
	{ return new DhApiConfigValue<>(WorldGenerator.tintWithAvoidedBlocks); }
	
	
}
