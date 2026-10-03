package com.dongchengqiao.nick;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Client-only settings: how a nickname is shown per display location. */
public class NickClientConfig {
	public enum DisplayMode {
		/** "Follow whatever the default mode is" - only valid as a per-location override. */
		DEFAULT("default"),
		NICK_ONLY("nick_only"),
		NICK_AND_ORIGINAL("nick_and_original"),
		HIDE("hide");

		private final String key;

		DisplayMode(String key) {
			this.key = key;
		}

		public static DisplayMode fromKey(String key) {
			for (DisplayMode mode : values()) {
				if (mode.key.equals(key)) {
					return mode;
				}
			}
			return NICK_ONLY;
		}

		public String getKey() {
			return key;
		}

		public String getTranslationKey() {
			return "nick.display_mode." + key;
		}

		public DisplayMode resolve(DisplayMode fallback) {
			return this == DEFAULT ? fallback : this;
		}
	}

	public enum DisplayLocation {
		NAMETAG("nametag"),
		CHAT("chat"),
		TAB_LIST("tab_list");

		private final String key;

		DisplayLocation(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}

		public String getTranslationKey() {
			return "nick.display_location." + key;
		}

		public static DisplayLocation fromKey(String key) {
			for (DisplayLocation loc : values()) {
				if (loc.key.equals(key)) {
					return loc;
				}
			}
			return NAMETAG;
		}
	}

	private static final String DEFAULT_KEY = "default";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<DisplayLocation, DisplayMode> overrides = new LinkedHashMap<>();
	private static DisplayMode defaultMode = DisplayMode.NICK_ONLY;

	private NickClientConfig() {
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("nick-client.json");
	}

	public static void load() {
		Path path = path();
		if (!Files.exists(path)) {
			save();
			return;
		}
		try {
			JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
			if (root == null) {
				return;
			}
			JsonElement fallback = root.get(DEFAULT_KEY);
			if (fallback != null && fallback.isJsonPrimitive()) {
				defaultMode = DisplayMode.fromKey(fallback.getAsString());
			}
			for (DisplayLocation location : DisplayLocation.values()) {
				JsonElement value = root.get(location.getKey());
				if (value != null && value.isJsonPrimitive()) {
					overrides.put(location, DisplayMode.fromKey(value.getAsString()));
				}
			}
		} catch (Exception e) {
			Nick.LOGGER.error("Failed to load {}", path, e);
		}
	}

	public static DisplayMode getDefaultMode() {
		return defaultMode;
	}

	public static void setDefaultMode(DisplayMode mode) {
		defaultMode = mode;
		save();
	}

	public static DisplayMode getDisplayMode(DisplayLocation location) {
		DisplayMode override = overrides.get(location);
		return override != null ? override.resolve(defaultMode) : defaultMode;
	}

	public static DisplayMode getOverride(DisplayLocation location) {
		return overrides.get(location);
	}

	public static void setOverride(DisplayLocation location, DisplayMode mode) {
		if (mode == DisplayMode.DEFAULT) {
			overrides.remove(location);
		} else {
			overrides.put(location, mode);
		}
		save();
	}

	public static void save() {
		Path path = path();
		try {
			JsonObject root = new JsonObject();
			root.addProperty(DEFAULT_KEY, defaultMode.getKey());
			for (Map.Entry<DisplayLocation, DisplayMode> entry : overrides.entrySet()) {
				root.addProperty(entry.getKey().getKey(), entry.getValue().getKey());
			}
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (Exception e) {
			Nick.LOGGER.error("Failed to save {}", path, e);
		}
	}
}
