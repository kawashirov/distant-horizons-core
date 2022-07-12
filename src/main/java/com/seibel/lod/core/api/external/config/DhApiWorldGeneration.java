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

package com.seibel.lod.core.api.external.config;

import com.seibel.lod.core.api.external.config.objects.enums.EDhApiDistanceGenerationMode;
import com.seibel.lod.core.api.external.config.objects.enums.EDhApiBlocksToAvoid;
import com.seibel.lod.core.api.external.config.objects.enums.EDhApiLightGenerationMode;
import com.seibel.lod.core.api.external.config.objects.wrapperInterfaces.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.core.api.implementation.wrappers.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.WorldGenerator;
import com.seibel.lod.core.enums.config.EBlocksToAvoid;
import com.seibel.lod.core.enums.config.EDistanceGenerationMode;
import com.seibel.lod.core.enums.config.EGenerationPriority;
import com.seibel.lod.core.enums.config.ELightGenerationMode;

/**
 * Distant Horizons' world generation configuration. <br><br>
 *
 * Note: Fake chunks are NOT saved in Minecraft's vanilla save system.
 *
 * @author James Seibel
 * @version 2022-7-11
 */
public class DhApiWorldGeneration
{
	
	/**
	 * Defines whether fake chunks will be generated
	 * outside Minecraft's vanilla render distance.
	 */
	public static IDhApiConfig<Boolean> getEnableDistantWorldGenerationConfig()
	{ return new DhApiConfig<Boolean, Boolean>(WorldGenerator.enableDistantGeneration); }
	
	/** Defines to what level fake chunks will be generated. */
	public static IDhApiConfig<EDhApiDistanceGenerationMode> getDistantGeneratorDetailLevelConfig()
	{ return new DhApiConfig<EDistanceGenerationMode, EDhApiDistanceGenerationMode>(WorldGenerator.distanceGenerationMode, new GenericEnumConverter<>(EDistanceGenerationMode.class, EDhApiDistanceGenerationMode.class)); }
	
	/** Defines how generated fake chunks will be lit. */
	public static IDhApiConfig<EDhApiLightGenerationMode> getLightingModeConfig()
	{ return new DhApiConfig<ELightGenerationMode, EDhApiLightGenerationMode>(WorldGenerator.lightGenerationMode, new GenericEnumConverter<>(ELightGenerationMode.class, EDhApiLightGenerationMode.class)); }
	
	/** Defines the order in which fake chunks will be generated. */
	public static IDhApiConfig<EDhApiLightGenerationMode> getGenerationPriorityConfig()
	{ return new DhApiConfig<EGenerationPriority, EDhApiLightGenerationMode>(WorldGenerator.generationPriority, new GenericEnumConverter<>(EGenerationPriority.class, EDhApiLightGenerationMode.class)); }
	
	/**
	 * Defines what blocks will be ignored when generating LODs.
	 *
	 * TODO if this isn't deprecated before 1.7 it should probably be moved to the graphics tab
	 * @deprecated this method won't be needed once we transition to an ID based save system <br>
	 * 				(vs the color based system we have currently)
	 */
	@Deprecated
	public static IDhApiConfig<EDhApiBlocksToAvoid> getBlocksToAvoidConfig()
	{ return new DhApiConfig<EBlocksToAvoid, EDhApiBlocksToAvoid>(WorldGenerator.blocksToAvoid, new GenericEnumConverter<>(EBlocksToAvoid.class, EDhApiBlocksToAvoid.class)); }
	
	/**
	 * Defines if the color of avoided blocks will color the block below them. <Br>
	 * (IE: if flowers are avoided should they color the grass below them?)
	 *
	 * TODO if this isn't deprecated before 1.7 it should probably be moved to the graphics tab
	 * @deprecated this method won't be needed once we transition to an ID based save system <br>
	 * 				(vs the color based system we have currently)
	 */
	@Deprecated
	public static IDhApiConfig<Boolean> getTintWithAvoidedBlocksConfig()
	{ return new DhApiConfig<Boolean, Boolean>(WorldGenerator.tintWithAvoidedBlocks); }
	
	
}
