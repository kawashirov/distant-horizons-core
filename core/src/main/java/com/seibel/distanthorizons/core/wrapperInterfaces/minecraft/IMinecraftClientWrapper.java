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

package com.seibel.distanthorizons.core.wrapperInterfaces.minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.enums.ELodDirection;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Level;

/**
 * Contains everything related to the Minecraft object.
 * 
 * @author James Seibel
 * @version 2022-8-20
 */
public interface IMinecraftClientWrapper extends IBindable
{
	//================//
	// helper methods //
	//================//
	
	/**
	 * This should be called at the beginning of every frame to
	 * clear any Minecraft data that becomes out of date after a frame. <br> <br>
	 * <p>
	 * LightMaps and other time sensitive objects fall in this category. <br> <br>
	 * <p>
	 * This doesn't affect OpenGL objects in any way.
	 */
	void clearFrameObjectCache();
	
	//=================//
	// method wrappers //
	//=================//
	
	float getShade(ELodDirection lodDirection);
	
	boolean hasSinglePlayerServer();
	boolean clientConnectedToDedicatedServer();
	
	String getCurrentServerName();
	String getCurrentServerIp();
	String getCurrentServerVersion();

	//=============//
	// Simple gets //
	//=============//
	
	boolean playerExists();

	UUID getPlayerUUID();

	DhBlockPos getPlayerBlockPos();
	
	DhChunkPos getPlayerChunkPos();

	/** 
	 * Returns the level the client is currently in. <br> 
	 * Returns null if the client isn't in a level.
	 */
	ILevelWrapper getWrappedClientWorld();

	/** Please move over to getInstallationDirectory() within the IMinecraftSharedWrapper */
	@Deprecated
	File getGameDirectory();
	
	IProfilerWrapper getProfiler();
	
	/** Returns all worlds available to the server */
	ArrayList<ILevelWrapper> getAllServerWorlds();
	
	void sendChatMessage(String string);
	
	/** Sends the given message to chat with a formatted prefix and color based on the log level. */
	default void logToChat(Level logLevel, String message)
	{
		String prefix = "[" + ModInfo.READABLE_NAME + "] ";
		if (logLevel == Level.ERROR)
		{
			prefix += "\u00A74";
		}
		else if (logLevel == Level.WARN)
		{
			prefix += "\u00A76";
		}
		else if (logLevel == Level.INFO)
		{
			prefix += "\u00A7f";
		}
		else if (logLevel == Level.DEBUG)
		{
			prefix += "\u00A77";
		}
		else if (logLevel == Level.TRACE)
		{
			prefix += "\u00A78";
		}
		else
		{
			prefix += "\u00A7f";
		}
		prefix += "\u00A7l\u00A7u";
		prefix += logLevel.name();
		prefix += ":\u00A7r ";
		
		this.sendChatMessage(prefix + message);
	}
	
	/**
	 * Crashes Minecraft, displaying the given errorMessage <br> <br>
	 * In the following format: <br>
	 * 
	 * The game crashed whilst <strong>errorMessage</strong>  <br>
	 * Error: <strong>ExceptionClass: exceptionErrorMessage</strong>  <br>
	 * Exit Code: -1  <br>
	 */
	void crashMinecraft(String errorMessage, Throwable exception); //FIXME: Move to IMinecraftSharedWrapper

    Object getOptionsObject();
	
}
