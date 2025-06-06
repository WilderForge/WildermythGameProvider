package com.wildermods.provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

public class ProviderSettings {
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	@Deprecated
	private boolean enableWorkshopCoremods = false;
	
	public ProviderSettings() {
		this(false);
	}
	
	public ProviderSettings(boolean enableWorkshopCoremods) {
		this.enableWorkshopCoremods = false;
	}
	
	public boolean workshopCoremodsEnabled() {
		return false;
	}
	
	public static ProviderSettings fromJson(Path file) throws JsonIOException, JsonSyntaxException, IOException {
		if(Files.exists(file)) {
			JsonReader reader = new JsonReader(Files.newBufferedReader(file));
			ProviderSettings settings = GSON.fromJson(reader, ProviderSettings.class);
			reader.close();
			return settings;
		}
		else {
			ProviderSettings settings = new ProviderSettings();
			System.out.println("Settings file doesn't exist, creating settings file at " + file);
			String json = GSON.toJson(settings);
			Files.write(file, json.getBytes(), StandardOpenOption.CREATE_NEW);
			return settings;
		}
	}
	
}
