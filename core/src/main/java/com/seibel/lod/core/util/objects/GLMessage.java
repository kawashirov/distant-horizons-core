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
 
package com.seibel.lod.core.util.objects;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.function.Function;

public final class GLMessage
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	
	static final String HEADER = "[LWJGL] OpenGL debug message";
	public final GLMessage.Type type;
	public final GLMessage.Severity severity;
	public final GLMessage.Source source;
	public final String id;
	public final String message;

	// This is needed since gl callback will not have the correct class loader set, and causes issues.
	static void initLoadClass() {
		Builder dummy = new Builder();
		dummy.add(GLMessage.HEADER);
		dummy.add("ID");
		dummy.add(":");
		dummy.add("dummyId");
		dummy.add("Source");
		dummy.add(":");
		dummy.add(Source.API.str);
		dummy.add("Type");
		dummy.add(":");
		dummy.add(Type.OTHER.str);
		dummy.add("Severity");
		dummy.add(":");
		dummy.add(Severity.LOW.str);
		dummy.add("Message");
		dummy.add(":");
		dummy.add("dummyMessage");
	}

	static {
		initLoadClass();
	}

	GLMessage(GLMessage.Type t, GLMessage.Severity s, GLMessage.Source sr, String id, String ms) {
		this.type = t;
		this.source = sr;
		this.severity = s;
		this.id = id;
		this.message = ms;
	}
	@Override
	public String toString() {
		return "[level:"+severity+", type:"+type+", source:"+source+", id:"+id+", msg:{"+message+"}]";
	}

	public enum Type {
		ERROR,
		DEPRECATED_BEHAVIOR,
		UNDEFINED_BEHAVIOR,
		PORTABILITY,
		PERFORMANCE,
		MARKER,
		PUSH_GROUP,
		POP_GROUP,
		OTHER;
		public final String str;
		Type() {
			str = super.toString().toUpperCase();
		}
		@Override
		public final String toString() {
			return str;
		}
	    static final HashMap<String, GLMessage.Type> toEnum;
	    static {
	    	toEnum = new HashMap<String, GLMessage.Type>();
	    	for (GLMessage.Type t : Type.values()) {
	    		toEnum.put(t.str, t);
	    	}
	    }
	    public final static GLMessage.Type get(String str) {
	    	return toEnum.get(str);
	    }
	}
	public enum Source {
		API,
		WINDOW_SYSTEM,
		SHADER_COMPILER,
		THIRD_PARTY,
		APPLICATION,
		OTHER;
		public final String str;
		private Source() {
			str = super.toString().toUpperCase();
		}
	    static final HashMap<String, GLMessage.Source> toEnum;
	    static {
	    	toEnum = new HashMap<String, GLMessage.Source>();
	    	for (GLMessage.Source t : Source.values()) {
	    		toEnum.put(t.str, t);
	    	}
	    }
	    public final static GLMessage.Source get(String str) {
	    	return toEnum.get(str);
	    }
	}
	public enum Severity {
		HIGH, MEDIUM, LOW, NOTIFICATION;
		public final String str;
		private Severity() {
			str = super.toString().toUpperCase();
		}
	    static final HashMap<String, GLMessage.Severity> toEnum;
	    static {
	    	toEnum = new HashMap<String, GLMessage.Severity>();
	    	for (GLMessage.Severity t : Severity.values()) {
	    		toEnum.put(t.str, t);
	    	}
	    }
	    public final static GLMessage.Severity get(String str) {
	    	return toEnum.get(str);
	    }
	}
	
	public static class Builder {
		static {
			initLoadClass();
		}
		GLMessage.Type type;
		GLMessage.Severity severity;
		GLMessage.Source source;
		Function<Type, Boolean> typeFilter;
		Function<Severity, Boolean> severityFilter;
		Function<Source, Boolean> sourceFilter;
		
		String id;
		String message;
		int stage = 0;
		public Builder() {
			this(null, null, null);
		}
		public Builder(
				Function<Type, Boolean> typeFilter,
				Function<Severity, Boolean> severityFilter,
				Function<Source, Boolean> sourceFilter) {
			this.typeFilter = typeFilter;
			this.severityFilter = severityFilter;
			this.sourceFilter = sourceFilter;
		}
		
		public GLMessage add(String str) {
			str = str.trim();
			if (str.isEmpty()) return null;
			boolean b = runStage(str);
			if (b && stage >= 16) {
				stage = 0;
				GLMessage msg = new GLMessage(type, severity, source, id, message);
				if (filterMessage(msg)) {
					return msg;
				}
			} else if (!b) {
				LOGGER.warn("Failed to parse GLMessage line '{}' at stage {}", str, stage);
			}
			return null;
		}
		
		public void setTypeFilter(Function<Type, Boolean> typeFilter) {
			this.typeFilter = typeFilter;
		}
		public void setSeverityFilter(Function<Severity, Boolean> severityFilter) {
			this.severityFilter = severityFilter;
		}
		public void setSourceFilter(Function<Source, Boolean> sourceFilter) {
			this.sourceFilter = sourceFilter;
		}

		private boolean filterMessage(GLMessage msg) {
			if (sourceFilter!=null && !sourceFilter.apply(msg.source)) return false;
			if (typeFilter!=null && !typeFilter.apply(msg.type)) return false;
			if (severityFilter!=null && !severityFilter.apply(msg.severity)) return false;
			return true;
		}

		private boolean runStage(String str) {
			switch (stage) {
			case 0:
				return checkAndIncStage(str, GLMessage.HEADER);
			case 1:
				return checkAndIncStage(str, "ID");
			case 2:
				return checkAndIncStage(str, ":");
			case 3:
				id = str;
				stage++;
				return true;
			case 4:
				return checkAndIncStage(str, "Source");
			case 5:
				return checkAndIncStage(str, ":");
			case 6:
				source = Source.get(str);
				stage++;
				return true;
			case 7:
				return checkAndIncStage(str, "Type");
			case 8:
				return checkAndIncStage(str, ":");
			case 9:
				type = Type.get(str);
				stage++;
				return true;
			case 10:
				return checkAndIncStage(str, "Severity");
			case 11:
				return checkAndIncStage(str, ":");
			case 12:
				severity = Severity.get(str);
				stage++;
				return true;
			case 13:
				return checkAndIncStage(str, "Message");
			case 14:
				return checkAndIncStage(str, ":");
			case 15:
				message = str;
				stage++;
				return true;
			default: return false;
			}
		}

		private boolean checkAndIncStage(String str, String comp) {
			boolean result = str.equals(comp);
			if (result) stage++;
			return result;
		}
		
	};
}