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

package com.seibel.distanthorizons.core.api.internal;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.*;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.core.level.IKeyedClientLevelManager;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.objects.Pair;
import com.seibel.distanthorizons.core.world.*;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.api.enums.rendering.EDebugRendering;
import com.seibel.distanthorizons.api.enums.rendering.ERendererMode;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.logging.ConfigBasedLogger;
import com.seibel.distanthorizons.core.logging.ConfigBasedSpamLogger;
import com.seibel.distanthorizons.core.logging.SpamReducedLogger;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import com.seibel.distanthorizons.core.render.glObject.GLProxy;
import com.seibel.distanthorizons.core.render.renderer.TestRenderer;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
//import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * This holds the methods that should be called
 * by the host mod loader (Fabric, Forge, etc.).
 * Specifically for the client.
 */
public class ClientApi
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static boolean prefLoggerEnabled = false;
	
	public static final ClientApi INSTANCE = new ClientApi();
	public static TestRenderer testRenderer = new TestRenderer();
	
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final IKeyedClientLevelManager KEYED_CLIENT_LEVEL_MANAGER = SingletonInjector.INSTANCE.get(IKeyedClientLevelManager.class);
	
	public static final long SPAM_LOGGER_FLUSH_NS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
	
	private boolean configOverrideReminderPrinted = false;
	public boolean rendererDisabledBecauseOfExceptions = false;
	
	private long lastFlushNanoTime = 0;
	
	private boolean isServerCommunicationEnabled = false;
	
	/** set to true if any unexpected responses are received from the server */
	private boolean serverNetworkingIsMalformed = false;
	
	/** Holds any levels that were loaded before the {@link ClientApi#onClientOnlyConnected} was fired. */
	private final HashSet<IClientLevelWrapper> waitingClientLevels = new HashSet<>();
	/** Holds any chunks that were loaded before the {@link ClientApi#clientLevelLoadEvent(IClientLevelWrapper)} was fired. */
	private final HashMap<Pair<IClientLevelWrapper, DhChunkPos>, IChunkWrapper> waitingChunkByClientLevelAndPos = new HashMap<>();
	
	
	
	//==============//
	// constructors //
	//==============//
	
	private ClientApi() { }
	
	
	
	//==============//
	// world events //
	//==============//
	
	/**
	 * May be fired slightly before or after the associated
	 * {@link ClientApi#clientLevelLoadEvent(IClientLevelWrapper)} event
	 * depending on how the host mod loader functions.
	 */
	public void onClientOnlyConnected()
	{
		// only continue if the client is connected to a different server
		if (MC.clientConnectedToDedicatedServer())
		{
			LOGGER.info("Client on ClientOnly mode connecting.");
			
			// firing after clientLevelLoadEvent
			// TODO if level has prepped to load it should fire level load event
			SharedApi.setDhWorld(new DhClientWorld());
			
			
			LOGGER.info("Loading [" + this.waitingClientLevels.size() + "] waiting client level wrappers.");
			for (IClientLevelWrapper level : this.waitingClientLevels)
			{
				this.clientLevelLoadEvent(level);
			}
			
			this.waitingClientLevels.clear();
		}
	}
	
	public void onClientOnlyDisconnected()
	{
		if (MC.clientConnectedToDedicatedServer())
		{
			AbstractDhWorld world = SharedApi.getAbstractDhWorld();
			if (world != null)
			{
				LOGGER.info("Client on ClientOnly mode disconnecting.");
				
				world.close();
				SharedApi.setDhWorld(null);
			}
			
			// clear the previous server's information
			this.isServerCommunicationEnabled = false;
			this.serverNetworkingIsMalformed = false;
			KEYED_CLIENT_LEVEL_MANAGER.setUseOverrideWrapper(false);
			KEYED_CLIENT_LEVEL_MANAGER.setServerKeyedLevel(null);
			
			// remove any waiting items
			this.waitingChunkByClientLevelAndPos.clear();
			this.waitingClientLevels.clear();
		}
	}
	
	
	
	//==============//
	// level events //
	//==============//
	
	public void clientLevelUnloadEvent(IClientLevelWrapper level)
	{
		LOGGER.info("Unloading client level [" + level + "].");
		
		AbstractDhWorld world = SharedApi.getAbstractDhWorld();
		if (world != null)
		{
			world.unloadLevel(level);
			ApiEventInjector.INSTANCE.fireAllEvents(DhApiLevelUnloadEvent.class, new DhApiLevelUnloadEvent.EventParam(level));
		}
		else
		{
			this.waitingClientLevels.remove(level);
		}
	}
	
	public void clientLevelLoadEvent(IClientLevelWrapper level) { this.clientLevelLoadEvent(level, false); }
	public void multiverseClientLevelLoadEvent(IClientLevelWrapper level) { this.clientLevelLoadEvent(level, true); }
	private void clientLevelLoadEvent(IClientLevelWrapper level, boolean isServerCommunication)
	{
		if (this.isServerCommunicationEnabled && !isServerCommunication)
		{
			LOGGER.info("Server supports communication, deferring loading.");
			return;
		}
		
		
		LOGGER.info("Loading " + (isServerCommunication ? "Multiverse" : "") + " client level [" + level + "].");
		
		AbstractDhWorld world = SharedApi.getAbstractDhWorld();
		if (world != null)
		{
			world.getOrLoadLevel(level);
			ApiEventInjector.INSTANCE.fireAllEvents(DhApiLevelLoadEvent.class, new DhApiLevelLoadEvent.EventParam(level));
			
			this.loadWaitingChunksForLevel(level);
		}
		else
		{
			this.waitingClientLevels.add(level);
		}
	}
	private void loadWaitingChunksForLevel(IClientLevelWrapper level)
	{
		HashSet<Pair<IClientLevelWrapper, DhChunkPos>> keysToRemove = new HashSet<>();
		for (Pair<IClientLevelWrapper, DhChunkPos> levelChunkPair : this.waitingChunkByClientLevelAndPos.keySet())
		{
			// only load chunks that came from this level
			IClientLevelWrapper levelWrapper = levelChunkPair.first;
			if (levelWrapper.equals(level))
			{
				IChunkWrapper chunkWrapper = this.waitingChunkByClientLevelAndPos.get(levelChunkPair);
				this.applyChunkUpdate(chunkWrapper, levelWrapper, false);
				keysToRemove.add(levelChunkPair);
			}
		}
		LOGGER.info("Loaded [" + keysToRemove.size() + "] waiting chunk wrappers.");
		
		for (Pair<IClientLevelWrapper, DhChunkPos> keyToRemove : keysToRemove)
		{
			this.waitingChunkByClientLevelAndPos.remove(keyToRemove);
		}
	}
	
	
	
	//=======================//
	// chunk modified events //
	//=======================//
	
	/** handles both block place and break events */
	public void clientChunkBlockChangedEvent(IChunkWrapper chunk, IClientLevelWrapper level) { this.applyChunkUpdate(chunk, level, true); }
	
	public void clientChunkLoadEvent(IChunkWrapper chunk, IClientLevelWrapper level) { this.applyChunkUpdate(chunk, level, false); }
	public void clientChunkSaveEvent(IChunkWrapper chunk, IClientLevelWrapper level) { this.applyChunkUpdate(chunk, level, false); }
	
	private void applyChunkUpdate(IChunkWrapper chunkWrapper, IClientLevelWrapper level, boolean updateNeighborChunks)
	{
		// if the user is in a single player world the chunk updates are handled on the server side
		if (SharedApi.getEnvironment() != EWorldEnvironment.Client_Only)
		{
			return;
		}
		
		// only continue if the level is loaded
		IDhLevel dhLevel = SharedApi.getAbstractDhWorld().getLevel(level);
		if (dhLevel == null)
		{
			// If the level isn't loaded yet, keep track of which chunks were loaded so we can use them later.
			// This may happen if the world and level load events happen out of order
			this.waitingChunkByClientLevelAndPos.replace(new Pair<>(level, chunkWrapper.getChunkPos()), chunkWrapper);
			
			return;
		}
		
		
		if (!updateNeighborChunks)
		{
			dhLevel.updateChunkAsync(chunkWrapper);
		}
		else 
		{
			// update any existing neighbour chunks so lighting changes are propagated correctly
			for (int xOffset = -1; xOffset <= 1; xOffset++)
			{
				for (int zOffset = -1; zOffset <= 1; zOffset++)
				{
					if (xOffset == 0 && zOffset == 0)
					{
						// center chunk
						dhLevel.updateChunkAsync(chunkWrapper);
					}
					else
					{
						// neighboring chunk
						DhChunkPos neighbourPos = new DhChunkPos(chunkWrapper.getChunkPos().x + xOffset, chunkWrapper.getChunkPos().z + zOffset);
						IChunkWrapper neighbourChunk = dhLevel.getLevelWrapper().tryGetChunk(neighbourPos);
						if (neighbourChunk != null)
						{
							dhLevel.updateChunkAsync(neighbourChunk);
						}
					}
				}
			}
		}
	}
	
	
	
	//===============//
	// render events //
	//===============//
	
	public void rendererShutdownEvent()
	{
		LOGGER.info("Renderer shutting down.");
		
		IProfilerWrapper profiler = MC.getProfiler();
		profiler.push("DH-RendererShutdown");
		
		profiler.pop();
	}
	
	public void rendererStartupEvent()
	{
		LOGGER.info("Renderer starting up.");
		
		IProfilerWrapper profiler = MC.getProfiler();
		profiler.push("DH-RendererStartup");
		
		// make sure the GLProxy is created before the LodBufferBuilder needs it
		GLProxy.getInstance();
		profiler.pop();
	}
	
	public void clientTickEvent()
	{
		IProfilerWrapper profiler = MC.getProfiler();
		profiler.push("DH-ClientTick");
		
		boolean doFlush = System.nanoTime() - this.lastFlushNanoTime >= SPAM_LOGGER_FLUSH_NS;
		if (doFlush)
		{
			this.lastFlushNanoTime = System.nanoTime();
			SpamReducedLogger.flushAll();
		}
		ConfigBasedLogger.updateAll();
		ConfigBasedSpamLogger.updateAll(doFlush);
		
		IDhClientWorld clientWorld = SharedApi.getIDhClientWorld();
		if (clientWorld != null)
		{
			clientWorld.clientTick();
			
			// Ignore local world gen, as it's managed by server ticking
			if (!(clientWorld instanceof DhClientServerWorld))
			{
				SharedApi.worldGenTick(clientWorld::doWorldGen);
			}
		}
		profiler.pop();
	}
	
	
	
	//============//
	// networking //
	//============//

//	/** @param byteBuf is Netty's {@link ByteBuffer} wrapper. */
//	public void serverMessageReceived(ByteBuf byteBuf)
//	{
//		if (!Config.Client.Advanced.Multiplayer.enableMultiverseNetworking.get())
//		{
//			// multiverse networking disabled, ignore anything sent from the server
//			return;
//		}
//		
//		
//		
//		// either value can be set to true to debug the received byte stream
//		boolean stopAndDisplayInputAsByteArray = false;
//		boolean stopAndDisplayInputAsString = false;
//		if (stopAndDisplayInputAsByteArray || stopAndDisplayInputAsString)
//		{
//			String messageString = "";
//			if (stopAndDisplayInputAsByteArray)
//			{
//				int byteCount = byteBuf.readableBytes();
//				byte[] arr = new byte[byteCount];
//				StringBuilder stringBuilder = new StringBuilder("Server message received: [");
//				for (int i = 0; i < byteCount; i++)
//				{
//					arr[i] = byteBuf.readByte();
//					stringBuilder.append(arr[i]);
//				}
//				stringBuilder.append("]");
//				
//				messageString = stringBuilder.toString();
//			}
//			else if (stopAndDisplayInputAsString)
//			{
//				messageString = byteBuf.toString(StandardCharsets.UTF_8);
//			}
//			
//			// this is logged as an error so it is easier to see in an Intellij log
//			LOGGER.error(messageString);
//			return;
//		}
//		
//		
//		
//		
//		// It is important to ensure malicious server input is ignored.
//		if (this.serverNetworkingIsMalformed)
//		{
//			return;
//		}
//		
//		// check that the incoming message is within the expected size
//		short commandLength = byteBuf.readShort();
//		if (commandLength < 1 || commandLength > 32)
//		{
//			LOGGER.error("Server command length ["+commandLength+"] outside the expected range of 1 to 32 (inclusive).");
//			ClientApi.INSTANCE.serverNetworkingIsMalformed = true;
//			return;
//		}
//		
//		// parse the command
//		String eventType;
//		try
//		{
//			eventType = byteBuf.readCharSequence(commandLength, StandardCharsets.UTF_8).toString();
//		}
//		catch (Exception e)
//		{
//			LOGGER.error("Server sent un-parsable command. Error: "+e.getMessage());
//			return;
//		}
//		
//		switch (eventType)
//		{
//			case "ServerCommsEnabled":
//				LOGGER.info("Server supports DH multiverse protocol.");
//				ClientApi.INSTANCE.isServerCommunicationEnabled = true;
//				KEYED_CLIENT_LEVEL_MANAGER.setUseOverrideWrapper(true);
//				MC.executeOnRenderThread(() -> 
//				{
//					// Unload the current world, since it may be wrong.
//					// A followup WorldChanged event should be received from the server soon after this.
//					LOGGER.info("Unloading current client level so the server can define the correct multiverse level.");
//					this.clientLevelUnloadEvent((IClientLevelWrapper) MC.getWrappedClientWorld());
//				});
//				break;
//			
//			case "LevelChanged":
//				short levelKeyLength = byteBuf.readShort();
//				if (levelKeyLength < 1 || levelKeyLength > 128) // TODO 128 should be put into a constant somewhere
//				{
//					LOGGER.error("Server [LevelChanged] command length ["+commandLength+"] outside the expected range of 1 to 128 (inclusive).");
//					this.serverNetworkingIsMalformed = true;
//					return;
//				}
//				
//				String levelKey = byteBuf.readCharSequence(levelKeyLength, StandardCharsets.UTF_8).toString();
//				if (!levelKey.matches("[a-zA-Z0-9_]+"))
//				{
//					LOGGER.error("Server sent invalid world key name, and is being ignored.");
//					this.isServerCommunicationEnabled = false;
//					this.serverNetworkingIsMalformed = true;
//					return;
//				}
//				
//				LOGGER.info("Server level change event received, changing the level to ["+levelKey+"].");
//				MC.executeOnRenderThread(() -> {
//					if (MC.getWrappedClientWorld() != null)
//					{
//						this.clientLevelUnloadEvent((IClientLevelWrapper) MC.getWrappedClientWorld());
//					}
//					IServerKeyedClientLevel clientLevel = KEYED_CLIENT_LEVEL_MANAGER.getServerKeyedLevel(MC.getWrappedClientWorld(), levelKey);
//					KEYED_CLIENT_LEVEL_MANAGER.setServerKeyedLevel(clientLevel);
//					this.multiverseClientLevelLoadEvent(clientLevel);
//				});
//				break;
//		}
//	}
	
	
	
	//===========//
	// rendering //
	//===========//
	
	public void renderLods(IClientLevelWrapper levelWrapper, Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks)
	{
		if (ModInfo.IS_DEV_BUILD && !this.configOverrideReminderPrinted && MC.playerExists())
		{
			// remind the user that this is a development build
			MC.sendChatMessage(ModInfo.READABLE_NAME + " experimental build " + ModInfo.VERSION);
			MC.sendChatMessage("You are running an unsupported version of Distant Horizons!");
			MC.sendChatMessage("Here be dragons!");
			this.configOverrideReminderPrinted = true;
		}
		
		
		IProfilerWrapper profiler = MC.getProfiler();
		profiler.pop(); // get out of "terrain"
		profiler.push("DH-RenderLevel");
		try
		{
			if (!RenderUtil.shouldLodsRender(levelWrapper))
			{
				return;
			}
			
			
			//FIXME: Improve class hierarchy of DhWorld, IClientWorld, IServerWorld to fix all this hard casting
			// (also in RenderUtil)
			IDhClientWorld dhClientWorld = SharedApi.getIDhClientWorld();
			IDhClientLevel level = dhClientWorld.getOrLoadClientLevel(levelWrapper);
			
			
			
			
			try
			{
				if (Config.Client.Advanced.Debugging.rendererMode.get() == ERendererMode.DEFAULT)
				{
					DhApiRenderParam renderEventParam =
							new DhApiRenderParam(mcProjectionMatrix, mcModelViewMatrix,
									RenderUtil.createLodProjectionMatrix(mcProjectionMatrix, partialTicks),
									RenderUtil.createLodModelViewMatrix(mcModelViewMatrix), partialTicks);
					
					boolean renderingCanceled = ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderEvent.class, new DhApiBeforeRenderEvent.EventParam(renderEventParam));
					if (!this.rendererDisabledBecauseOfExceptions && !renderingCanceled)
					{
						level.render(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
						ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterRenderEvent.class, new DhApiAfterRenderEvent.EventParam(renderEventParam));
					}
				}
				else if (Config.Client.Advanced.Debugging.rendererMode.get() == ERendererMode.DEBUG)
				{
					profiler.push("Render Debug");
					ClientApi.testRenderer.render();
					profiler.pop();
				}
				// the other rendererMode is DISABLED
			}
			catch (RuntimeException e)
			{
				this.rendererDisabledBecauseOfExceptions = true;
				LOGGER.error("Renderer thrown an uncaught exception: ", e);
				
				MC.sendChatMessage("\u00A74\u00A7l\u00A7uERROR: Distant Horizons"
						+ " renderer has encountered an exception!");
				MC.sendChatMessage("\u00A74Renderer is now disabled to prevent further issues.");
				MC.sendChatMessage("\u00A74Exception detail: " + e);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("client level rendering uncaught exception: ", e);
		}
		finally
		{
			profiler.pop(); // end LOD
			profiler.push("terrain"); // go back into "terrain"
		}
	}
	
	
	
	//=================//
	//    DEBUG USE    //
	//=================//
	
	/** Trigger once on key press, with CLIENT PLAYER. */
	public void keyPressedEvent(int glfwKey)
	{
		if (!Config.Client.Advanced.Debugging.enableDebugKeybindings.get())
		{
			// keybindings are disabled
			return;
		}
		
		
		if (glfwKey == GLFW.GLFW_KEY_F8)
		{
			Config.Client.Advanced.Debugging.debugRendering.set(EDebugRendering.next(Config.Client.Advanced.Debugging.debugRendering.get()));
			MC.sendChatMessage("F8: Set debug mode to " + Config.Client.Advanced.Debugging.debugRendering.get());
		}
		else if (glfwKey == GLFW.GLFW_KEY_F6)
		{
			Config.Client.Advanced.Debugging.rendererMode.set(ERendererMode.next(Config.Client.Advanced.Debugging.rendererMode.get()));
			MC.sendChatMessage("F6: Set rendering to " + Config.Client.Advanced.Debugging.rendererMode.get());
		}
		else if (glfwKey == GLFW.GLFW_KEY_P)
		{
			prefLoggerEnabled = !prefLoggerEnabled;
			MC.sendChatMessage("P: Debug Pref Logger is " + (prefLoggerEnabled ? "enabled" : "disabled"));
		}
	}
	
	
}
