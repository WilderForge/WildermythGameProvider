package com.wildermods.provider.internal.classload;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class DummyClassLoader extends ClassLoader {

	static {
		registerAsParallelCapable();
	}
	
	@Override
	public URL getResource(String name) {
		return null;
	}
	
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		throw new ClassNotFoundException(name);
	}
	
	private static final Enumeration<URL> NULL_ENUMERATION = new Enumeration<URL>() {
		@Override
		public boolean hasMoreElements() {
			return false;
		}

		@Override
		public URL nextElement() {
			return null;
		}
	};
	
	@Override
	public Enumeration<URL> getResources(String var1) throws IOException {
		return NULL_ENUMERATION;
	}
	
}
