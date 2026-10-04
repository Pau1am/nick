package com.dongchengqiao.nick;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the nickname of every player, keyed by their real (Mojang) name.
 * The on-disk format is {@code {"<real name>": {"nick": "<nickname>"}}}.
 */
public final class NickConfig {
	private static final String NICK_FIELD = "nick";
	/** Insertion order is kept so the file does not churn between saves. */
	private static final Map<String, String> data = new LinkedHashMap<>();

	private NickConfig() {
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("nick.json");
	}

	public static synchronized void load() {
		data.clear();
		JsonObject root = ConfigIo.readObject(path());
		if (root == null) {
			return;
		}
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			JsonElement value = entry.getValue();
			// Skip malformed entries instead of aborting the whole file.
			if (value == null || !value.isJsonObject()) {
				Nick.LOGGER.warn("Skipping malformed nick entry for {}", entry.getKey());
				continue;
			}
			JsonElement nick = value.getAsJsonObject().get(NICK_FIELD);
			if (nick == null || !nick.isJsonPrimitive()) {
				Nick.LOGGER.warn("Skipping nick entry without a '{}' field: {}", NICK_FIELD, entry.getKey());
				continue;
			}
			// Normalise on the way in so a hand-edited file cannot produce a name that is
			// invisible or that matches nothing when commands are resolved.
			String realName = entry.getKey().strip();
			String nickname = nick.getAsString().strip();
			if (realName.isEmpty() || nickname.isEmpty()) {
				Nick.LOGGER.warn("Skipping blank nick entry for '{}'", entry.getKey());
				continue;
			}
			data.put(realName, nickname);
		}
	}

	public static synchronized void save() {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, String> entry : data.entrySet()) {
			JsonObject inner = new JsonObject();
			inner.addProperty(NICK_FIELD, entry.getValue());
			root.add(entry.getKey(), inner);
		}
		ConfigIo.writeAtomically(path(), ConfigIo.GSON.toJson(root));
	}

	/** @return the nickname of {@code realName}, or {@code null} if there is none. */
	public static synchronized String getNick(String realName) {
		return data.get(realName);
	}

	public static synchronized void setNick(String realName, String nick) {
		data.put(realName, nick);
		save();
	}

	public static synchronized void removeNick(String realName) {
		if (data.remove(realName) != null) {
			save();
		}
	}

	/**
	 * @return the real name of the player currently using {@code nick}, or {@code null}.
	 *         Nicknames are compared case-insensitively, like vanilla player names.
	 */
	public static synchronized String findOwner(String nick) {
		for (Map.Entry<String, String> entry : data.entrySet()) {
			if (entry.getValue().equalsIgnoreCase(nick)) {
				return entry.getKey();
			}
		}
		return null;
	}
}
