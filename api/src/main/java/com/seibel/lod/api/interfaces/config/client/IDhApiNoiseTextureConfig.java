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

package com.seibel.lod.api.interfaces.config.client;

import com.seibel.lod.api.interfaces.config.IDhApiConfigGroup;
import com.seibel.lod.api.interfaces.config.IDhApiConfigValue;

/**
 * Distant Horizons' noise texture configuration. <br><br>
 *
 * @author James Seibel
 * @version 2022-6-14
 */
public interface IDhApiNoiseTextureConfig extends IDhApiConfigGroup
{
	/**
	 * TODO
	 */
	IDhApiConfigValue<Boolean> noiseEnabled();
	
	/**
	 * TODO
	 */
	IDhApiConfigValue<Integer> noiseSteps();
	
	/**
	 * TODO
	 */
	IDhApiConfigValue<Double> noiseIntensity();
	
	/**
	 * TODO
	 */
	IDhApiConfigValue<Double> noiseDropoff();
	
}
