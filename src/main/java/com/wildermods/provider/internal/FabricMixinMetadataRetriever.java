package com.wildermods.provider.internal;

import java.util.HashMap;

import org.spongepowered.asm.launch.MixinBootstrap;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;

public class FabricMixinMetadataRetriever {

	public static BuiltinModMetadata.Builder retrieve() {
		try {
			HashMap<String, String> fabricMixinContactInformation = new HashMap<>();
			
			fabricMixinContactInformation.put("homepage", "https://github.com/FabricMC/Mixin");
			fabricMixinContactInformation.put("issues", "https://github.com/FabricMC/Mixin/issues");
			fabricMixinContactInformation.put("sources", "https://github.com/FabricMC/Mixin");
			fabricMixinContactInformation.put("license", "https://github.com/FabricMC/Mixin/blob/main/LICENSE.txt");
			
			HashMap<String, String> mixinContactInformation = new HashMap<>();
			mixinContactInformation.put("homepage", "https://github.com/SpongePowered/Mixin");
			mixinContactInformation.put("issues", "https://github.com/SpongePowered/Mixin/issues");
			mixinContactInformation.put("sources", "https://github.com/SpongePowered/Mixin");
			mixinContactInformation.put("license", "https://github.com/SpongePowered/Mixin/blob/master/LICENSE.txt");
			
			BuiltinModMetadata.Builder mixinMetaData = 
					new BuiltinModMetadata.Builder("mixin", MixinBootstrap.VERSION)
					.setName("Spongepowered Mixin (Fabric Fork)")
					.addAuthor("Mumfrey", mixinContactInformation)
					.addAuthor("FabricMC Team", fabricMixinContactInformation)
					.addAuthor("Spongepowered Team", mixinContactInformation)
					.setContact(new ContactInformationImpl(fabricMixinContactInformation))
					.setDescription("""
							FabricMC's fork of Mixin, A bytecode weaving framework for Java using ASM.
							
							Original by Mumfrey.
							""")
					.addLicense("https://github.com/SpongePowered/Mixin/blob/master/LICENSE.txt");
			
			return mixinMetaData;
		}
		catch(LinkageError e) {
			e.printStackTrace();
			return null;
		}

	}
	
}
