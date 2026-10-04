package com.dongchengqiao.nick;

import com.dongchengqiao.nick.NickClientConfig.DisplayMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the display-mode decision table and the team-decoration stripping it depends on.
 * Both are pure functions, which is the point of {@link NickDisplayText}: the modes used to be
 * decided inside four separate mixins, where nothing could reach them without a running client.
 */
class NickDisplayTextTest {
	private static final String REAL = "dongchengqiao";
	private static final String NICK = "脏小豆";

	private static String text(DisplayMode mode, String nickname) {
		return NickDisplayText.resolve(mode, REAL, nickname).getString();
	}

	@Nested
	@DisplayName("resolve")
	class Resolve {
		@Test
		void nickOnlyShowsTheNickname() {
			assertEquals(NICK, text(DisplayMode.NICK_ONLY, NICK));
		}

		@Test
		void nickAndOriginalShowsBoth() {
			assertEquals("[" + NICK + "]" + REAL, text(DisplayMode.NICK_AND_ORIGINAL, NICK));
		}

		@Test
		void hideShowsTheRealName() {
			assertEquals(REAL, text(DisplayMode.HIDE, NICK));
		}

		@Test
		void everyModeFallsBackToTheRealNameWithoutANickname() {
			// Without a nickname there is nothing to show instead, and "[null]name" must never appear.
			for (DisplayMode mode : DisplayMode.values()) {
				assertEquals(REAL, text(mode, null), "mode " + mode);
			}
		}

		@Test
		void defaultBehavesLikeNickOnly() {
			// DEFAULT is resolved before it reaches here, but a stray value must not produce a
			// real-name-only display.
			assertEquals(NICK, text(DisplayMode.DEFAULT, NICK));
			assertEquals(REAL, text(DisplayMode.DEFAULT, null));
		}
	}

	@Nested
	@DisplayName("stripTeamDecoration")
	class StripTeamDecoration {
		@Test
		void removesAMatchingPrefixAndSuffix() {
			assertEquals(NICK, NickDisplayText.stripTeamDecoration("[VIP] " + NICK + " ★", "[VIP] ", " ★"));
		}

		@Test
		void keepsTheNameWhenNeitherSideMatches() {
			assertEquals(NICK, NickDisplayText.stripTeamDecoration(NICK, "[VIP] ", " ★"));
			assertEquals(NICK, NickDisplayText.stripTeamDecoration(NICK, "", ""));
		}

		@Test
		void stripsEachSideIndependently() {
			// A team can have only a prefix, or only a suffix.
			assertEquals(NICK, NickDisplayText.stripTeamDecoration("[VIP] " + NICK, "[VIP] ", ""));
			assertEquals(NICK, NickDisplayText.stripTeamDecoration(NICK + " ★", "", " ★"));
		}

		@Test
		void handlesANameThatIsEntirelyDecoration() {
			assertEquals("", NickDisplayText.stripTeamDecoration("[]", "[", "]"));
		}

		@Test
		@DisplayName("documents the known ambiguity: only the outermost decoration is removed")
		void removesTheOutermostDecorationOnly() {
			// The client is only told the already-formatted tab list name, so recovering the bare
			// nickname is guesswork. Stripping one prefix and one suffix is the best available
			// approximation; a nickname that itself starts with the team prefix keeps the rest.
			// Pinned down here so the trade-off is explicit rather than folklore.
			assertEquals("[VIP] " + NICK, NickDisplayText.stripTeamDecoration("[VIP] [VIP] " + NICK, "[VIP] ", ""));
		}
	}
}
