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

package com.seibel.lod.core.api.external.config.objects.enums;

/**
 * AUTO, 					<br>
 * BUFFER_STORAGE, 			<br>
 * SUB_DATA, 				<br>
 * BUFFER_MAPPING, 			<br>
 * DATA						<br>
 *
 * @author Leetom
 * @author James Seibel
 * @version 2022-7-2
 */
public enum EDhApiGpuUploadMethod
{
	/** Picks the best option based on the GPU the user has. */
	AUTO,
	
	/**
	 * Default for NVIDIA if OpenGL 4.5 is supported. <br>
	 * Fast rendering, no stuttering.
	 */
	BUFFER_STORAGE,
	
	/**
	 * Backup option for NVIDIA. <br>
	 * Fast rendering but may stutter when uploading.
	 */
	SUB_DATA,
	
	/**
	 * Default option for AMD/Intel. <br>
	 * May end up storing buffers in System memory. <br>
	 * Fast rending if in GPU memory, slow if in system memory, <br>
	 * but won't stutter when uploading.
	 */
	BUFFER_MAPPING,
	
	/**
	 * Backup option for AMD/Intel. <br>
	 * Fast rendering but may stutter when uploading.
	 */
	DATA;
	
}