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

package com.seibel.lod.core.api.external.coreImplementations.methods.config.client;

import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.api.items.interfaces.config.client.IDhApiBuffersConfig;
import com.seibel.lod.api.items.objects.config.DhApiConfig;
import com.seibel.lod.core.config.Config.Client.Advanced.Buffers;
import com.seibel.lod.api.items.enums.config.EGpuUploadMethod;

/**
 * Distant Horizons' OpenGL buffer configuration.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public class DhApiBuffersConfig implements IDhApiBuffersConfig
{
	
	public IDhApiConfig<EGpuUploadMethod> getGpuUploadMethodConfig()
	{ return new DhApiConfig<>(Buffers.gpuUploadMethod); }
	
	public IDhApiConfig<Integer> getBufferUploadTimeoutPerMegabyteInMillisecondsConfig()
	{ return new DhApiConfig<>(Buffers.gpuUploadPerMegabyteInMilliseconds); }
	
}
