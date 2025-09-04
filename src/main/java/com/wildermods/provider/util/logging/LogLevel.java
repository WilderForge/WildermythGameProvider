package com.wildermods.provider.util.logging;

import org.apache.logging.log4j.Level;

public enum LogLevel {
		TRACE(Level.TRACE, net.fabricmc.loader.impl.util.log.LogLevel.TRACE),
		DEBUG(Level.DEBUG, net.fabricmc.loader.impl.util.log.LogLevel.DEBUG),
		INFO(Level.INFO, net.fabricmc.loader.impl.util.log.LogLevel.INFO),
		WARN(Level.WARN, net.fabricmc.loader.impl.util.log.LogLevel.WARN),
		ERROR(Level.ERROR, net.fabricmc.loader.impl.util.log.LogLevel.ERROR),
		FATAL(Level.FATAL, net.fabricmc.loader.impl.util.log.LogLevel.ERROR);
		
		private final Level log4jLevel;
		private final net.fabricmc.loader.impl.util.log.LogLevel fabricLevel;
		
		LogLevel(Level log4jLevel, net.fabricmc.loader.impl.util.log.LogLevel fabricLevel) {
			this.log4jLevel = log4jLevel;
			this.fabricLevel = fabricLevel;
		}
		
		public Level toLog4j() {
			return log4jLevel;
		}
		
		public net.fabricmc.loader.impl.util.log.LogLevel toFabric() {
			return fabricLevel;
		}
		
		public static LogLevel getLevel(net.fabricmc.loader.impl.util.log.LogLevel level) {
			switch(level) {
				case TRACE:
					return TRACE;
				case DEBUG:
					return DEBUG;
				default:
				case INFO:
					return INFO;
				case WARN:
					return WARN;
				case ERROR:
					return ERROR;
			}
		}
		
		public static net.fabricmc.loader.impl.util.log.LogLevel toFabricLevel(LogLevel level) {
			switch(level) {
				case TRACE:
					return net.fabricmc.loader.impl.util.log.LogLevel.TRACE;
				case DEBUG:
					return net.fabricmc.loader.impl.util.log.LogLevel.DEBUG;
				default:
				case INFO:
					return net.fabricmc.loader.impl.util.log.LogLevel.INFO;
				case WARN:
					return net.fabricmc.loader.impl.util.log.LogLevel.WARN;
				case ERROR:
				case FATAL:
					return net.fabricmc.loader.impl.util.log.LogLevel.ERROR;
			}
		}
		
		public static LogLevel getLevel(org.apache.logging.log4j.Level level) {
			switch(level.getStandardLevel()) {
				case ALL:
				case TRACE:
					return LogLevel.TRACE;
				case DEBUG:
					return LogLevel.DEBUG;
				default:
				case INFO:
					return LogLevel.INFO;
				case WARN:
					return LogLevel.WARN;
				case ERROR:
					return LogLevel.ERROR;
				case FATAL:
					return LogLevel.FATAL;
			
			}
		}
		
		public static LogLevel getLevel(int level) {
			switch (level) {
				case 0:
					return TRACE;
				case 1:
					return DEBUG;
				default:
				case 2:
					return INFO;
				case 3:
					return WARN;
				case 4:
					return ERROR;
				case 5:
					return FATAL;
			}
		}
	}