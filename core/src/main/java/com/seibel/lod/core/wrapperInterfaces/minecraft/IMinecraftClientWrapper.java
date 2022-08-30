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

package com.seibel.lod.core.wrapperInterfaces.minecraft;

import java.io.File;
import java.util.ArrayList;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
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
	
	String getCurrentServerName();
	String getCurrentServerIp();
	String getCurrentServerVersion();

	//=============//
	// Simple gets //
	//=============//
	
	boolean playerExists();
	
	DHBlockPos getPlayerBlockPos();
	
	DHChunkPos getPlayerChunkPos();

	@Deprecated
	ILevelWrapper getWrappedClientWorld();
	
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
	void crashMinecraft(String errorMessage, Throwable exception);

    Object getOptionsObject();
	
}
