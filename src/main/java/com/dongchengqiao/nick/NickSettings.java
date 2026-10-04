package com.dongchengqiao.nick;

import carpet.api.settings.Rule;

public class NickSettings {
	public static final String NICK = "Nick";

	@Rule(categories = NICK)
	public static boolean commandNick = false;

	@Rule(categories = NICK)
	public static boolean commandPlayerCN = false;

	@Rule(categories = NICK)
	public static boolean commandPlayerCNNoSpawn = false;

	/**
	 * Whether brigadier should accept non-ASCII characters in unquoted command input.
	 * <p>
	 * {@code /nick set} needs this for a bare Chinese nickname, and {@code /player} needs it when
	 * {@link #commandPlayerCN} is on, so either rule enables it.
	 */
	public static boolean unicodeInputEnabled() {
		return commandNick || commandPlayerCN;
	}
}
