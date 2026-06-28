package com.wildermods.provider.internal.classload;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public final class ProviderJarURLStreamHandlerFactory implements URLStreamHandlerFactory {

	private static final URLStreamHandler DEFAULT_HANDLER;
	private static final Method OPEN_CONNECTION;
	static {
		try {
			Class<?> defaultHandlerClass = Class.forName("sun.net.www.protocol.jar.Handler");
			Constructor<?> c = defaultHandlerClass.getConstructor();
			c.setAccessible(true);
			DEFAULT_HANDLER = (URLStreamHandler) c.newInstance();
			OPEN_CONNECTION = URLStreamHandler.class.getDeclaredMethod("openConnection", URL.class);
			OPEN_CONNECTION.setAccessible(true);
		} catch (Throwable t) {
			throw new Error(t);
		}
	}
	
	@Override
	public WildermythProviderJarURLStreamHandler createURLStreamHandler(String protocol) {
		if("jar".equals(protocol)) {
			//System.err.println("[ProviderJarURLStreamHandlerFactory] createURLStreamHandler called for protocol: " + protocol);
			return new WildermythProviderJarURLStreamHandler(protocol);
		}
		else {
			return null;
		}
	}
	
	static final class WildermythProviderJarURLStreamHandler extends URLStreamHandler {

	    WildermythProviderJarURLStreamHandler(String protocol) {
	        //System.err.println("[WildermythProviderJarURLStreamHandler] constructor, protocol: " + protocol);
	    }

	    @Override
	    protected URLConnection openConnection(URL url) throws IOException {
	        //System.err.println("[WildermythProviderJarURLStreamHandler] openConnection called for URL: " + url);
	        // Delegate to the default handler to create the connection,
	        // then wrap it with our custom connection that strips Class-Path.
	        JarURLConnection defaultConn = null;
	        URLConnection connection = null;
	        try {
	            defaultConn = (JarURLConnection) OPEN_CONNECTION.invoke(DEFAULT_HANDLER, url);
	            //System.err.println("[WildermythProviderJarURLStreamHandler] defaultConn obtained: " + defaultConn.getClass().getName());
	        } catch (Throwable t) {
	            //System.err.println("[WildermythProviderJarURLStreamHandler] Failed to get default connection for: " + url);
	            t.printStackTrace();
	            throw new AssertionError(t);
	        }
			
	        if (defaultConn instanceof JarURLConnection) {
	            //System.err.println("[WildermythProviderJarURLStreamHandler] defaultConn is a JarURLConnection, wrapping with ProviderJarURLConnection");
	            try {
	                connection = new ProviderJarURLConnection(url, (JarURLConnection) defaultConn);
	            } catch (MalformedURLException e) {
	                System.err.println("[WildermythProviderJarURLStreamHandler] Failed to create ProviderJarURLConnection for: " + url);
	                e.printStackTrace();
	            }
	        } else {
	            System.err.println("[WildermythProviderJarURLStreamHandler] defaultConn is not a JarURLConnection, returning default connection");
	        }
	        if (connection == null) {
	            System.err.println("[WildermythProviderJarURLStreamHandler] No custom connection created, using defaultConn");
	            connection = defaultConn;
	        }
	        return connection;
	    }
	}
}