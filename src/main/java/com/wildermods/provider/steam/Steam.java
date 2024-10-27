package com.wildermods.provider.steam;

import java.nio.file.Path;

import com.wildermods.provider.loader.util.OS;

public class Steam {
	
	public static final Path getSteamDir() {
		return OS.getSteamDefaultDirectory().resolve("Steam");
	}
	
	public static class Workshop {
	
		public static final Path getWorkshopDir() {
			return getSteamDir().resolve("steamapps").resolve("workshop");
		}
		
		public static final Path getModDir() {
			return getWorkshopDir().resolve("content").resolve("763890");
		}
	}
	
}
