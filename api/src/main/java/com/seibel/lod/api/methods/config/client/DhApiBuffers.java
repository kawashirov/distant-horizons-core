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

package com.seibel.lod.api.methods.config.client;

import com.seibel.lod.api.items.enums.config.EDhApiGpuUploadMethod;
import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.core.api.implementation.objects.GenericEnumConverter;
import com.seibel.lod.api.methods.config.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Advanced.Buffers;
import com.seibel.lod.core.enums.config.EGpuUploadMethod;

/**
 * Distant Horizons' OpenGL buffer configuration.
 *
 * @author James Seibel
 * @version 2022-7-5
 */
public class DhApiBuffers
{
	
	/** Defines how geometry data is uploaded to the GPU. */
	public static IDhApiConfig<EDhApiGpuUploadMethod> getGpuUploadMethodConfig()
	{ return new DhApiConfig<EGpuUploadMethod, EDhApiGpuUploadMethod>(Buffers.gpuUploadMethod, new GenericEnumConverter<>(EGpuUploadMethod.class, EDhApiGpuUploadMethod.class)); }
	
	/**
	 * Defines how long we should wait after uploading one
	 * Megabyte of geometry data to the GPU before uploading
	 * the next Megabyte of data. <br>
	 * This can be set to a non-zero number to reduce stuttering caused by
	 * uploading buffers to the GPU.
	 */
	public static IDhApiConfig<Integer> getBufferUploadTimeoutPerMegabyteInMillisecondsConfig()
	{ return new DhApiConfig<Integer, Integer>(Buffers.gpuUploadPerMegabyteInMilliseconds); }
	
}
