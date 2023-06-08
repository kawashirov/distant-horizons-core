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

package com.seibel.lod.core.api.external.methods.config.client;

import com.seibel.lod.api.interfaces.config.IDhApiConfigValue;
import com.seibel.lod.api.interfaces.config.client.IDhApiThreadingConfig;
import com.seibel.lod.api.objects.config.DhApiConfigValue;
import com.seibel.lod.core.config.Config.Client.Advanced.MultiThreading;

/**
 * Distant Horizons' threading configuration.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public class DhApiThreadingConfig implements IDhApiThreadingConfig
{
	public static DhApiThreadingConfig INSTANCE = new DhApiThreadingConfig();
	
	private DhApiThreadingConfig() { }
	
	
	
	@Override
	public IDhApiConfigValue<Integer> getWorldGeneratorThread()
	{ return new DhApiConfigValue<>(MultiThreading.numberOfWorldGenerationThreads); }
	
	@Override
	public IDhApiConfigValue<Integer> getBufferBuilderThread()
	{ return new DhApiConfigValue<>(MultiThreading.numberOfBufferBuilderThreads); }
	
	@Override
	public IDhApiConfigValue<Integer> getFileHandlerThread()
	{ return new DhApiConfigValue<>(MultiThreading.numberOfFileHandlerThreads); }
	
	@Override
	public IDhApiConfigValue<Integer> getDataConverterThread()
	{ return new DhApiConfigValue<>(MultiThreading.numberOfDataConverterThreads); }
	
}
