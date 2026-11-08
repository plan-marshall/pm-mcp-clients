/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Display")
class DisplayTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    static Stream<Arguments> durations() {
        return Stream.of(
                Arguments.of(0L, "0 s", "0 s"),
                Arguments.of(30L, "30 s", "30 s"),
                Arguments.of(1500L, "25 min", "25 min"),
                Arguments.of(3600L, "1 h", "1 h"),
                Arguments.of(7800L, "2 h 10 min", "2 h 10 min"),
                Arguments.of(86400L, "24 h", "24 h"),
                Arguments.of(86400L * 2, "2 d", "2 T"),
                Arguments.of(86400L * 9, "9 d", "9 T"),
                Arguments.of(86400L * 9 + 3600L, "9 d 1 h", "9 T 1 h"));
    }

    @ParameterizedTest(name = "{0} seconds → {1} / {2}")
    @MethodSource("durations")
    @DisplayName("duration renders the three rows of the rule in both languages")
    void durationTable(long seconds, String english, String german) {
        assertEquals(english, Display.duration("en", seconds));
        assertEquals(german, Display.duration("de", seconds));
    }

    static Stream<Arguments> times() {
        return Stream.of(
                Arguments.of(NOW.minusSeconds(30), "just now", "gerade eben"),
                Arguments.of(NOW.minusSeconds(300), "5 min ago", "vor 5 min"),
                Arguments.of(NOW.minusSeconds(3 * 3600), "3 h ago", "vor 3 h"),
                Arguments.of(NOW.minusSeconds(9 * 86400), "9 d ago", "vor 9 T"),
                Arguments.of(NOW.minusSeconds(45 * 86400), "last month", "letzten Monat"),
                Arguments.of(NOW.minusSeconds(59 * 86400), "last month", "letzten Monat"),
                Arguments.of(NOW.minusSeconds(60 * 86400), "2 months ago", "vor 2 Monaten"),
                Arguments.of(NOW.minusSeconds(400 * 86400), "last year", "letztes Jahr"),
                Arguments.of(NOW.minusSeconds(800 * 86400), "2 years ago", "vor 2 Jahren"),
                Arguments.of(NOW.minusSeconds(17 * 3600 + 20 * 60), "17 h ago", "vor 17 h"));
    }

    @ParameterizedTest(name = "{0} → {1} / {2}")
    @MethodSource("times")
    @DisplayName("time renders the age rule in both languages, never a bare yesterday")
    void timeTable(Instant instant, String english, String german) {
        assertEquals(english, Display.time("en", instant, CLOCK));
        assertEquals(german, Display.time("de", instant, CLOCK));
    }

    @Test
    @DisplayName("time SHORT renders the moment itself to the minute")
    void shortStyle() {
        assertEquals("today 10:00", Display.time("en", NOW.minusSeconds(2 * 3600), Display.TimeStyle.SHORT, CLOCK));
        assertEquals("heute 10:00", Display.time("de", NOW.minusSeconds(2 * 3600), Display.TimeStyle.SHORT, CLOCK));
        assertEquals("yesterday 11:00", Display.time("en", NOW.minusSeconds(86400 + 3600), Display.TimeStyle.SHORT, CLOCK));
        assertEquals("gestern 11:00", Display.time("de", NOW.minusSeconds(86400 + 3600), Display.TimeStyle.SHORT, CLOCK));
        assertEquals("Sun 12:00", Display.time("en", NOW.minusSeconds(2 * 86400), Display.TimeStyle.SHORT, CLOCK));
        assertEquals("5 Oct, 12:00", Display.time("en", NOW.minusSeconds(15 * 86400), Display.TimeStyle.SHORT, CLOCK));
        assertEquals("14:02:11", Display.time("en", Instant.parse("2026-10-20T14:02:11Z"),
                Display.TimeStyle.CLOCK, CLOCK));
    }

    @Test
    @DisplayName("time passes a non-instant string through unchanged")
    void timeStringOverload() {
        assertEquals("", Display.time("en", (String) null, CLOCK));
        assertEquals("", Display.time("en", "  ", CLOCK));
        assertEquals("not-an-instant", Display.time("en", "not-an-instant", CLOCK));
        assertEquals("2 h ago", Display.time("en", "2026-10-20T10:00:00Z", CLOCK));
    }

    @Test
    @DisplayName("word reads the shared state key and refuses a raw value with no text")
    void wordReadsStateKey() {
        assertEquals("running", Display.word("en", "plan", "running"));
        assertEquals("läuft", Display.word("de", "plan", "running"));
        assertThrows(IllegalArgumentException.class, () -> Display.word("en", "plan", "raw_state_id"));
        assertThrows(IllegalArgumentException.class, () -> Display.word("en", "plan", null));
    }

    @Test
    @DisplayName("title prefers the title, renders a unit id, and refuses an id alone")
    void titleNeverShowsAnIdAlone() {
        assertEquals("Fix Relay", Display.title("en", "Fix Relay", "fix-relay"));
        assertEquals("Pull request", Display.title("en", null, "landing.pull-request"));
        assertThrows(IllegalArgumentException.class, () -> Display.title("en", null, "fix-relay"));
        assertThrows(IllegalArgumentException.class, () -> Display.title("en", "  ", "fix-relay"));
    }
}
