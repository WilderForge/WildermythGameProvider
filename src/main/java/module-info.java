module com.wildermods.provider {
	
	requires net.fabricmc.loader;
	requires org.spongepowered.mixin;

	exports com.wildermods.provider.services;
	
	exports com.wildermods.provider to net.fabricmc.loader, org.spongepowered.mixin;
	exports com.wildermods.provider.classloader to net.fabricmc.loader, org.spongepowered.mixin;
	exports com.wildermods.provider.patch to net.fabricmc.loader, org.spongepowered.mixin;
	
}