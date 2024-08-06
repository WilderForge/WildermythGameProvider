package com.wildermods.provider.services;

import java.util.ServiceLoader;

public interface CrashLogService {

	public void logCrash(Throwable t);
	
	public static CrashLogService obtain(ClassLoader loader) {
		return ServiceLoader.load(CrashLogService.class, loader).findFirst().orElse(null);
	}
	
	public static CrashLogService obtain() {
		return ServiceLoader.load(CrashLogService.class).findFirst().orElse(null);
	}
	
}
