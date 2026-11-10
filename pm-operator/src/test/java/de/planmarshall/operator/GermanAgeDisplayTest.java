/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import de.planmarshall.api.overview.AlongsideLanding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The ages of {@code projects} and {@code status}, rendered through the one display rule at the
 * context's clock instead of the wall clock.
 */
@DisplayName("Ages of projects and status")
class GermanAgeDisplayTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Europe/Berlin"));

    @Test
    @DisplayName("projects and status print a German session's ages from the display rule")
    void germanAges() {
        assertEquals("gerade eben", ProjectsCommand.formatRelativeTime("de", NOW.minusSeconds(30), CLOCK));
        assertEquals("vor 5 min", ProjectsCommand.formatRelativeTime("de", NOW.minusSeconds(300), CLOCK));
        assertEquals("vor 3 h", ProjectsCommand.formatRelativeTime("de", NOW.minusSeconds(3 * 3600), CLOCK));
        assertEquals("vor 9 T", ProjectsCommand.formatRelativeTime("de", NOW.minusSeconds(9 * 86400), CLOCK));
        assertEquals("vor 2 Monaten", ProjectsCommand.formatRelativeTime("de", NOW.minusSeconds(60 * 86400), CLOCK));

        assertEquals("gerade eben", StatusCommand.formatRelativeTime("de", NOW.minusSeconds(30), CLOCK));
        assertEquals("vor 5 min", StatusCommand.formatRelativeTime("de", NOW.minusSeconds(300), CLOCK));
        assertEquals("vor 9 T", StatusCommand.formatRelativeTime("de", NOW.minusSeconds(9 * 86400), CLOCK));
    }

    @Test
    @DisplayName("ages count from the context clock, not the wall clock")
    void agesFollowTheContextClock() {
        assertEquals("5 min ago", ProjectsCommand.formatRelativeTime("en", NOW.minusSeconds(300), CLOCK));
        assertEquals("5 min ago", StatusCommand.formatRelativeTime("en", NOW.minusSeconds(300).toString(), CLOCK));
    }

    @Test
    @DisplayName("a missing ended time says unknown, never just now")
    void missingTimeIsUnknown() {
        assertEquals("unknown", StatusCommand.formatRelativeTime("en", null, CLOCK));
        assertEquals("unbekannt", StatusCommand.formatRelativeTime("de", null, CLOCK));
    }

    @Test
    @DisplayName("the alongside landings speak through their catalogue words in both languages")
    void alongsideLandingsAreLocalized() {
        var failedUnit = new AlongsideLanding("remediation.ci-failure", "failed", null, "unit", "landing_failed");
        var removal = new AlongsideLanding(null, "running", 2, "removal", null);

        var en = new StringWriter();
        StatusCommand.renderAlongsideLandings(new PrintWriter(en, true), List.of(failedUnit, removal), "en");
        assertTrue(en.toString().contains("! landing failed"), en.toString());
        assertTrue(en.toString().contains("CI failure \u00b7 failed"), en.toString());
        assertTrue(en.toString().contains("removing 2 moved documents from git \u00b7 running"), en.toString());

        var de = new StringWriter();
        StatusCommand.renderAlongsideLandings(new PrintWriter(de, true), List.of(failedUnit, removal), "de");
        assertTrue(de.toString().contains("! Landen fehlgeschlagen"), de.toString());
        assertTrue(de.toString().contains("CI-Fehler \u00b7 fehlgeschlagen"), de.toString());
        assertTrue(de.toString().contains("entferne 2 verschobene Dokumente aus git \u00b7 l\u00e4uft"), de.toString());
        assertFalse(de.toString().contains("remediation."), de.toString());
    }

    @Test
    @DisplayName("each of the unit-title sites renders the placeholder for a missing or unknown unit")
    void unitTitleSitesHandleMissingAndUnknownUnit() {
        var missing = new AlongsideLanding(null, "running", null, "unit", null);
        var unknown = new AlongsideLanding("no.such.unit", "running", null, "unit", null);

        var statusEn = new StringWriter();
        StatusCommand.renderAlongsideLandings(new PrintWriter(statusEn, true), List.of(missing, unknown), "en");
        assertTrue(statusEn.toString().contains("landing unknown \u00b7 running"), statusEn.toString());
        assertFalse(statusEn.toString().contains("no.such.unit"), statusEn.toString());

        var statusDe = new StringWriter();
        StatusCommand.renderAlongsideLandings(new PrintWriter(statusDe, true), List.of(missing, unknown), "de");
        assertTrue(statusDe.toString().contains("lande unbekannt \u00b7 l\u00e4uft"), statusDe.toString());
        assertFalse(statusDe.toString().contains("no.such.unit"), statusDe.toString());

        assertEquals("unknown", EpicCommand.landingTitle("en", null));
        assertEquals("unbekannt", EpicCommand.landingTitle("de", ""));
        assertEquals("unknown · done", EpicCommand.landingTitle("en", "no.such.unit · done"));
        assertEquals("unbekannt · fertig", EpicCommand.landingTitle("de", "no.such.unit · done"));
        assertEquals("CI failure · done", EpicCommand.landingTitle("en", "remediation.ci-failure · done"));
        assertEquals("CI-Fehler · fertig", EpicCommand.landingTitle("de", "remediation.ci-failure · done"));
    }
}
