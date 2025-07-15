package com.wildermods.provider.internal;

import java.util.HashMap;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.util.asm.ASM;

import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;

public class ASMMetadataRetriever {

	public static BuiltinModMetadata.Builder retrieve() {
		try {
			HashMap<String, String> asmContactInformation = new HashMap<>();
			asmContactInformation.put("homepage", "https://asm.ow2.io/index.html");
			asmContactInformation.put("issues", "https://gitlab.ow2.org/asm/asm/-/issues");
			asmContactInformation.put("sources", "https://gitlab.ow2.org/asm/asm");
			asmContactInformation.put("license", "https://asm.ow2.io/license.html");
			
			BuiltinModMetadata.Builder asmMetaData = 
					new BuiltinModMetadata.Builder("asm", Opcodes.class.getPackage().getImplementationVersion())
					.setName(ASM.getVersionString())
					.addAuthor("INRIA, France Telecom", asmContactInformation)
					.setContact(new ContactInformationImpl(asmContactInformation))
					.setDescription("ASM is an all purpose Java bytecode manipulation and analysis framework. It can be used to modify existing classes or to dynamically generate classes, directly in binary form."
							+ "\n\n"
							+ "Currently supports " + ASM.getClassVersionString())
					.addLicense("https://asm.ow2.io/license.html");
			return asmMetaData;
		}
		catch(LinkageError e) {
			e.printStackTrace();
			return null;
		}

	}
	
}
