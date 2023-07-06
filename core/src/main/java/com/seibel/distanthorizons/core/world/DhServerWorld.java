package com.seibel.distanthorizons.core.world;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.seibel.distanthorizons.core.file.structure.LocalSaveStructure;
import com.seibel.distanthorizons.core.level.DhServerLevel;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.network.NetworkServer;
import com.seibel.distanthorizons.core.network.messages.*;
import com.seibel.distanthorizons.core.network.messages.RequestChunksMessage;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DhServerWorld extends AbstractDhWorld implements IDhServerWorld
{
	private final HashMap<IServerLevelWrapper, DhServerLevel> levels;
	public final LocalSaveStructure saveStructure;

	private final NetworkServer networkServer;
	private final HashMap<UUID, DhRemotePlayer> playersByUUID;
	private final BiMap<ChannelHandlerContext, DhRemotePlayer> playersByConnection;

	
	public DhServerWorld()
	{
		super(EWorldEnvironment.Server_Only);
		
		this.saveStructure = new LocalSaveStructure();
		this.levels = new HashMap<>();

		// TODO move to global config once server specific configs are implemented
		this.networkServer = new NetworkServer(25049);
		this.playersByUUID = new HashMap<>();
		this.playersByConnection = HashBiMap.create();
		registerNetworkHandlers();
		
		LOGGER.info("Started "+DhServerWorld.class.getSimpleName()+" of type "+this.environment);
	}

	private void registerNetworkHandlers() {
		networkServer.registerHandler(CloseMessage.class, (msg, ctx) -> {
			DhRemotePlayer dhPlayer = playersByConnection.remove(ctx);
			if (dhPlayer != null)
				dhPlayer.ctx = null;
		});

		networkServer.registerHandler(PlayerUUIDMessage.class, (msg, ctx) -> {
			DhRemotePlayer dhPlayer = playersByUUID.get(msg.playerUUID);

			if (dhPlayer == null) {
				networkServer.disconnectClient(ctx, "Player is not logged in.");
				return;
			}

			if (dhPlayer.ctx != null) {
				networkServer.disconnectClient(ctx, "Another connection is already in use.");
				return;
			}

			dhPlayer.ctx = ctx;
			playersByConnection.put(ctx, dhPlayer);

			ctx.writeAndFlush(new AckMessage(PlayerUUIDMessage.class));
		});

		networkServer.registerHandler(LodConfigMessage.class, (msg, ctx) -> {
			// TODO Take notice of received config
			ctx.writeAndFlush(new AckMessage(LodConfigMessage.class));
		});

		networkServer.registerHandler(RequestChunksMessage.class, (msg, ctx) -> {
			// hasReceivedChunkRequest should be false somewhere ???
			// to avoid sending updates until client says at least something about its state
		});
	}
	
	public void addPlayer(IServerPlayerWrapper serverPlayer) {
		playersByUUID.put(serverPlayer.getUUID(), new DhRemotePlayer(serverPlayer));
	}

	public void removePlayer(IServerPlayerWrapper serverPlayer) {
		DhRemotePlayer dhPlayer = playersByUUID.remove(serverPlayer.getUUID());
		ChannelHandlerContext ctx = playersByConnection.inverse().remove(dhPlayer);
		if (ctx != null) {
			ctx.writeAndFlush(new CloseReasonMessage("You are being disconnected."))
					.addListener(future -> ctx.close());
		}
	}
	
	@Override
	public DhServerLevel getOrLoadLevel(ILevelWrapper wrapper)
	{
		if (!(wrapper instanceof IServerLevelWrapper))
		{
			return null;
		}
		
		return this.levels.computeIfAbsent((IServerLevelWrapper) wrapper, (w) ->
		{
			File levelFile = this.saveStructure.getLevelFolder(wrapper);
			LodUtil.assertTrue(levelFile != null);
			return new DhServerLevel(this.saveStructure, w);
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
		this.networkServer.close();

		for (DhServerLevel level : this.levels.values())
		{
			LOGGER.info("Unloading level " + level.getLevelWrapper().getDimensionType().getDimensionName());
			level.close();
		}
		
		this.levels.clear();
		LOGGER.info("Closed DhWorld of type "+this.environment);
	}
	
	
	
}
