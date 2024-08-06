package com.wildermods.provider.services;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.spongepowered.asm.mixin.Mixin;

import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.version.StringVersion;

public class WildermythGameProvider implements GameProvider {
	
	private Arguments arguments;
	private String entrypoint;
	private Path gameJar;
	private Path mixinJar;
	private boolean development;
	
	@Override
	public String getGameId() {
		return "wildermyth";
	}

	@Override
	public String getGameName() {
		return "Wildermyth";
	}

	@Override
	public String getRawGameVersion() {
		return getGameVersion().getFriendlyString();
	}

	@Override
	public String getNormalizedGameVersion() {
		return getRawGameVersion();
	}

	@Override
	public Collection<BuiltinMod> getBuiltinMods() {
		return Collections.emptyList();
	}

	@Override
	public String getEntrypoint() {
		return "com.worldwalkergames.legacy.LegacyDesktop";
	}

	@Override
	public Path getLaunchDirectory() {
		return Path.of(".");
	}

	@Override
	public boolean isObfuscated() {
		return false;
	}

	@Override
	public boolean requiresUrlClassLoader() {
		return false;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean locateGame(FabricLauncher launcher, String[] args) {
		if(System.getProperty(SystemProperties.DEVELOPMENT) == "true") {
			development = true;
		}
		this.arguments = new Arguments();
		arguments.parse(args);
		
		Map<Path, ZipFile> zipFiles = new HashMap<>();
		List<Path> lookupPaths = new ArrayList<>();
		try {
			String gameJarProperty = System.getProperty(SystemProperties.GAME_JAR_PATH);
			GameProviderHelper.FindResult result = null;
			if(gameJarProperty == null) {
				gameJarProperty = "./wildermyth.jar";
			}
			if(gameJarProperty != null) {
				Path path = Paths.get(gameJarProperty);
				if (!Files.exists(path)) {
					throw new RuntimeException("Game jar configured through " + SystemProperties.GAME_JAR_PATH + " system property doesn't exist");
				}
				lookupPaths.add(path);
				result = GameProviderHelper.findFirst(Collections.singletonList(path), zipFiles, true, getEntrypoint());
			}
			
			if(result == null) {
				return false;
			}
			
			entrypoint = result.name;
			gameJar = result.path;
			mixinJar = Path.of("./fabric/sponge-mixin-0.15.0+mixin.0.8.7.jar");
			
		}
		finally {
			for(ZipFile f : zipFiles.values()) {
				try {
					f.close();
				}
				catch(IOException e) {
					//ignore
				}
			}
		}
		return gameJar != null;
	}

	@Override
	public void initialize(FabricLauncher launcher) {

	}

	@Override
	public GameTransformer getEntrypointTransformer() {
		return new GameTransformer() {
			@Override
			public byte[] transform(String s) {
				return null;
			}
		};
	}

	@Override
	public void unlockClassPath(FabricLauncher launcher) {
		launcher.addToClassPath(gameJar);
		launcher.addToClassPath(mixinJar);
	}

	@Override
	public void launch(ClassLoader loader) {
		try {
			Class main = loader.loadClass(getEntrypoint());
			Method method = main.getMethod("main", String[].class);
			
			method.invoke(null, (Object)this.arguments.toArray());
		}
		catch(Throwable t) {
			throw new Error(t);
		}
	}

	@Override
	public Arguments getArguments() {
		return arguments;
	}

	@Override
	public String[] getLaunchArguments(boolean sanitize) {
		return this.getArguments().toArray();
	}

	private StringVersion getGameVersion() {
		File versionFile = new File("./version.txt");
		try {
			if(versionFile.exists()) {
				return new StringVersion(Files.readString(versionFile.toPath()).split(" ")[0]);
			}
		}
		catch(IOException e) {
			throw new Error("Could not detect wildermyth version");
		}
		throw new Error("Could not detect wildermyth version. Missing versions.txt?");
	}
	
}
