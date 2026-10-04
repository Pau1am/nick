package com.dongchengqiao.nick;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reading and writing the mod's JSON config files.
 * <p>
 * Both config classes need the same "write to a temp file, then move it into place" dance so an
 * interrupted save cannot leave a truncated file behind, and the same tolerance for a file that
 * fails to parse. Keeping it in one place stops the two configs from drifting apart - which is
 * exactly what happened before: the server config wrote atomically and the client config did not.
 */
final class ConfigIo {
	static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ConfigIo() {
	}

	/**
	 * @return the parsed object, or {@code null} when the file is absent or unreadable.
	 *         A broken config is reported and then treated as empty rather than crashing the game.
	 */
	static JsonObject readObject(Path path) {
		if (!Files.exists(path)) {
			return null;
		}
		try {
			return GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
		} catch (Exception e) {
			Nick.LOGGER.error("Failed to read {}, treating it as empty", path, e);
			return null;
		}
	}

	/** Writes {@code json} to {@code path} via a temporary file and an atomic move. */
	static void writeAtomically(Path path, String json) {
		Path temp = path.resolveSibling(path.getFileName() + ".tmp");
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(temp, json, StandardCharsets.UTF_8);
			try {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			Nick.LOGGER.error("Failed to write {}", path, e);
		}
	}
}
