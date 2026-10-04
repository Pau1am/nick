package com.dongchengqiao.nick;

import com.mojang.brigadier.StringReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The nickname argument is parsed with vanilla's {@code StringArgumentType.string()}, which cannot
 * read CJK without quotes; {@link UnicodeStrings} is what widens it. Quoting has to keep working
 * for names containing spaces.
 */
class UnicodeStringsTest {
	private static String read(String input) throws Exception {
		StringReader reader = new StringReader(input);
		return UnicodeStrings.readWordOrQuoted(reader);
	}

	@Test
	void readsAnAsciiWord() throws Exception {
		assertEquals("xiaoming", read("xiaoming"));
	}

	@Test
	void readsABareCjkWord() throws Exception {
		assertEquals("小明", read("小明"));
	}

	@Test
	void readsAQuotedPhraseWithSpaces() throws Exception {
		assertEquals("小明 同学", read("\"小明 同学\""));
	}

	@Test
	void readsAQuotedPhraseContainingQuotes() throws Exception {
		assertEquals("a\"b", read("\"a\\\"b\""));
	}

	@Test
	void stopsAtAsciiPunctuationThatVanillaExcludes() throws Exception {
		// Mirrors vanilla tokenising: the word ends where the punctuation starts.
		StringReader reader = new StringReader("小明 同学");
		assertEquals("小明", UnicodeStrings.readWordOrQuoted(reader));
		assertEquals(2, reader.getCursor(), "cursor should sit on the space, after the two characters");
	}

	@Test
	void keepsPunctuationVanillaAllows() throws Exception {
		assertEquals("a_b-c.d", read("a_b-c.d"));
	}

	@Test
	void returnsEmptyForEmptyInput() throws Exception {
		assertEquals("", read(""));
	}

	@Test
	void readsAnEmptyQuotedString() throws Exception {
		assertEquals("", read("\"\""));
	}

	@Test
	void readsMixedScriptAndEmoji() throws Exception {
		assertEquals("id-名字🐟", read("id-名字🐟"));
	}
}
