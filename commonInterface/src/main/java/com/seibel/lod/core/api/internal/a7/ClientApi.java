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

package com.seibel.lod.core.api.internal.a7;

import com.seibel.lod.core.a7.level.IClientLevel;
import com.seibel.lod.core.a7.world.*;
import com.seibel.lod.core.api.external.methods.events.abstractEvents.*;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.CoreDhApiRenderParam;
import com.seibel.lod.core.api.implementation.wrappers.DhApiLevelWrapper;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.rendering.EDebugMode;
import com.seibel.lod.core.enums.rendering.ERendererMode;
import com.seibel.lod.core.handlers.dependencyInjection.DhApiEventInjector;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.logging.ConfigBasedSpamLogger;
import com.seibel.lod.core.logging.SpamReducedLogger;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.render.GLProxy;
import com.seibel.lod.core.render.RenderSystemTest;
import com.seibel.lod.core.render.RenderUtil;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * This holds the methods that should be called
 * by the host mod loader (Fabric, Forge, etc.).
 * Specifically for the client.
 * 
 * @author James Seibel
 * @version 2022-8-23
 */
public class ClientApi
{
	private static final Logger LOGGER = LogManager.getLogger(ClientApi.class.getSimpleName());
	public static final boolean ENABLE_EVENT_LOGGING = true;
	public static boolean prefLoggerEnabled = false;
	
	public static final ClientApi INSTANCE = new ClientApi();
	public static RenderSystemTest testRenderer = new RenderSystemTest();
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	public static final boolean ENABLE_LAG_SPIKE_LOGGING = false;
	public static final long LAG_SPIKE_THRESHOLD_NS = TimeUnit.NANOSECONDS.convert(16, TimeUnit.MILLISECONDS);
	
	public static final long SPAM_LOGGER_FLUSH_NS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
	
	private boolean configOverrideReminderPrinted = false;
	public boolean rendererDisabledBecauseOfExceptions = false;
	
	private long lastFlush = 0;
	
	
	
	
	public static class LagSpikeCatcher
	{
		long timer = System.nanoTime();
		
		public LagSpikeCatcher()
		{
		}
		
		public void end(String source)
		{
			if (!ENABLE_LAG_SPIKE_LOGGING)
				return;
			timer = System.nanoTime() - timer;
			if (timer > LAG_SPIKE_THRESHOLD_NS)
			{
				LOGGER.info("LagSpikeCatcher: " + source + " took " + Duration.ofNanos(timer) + "!");
			}
		}
	}
	
	
	
	//==============//
	// constructors //
	//==============//
	
	private ClientApi()
	{
		
	}
	
	
	
	//========//
	// events //
	//========//
	
	public void onClientOnlyConnected()
	{
		if (ENABLE_EVENT_LOGGING)
			LOGGER.info("Client on ClientOnly mode connecting.");
		SharedApi.currentWorld = new DhClientWorld();
	}
	
	public void onClientOnlyDisconnected()
	{
		if (ENABLE_EVENT_LOGGING)
			LOGGER.info("Client on ClientOnly mode disconnecting.");
		SharedApi.currentWorld.close();
		SharedApi.currentWorld = null;
	}
	
	public void clientChunkLoadEvent(IChunkWrapper chunk, IClientLevelWrapper level)
	{
		if (SharedApi.getEnvironment() == WorldEnvironment.Client_Only)
		{
			//TODO: Implement
		}
	}
	
	public void clientChunkSaveEvent(IChunkWrapper chunk, IClientLevelWrapper level)
	{
		if (SharedApi.getEnvironment() == WorldEnvironment.Client_Only)
		{
			//TODO: Implement
			
			// TODO: potentially add a list of chunks that were updated during the save
			DhApiEventInjector.INSTANCE.fireAllEvents(DhApiLevelSaveEvent.class, new DhApiLevelSaveEvent.EventParam(new DhApiLevelWrapper(level)));
		}
	}
	
	public void clientLevelUnloadEvent(IClientLevelWrapper level)
	{
		if (ENABLE_EVENT_LOGGING)
			LOGGER.info("Client level {} unloading.", level);
		if (SharedApi.currentWorld != null)
		{
			SharedApi.currentWorld.unloadLevel(level);
			DhApiEventInjector.INSTANCE.fireAllEvents(DhApiLevelUnloadEvent.class, new DhApiLevelUnloadEvent.EventParam(new DhApiLevelWrapper(level)));
		}
	}
	
	public void clientLevelLoadEvent(IClientLevelWrapper level)
	{
		if (ENABLE_EVENT_LOGGING)
			LOGGER.info("Client level {} loading.", level);
		if (SharedApi.currentWorld != null)
		{
			SharedApi.currentWorld.getOrLoadLevel(level);
			DhApiEventInjector.INSTANCE.fireAllEvents(DhApiLevelLoadEvent.class, new DhApiLevelLoadEvent.EventParam(new DhApiLevelWrapper(level)));
		}
	}
	
	public void rendererShutdownEvent()
	{
		if (ENABLE_EVENT_LOGGING)
			LOGGER.info("Renderer shutting down.");
		IProfilerWrapper profiler = MC.getProfiler();
		profiler.push("DH-RendererShutdown");
		
		profiler.pop();
	}
	
	public void rendererStartupEvent()
	{
		if (ENABLE_EVENT_LOGGING)
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
		
		boolean doFlush = System.nanoTime() - lastFlush >= SPAM_LOGGER_FLUSH_NS;
		if (doFlush)
		{
			lastFlush = System.nanoTime();
			SpamReducedLogger.flushAll();
		}
		ConfigBasedLogger.updateAll();
		ConfigBasedSpamLogger.updateAll(doFlush);
		
		if (SharedApi.currentWorld instanceof IClientWorld)
		{
			((IClientWorld) SharedApi.currentWorld).clientTick();
		}
		profiler.pop();
	}
	
	
	
	//===========//
	// rendering //
	//===========//
	
	public void renderLods(IClientLevelWrapper levelWrapper, Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks)
	{
		if (ModInfo.IS_DEV_BUILD && !configOverrideReminderPrinted && MC.playerExists())
		{
			// remind the user that this is a development build
			MC.sendChatMessage(ModInfo.READABLE_NAME + " experimental build " + ModInfo.VERSION);
			MC.sendChatMessage("You are running an unsupported version of Distant Horizons!");
			MC.sendChatMessage("Here be dragons!");
			configOverrideReminderPrinted = true;
		}
		
		
		IProfilerWrapper profiler = MC.getProfiler();
		profiler.pop(); // get out of "terrain"
		profiler.push("DH-RenderLevel");
		try
		{
			if (!RenderUtil.shouldLodsRender(levelWrapper))
				return;
			
			//FIXME: Improve class hierarchy of DhWorld, IClientWorld, IServerWorld to fix all this hard casting
			// (also in RenderUtil)
			DhWorld dhWorld = SharedApi.currentWorld;
			IClientLevel level = (IClientLevel) dhWorld.getOrLoadLevel(levelWrapper);
			
			if (prefLoggerEnabled)
			{
				level.dumpRamUsage();
			}
			
			
			profiler.push("Render" + ( Config.Client.Advanced.Debugging.rendererMode.get() == ERendererMode.DEFAULT ? "-lods" : "-debug"));
			try
			{
				if (Config.Client.Advanced.Debugging.rendererMode.get() == ERendererMode.DEFAULT)
				{
					CoreDhApiRenderParam renderEventParam =
							new CoreDhApiRenderParam(mcProjectionMatrix, mcModelViewMatrix,
								RenderUtil.createLodProjectionMatrix(mcProjectionMatrix, partialTicks),
								RenderUtil.createLodModelViewMatrix(mcModelViewMatrix), partialTicks);
					
					boolean renderingCanceled = DhApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderEvent.class, new DhApiBeforeRenderEvent.EventParam(renderEventParam));
					
					if (!rendererDisabledBecauseOfExceptions && !renderingCanceled)
					{
						level.render(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
						DhApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterRenderEvent.class, new DhApiAfterRenderEvent.EventParam(renderEventParam));
					}
				}
				else if (Config.Client.Advanced.Debugging.rendererMode.get() == ERendererMode.DEBUG)
				{
					ClientApi.testRenderer.render();
				}
				// the other rendererMode is DISABLED
			}
			catch (RuntimeException e)
			{
				rendererDisabledBecauseOfExceptions = true;
				LOGGER.error("Renderer thrown an uncaught exception: ", e);
				
				MC.sendChatMessage("\u00A74\u00A7l\u00A7uERROR: Distant Horizons"
						+ " renderer has encountered an exception!");
				MC.sendChatMessage("\u00A74Renderer is now disabled to prevent further issues.");
				MC.sendChatMessage("\u00A74Exception detail: " + e);
			}
			profiler.pop();
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
	
	// Trigger once on key press, with CLIENT PLAYER.
	public void keyPressedEvent(int glfwKey)
	{
		if (!Config.Client.Advanced.Debugging.enableDebugKeybindings.get())
			return;
		
		if (glfwKey == GLFW.GLFW_KEY_F8)
		{
			Config.Client.Advanced.Debugging.debugMode.set(EDebugMode.next(Config.Client.Advanced.Debugging.debugMode.get()));
			MC.sendChatMessage("F8: Set debug mode to " + Config.Client.Advanced.Debugging.debugMode.get());
		}
		if (glfwKey == GLFW.GLFW_KEY_F6)
		{
			Config.Client.Advanced.Debugging.rendererMode.set(ERendererMode.next(Config.Client.Advanced.Debugging.rendererMode.get()));
			MC.sendChatMessage("F6: Set rendering to " + Config.Client.Advanced.Debugging.rendererMode.get());
		}
		if (glfwKey == GLFW.GLFW_KEY_P)
		{
			prefLoggerEnabled = !prefLoggerEnabled;
			MC.sendChatMessage("P: Debug Pref Logger is " + (prefLoggerEnabled ? "enabled" : "disabled"));
		}
	}
	
	
}
