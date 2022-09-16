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

package com.seibel.lod.core.api.external.coreImplementations.methods.config.both;

import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.api.items.interfaces.config.both.IDhApiWorldGenerationConfig;
import com.seibel.lod.api.items.objects.config.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.WorldGenerator;
import com.seibel.lod.api.items.enums.config.EBlocksToAvoid;
import com.seibel.lod.api.items.enums.config.EDistanceGenerationMode;
import com.seibel.lod.api.items.enums.config.EGenerationPriority;
import com.seibel.lod.api.items.enums.config.ELightGenerationMode;

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
	
	@Override
	public IDhApiConfig<Boolean> getEnableDistantWorldGenerationConfig()
	{ return new DhApiConfig<>(WorldGenerator.enableDistantGeneration); }
	
	@Override
	public IDhApiConfig<EDistanceGenerationMode> getDistantGeneratorDetailLevelConfig()
	{ return new DhApiConfig<>(WorldGenerator.distanceGenerationMode); }
	
	@Override
	public IDhApiConfig<ELightGenerationMode> getLightingModeConfig()
	{ return new DhApiConfig<>(WorldGenerator.lightGenerationMode); }
	
	@Override
	public IDhApiConfig<EGenerationPriority> getGenerationPriorityConfig()
	{ return new DhApiConfig<>(WorldGenerator.generationPriority); }
	
	@Deprecated
	@Override
	public IDhApiConfig<EBlocksToAvoid> getBlocksToAvoidConfig()
	{ return new DhApiConfig<>(WorldGenerator.blocksToAvoid); }
	
	@Deprecated
	@Override
	public IDhApiConfig<Boolean> getTintWithAvoidedBlocksConfig()
	{ return new DhApiConfig<>(WorldGenerator.tintWithAvoidedBlocks); }
	
	
}
