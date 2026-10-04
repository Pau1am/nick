package com.dongchengqiao.nick;

import com.dongchengqiao.nick.NickClientConfig.DisplayMode;
import net.minecraft.network.chat.Component;

/**
 * Turns a display mode into the text to render.
 * <p>
 * Every display path (name tag, chat, tab list, suggestions) used to carry its own copy of this
 * switch. Besides the duplication, that made the modes impossible to test without a running client
 * and let the copies drift apart. The whole decision is a pure function, so it lives here.
 */
public final class NickDisplayText {
	private NickDisplayText() {
	}

	/**
	 * The text to render for {@code mode}.
	 *
	 * @param nickname the nickname in effect, or {@code null} when the player has none - in which
	 *                 case the real name is used instead of showing an empty or bracketed name
	 */
	public static Component resolve(DisplayMode mode, String realName, String nickname) {
		return switch (mode) {
			case HIDE -> Component.literal(realName);
			case NICK_ONLY, DEFAULT -> Component.literal(nickname != null ? nickname : realName);
			case NICK_AND_ORIGINAL ->
				Component.literal(nickname != null ? "[" + nickname + "]" + realName : realName);
		};
	}

	/**
	 * Removes the team prefix/suffix that {@code PlayerTeam#formatNameForTeam} wrapped a name in.
	 * <p>
	 * Needed because the only nickname the client is told about is the already-formatted tab list
	 * name; {@code NICK_AND_ORIGINAL} has to recover the bare nickname out of it.
	 */
	public static String stripTeamDecoration(String text, String prefix, String suffix) {
		String result = text;
		if (!prefix.isEmpty() && result.startsWith(prefix)) {
			result = result.substring(prefix.length());
		}
		if (!suffix.isEmpty() && result.length() >= suffix.length() && result.endsWith(suffix)) {
			result = result.substring(0, result.length() - suffix.length());
		}
		return result;
	}
}
