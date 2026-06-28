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
		//System.err.println("[ProviderJarFile] Wrapping existing JarFile: " + unsanitized.getName());
	}
	
	public ProviderJarFile(JarFile unsanitized, boolean verify) throws IOException {
		this(unsanitized.getName(), verify);
		//System.err.println("[ProviderJarFile] Wrapping existing JarFile (verify=" + verify + "): " + unsanitized.getName());
	}
	
	public ProviderJarFile(String name) throws IOException {
		super(name);
		//System.err.println("[ProviderJarFile] Opened JarFile from name: " + name);
	}

	public ProviderJarFile(String name, boolean verify) throws IOException {
		super(name, verify);
		//System.err.println("[ProviderJarFile] Opened JarFile from name (verify=" + verify + "): " + name);
	}
	
	public ProviderJarFile(File file) throws IOException {
		super(file);
		//System.err.println("[ProviderJarFile] Opened JarFile from File: " + file.getAbsolutePath());
	}
	
	public ProviderJarFile(File file, boolean verify) throws IOException {
		super(file, verify);
		//System.err.println("[ProviderJarFile] Opened JarFile from File (verify=" + verify + "): " + file.getAbsolutePath());
	}
	
	public ProviderJarFile(File file, boolean verify, int mode) throws IOException {
		super(file, verify, mode);
		//System.err.println("[ProviderJarFile] Opened JarFile from File (verify=" + verify + ", mode=" + mode + "): " + file.getAbsolutePath());
	}
	
	public ProviderJarFile(File file, boolean verify, int mode, Runtime.Version version) throws IOException {
		super(file, verify, mode, version);
		//System.err.println("[ProviderJarFile] Opened JarFile from File (verify=" + verify + ", mode=" + mode + ", version=" + version + "): " + file.getAbsolutePath());
	}
	
	@Override
	public JarEntry getJarEntry(String name) {
		//System.err.println("[ProviderJarFile] getJarEntry called for entry: " + name);
		JarEntry stripped = super.getJarEntry(name);
		if (stripped == null) {
			//System.err.println("[ProviderJarFile] Entry not found: " + name);
			return null;
		}
		try {
			Attributes attrs = stripped.getAttributes();
			if (attrs != null && attrs.containsKey(Attributes.Name.CLASS_PATH)) {
				//System.err.println("[ProviderJarFile] Removing CLASS_PATH attribute from entry: " + name);
				attrs.remove(Attributes.Name.CLASS_PATH);
			} else {
				//System.err.println("[ProviderJarFile] No CLASS_PATH attribute on entry: " + name);
			}
		} catch (IOException e) {
			//System.err.println("[ProviderJarFile] IOException while stripping CLASS_PATH from entry " + name);
			e.printStackTrace();
			throw new UncheckedIOException(e);
		}
		return stripped;
	}
	
	@Override
	public Manifest getManifest() throws IOException {
		//System.err.println("[ProviderJarFile] getManifest() called");
		Manifest stripped = super.getManifest();
		if (stripped == null) {
			//System.err.println("[ProviderJarFile] No manifest found");
			return null;
		}
		if (stripped.getMainAttributes().containsKey(Attributes.Name.CLASS_PATH)) {
			//System.err.println("[ProviderJarFile] Removing CLASS_PATH attribute from main manifest");
			stripped.getMainAttributes().remove(Attributes.Name.CLASS_PATH);
		} else {
			//System.err.println("[ProviderJarFile] CLASS_PATH attribute not present in main manifest");
		}
		return stripped;
	}
}