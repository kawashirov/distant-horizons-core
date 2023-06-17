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

package com.seibel.distanthorizons.api.interfaces.config.both;

import com.seibel.distanthorizons.api.enums.config.ELightGenerationMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigGroup;

/**
 * Distant Horizons' world generation configuration. <br><br>
 *
 * Note: Fake chunks are NOT saved in Minecraft's vanilla save system.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public interface IDhApiWorldGenerationConfig extends IDhApiConfigGroup
{
	
	/**
	 * Defines whether fake chunks will be generated
	 * outside Minecraft's vanilla render distance.
	 */
	IDhApiConfigValue<Boolean> enableDistantWorldGeneration();
	
	/** Defines to what level fake chunks will be generated. */
	IDhApiConfigValue<EDhApiDistantGeneratorMode> distantGeneratorMode();
	
	/** 
	 * TODO
	 */
	IDhApiConfigValue<ELightGenerationMode> lightingEngine();
	
}
