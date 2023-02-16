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

package com.seibel.lod.core.api.internal;

import com.seibel.lod.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.lod.api.methods.events.abstractEvents.DhApiLevelSaveEvent;
import com.seibel.lod.api.methods.events.abstractEvents.DhApiLevelUnloadEvent;
import com.seibel.lod.core.DependencyInjection.ApiEventInjector;
import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.world.DhClientServerWorld;
import com.seibel.lod.core.world.DhServerWorld;
import com.seibel.lod.core.world.IDhServerWorld;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

/**
 * This holds the methods that should be called by the host mod loader (Fabric,
 * Forge, etc.). Specifically server events.
 *
 * @author James Seibel
 * @version 2022-9-16
 */
public class ServerApi
{
	public static final ServerApi INSTANCE = new ServerApi();
	public static final boolean ENABLE_EVENT_LOGGING = true;
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private int lastWorldGenTickDelta = 0;
	
	
	
	private ServerApi() { }
	
	
	
	// =============//
	// tick events  //
	// =============//
	
	public void serverTickEvent()
	{
		if (SharedApi.currentWorld instanceof IDhServerWorld)
		{
			IDhServerWorld serverWorld = (IDhServerWorld) SharedApi.currentWorld;
			serverWorld.serverTick();
			this.lastWorldGenTickDelta--;
			if (this.lastWorldGenTickDelta <= 0)
			{
				serverWorld.doWorldGen();
				this.lastWorldGenTickDelta = 20;
			}
		}
	}
	public void serverLevelTickEvent(IServerLevelWrapper level)
	{
		//TODO
	}

	public void serverLoadEvent(boolean isDedicatedEnvironment)
	{
		if (ENABLE_EVENT_LOGGING)
		{
			LOGGER.info("Server World loading with (dedicated?:{})", isDedicatedEnvironment);
		}
		
		if (isDedicatedEnvironment)
		{
			SharedApi.currentWorld = new DhServerWorld();
		}
		else
		{
			SharedApi.currentWorld = new DhClientServerWorld();
		}
	}
	
	public void serverUnloadEvent()
	{
		if (ENABLE_EVENT_LOGGING)
		{
			LOGGER.info("Server World "+SharedApi.currentWorld+" unloading");
		}
		
		SharedApi.currentWorld.close();
		SharedApi.currentWorld = null;
	}
	
	public void serverLevelLoadEvent(IServerLevelWrapper level)
	{
		if (ENABLE_EVENT_LOGGING)
		{
			LOGGER.info("Server Level {} loading", level);
		}
		
		if (SharedApi.currentWorld != null)
		{
			SharedApi.currentWorld.getOrLoadLevel(level);
			ApiEventInjector.INSTANCE.fireAllEvents(DhApiLevelLoadEvent.class, new DhApiLevelLoadEvent.EventParam(level));
		}
	}
	public void serverLevelUnloadEvent(IServerLevelWrapper level)
	{
		if (ENABLE_EVENT_LOGGING)
		{
			LOGGER.info("Server Level {} unloading", level);
		}
		
		if (SharedApi.currentWorld != null)
		{
			SharedApi.currentWorld.unloadLevel(level);
			ApiEventInjector.INSTANCE.fireAllEvents(DhApiLevelUnloadEvent.class, new DhApiLevelUnloadEvent.EventParam(level));
		}
	}

	@Deprecated
	public void serverSaveEvent()
	{
		if (ENABLE_EVENT_LOGGING)
		{
			LOGGER.info("Server world {} saving", SharedApi.currentWorld);
		}
		
		if (SharedApi.currentWorld instanceof IDhServerWorld)
		{
			SharedApi.currentWorld.saveAndFlush();
			
			for (IDhLevel level : SharedApi.currentWorld.getAllLoadedLevels())
			{
				ApiEventInjector.INSTANCE.fireAllEvents(DhApiLevelSaveEvent.class, new DhApiLevelSaveEvent.EventParam(level.getLevelWrapper()));
			}
		}
	}

	public void serverChunkLoadEvent(IChunkWrapper chunk, ILevelWrapper level)
	{
		IDhLevel dhLevel = SharedApi.currentWorld.getLevel(level);
		if (dhLevel != null)
		{
			dhLevel.updateChunk(chunk);
		}
	}
	public void serverChunkSaveEvent(IChunkWrapper chunk, ILevelWrapper level)
	{
		IDhLevel dhLevel = SharedApi.currentWorld.getLevel(level);
		if (dhLevel != null)
		{
			dhLevel.updateChunk(chunk);
		}
	}
	
}
