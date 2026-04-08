package com.wildermods.provider.internal;

import java.net.URL;
import java.nio.file.Path;

import org.apache.logging.log4j.core.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.util.CheckClassAdapter;
import org.spongepowered.asm.launch.MixinBootstrap;

import com.wildermods.provider.WildermythGameProvider;

import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweaker;
import net.fabricmc.loader.impl.util.UrlConversionException;
import net.fabricmc.loader.impl.util.UrlUtil;

public enum AppClassloaderLibrary {
	FABRIC_LOADER(UrlUtil.LOADER_CODE_SOURCE),
	SPONGE_MIXIN(MixinBootstrap.class),
	CLASS_TWEAKER(ClassTweaker.class),
	ASM(ClassReader.class),
	ASM_ANALYSIS(Analyzer.class),
	ASM_COMMONS(Remapper.class),
	ASM_TREE(ClassNode.class),
	ASM_UTIL(CheckClassAdapter.class),
	PROVIDER(WildermythGameProvider.class),
	LOG4J(Logger.class);
	;
	
	final Path path;
	
	AppClassloaderLibrary(Class<?> cls) {
		this(UrlUtil.getCodeSource(cls));
	}
	
	AppClassloaderLibrary(Path path) {
		if (path == null) {
			throw new RuntimeException("Missing appcl library " + name());
		}
		this.path = path;
	}
	
	AppClassloaderLibrary(String file) {
		URL url = getClass().getClassLoader().getResource(file);
		
		try {
			this.path = url != null ? UrlUtil.getCodeSource(url, file) : null;
		}
		catch(UrlConversionException e) {
			throw new RuntimeException(e);
		}
	}
}
