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

package com.seibel.lod.core.logging;

import com.seibel.lod.core.enums.config.ELoggerMode;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class ConfigBasedLogger
{
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	
	public static final List<WeakReference<ConfigBasedLogger>> loggers
			= Collections.synchronizedList(new LinkedList<WeakReference<ConfigBasedLogger>>());
	
	public static synchronized void updateAll()
	{
		loggers.removeIf((logger) -> logger.get() == null);
		loggers.forEach((logger) -> {
			ConfigBasedLogger l = logger.get();
			if (l != null)
				l.update();
		});
	}
	
	private ELoggerMode mode;
	private final Supplier<ELoggerMode> getter;
	private final Logger logger;
	
	public ConfigBasedLogger(Logger logger, Supplier<ELoggerMode> configQuery)
	{
		getter = configQuery;
		mode = getter.get();
		this.logger = logger;
		loggers.add(new WeakReference<>(this));
	}
	
	private String _throwableToDetailString(Throwable t)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(t.toString()).append('\n');
		StackTraceElement[] stacks = t.getStackTrace();
		
		if (stacks == null || stacks.length == 0)
			return sb.append("  at {Stack trace unavailable}").toString();
		
		for (StackTraceElement stack : stacks)
		{
			sb.append("  at ").append(stack.toString()).append("\n");
		}
		return sb.toString();
	}
	
	public void update()
	{
		mode = getter.get();
	}
	
	public boolean canMaybeLog()
	{
		return mode != ELoggerMode.DISABLED;
	}
	
	public void log(Level level, String str, Object... param)
	{
		
		Message msg = logger.getMessageFactory().newMessage(str, param);
		String msgStr = msg.getFormattedMessage();
		if (mode.levelForFile.isLessSpecificThan(level))
		{
			Level logLevel = level.isLessSpecificThan(Level.INFO) ? Level.INFO : level;
			if (param.length > 0 && param[param.length - 1] instanceof Throwable)
				logger.log(logLevel, msgStr, (Throwable) param[param.length - 1]);
			else
				logger.log(logLevel, msgStr);
		}
		if (mode.levelForChat.isLessSpecificThan(level))
		{
			if (param.length > 0 && param[param.length - 1] instanceof Throwable)
				MC.logToChat(level, msgStr + "\n" +
						_throwableToDetailString(((Throwable) param[param.length - 1])));
			else
				MC.logToChat(level, msgStr);
		}
	}
	
	public void error(String str, Object... param)
	{
		log(Level.ERROR, str, param);
	}
	
	public void warn(String str, Object... param)
	{
		log(Level.WARN, str, param);
	}
	
	public void info(String str, Object... param)
	{
		log(Level.INFO, str, param);
	}
	
	public void debug(String str, Object... param)
	{
		log(Level.DEBUG, str, param);
	}
	
	public void trace(String str, Object... param)
	{
		log(Level.TRACE, str, param);
	}
}
