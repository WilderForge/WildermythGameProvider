package com.wildermods.provider.internal.classload;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * A ClassLoader that loads classes and resources from a list of URLs.
 * Supports both JAR files (jar:file:...) and directories (file:... ending with '/').
 * Manifest Class-Path entries are stripped from JARs via ProviderJarURLConnection.
 */
public class StrippingClassLoader extends ClassLoader implements Closeable {
	private static final ProviderJarURLStreamHandlerFactory jarStreamFactory = new ProviderJarURLStreamHandlerFactory();
    private final List<ResourceLoader> loaders = new CopyOnWriteArrayList<>();
    private final List<URL> originUrls = new CopyOnWriteArrayList<>();
    private final Map<String, LoadedEntry> resourceCache = new ConcurrentHashMap<>();
    
	static {
		registerAsParallelCapable();
	}
    
    /**
     * @param urls   array of URLs (jar:file:/path/to.jar!/ or file:/path/to/dir/)
     * @param parent parent ClassLoader
     */
    public StrippingClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
        try {
            for (URL url : urls) {
                addURL(url);
            }
        } catch (Throwable t) {
            // close all already opened loaders
            for (ResourceLoader loader : loaders) {
                try { loader.close(); } catch (IOException ignored) {}
            }
            throw t;
        }
    }
    
    protected void addURL(URL url) {
        ResourceLoader loader = createLoader(url);
        if (loader == null) return;
        if(originUrls.contains(url)) {
        	return;
        }
        try {
	        loaders.add(loader);
	        cacheLoader(loader);
	        originUrls.add(url);
        }
        catch(Throwable t) {
        	if(loader != null) {
        		loaders.remove(loader);
        		try { loader.close(); } catch (IOException ignored) {}
        	}
        	originUrls.remove(url);
        	//swallow
        }
    }
    
    protected URL[] getURLs() {
    	return originUrls.toArray(new URL[] {});
    }

    private ResourceLoader createLoader(URL url) {
        String protocol = url.getProtocol();
        String file = url.getFile();
        if ("file".equals(protocol) && file != null && file.endsWith("/")) {
            return new DirectoryLoader(url);
        } else if ("jar".equals(protocol)) {
            return new JarLoader(url);
        } else {
            // Unsupported, ignore
            return null;
        }
    }

    private void cacheLoader(ResourceLoader loader) {
        // For performance, list all resources and cache them
        for (String name : loader.listResources()) {
            resourceCache.putIfAbsent(name, new LoadedEntry(loader, name));
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";
        LoadedEntry entry = resourceCache.get(path);
        if (entry == null) {
            throw new ClassNotFoundException(name);
        }
        try (InputStream is = entry.loader.getResourceAsStream(entry.name)) {
            if (is == null) throw new ClassNotFoundException(name);
            byte[] bytes = is.readAllBytes();
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    @Override
    protected URL findResource(String name) {
        LoadedEntry entry = resourceCache.get(name);
        if (entry == null) return null;
        return entry.loader.getResourceURL(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        List<URL> results = new ArrayList<>();
        for (ResourceLoader loader : loaders) {
            URL url = loader.getResourceURL(name);
            if (url != null) results.add(url);
        }
        return Collections.enumeration(results);
    }

    @Override
    public void close() throws IOException {
        List<IOException> exceptions = new ArrayList<>();
        for (ResourceLoader loader : loaders) {
            try {
                loader.close();
            } catch (IOException e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            IOException first = exceptions.get(0);
            for (int i = 1; i < exceptions.size(); i++) first.addSuppressed(exceptions.get(i));
            throw first;
        }
    }

    // ========== Internal interfaces and implementations ==========

    private interface ResourceLoader extends Closeable {
        InputStream getResourceAsStream(String name) throws IOException;
        URL getResourceURL(String name);
        Collection<String> listResources();
    }

    private static class DirectoryLoader implements ResourceLoader {
        private final Path baseDir;
        private final URL baseURL;

        DirectoryLoader(URL url) {
            this.baseURL = url;
            String path = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
            this.baseDir = Path.of(path);
        }

        @Override
        public InputStream getResourceAsStream(String name) throws IOException {
        	Path file = baseDir.resolve(name.replace('/', File.separatorChar));
            if (Files.exists(file) && Files.isRegularFile(file)) {
                return new FileInputStream(file.toFile());
            }
            return null;
        }

        @Override
        public URL getResourceURL(String name) {
            try {
            	Path file = baseDir.resolve(name.replace('/', File.separatorChar));
            	if (Files.exists(file) && Files.isRegularFile(file)) {
                    return new URL(baseURL, name);
                }
            } catch (Exception ignored) {}
            return null;
        }

        @Override
        public Collection<String> listResources() {
            List<String> results = new ArrayList<>();
            collectResources(baseDir, "", results);
            return results;
        }

        private void collectResources(Path dir, String relativePath, List<String> out) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.forEach(f -> {
                    String childPath = relativePath.isEmpty() ? f.getFileName().toString() : relativePath + "/" + f.getFileName();
                    if (Files.isDirectory(f)) {
                        collectResources(f, childPath, out);
                    } else {
                        out.add(childPath);
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void close() throws IOException {
            // Nothing to close for directories
        }
    }

    private class JarLoader implements ResourceLoader {
        private final JarFile jarFile;

        JarLoader(URL url) {
            try {
                URLConnection conn = jarStreamFactory.createURLStreamHandler("jar").openConnection(url);
                if (!(conn instanceof JarURLConnection)) {
                    throw new AssertionError("Not a JarURLConnection??: " + url);
                }
                
                JarURLConnection jarConn = (JarURLConnection) conn;
                this.jarFile = jarConn.getJarFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to open JAR: " + url, e);
            }
        }

        @Override
        public InputStream getResourceAsStream(String name) throws IOException {
            JarEntry entry = jarFile.getJarEntry(name);
            if (entry != null && !entry.isDirectory()) {
                return jarFile.getInputStream(entry);
            }
            return null;
        }

        @Override
        public URL getResourceURL(String name) {
            JarEntry entry = jarFile.getJarEntry(name);
            if (entry != null && !entry.isDirectory()) {
                try {
                    // Return a jar: URL pointing to this resource
                	URL fileURL = Path.of(jarFile.getName()).toUri().toURL();
                	URL jarURL = new URL("jar", null, -1, fileURL.toExternalForm() + "!/" + name, jarStreamFactory.createURLStreamHandler("jar"));
                	return jarURL;
                } catch (Exception ignored) {}
            }
            return null;
        }

        @Override
        public Collection<String> listResources() {
            List<String> results = new ArrayList<>();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    results.add(entry.getName());
                }
            }
            return results;
        }

        @Override
        public void close() throws IOException {
            jarFile.close();
        }
    }

    private static record LoadedEntry(ResourceLoader loader, String name) {}
}