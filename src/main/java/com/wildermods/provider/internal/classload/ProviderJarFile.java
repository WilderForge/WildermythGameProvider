package com.wildermods.provider.internal.classload;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ProviderJarFile extends JarFile {

	public ProviderJarFile(JarFile unsanitized) throws IOException {
		this(unsanitized.getName());
	}
	
	public ProviderJarFile(JarFile unsanitized, boolean verify) throws IOException {
		this(unsanitized.getName(), verify);
	}
	
	public ProviderJarFile(String name) throws IOException {
		super(name);
	}

	public ProviderJarFile(String name, boolean verify) throws IOException {
		super(name, verify);
	}
	
	public ProviderJarFile(File file) throws IOException {
		super(file);
	}
	
	public ProviderJarFile(File file, boolean verify) throws IOException {
		super(file, verify);
	}
	
	public ProviderJarFile(File file, boolean verify, int mode) throws IOException {
		super(file, verify, mode);
	}
	
	public ProviderJarFile(File file, boolean verify, int mode, Runtime.Version version) throws IOException {
		super(file, verify, mode, version);
	}
	
	@Override
	public JarEntry getJarEntry(String name) {
		JarEntry stripped = super.getJarEntry(name);
		if(stripped == null) return null;
		try {
			stripped.getAttributes().remove(Attributes.Name.CLASS_PATH);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return stripped;
	}
	
	@Override
	public Manifest getManifest() throws IOException {
		Manifest stripped = super.getManifest();
        if (stripped == null) return null;
        stripped.getMainAttributes().remove(Attributes.Name.CLASS_PATH);
        return stripped;
	}

}
