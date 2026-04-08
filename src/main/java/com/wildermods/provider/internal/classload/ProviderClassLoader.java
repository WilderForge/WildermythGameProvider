package com.wildermods.provider.internal.classload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import net.fabricmc.loader.impl.launch.knot.URLLoader;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public class ProviderClassLoader extends URLClassLoader implements URLLoader {

	static {
		registerAsParallelCapable();
		System.err.println("Provider classloader loaded by: " + ProviderClassLoader.class.getClassLoader());
	}
	
	public ProviderClassLoader() {
		super(new URL[] {}, new DummyClassLoader(), new ProviderJarURLStreamHandlerFactory());
		if(System.getProperty("provider.dev.classpath") != null) {
			for(String s : System.getProperty("provider.dev.classpath").split(File.pathSeparator)) {
				try {
					File f = new File(s).getAbsoluteFile().getCanonicalFile();
					if(!f.exists()) {
						throw new AssertionError(f + " does not exist");
					}
					addURL(new File(s).getAbsoluteFile().toURI().toURL());
				} catch (IOException e) {
					Log.warn(LogCategory.create("DEV_CLASSPATH"), "Not a URL: " + s, e);
				}
			}
		}
	}
	
	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}
	
}
