package com.wildermods.provider.services;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipFile;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.util.asm.ASM;

import com.wildermods.provider.internal.InternalUtils;
import com.wildermods.provider.patch.LegacyPatch;

import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.version.StringVersion;

public class WildermythGameProvider implements GameProvider {

	private static final String[] ALLOWED_EARLY_CLASS_PREFIXES = {"com.wildermods.provider.services.CrashLogService", "net.fabricmc.loader.impl.util.log."};
	private static final String[] ENTRYPOINTS = new String[]{"com.worldwalkergames.legacy.LegacyDesktop"};
	private static final String[] ASM_ = new String[] {"org.objectweb.asm.Opcodes"};
	private static final String[] MIXIN = new String[] {"org.spongepowered.asm.mixin.Mixin"};
	private static final String[] LOADER = new String[] {"net.fabricmc.loader.impl.launch.FabricLauncher"};
	private static final HashSet<String> SENSITIVE_ARGS = new HashSet<String>(Arrays.asList(new String[] {}));
	
	private Arguments arguments;
	private String entrypoint;
	private Path launchDir;
	private Path libDir;
	private Path gameJar;
	private Path asmJar;
	private Path mixinJar;
	private boolean development = false;
	private final List<Path> miscGameLibraries = new ArrayList<>();
	
	private Object crashLogService;
	
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
				new BuiltinModMetadata.Builder("asm", ASM.getApiVersionString())
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
			System.out.println("Built in mod " + mod.metadata.getName() + " version " + mod.metadata.getVersion() + " defined");
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

	@SuppressWarnings("unchecked")
	@Override
	public boolean locateGame(FabricLauncher launcher, String[] args) {
		System.out.println("BEGIN INITIALIZATION");
		System.setProperty("fabric.debug.disableClassPathIsolation", "");
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
				gameJarProperty = "./wildermyth.jar";
			}
			if(gameJarProperty != null) {
				Path path = Paths.get(gameJarProperty);
				if (!Files.exists(path)) {
					throw new RuntimeException("Game jar configured through " + SystemProperties.GAME_JAR_PATH + " system property doesn't exist");
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

	private void locateFilesystemDependencies() {
		File lib = libDir.toFile();
		for(File dep : lib.listFiles()) {
			if(dep.getName().endsWith(".jar")) {
				miscGameLibraries.add(dep.toPath());
				System.out.println("Adding " + dep);
			}
			else {
				System.out.println("Skipping non-jar dependency " + dep);
			}
		}
		
		for(File dep : launchDir.toFile().listFiles()) {
			if(dep.getName().endsWith(".jar")) {
				if(dep.getName().equals("wildermyth.jar")) {
					System.out.println("Skipping wildermyth.jar");
				}
				else if (dep.getName().contains("wilderforge-")) {
					System.out.println("Skipping " + dep.getName() + " because we are in a development environment");
				}
				else if(dep.getPath().contains("fabric/") || dep.getName().startsWith("fabric-")) {
					System.out.println("Skipping fabric dep " + dep.getName());
				}
				else if(dep.getName().startsWith("provider")) {
					System.out.println("Skipping game provider " + dep.getName());
				}
				else if (dep.getName().endsWith(".jar")) {
					System.out.println("Adding " + dep.toPath());
					miscGameLibraries.add(dep.toPath());
				}
			}
			else {
				System.out.println("Skipping non-jar file " + dep);
			}
		}

	}

	@Override
	public void initialize(FabricLauncher launcher) {
		
		try {
			Class<?> crashServiceClass = Thread.currentThread().getContextClassLoader().loadClass("com.wildermods.provider.services.CrashLogService");
			Path crashService = InternalUtils.getCodeSource(crashServiceClass);
			
			Class<?> logHandlerClass = Thread.currentThread().getContextClassLoader().loadClass("net.fabricmc.loader.impl.util.log.LogHandler");
			Path logHandler = InternalUtils.getCodeSource(logHandlerClass);
			
			launcher.addToClassPath(crashService, ALLOWED_EARLY_CLASS_PREFIXES);
			launcher.setAllowedPrefixes(logHandler, ALLOWED_EARLY_CLASS_PREFIXES);
			
			System.out.println("Crash service code source: " + crashService);
			
			launcher.loadIntoTarget("com.wildermods.provider.services.CrashLogService");
			
			System.out.println("Target CL: " + launcher.getTargetClassLoader());
			System.out.println("Context CL: " + Thread.currentThread().getContextClassLoader());
			System.out.println("Provider CL:" + getClass().getClassLoader());
			System.out.println("Provider Parent CL:" + getClass().getClassLoader().getParent());
			
		} catch (SecurityException | ClassNotFoundException | IllegalArgumentException e1) {
			System.out.println("Target: " + launcher.getTargetClassLoader());
			System.out.println("Context: " + Thread.currentThread().getContextClassLoader());
			System.out.println("Provider CL:" + getClass().getClassLoader());
			System.out.println("Provider Parent CL:" + getClass().getClassLoader().getParent());
			e1.printStackTrace();
		}
		
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
		String targetClass = entrypoint;
		
		crashLogService = null;

		try {
			System.out.println(getClass().getClassLoader());
			Class<?> c = Class.forName("com.wildermods.provider.services.CrashLogService", true, loader);
			Method method = c.getDeclaredMethod("obtain", ClassLoader.class);
			crashLogService = method.invoke(null, loader);
		}
		catch(Throwable t) {
			throw new Error(t);
		}
		
		System.err.println("Crash log service is: " + crashLogService);
		
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
			Method logCrash = crashLogService.getClass().getDeclaredMethod("logCrash", Throwable.class);
			logCrash.invoke(crashLogService, t);
			
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
		System.out.println("Launch directory is " + launchDir);
		libDir = launchDir.resolve(Path.of("./lib"));
	}
	
	private static Path getLaunchDirectory(Arguments arguments) {
		return Paths.get(arguments.getOrDefault("gameDir", "."));
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
