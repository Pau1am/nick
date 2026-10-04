package com.dongchengqiao.nick;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * Brigadier parsing helpers that accept non-ASCII text.
 * <p>
 * Vanilla's {@code StringReader} only allows a fixed set of punctuation in unquoted input, so a
 * bare Chinese nickname is rejected before any of our code runs. These helpers accept it, and
 * still honour quoting for values containing spaces.
 */
public final class UnicodeStrings {
	private UnicodeStrings() {
	}

	/**
	 * Reads a quoted string, otherwise a single token that - unlike vanilla's
	 * {@code readUnquotedString} - also accepts non-ASCII characters.
	 *
	 * @return the parsed text, or an empty string if nothing could be read
	 */
	public static String readWordOrQuoted(StringReader reader) throws CommandSyntaxException {
		if (!reader.canRead()) {
			return "";
		}
		if (StringReader.isQuotedStringStart(reader.peek())) {
			return reader.readString();
		}
		int start = reader.getCursor();
		while (reader.canRead()) {
			char c = reader.peek();
			// ASCII punctuation that brigadier excludes still terminates the token; anything
			// above ASCII (CJK, accents, emoji, ...) is kept.
			if (c <= 127 && !StringReader.isAllowedInUnquotedString(c)) {
				break;
			}
			reader.skip();
		}
		return reader.getString().substring(start, reader.getCursor());
	}
}
