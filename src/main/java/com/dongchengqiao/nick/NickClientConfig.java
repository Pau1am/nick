package com.dongchengqiao.nick;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

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
			Nick.LOGGER.warn("Unknown display mode '{}', falling back to nickname only", key);
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
	/** Insertion order is kept so the file does not churn between saves. */
	private static final Map<DisplayLocation, DisplayMode> overrides = new LinkedHashMap<>();
	private static DisplayMode defaultMode = DisplayMode.NICK_ONLY;

	private NickClientConfig() {
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("nick-client.json");
	}

	public static void load() {
		overrides.clear();
		Path path = path();
		if (!Files.exists(path)) {
			save();
			return;
		}
		JsonObject root = ConfigIo.readObject(path);
		if (root == null) {
			return;
		}
		JsonElement fallback = root.get(DEFAULT_KEY);
		if (fallback != null && fallback.isJsonPrimitive()) {
			// DEFAULT only means something as a per-location override. Were it allowed here,
			// every "resolve it later" path would resolve to itself and no nickname would show.
			DisplayMode parsed = DisplayMode.fromKey(fallback.getAsString());
			defaultMode = parsed == DisplayMode.DEFAULT ? DisplayMode.NICK_ONLY : parsed;
		}
		for (DisplayLocation location : DisplayLocation.values()) {
			JsonElement value = root.get(location.getKey());
			if (value != null && value.isJsonPrimitive()) {
				overrides.put(location, DisplayMode.fromKey(value.getAsString()));
			}
		}
	}

	public static DisplayMode getDefaultMode() {
		return defaultMode;
	}

	public static void setDefaultMode(DisplayMode mode) {
		defaultMode = mode.resolve(DisplayMode.NICK_ONLY);
		save();
	}

	public static DisplayMode getDisplayMode(DisplayLocation location) {
		DisplayMode override = overrides.get(location);
		return override != null ? override.resolve(defaultMode) : defaultMode;
	}

	/** @return the raw per-location override, or {@code null} when the location follows the default. */
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
		JsonObject root = new JsonObject();
		root.addProperty(DEFAULT_KEY, defaultMode.getKey());
		for (Map.Entry<DisplayLocation, DisplayMode> entry : overrides.entrySet()) {
			root.addProperty(entry.getKey().getKey(), entry.getValue().getKey());
		}
		ConfigIo.writeAtomically(path(), ConfigIo.GSON.toJson(root));
	}
}
