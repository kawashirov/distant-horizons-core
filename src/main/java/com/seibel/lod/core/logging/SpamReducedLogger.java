/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.seibel.lod.core.logging;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.lod.core.api.ApiShared;
import org.apache.logging.log4j.Level;

public class SpamReducedLogger {
	public static final List<WeakReference<SpamReducedLogger>> loggers
			= Collections.synchronizedList(new LinkedList<WeakReference<SpamReducedLogger>>());
	public static synchronized void flushAll() {
		loggers.removeIf((logger) -> logger.get()==null);
		loggers.forEach((logger) -> {
			SpamReducedLogger l = logger.get();
			if (l!=null) l.reset();
		});
	}

	private final int maxLogCount;
	private final AtomicInteger logTries = new AtomicInteger(0);

	public SpamReducedLogger(int maxLogPerSec) {
		maxLogCount = maxLogPerSec;
		loggers.add(new WeakReference<SpamReducedLogger>(this));
	}
	public void reset() {logTries.set(0);}
	public boolean canMaybeLog() {return logTries.get() < maxLogCount;}

	public void log(Level level, String str, Object... param) {
		if (logTries.get() >= maxLogCount) return;
		ApiShared.LOGGER.log(level.isLessSpecificThan(Level.INFO) ? Level.INFO : level, str, param);
	}

	public void error(String str, Object... param) {
		log(Level.ERROR, str, param);
	}
	public void warn(String str, Object... param) {
		log(Level.WARN, str, param);
	}
	public void info(String str, Object... param) {
		log(Level.INFO, str, param);
	}
	public void debug(String str, Object... param) {
		log(Level.DEBUG, str, param);
	}
	public void trace(String str, Object... param) {
		log(Level.TRACE, str, param);
	}

	public void incLogTries() {
		logTries.getAndIncrement();
	}

	public void logInc(Level level, String str, Object... param) {
		if (logTries.getAndIncrement() >= maxLogCount) return;
		ApiShared.LOGGER.log(level.isLessSpecificThan(Level.INFO) ? Level.INFO : level, str, param);
	}

	public void errorInc(String str, Object... param) {
		logInc(Level.ERROR, str, param);
	}
	public void warnInc(String str, Object... param) {
		logInc(Level.WARN, str, param);
	}
	public void infoInc(String str, Object... param) {
		logInc(Level.INFO, str, param);
	}
	public void debugInc(String str, Object... param) {
		logInc(Level.DEBUG, str, param);
	}
	public void traceInc(String str, Object... param) {
		logInc(Level.TRACE, str, param);
	}
}