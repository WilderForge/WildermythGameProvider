package com.wildermods.provider;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.zip.ZipFile;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.util.asm.ASM;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.wildermods.provider.patch.LegacyPatch;
import com.wildermods.provider.services.CrashLogService;
import com.wildermods.provider.steam.Steam;
import com.wildermods.provider.util.logging.Logger;

import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.log.LogHandler;
import net.fabricmc.loader.impl.util.log.LogLevel;
import net.fabricmc.loader.impl.util.version.StringVersion;
import static net.fabricmc.loader.impl.util.SystemProperties.ADD_MODS;;

public class WildermythGameProvider implements GameProvider {

	private static final String[] ENTRYPOINTS = new String[]{"com.worldwalkergames.legacy.LegacyDesktop"};
	private static final String[] ASM_ = new String[] {"org.objectweb.asm.Opcodes"};
	private static final String[] MIXIN = new String[] {"org.spongepowered.asm.mixin.Mixin"};
	private static final HashSet<String> SENSITIVE_ARGS = new HashSet<String>(Arrays.asList(new String[] {}));
	private static final Path PROVIDER_SETTINGS_FILE = Path.of(".").normalize().resolve("providerSettings.json");
	private static final ProviderSettings SETTINGS;
	static {
		ProviderSettings settings;
		try {
			settings = ProviderSettings.fromJson(PROVIDER_SETTINGS_FILE);
		} catch (JsonIOException | JsonSyntaxException | IOException e) {
			Log.error(LogCategory.GAME_PROVIDER, "Could not load provider settings. Using default settings. ", e);
			settings = new ProviderSettings();
		}
		SETTINGS = settings;
	}
	
	private Arguments arguments;
	private String entrypoint;
	private Path launchDir;
	private Path libDir;
	private Path gameJar;
	private Path asmJar;
	private Path mixinJar;
	private boolean development = false;
	private final List<Path> miscGameLibraries = new ArrayList<>();
	
	private CrashLogService crashLogService;
	
	private static final GameTransformer TRANSFORMER = new GameTransformer(new LegacyPatch());
	
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
		
		HashMap<String, String> wildermythContactInformation = new HashMap<>();
		wildermythContactInformation.put("homepage", "https://wildermyth.com/");
		wildermythContactInformation.put("issues", "https://discord.gg/wildermyth");
		wildermythContactInformation.put("license", "https://wildermyth.com/terms.php");
		
		HashMap<String, String> asmContactInformation = new HashMap<>();
		asmContactInformation.put("homepage", "https://asm.ow2.io/index.html");
		asmContactInformation.put("issues", "https://gitlab.ow2.org/asm/asm/-/issues");
		asmContactInformation.put("sources", "https://gitlab.ow2.org/asm/asm");
		asmContactInformation.put("license", "https://asm.ow2.io/license.html");
		
		HashMap<String, String> fabricMixinContactInformation = new HashMap<>();
		
		fabricMixinContactInformation.put("homepage", "https://github.com/FabricMC/Mixin");
		fabricMixinContactInformation.put("issues", "https://github.com/FabricMC/Mixin/issues");
		fabricMixinContactInformation.put("sources", "https://github.com/FabricMC/Mixin");
		fabricMixinContactInformation.put("license", "https://github.com/FabricMC/Mixin/blob/main/LICENSE.txt");
		
		HashMap<String, String> mixinContactInformation = new HashMap<>();
		mixinContactInformation.put("homepage", "https://github.com/SpongePowered/Mixin");
		mixinContactInformation.put("issues", "https://github.com/SpongePowered/Mixin/issues");
		mixinContactInformation.put("sources", "https://github.com/SpongePowered/Mixin");
		mixinContactInformation.put("license", "https://github.com/SpongePowered/Mixin/blob/master/LICENSE.txt");
		
		BuiltinModMetadata.Builder wildermythMetaData = 
				new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
				.setName(getGameName())
				.addAuthor("Worldwalker Games, LLC.", wildermythContactInformation)
				.setContact(new ContactInformationImpl(wildermythContactInformation))
				.setDescription("A procedural storytelling RPG where tactical combat and story decisions will alter your world and reshape your cast of characters.");
		
		BuiltinModMetadata.Builder asmMetaData = 
				new BuiltinModMetadata.Builder("asm", Opcodes.class.getPackage().getImplementationVersion())
				.setName(ASM.getVersionString())
				.addAuthor("INRIA, France Telecom", wildermythContactInformation)
				.setContact(new ContactInformationImpl(asmContactInformation))
				.setDescription("ASM is an all purpose Java bytecode manipulation and analysis framework. It can be used to modify existing classes or to dynamically generate classes, directly in binary form."
						+ "\n\n"
						+ "Currently supports " + ASM.getClassVersionString())
				.addLicense("https://asm.ow2.io/license.html");
		
		BuiltinModMetadata.Builder mixinMetaData = 
				new BuiltinModMetadata.Builder("mixin", MixinBootstrap.VERSION)
				.setName("Spongepowered Mixin (Fabric Fork)")
				.addAuthor("Mumfrey", mixinContactInformation)
				.addAuthor("FabricMC Team", fabricMixinContactInformation)
				.addAuthor("Spongepowered Team", mixinContactInformation)
				.setContact(new ContactInformationImpl(fabricMixinContactInformation))
				.setDescription("""
						FabricMC's fork of Mixin, A bytecode weaving framework for Java using ASM.
						
						Original by Mumfrey.
						""")
				.addLicense("https://github.com/SpongePowered/Mixin/blob/master/LICENSE.txt");
		
		ArrayList<BuiltinMod> builtinMods = new ArrayList<>();
		builtinMods.add(new BuiltinMod(List.of(gameJar), wildermythMetaData.build()));
		
		if(asmJar != null) {
			builtinMods.add(new BuiltinMod(List.of(asmJar), asmMetaData.build()));
		}
		
		if(mixinJar != null) {
			builtinMods.add(new BuiltinMod(List.of(mixinJar), mixinMetaData.build()));
		}
		
		for(BuiltinMod mod : builtinMods) {
			Log.info(LogCategory.DISCOVERY, "Built in mod " + mod.metadata.getName() + " version " + mod.metadata.getVersion() + " defined");
		}
		
		
		return Collections.unmodifiableList(builtinMods);
	}

	@Override
	public String getEntrypoint() {
		return entrypoint;
	}

	@Override
	public Path getLaunchDirectory() {
		if (arguments == null) {
			return Paths.get(".");
		}
		
		return getLaunchDirectory(arguments);
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
		System.setProperty("fabric.debug.disableClassPathIsolation", "");
		try {
			if("true".equals(System.getProperty("steam.workshop.coremods")) || SETTINGS.workshopCoremodsEnabled()) {
				gatherWorkshopCoremods();
			}
			else {
				Log.error(LogCategory.DISCOVERY, "WORKSHOP COREMODS DISABLED");
			}
		} catch (IOException e) {
			throw new Error(e);
		}
		

		
		
		this.arguments = new Arguments();
		arguments.parse(args);
		Map<Path, ZipFile> zipFiles = new HashMap<>();
		List<Path> lookupPaths = new ArrayList<>();
		
		if(System.getProperty(SystemProperties.DEVELOPMENT) == "true") {
			development = true;
		}
		
		try {
			String gameJarProperty = System.getProperty(SystemProperties.GAME_JAR_PATH);
			GameProviderHelper.FindResult result = null;
			if(gameJarProperty == null) {
				gameJarProperty = getLaunchDirectory().resolve("wildermyth.jar").toString();
			}
			if(gameJarProperty != null) {
				Path path = Paths.get(gameJarProperty);
				if (!Files.exists(path)) {
					throw new RuntimeException("Game jar configured through " + SystemProperties.GAME_JAR_PATH + " system property doesn't exist (" + path.normalize().toAbsolutePath() + ")");
				}
				lookupPaths.add(path);
				result = GameProviderHelper.findFirst(Collections.singletonList(path), zipFiles, true, ENTRYPOINTS);
			}
			
			if(result == null) {
				return false;
			}
			
			entrypoint = result.name;
			gameJar = result.path;
			try {
				asmJar = GameProviderHelper.findFirst(Collections.singletonList(Paths.get(Opcodes.class.getProtectionDomain().getCodeSource().getLocation().toURI())), zipFiles, true, ASM_).path;
			} catch (URISyntaxException e) {
				asmJar = null;
			}
			
			try {
				mixinJar = GameProviderHelper.findFirst(Collections.singletonList(Paths.get(Mixin.class.getProtectionDomain().getCodeSource().getLocation().toURI())), zipFiles, true, MIXIN).path;
			} catch (URISyntaxException e) {
				asmJar = null;
			}
			
			
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
		
		processArgumentMap(arguments);
		
		
		
		locateFilesystemDependencies();
		
		return true;
		
	}
	
	private void initializeLogging(ClassLoader loader) {
		ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
		
		crashLogService = null;

		try {
			crashLogService = CrashLogService.obtain();
		}
		catch(Throwable t) {
			throw new Error(t);
		}
		
		LogHandler logHandler = null;
		try {
			//Constructor<? extends LogHandler> loggerClass = (Constructor<? extends LogHandler>) loader.loadClass("com.wildermods.provider.util.logging.Logger").getConstructor(String.class);
			//logHandler = loggerClass.newInstance("Fabric Loader");
			Log.init(new Logger("Fabric Loader"));
			Log.log(LogLevel.ERROR, LogCategory.GAME_PATCH, "Logging Initialized");
			
		} catch (Throwable t) {
			Log.error(LogCategory.GAME_PROVIDER, "Crash log service could not be defined", t);
		}

		Log.log(LogLevel.ERROR, LogCategory.GAME_PROVIDER, "Crash log service is: " + crashLogService);
		
		Thread.currentThread().setContextClassLoader(prevCL);
	}

	private void locateFilesystemDependencies() {
		File lib = libDir.toFile();
		for(File dep : lib.listFiles()) {
			if(dep.getName().endsWith(".jar")) {
				miscGameLibraries.add(dep.toPath());
				Log.log(LogLevel.TRACE, LogCategory.GAME_PROVIDER, "Adding " + dep);
			}
			else {
				Log.log(LogLevel.TRACE, LogCategory.GAME_PROVIDER, "Skipping non-jar dependency " + dep);
			}
		}
		
		for(File dep : launchDir.toFile().listFiles()) {
			if(dep.getName().endsWith(".jar")) {
				if(dep.getName().equals("wildermyth.jar")) {
					Log.log(LogLevel.TRACE, LogCategory.GAME_PROVIDER,"Skipping wildermyth.jar");
				}
				else if (dep.getName().contains("wilderforge-")) {
					Log.log(LogLevel.WARN, LogCategory.GAME_PROVIDER, "Skipping " + dep.getName() + " because we are in a development environment");
				}
				else if(dep.getPath().contains("fabric/") || dep.getName().startsWith("fabric-")) {
					Log.log(LogLevel.TRACE, LogCategory.GAME_PROVIDER, "Skipping fabric dep " + dep.getName());
				}
				else if(dep.getName().startsWith("provider")) {
					Log.log(LogLevel.TRACE, LogCategory.GAME_PROVIDER, "Skipping game provider " + dep.getName());
				}
				else if (dep.getName().endsWith(".jar")) {
					Log.log(LogLevel.TRACE, LogCategory.GAME_PROVIDER, "Adding " + dep.toPath());
					miscGameLibraries.add(dep.toPath());
				}
			}
			else {
				Log.log(LogLevel.TRACE, LogCategory.GAME_PROVIDER, "Skipping non-jar file " + dep);
			}
		}

	}

	@Override
	public void initialize(FabricLauncher launcher) {
		
		initializeLogging(launcher.getTargetClassLoader());
		
		TRANSFORMER.locateEntrypoints(launcher, List.of(gameJar));
	}

	@Override
	public GameTransformer getEntrypointTransformer() {
		return TRANSFORMER;
	}

	@Override
	public void unlockClassPath(FabricLauncher launcher) {
		launcher.addToClassPath(gameJar);
		
		for(Path lib : miscGameLibraries) {
			launcher.addToClassPath(lib);
		}
		
	}

	@Override
	public void launch(ClassLoader loader) {
		
		initializeLogging(loader);
		String targetClass = entrypoint;
		
		
		try {
			Class<?> c = loader.loadClass(targetClass);
			Method m = c.getMethod("main", String[].class);
			m.invoke(null, (Object) arguments.toArray());
		}
		catch(InvocationTargetException e) {
			throw new FormattedException("Wildermyth has crashed!", e.getCause());
		}
		catch(ReflectiveOperationException e) {
			throw new FormattedException("Failed to start Wildermyth", e);
		}
		
	}
	
	@Override
	public boolean displayCrash(Throwable t, String context) {
		try {
			if(crashLogService != null) {
				Method logCrash = crashLogService.getClass().getDeclaredMethod("logCrash", Throwable.class);
				logCrash.invoke(crashLogService, t);
			}
		} catch (Throwable t2) {
			throw new Error(t2);
		}
		return false;
	}

	@Override
	public Arguments getArguments() {
		return arguments;
	}

	@Override
	public String[] getLaunchArguments(boolean sanitize) {
		if (arguments == null) return new String[0];

		String[] ret = arguments.toArray();
		if (!sanitize) return ret;

		int writeIdx = 0;

		for (int i = 0; i < ret.length; i++) {
			String arg = ret[i];

			if (i + 1 < ret.length
					&& arg.startsWith("--")
					&& SENSITIVE_ARGS.contains(arg.substring(2).toLowerCase(Locale.ENGLISH))) {
				i++; // skip value
			} else {
				ret[writeIdx++] = arg;
			}
		}

		if (writeIdx < ret.length) ret = Arrays.copyOf(ret, writeIdx);

		return ret;
	}
	
	private void processArgumentMap(Arguments arguments) {
		if (!arguments.containsKey("gameDir")) {
			arguments.put("gameDir", getLaunchDirectory(arguments).toAbsolutePath().normalize().toString());
		}
		
		launchDir = Path.of(arguments.get("gameDir"));
		Log.info(LogCategory.ENTRYPOINT, "Launch directory is " + launchDir);
		
		if(!arguments.containsKey("libDir")) {
			libDir = launchDir.resolve("lib");
		}
		else {
			libDir = Path.of(arguments.get("libDir"));
		}

		Log.info(LogCategory.DISCOVERY, "Lib directory is " + libDir);
		
		if(!Files.exists(libDir)) {
			try {
				Files.createDirectories(libDir);
				Log.trace(LogCategory.GAME_PROVIDER, "Created " + libDir);
			} catch (IOException e) {
				throw new IOError(e);
			}
		}
		
	}
	
	private static Path getLaunchDirectory(Arguments arguments) {
		return Paths.get(arguments.getOrDefault("gameDir", "."));
	}
	
	private static final void gatherWorkshopCoremods() throws IOException {
		StringJoiner addedMods = new StringJoiner(File.pathSeparator);
		
		if(System.getProperty(ADD_MODS) != null) {
			addedMods.add(System.getProperty(ADD_MODS));
		}
		
		if(Files.exists(Steam.Workshop.getModDir())) {
		
			Files.walkFileTree(Steam.Workshop.getModDir(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (file.toString().endsWith(".jar")) {
						Log.warn(LogCategory.DISCOVERY, "Adding workshop coremod at " + file);
						addedMods.add(file.normalize().toAbsolutePath().toString());  // Add the first JAR found
						return FileVisitResult.SKIP_SIBLINGS;  // Skip other files in the same mod folder
					}
					return FileVisitResult.CONTINUE;
				}
			});
			
		}
		else {
			Log.warn(LogCategory.DISCOVERY, "Could not locate worshop mod directory at " + Steam.Workshop.getModDir());
		}
		
		System.setProperty(ADD_MODS, addedMods.toString());

	}
	
	private StringVersion getGameVersion() {
		Path versionFile = getLaunchDirectory().resolve("version.txt");
		try {
			if(Files.exists(versionFile)) {
				return new StringVersion(Files.readString(versionFile).split(" ")[0]);
			}
		}
		catch(IOException e) {
			throw new Error("Could not detect wildermyth version");
		}
		throw new Error("Could not detect wildermyth version. Missing versions.txt?");
	}

}
