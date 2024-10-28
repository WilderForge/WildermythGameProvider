package com.wildermods.provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;

public class ProviderSettings {
	
	private static final Gson GSON = new Gson();
	private @SerializedName("_WARNING_WARNING_") String NOTICE = "WARNING - Enabling coremods from the steam workshop does impose a security risk. Additionally, an automatic update to a coremod may break your game unexpectedly. Enabling workshop coremods is not recommended unless you know and understand the risks. Workshop coremod loading may be disabled entirely in future versions if it causes too many issues.";
	private boolean enableWorkshopCoremods = false;
	
	public ProviderSettings() {
		this(false);
	}
	
	public ProviderSettings(boolean enableWorkshopCoremods) {
		this.enableWorkshopCoremods = false;
	}
	
	public boolean workshopCoremodsEnabled() {
		return enableWorkshopCoremods;
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
