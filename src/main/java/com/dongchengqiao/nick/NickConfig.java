package com.dongchengqiao.nick;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the nickname of every player, keyed by their real (Mojang) name.
 * The on-disk format is {@code {"<real name>": {"nick": "<nickname>"}}}.
 */
public final class NickConfig {
	private static final String NICK_FIELD = "nick";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	/** Insertion order is kept so the file does not churn between saves. */
	private static final Map<String, String> data = new LinkedHashMap<>();

	private NickConfig() {
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("nick.json");
	}

	public static synchronized void load() {
		data.clear();
		Path path = path();
		if (!Files.exists(path)) {
			return;
		}
		try {
			JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
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
				data.put(entry.getKey(), nick.getAsString());
			}
		} catch (Exception e) {
			Nick.LOGGER.error("Failed to load {}", path, e);
		}
	}

	public static synchronized void save() {
		Path path = path();
		Path temp = path.resolveSibling(path.getFileName() + ".tmp");
		try {
			JsonObject root = new JsonObject();
			for (Map.Entry<String, String> entry : data.entrySet()) {
				JsonObject inner = new JsonObject();
				inner.addProperty(NICK_FIELD, entry.getValue());
				root.add(entry.getKey(), inner);
			}
			Files.createDirectories(path.getParent());
			// Write to a temp file first: an interrupted write must not leave a truncated config.
			Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
			try {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			Nick.LOGGER.error("Failed to save {}", path, e);
		}
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
