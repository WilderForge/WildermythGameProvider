package com.wildermods.provider.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;

public class DependencyFirstClassLoader extends URLClassLoader {

	private ClassLoader grandparent; //we don't want anyone to impersonate system classes
	
	public DependencyFirstClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		this.grandparent = parent.getParent();
		if(grandparent == null) {
			try {
				grandparent.hashCode(); //intentionally throw a NPE with detailed message
			}
			catch(Throwable t) {
				throw new AssertionError(t);
			}
		}
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> loadedClass = findLoadedClass(name);
		if(loadedClass == null) {
			try {
				loadedClass = grandparent.loadClass(name);
			} catch (ClassNotFoundException e) {
				try {
					if(loadedClass == null) {
						loadedClass = findClass(name);
					}
				} catch (ClassNotFoundException e2) {
					loadedClass = super.loadClass(name);
				}
			}
		}
		
		if(resolve) {
			resolveClass(loadedClass);
		}
		
		return loadedClass;
	}
	
	
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		LinkedHashSet<URL> resources = new LinkedHashSet<URL>();
		
		Collections.addAll(Collections.list(grandparent.getResources(name)));
		Collections.addAll(Collections.list(findResources(name)));
		Collections.addAll(Collections.list(getParent().getResources(name)));
		
		return Collections.enumeration(resources);
	}
	
	@Override
	public URL getResource(String name) {
		URL res = null;
		res = grandparent.getResource(name);
		if(res == null) {
			res = findResource(name);
		}
		if(res == null) {
			res = getParent().getResource(name);
		}
		
		return res;
	}
}
