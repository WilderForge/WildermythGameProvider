package com.wildermods.provider.internal;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

public class InternalUtils {

	private InternalUtils() {throw new AssertionError();}
	
	public static Path asPath(URL url) {
		try {
			return Paths.get(url.toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Path getCodeSource(Class<?> clazz) {
		CodeSource source = clazz.getProtectionDomain().getCodeSource();
		if(source == null) {
			return null;
		}
		
		return asPath(source.getLocation());
	}
	
}
