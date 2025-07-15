package com.wildermods.provider.services;

import java.util.ServiceLoader;

public interface CrashLogService {
	
	public void logCrash(Throwable t);
	
	public default ClassLoader getGameClassloader() {
		return Internal.gameLoader;
	}
	
	public static CrashLogService obtain(ClassLoader loader) {
		return obtain(loader, false);
	}
	
	public static CrashLogService obtain(ClassLoader loader, boolean override) {
		if(Internal.service == null || override) {
			Internal.service = ServiceLoader.load(CrashLogService.class, loader).findFirst().orElse(null);
			Internal.gameLoader = loader;
		}
		return Internal.service;
	}
	
	public static CrashLogService obtain() {
		return obtain(false);
	}
	
	public static CrashLogService obtain(boolean override) {
		if(Internal.service == null || override) {
			Internal.service = ServiceLoader.load(CrashLogService.class).findFirst().orElse(null);
			Internal.gameLoader = CrashLogService.class.getClassLoader();
		}
		return Internal.service;
	}
	
	public static void override(CrashLogService service) {
		Internal.service = service;
	}
	
	public static class Internal {
		private static transient CrashLogService service;
		private static transient ClassLoader gameLoader;
	}
	
}
