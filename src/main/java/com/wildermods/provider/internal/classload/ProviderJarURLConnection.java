package com.wildermods.provider.internal.classload;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

public class ProviderJarURLConnection extends JarURLConnection {

	final JarURLConnection delegate;
	
	public ProviderJarURLConnection(URL url, JarURLConnection delegate) throws MalformedURLException {
		super(url);
		this.delegate = delegate;
	}
	
	@Override
	public void connect() throws IOException {
		delegate.connect();
	}
	
    @Override
    public ProviderJarFile getJarFile() throws IOException {
        // Get the real JarFile from the delegate, then wrap it with our custom class
        JarFile unsanitized = delegate.getJarFile();
        return new ProviderJarFile(unsanitized);
    }

    @Override
    public URL getJarFileURL() {
        return delegate.getJarFileURL();
    }
    
    @Override
    public String getEntryName() { 
    	return delegate.getEntryName();
    }
    
    @Override
    public Permission getPermission() throws IOException {
    	return delegate.getPermission();
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
    	return delegate.getInputStream();
    }
    
    @Override
    public int getContentLength() {
    	return delegate.getContentLength();
    }
    
    @Override
    public long getContentLengthLong() {
    	return delegate.getContentLengthLong();
    }
    
    @Override
    public String getContentType() {
    	return delegate.getContentType();
    }
    
    @Override
    public String getHeaderField(String name) {
    	return delegate.getHeaderField(name);
    }
    
    @Override
    public String getRequestProperty(String key) {
    	return delegate.getRequestProperty(key);
    }
    
    @Override
    public void addRequestProperty(String key, String value) {
    	delegate.addRequestProperty(key, value);
    }
    
    @Override
    public Map<String, List<String>> getRequestProperties() {
    	return delegate.getRequestProperties();
    }
    
    @Override
    public void setAllowUserInteraction(boolean allowUserInteraction) {
    	delegate.setAllowUserInteraction(allowUserInteraction);
    }
    
    @Override
    public boolean getAllowUserInteraction() {
    	return delegate.getAllowUserInteraction();
    }
    
    @Override
    public void setUseCaches(boolean useCaches) {
    	delegate.setUseCaches(useCaches);
    }
    
    @Override
    public boolean getUseCaches() {
    	return delegate.getUseCaches();
    }
    
    @Override
    public void setIfModifiedSince(long ifModifiedSince) {
    	delegate.setIfModifiedSince(ifModifiedSince);
    }
    
    @Override
    public void setDefaultUseCaches(boolean defaultUseCaches) {
    	delegate.setDefaultUseCaches(defaultUseCaches);
    }
    
    public boolean getDefaultUseCaches() {
    	return delegate.getDefaultUseCaches();
    }

}
