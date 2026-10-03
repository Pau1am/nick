package com.dongchengqiao.nick;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;

/**
 * Like {@code StringArgumentType.string()} but without the ASCII-only restriction, so an unquoted
 * Chinese nickname works. Quoted input ({@code /nick set "my name"}) is supported as well.
 */
public class UnicodeWordArgumentType implements ArgumentType<String> {
	private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(
		Component.translatableWithFallback(
			"nick.error.invalid_argument",
			"Invalid nickname - quote it if it contains spaces, e.g. /nick set \"my name\""));

	public UnicodeWordArgumentType() {
	}

	public static UnicodeWordArgumentType unicodeWord() {
		return new UnicodeWordArgumentType();
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		String value = readWordOrQuoted(reader);
		if (value.isEmpty()) {
			throw ERROR_INVALID.createWithContext(reader);
		}
		return value;
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
		char next = reader.peek();
		if (StringReader.isQuotedStringStart(next)) {
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

	@Override
	public Collection<String> getExamples() {
		return List.of("word", "玩家", "示例");
	}
}
