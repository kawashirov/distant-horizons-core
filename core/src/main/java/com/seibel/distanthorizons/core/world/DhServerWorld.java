package com.seibel.distanthorizons.core.world;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.seibel.distanthorizons.core.file.structure.LocalSaveStructure;
import com.seibel.distanthorizons.core.level.DhServerLevel;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.network.NetworkServer;
import com.seibel.distanthorizons.core.network.messages.*;
import com.seibel.distanthorizons.core.network.messages.RequestChunksMessage;
import com.seibel.distanthorizons.core.network.objects.RemotePlayer;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
//import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DhServerWorld extends AbstractDhWorld implements IDhServerWorld
{
	private final HashMap<IServerLevelWrapper, DhServerLevel> levels;
	public final LocalSaveStructure saveStructure;

//	private final NetworkServer networkServer;
//	private final HashMap<UUID, RemotePlayer> playersByUUID;
//	private final BiMap<ChannelHandlerContext, RemotePlayer> playersByConnection;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public DhServerWorld()
	{
		super(EWorldEnvironment.Server_Only);
		
		this.saveStructure = new LocalSaveStructure();
		this.levels = new HashMap<>();
		
		// TODO move to global payload once server specific configs are implemented
//		this.networkServer = new NetworkServer(25049);
//		this.playersByUUID = new HashMap<>();
//		this.playersByConnection = HashBiMap.create();
//		this.registerNetworkHandlers();
		
		LOGGER.info("Started " + DhServerWorld.class.getSimpleName() + " of type " + this.environment);
	}
	
	private void registerNetworkHandlers()
	{
//		this.networkServer.registerHandler(CloseMessage.class, (closeMessage, channelContext) -> 
//		{
//			RemotePlayer dhPlayer = this.playersByConnection.remove(channelContext);
//			if (dhPlayer != null)
//			{
//				dhPlayer.channelContext = null;
//			}
//		});
//		
//		this.networkServer.registerHandler(PlayerUUIDMessage.class, (playerUUIDMessage, channelContext) -> 
//		{
//			RemotePlayer dhPlayer = this.playersByUUID.get(playerUUIDMessage.playerUUID);
//			
//			if (dhPlayer == null)
//			{
//				this.networkServer.disconnectClient(channelContext, "Player is not logged in.");
//				return;
//			}
//			
//			if (dhPlayer.channelContext != null)
//			{
//				this.networkServer.disconnectClient(channelContext, "Another connection is already in use.");
//				return;
//			}
//			
//			dhPlayer.channelContext = channelContext;
//			this.playersByConnection.put(channelContext, dhPlayer);
//			
//			channelContext.writeAndFlush(new AckMessage(PlayerUUIDMessage.class));
//		});
//		
//		this.networkServer.registerHandler(RemotePlayerConfigMessage.class, (dhRemotePlayerConfigMessage, channelContext) -> 
//		{
//			// TODO Take notice of received payload and possibly echo back a constrained version
//			channelContext.writeAndFlush(new AckMessage(RemotePlayerConfigMessage.class));
//		});
//		
//		this.networkServer.registerHandler(RequestChunksMessage.class, (msg, ctx) -> 
//		{
//			LOGGER.info("RequestChunksMessage");
//			// hasReceivedChunkRequest should be false somewhere ???
//			// to avoid sending updates until client says at least something about its state
//		});
	}
	
	
	
	//=========//
	// methods //
	//=========//
	
	public void addPlayer(IServerPlayerWrapper serverPlayer)
	{
		//this.playersByUUID.put(serverPlayer.getUUID(), new RemotePlayer(serverPlayer));
	}
	public void removePlayer(IServerPlayerWrapper serverPlayer)
	{
//		RemotePlayer dhPlayer = this.playersByUUID.remove(serverPlayer.getUUID());
//		ChannelHandlerContext channelContext = this.playersByConnection.inverse().remove(dhPlayer);
//		if (channelContext != null)
//		{
//			this.networkServer.disconnectClient(channelContext, "You are being disconnected.");
//		}
	}
	
	@Override
	public DhServerLevel getOrLoadLevel(ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IServerLevelWrapper))
		{
			return null;
		}
		
		return this.levels.computeIfAbsent((IServerLevelWrapper) wrapper, (serverLevelWrapper) ->
		{
			File levelFile = this.saveStructure.getLevelFolder(wrapper);
			LodUtil.assertTrue(levelFile != null);
			return new DhServerLevel(this.saveStructure, serverLevelWrapper);
		});
	}
	
	@Override
	public DhServerLevel getLevel(ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IServerLevelWrapper))
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
		if (!(wrapper instanceof IServerLevelWrapper))
		{
			return;
		}
		
		if (this.levels.containsKey(wrapper))
		{
			LOGGER.info("Unloading level {} ", this.levels.get(wrapper));
			wrapper.onUnload();
			this.levels.remove(wrapper).close();
		}
	}
	
	public void serverTick() { this.levels.values().forEach(DhServerLevel::serverTick); }
	
	public void doWorldGen() { this.levels.values().forEach(DhServerLevel::doWorldGen); }
	
	@Override
	public CompletableFuture<Void> saveAndFlush()
	{
		return CompletableFuture.allOf(this.levels.values().stream().map(DhServerLevel::saveAsync).toArray(CompletableFuture[]::new));
	}
	
	@Override
	public void close()
	{
//		this.networkServer.close();
		
		for (DhServerLevel level : this.levels.values())
		{
			LOGGER.info("Unloading level " + level.getLevelWrapper().getDimensionType().getDimensionName());
			
			// level wrapper shouldn't be null, but just in case
			IServerLevelWrapper serverLevelWrapper = level.getServerLevelWrapper();
			if (serverLevelWrapper != null)
			{
				serverLevelWrapper.onUnload();
			}
			
			level.close();
		}
		
		this.levels.clear();
		LOGGER.info("Closed DhWorld of type " + this.environment);
	}
	
}
