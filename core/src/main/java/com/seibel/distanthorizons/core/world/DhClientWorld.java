/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.core.world;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.file.structure.ClientOnlySaveStructure;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.level.DhClientLevel;
import com.seibel.distanthorizons.core.network.NetworkClient;
import com.seibel.distanthorizons.core.network.messages.*;
import com.seibel.distanthorizons.core.network.messages.PlayerUUIDMessage;
import com.seibel.distanthorizons.core.network.messages.RemotePlayerConfigMessage;
import com.seibel.distanthorizons.core.network.objects.RemotePlayer;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.util.objects.EventLoop;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class DhClientWorld extends AbstractDhWorld implements IDhClientWorld
{
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	private final ConcurrentHashMap<IClientLevelWrapper, DhClientLevel> levels;
	public final ClientOnlySaveStructure saveStructure;

//    private final NetworkClient networkClient;
	
	public ExecutorService dhTickerThread = ThreadUtil.makeSingleThreadPool("Client World Ticker Thread");
	public EventLoop eventLoop = new EventLoop(this.dhTickerThread, this::_clientTick);
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public DhClientWorld()
	{
		super(EWorldEnvironment.Client_Only);
		
		this.saveStructure = new ClientOnlySaveStructure();
		this.levels = new ConcurrentHashMap<>();
		
		//if (Config.Client.Advanced.Multiplayer.enableServerNetworking.get())
		//{
		//	// TODO server specific configs
		//	this.networkClient = new NetworkClient(MC_CLIENT.getCurrentServerIp(), 25049);
		//	this.registerNetworkHandlers();
		//}
		//else
		//{
		//	this.networkClient = null;
		//}
		
		LOGGER.info("Started DhWorld of type " + this.environment);
	}
	
	private void registerNetworkHandlers()
	{
//        this.networkClient.registerHandler(HelloMessage.class, (msg, ctx) ->
//        {
//            ctx.writeAndFlush(new PlayerUUIDMessage(MC_CLIENT.getPlayerUUID()));
//        });
//
//        // TODO Proper payload handling
//	    this.networkClient.registerAckHandler(PlayerUUIDMessage.class, ctx ->
//        {
//            ctx.writeAndFlush(new RemotePlayerConfigMessage(new RemotePlayer.Payload()));
//        });
//	    this.networkClient.registerHandler(RemotePlayerConfigMessage.class, (msg, ctx) ->
//        {
//
//        });
//	    
//	    this.networkClient.registerAckHandler(RemotePlayerConfigMessage.class, ctx ->
//        {
//            // TODO Actually request chunks
//            ctx.writeAndFlush(new RequestChunksMessage());
//        });
	}
	
	
	
	//=========//
	// methods //
	//=========//
	
	@Override
	public DhClientLevel getOrLoadLevel(ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IClientLevelWrapper))
		{
			return null;
		}
		
		return this.levels.computeIfAbsent((IClientLevelWrapper) wrapper, (clientLevelWrapper) ->
		{
			File file = this.saveStructure.getLevelFolder(wrapper);
			
			if (file == null)
			{
				return null;
			}
			
			return new DhClientLevel(this.saveStructure, clientLevelWrapper);
		});
	}
	
	@Override
	public DhClientLevel getLevel(ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IClientLevelWrapper))
		{
			return null;
		}
		
		return this.levels.get(wrapper);
	}
	
	@Override
	public Iterable<? extends IDhLevel> getAllLoadedLevels() { return this.levels.values(); }
	
	@Override
	public void unloadLevel(ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IClientLevelWrapper))
		{
			return;
		}
		
		if (this.levels.containsKey(wrapper))
		{
			LOGGER.info("Unloading level " + this.levels.get(wrapper));
			wrapper.onUnload();
			this.levels.remove(wrapper).close();
		}
	}
	
	private void _clientTick()
	{
		this.levels.values().forEach(DhClientLevel::clientTick);
	}
	
	public void clientTick() { this.eventLoop.tick(); }
	
	public void doWorldGen() {
		// Not implemented
	}

    @Override
    public CompletableFuture<Void> saveAndFlush()
	{
		return CompletableFuture.allOf(this.levels.values().stream().map(DhClientLevel::saveAsync).toArray(CompletableFuture[]::new));
	}
	
	@Override
	public void close()
	{
//		if (this.networkClient != null)
//		{
////			this.networkClient.close();
//		}
		
		
		this.saveAndFlush();
		for (DhClientLevel dhClientLevel : this.levels.values())
		{
			LOGGER.info("Unloading level " + dhClientLevel.getLevelWrapper().getDimensionType().getDimensionName());
			
			// level wrapper shouldn't be null, but just in case
			IClientLevelWrapper clientLevelWrapper = dhClientLevel.getClientLevelWrapper();
			if (clientLevelWrapper != null)
			{
				clientLevelWrapper.onUnload();
			}
			
			dhClientLevel.close();
		}
		
		this.levels.clear();
		this.eventLoop.close();
		LOGGER.info("Closed DhWorld of type " + this.environment);
	}
	
}
