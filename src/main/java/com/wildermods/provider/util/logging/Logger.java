package com.wildermods.provider.util.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;

import org.apache.logging.log4j.core.LoggerContext; // <- must be core!
import net.fabricmc.loader.impl.util.log.LogCategory;

public class Logger implements ILogger {
	
	private static final LogLevel DEFAULT_LEVEL;
	static {
		Configuration logConfig = ((LoggerContext)LogManager.getContext(false)).getConfiguration();
		LogLevel defaultLevel = null;
		for(AppenderRef ref : logConfig.getRootLogger().getAppenderRefs()) {
			if(ref.getRef().equals("SysOut")) {
				defaultLevel = LogLevel.getLevel(ref.getLevel());
				break;
			}
		}
		Logger logger;
		if(defaultLevel == null) {
			DEFAULT_LEVEL = LogLevel.INFO;
			logger = new Logger("Logger");
			logger.warn("Could not find reference 'SysOut' in log4j configuration.", "init");
		}
		else {
			DEFAULT_LEVEL = defaultLevel;
			logger = new Logger("Logger");
		}
		LogLevel level = DEFAULT_LEVEL;
		if(level.ordinal() < LogLevel.INFO.ordinal()) {
			level = LogLevel.INFO;
		}
		logger.log(level, "Logging " + DEFAULT_LEVEL + " or higher");
	}
	private final org.apache.logging.log4j.Logger logger;
	
	public Logger(Class clazz) {
		this(clazz.getSimpleName(), DEFAULT_LEVEL);
	}
	
	public Logger(Class clazz, LogLevel minLevel) {
		this(clazz.getSimpleName(), minLevel);
	}
	
	public Logger(String name) {
		this(name, LogLevel.INFO);
	}
	
	public Logger(String name, LogLevel minLevel) {
		this.logger = LogManager.getLogger(name);
		Configurator.setLevel(logger, minLevel.toLog4j());
	}
	
	@Override
	public void log(LogLevel level, String s) {
		logger.log(level.toLog4j(), s);
	}
	
	public void log(LogLevel level, String s, String tag) {
		if(tag != null) {
			logger.log(level.toLog4j(), MarkerManager.getMarker(tag), s);
		}
		else {
			log(level, s);
		}
	}

	@Override
	public void catching(LogLevel level, Throwable t) {
		logger.catching(level.toLog4j(), t);
	}

	@Override
	public void catching(LogLevel level, Throwable t, String marker) {
		logger.log(level.toLog4j(), MarkerManager.getMarker(marker), t.getMessage(), t);
	}
	
	@Override
	public void log(long time, net.fabricmc.loader.impl.util.log.LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
		LogLevel l = LogLevel.getLevel(level);
		if(exc == null) {
			log(l, msg, category.name);
		}
		else {
			catching(l, exc);
		}
	}

	@Override
	public boolean shouldLog(LogLevel level) {
		return logger.getLevel().isLessSpecificThan(level.toLog4j());
	}

}
