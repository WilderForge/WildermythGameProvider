package com.wildermods.provider.internal.classload;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

final class ProviderJarURLStreamHandlerFactory implements URLStreamHandlerFactory {

	@Override
	public WildermythProviderJarURLStreamHandler createURLStreamHandler(String protocol) {
		return new WildermythProviderJarURLStreamHandler(protocol);
	}
	
	static final class WildermythProviderJarURLStreamHandler extends URLStreamHandler {

	    WildermythProviderJarURLStreamHandler(String protocol) {

	    }

	    @Override
	    protected URLConnection openConnection(URL url) throws IOException {
	        // Delegate to the default handler to create the connection,
	        // then wrap it with our custom connection that strips Class-Path.
	        URLConnection defaultConn;
	        URLConnection connection = null;
			try {
				defaultConn = url.toURI().toURL().openConnection();
			} catch (IOException | URISyntaxException e) {
				throw new AssertionError();
			}
			
	        if (defaultConn instanceof JarURLConnection) {
	        	if(url.toString().contains("gameEngine")) {
	        		System.out.println("Hmm....");
	        	}
	        	try {
	        		connection = new ProviderJarURLConnection(url, (JarURLConnection) defaultConn);
	        	}
	        	catch(MalformedURLException e) {
	        		e.printStackTrace();
	        	}
	        }
	        if (connection == null) {
	        	System.err.println("Couldn't use ProviderURLConnection for " + url);
	        	connection = defaultConn;
	        }
	        System.err.println("CONNECTION: " + connection.getClass() + " - " + connection.getURL());
	        return connection;
	    }
		
	}

}
