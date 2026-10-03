package com.dongchengqiao.nick.client;

import com.dongchengqiao.nick.Nick;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class NickModMenuIntegration implements ModMenuApi {
	private static final String CLOTH_CONFIG = "cloth-config";

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		// Cloth Config is optional: without it the mod still works, there is just no GUI.
		// Returning null tells Mod Menu to hide the config button instead of crashing on click.
		if (!FabricLoader.getInstance().isModLoaded(CLOTH_CONFIG)) {
			Nick.LOGGER.info("Cloth Config is not installed, the Nick config screen will be unavailable");
			return null;
		}
		return NickConfigScreen::create;
	}
}
