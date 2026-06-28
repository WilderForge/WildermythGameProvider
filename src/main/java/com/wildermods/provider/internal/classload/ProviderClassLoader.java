package com.wildermods.provider.internal.classload;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import net.fabricmc.loader.impl.launch.knot.URLLoader;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public class ProviderClassLoader extends StrippingClassLoader implements URLLoader {

	static {
		registerAsParallelCapable();
	}
	
	public ProviderClassLoader() {
	    super(new URL[] {}, new DummyClassLoader());
	    if (System.getProperty("provider.dev.classpath") != null) {
	        for (String s : System.getProperty("provider.dev.classpath").split(File.pathSeparator)) {
	            try {
	                File f = new File(s).getAbsoluteFile().getCanonicalFile();
	                if (!f.exists()) {
	                    throw new AssertionError(f + " does not exist");
	                }
	                URL url;
	                if (f.isDirectory()) {
	                    // Directory URL must end with '/'
	                    url = f.toURI().toURL();
	                    String ext = url.toExternalForm();
	                    if (!ext.endsWith("/")) {
	                        url = new URL(ext + "/");
	                    }
	                } else {
	                    // JAR file – must end with "!/"
	                    URL fileURL = f.toURI().toURL();
	                    url = new URL("jar:" + fileURL.toExternalForm() + "!/");
	                }
	                addURL(url);
	            } catch (IOException e) {
	                Log.warn(LogCategory.create("DEV_CLASSPATH"), "Not a URL: " + s);
	                Log.warn(LogCategory.create("DEV_CLASSPATH"), "Not a URL: " + s, e);
	            }
	        }
	    }
	}
	
	public URL findResource(String resource) {
		return super.findResource(resource);
	}
	
	public Enumeration<URL> findResources(String name) {
		return super.findResources(name);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}

	@Override
	public URL[] getURLs() {
		return super.getURLs();
	}
	
}
