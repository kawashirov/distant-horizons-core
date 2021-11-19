/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.wrapperAdapters.handlers;

import com.seibel.lod.core.enums.rendering.FogQuality;
import com.seibel.lod.core.objects.math.Mat4f;

/**
 * A singleton used to get variables from methods
 * where they are private or potentially absent. 
 * Specifically the fog setting in Optifine or the
 * presence/absence of other mods.
 * 
 * @author James Seibel
 * @version 11-18-2021
 */
public interface IReflectionHandler
{
	/**
	 * Get what type of fog optifine is currently set to render.
	 * @return the fog quality
	 */
	public FogQuality getFogQuality();
	
	/** Detect if Vivecraft is present. Attempts to find the "VRRenderer" class. */
	public boolean vivecraftPresent();
	
	/**
	 * Modifies the projection matrix's clip planes.
	 * The projection matrix must be in column-major format.
	 * 
	 * @param projectionMatrix The projection matrix to be modified.
	 * @param newNearClipPlane the new near clip plane value.
	 * @param newFarClipPlane the new far clip plane value.
	 * @return The modified matrix.
	 */
	public Mat4f ModifyProjectionClipPlanes(Mat4f projectionMatrix, float newNearClipPlane, float newFarClipPlane);
}
