package com.dongchengqiao.nick;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Shared nickname rules, used both when a nickname is set and when it is re-applied on join. */
public final class NickNames {
	/** Vanilla caps player names at 16 chars; give nicknames a bit more room but not unlimited. */
	public static final int MAX_LENGTH = 32;

	private NickNames() {
	}

	/**
	 * Nicknames are matched case-insensitively, exactly like vanilla player names, so two
	 * differently-cased spellings have to be treated as the same name.
	 *
	 * @return true when the nickname is empty, too long, or contains characters that would let
	 *         someone forge formatting or break chat rendering
	 */
	public static boolean isMalformed(String nick) {
		if (nick.isEmpty() || nick.length() > MAX_LENGTH) {
			return true;
		}
		for (int i = 0; i < nick.length(); i++) {
			char c = nick.charAt(i);
			// '§' would inject colour/format codes, control chars would break chat and the tab list.
			if (c == '\u00a7' || c < 0x20 || c == 0x7f) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether the nickname is already spoken for, either by a real name or by someone else's
	 * nickname.
	 * <p>
	 * Player names always win over nicknames when resolving commands, so allowing a collision
	 * would silently make {@code /tp <name>} target the wrong player.
	 *
	 * @param excluding the player whose own nickname is being validated (never conflicts with itself)
	 */
	public static boolean isTaken(MinecraftServer server, ServerPlayer excluding, String nick) {
		String self = excluding.getGameProfile().name();
		for (ServerPlayer online : server.getPlayerList().getPlayers()) {
			if (online != excluding && online.getGameProfile().name().equalsIgnoreCase(nick)) {
				return true;
			}
		}
		String owner = NickConfig.findOwner(nick);
		return owner != null && !owner.equals(self);
	}
}
