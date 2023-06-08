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

package com.seibel.lod.api.enums.config.quickOptions;

/**
 * CUSTOM, <br><br>
 * 
 * MINIMAL_IMPACT, <br>
 * LOW_IMPACT, <br>
 * BALANCED, <br>
 * FAST_LOADING, <br>
 * I_PAID_FOR_THE_WHOLE_CPU, <br>
 */
public enum EQuickThread
{
	// Reminder:
	// when adding items up the API minor version
	// when removing items up the API major version
	
	CUSTOM,
	
	MINIMAL_IMPACT,
	LOW_IMPACT,
	BALANCED,
	FAST_LOADING,
	I_PAID_FOR_THE_WHOLE_CPU;
	
}