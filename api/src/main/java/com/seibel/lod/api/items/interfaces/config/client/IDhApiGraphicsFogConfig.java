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

import com.seibel.lod.api.items.enums.rendering.*;
import com.seibel.lod.api.items.interfaces.config.IDhApiConfigValue;
import com.seibel.lod.api.items.interfaces.config.IDhApiConfigGroup;

/**
 * Distant Horizons' fog configuration. <br><br>
 *
 * Note: unless an option explicitly states that it modifies
 * Minecraft's vanilla rendering (like DisableVanillaFog)
 * these settings will only affect Distant horizons' fog.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public interface IDhApiGraphicsFogConfig extends IDhApiConfigGroup
{
	
	//====================//
	// basic fog settings //
	//====================//
	
	/** Defines at what distance fog is rendered on fake chunks. */
	IDhApiConfigValue<EFogDistance> getFogDistance();
	
	/** Should be used to enable/disable fog rendering. */
	IDhApiConfigValue<EFogDrawMode> getFogRender();
	
	/** Can be used to enable support with mods that change vanilla MC's fog color. */
	IDhApiConfigValue<EFogColorMode> getFogColor();
	
	/**
	 * If enabled attempts to disable vanilla MC's fog on real chunks. <br>
	 * May not play nice with other fog editing mods.
	 */
	IDhApiConfigValue<Boolean> getDisableVanillaFog();
	
	
	//=======================//
	// advanced fog settings //
	//=======================//
	
	/**
	 * Defines where the fog starts as a percent of the
	 * fake chunks render distance radius. <br>
	 * Can be greater than the fog end distance to invert the fog direction. <br> <br>
	 *
	 * 0.0 = fog starts at the camera <br>
	 * 1.0 = fog starts at the edge of the fake chunk render distance <br>
	 */
	IDhApiConfigValue<Double> getFogStartDistance();
	
	/**
	 * Defines where the fog ends as a percent of the radius
	 * of the fake chunks render distance. <br>
	 * Can be less than the fog start distance to invert the fog direction. <br> <br>
	 *
	 * 0.0 = fog ends at the camera <br>
	 * 1.0 = fog ends at the edge of the fake chunk render distance <br>
	 */
	IDhApiConfigValue<Double> getFogEndDistance();
	
	/** Defines how opaque the fog is at its thinnest point. */
	IDhApiConfigValue<Double> getFogMinThickness();
	
	/** Defines how opaque the fog is at its thickest point. */
	IDhApiConfigValue<Double> getFogMaxThickness();
	
	/** Defines how the fog changes in thickness. */
	IDhApiConfigValue<EFogFalloff> getFogFalloff();
	
	/** Defines the fog density. */
	IDhApiConfigValue<Double> getFogDensity();
	
	
	//=====================//
	// height fog settings //
	//=====================//
	
	/** Defines how the height fog mixes. */
	IDhApiConfigValue<EHeightFogMixMode> getHeightFogMixMode();
	
	/** Defines how the height fog is drawn relative to the camera or world. */
	IDhApiConfigValue<EHeightFogMode> getHeightFogMode();
	
	/**
	 * Defines the height fog's base height if {@link IDhApiGraphicsFogConfig#getHeightFogMode()}
	 * is set to use a specific height.
	 */
	IDhApiConfigValue<Double> getHeightFogBaseHeight();
	
	/** Defines the height fog's starting height as a percent of the world height. */
	IDhApiConfigValue<Double> getHeightFogStartingHeightPercent();
	
	/** Defines the height fog's ending height as a percent of the world height. */
	IDhApiConfigValue<Double> getHeightFogEndingHeightPercent();
	
	/** Defines how opaque the height fog is at its thinnest point. */
	IDhApiConfigValue<Double> getHeightFogMinThickness();
	
	/** Defines how opaque the height fog is at its thickest point. */
	IDhApiConfigValue<Double> getHeightFogMaxThickness();
	
	/** Defines how the height fog changes in thickness. */
	IDhApiConfigValue<EFogFalloff> getHeightFogFalloff();
	
	/** Defines the height fog's density. */
	IDhApiConfigValue<Double> getHeightFogDensity();
	
}
