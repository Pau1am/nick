package com.dongchengqiao.nick;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The nickname rules are what stop a player from taking someone else's identity or forging
 * formatting codes, so the boundaries are worth pinning down.
 */
class NickNamesTest {
	@Test
	void acceptsOrdinaryNames() {
		assertFalse(NickNames.isMalformed("xiaoming"));
		assertFalse(NickNames.isMalformed("小明"));
		assertFalse(NickNames.isMalformed("player_123"));
		assertFalse(NickNames.isMalformed("名字 with spaces"));
		assertFalse(NickNames.isMalformed("emoji 🐟"));
	}

	@Test
	void rejectsAnEmptyName() {
		assertTrue(NickNames.isMalformed(""));
	}

	@Test
	void acceptsExactlyTheMaximumLength() {
		assertFalse(NickNames.isMalformed("a".repeat(NickNames.MAX_LENGTH)));
		assertTrue(NickNames.isMalformed("a".repeat(NickNames.MAX_LENGTH + 1)));
	}

	@Test
	void countsCodeUnitsNotGraphemeClustersForCjk() {
		// A CJK character is a single char, so 32 of them fit.
		assertFalse(NickNames.isMalformed("一".repeat(NickNames.MAX_LENGTH)));
		assertTrue(NickNames.isMalformed("一".repeat(NickNames.MAX_LENGTH + 1)));
	}

	@ParameterizedTest
	@DisplayName("rejects characters that would forge formatting or break rendering")
	@ValueSource(strings = {
		"\u00a7cRed",   // '§' injects colour codes
		"line\nbreak",
		"tab\there",
		"bell\u0007",
		"del\u007f",
	})
	void rejectsFormattingAndControlCharacters(String nick) {
		assertTrue(NickNames.isMalformed(nick));
	}

	@Test
	void doesNotRejectWhitespaceItself() {
		// NickCommand strips first, so whitespace is handled there; this method is also used on
		// values read straight from the config, which are stripped on load.
		assertFalse(NickNames.isMalformed(" "));
	}
}
