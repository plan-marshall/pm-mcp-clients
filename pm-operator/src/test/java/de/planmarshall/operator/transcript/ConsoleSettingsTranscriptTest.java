/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The machine settings in the console (UC-ONB-10): the settings screen, the paired devices, and
 * the remembered consents, typed at the prompt and compared with a recorded transcript.
 */
@DisplayName("pm-operator console settings transcript")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ConsoleSettingsTranscriptTest {

    private static final Instant BASE_INSTANT = Instant.parse("2026-10-18T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(BASE_INSTANT, ZoneId.of("UTC"));
    private static final Clock FIXED_CLOCK_120_DAYS = Clock.fixed(BASE_INSTANT.plus(Duration.ofDays(120)), ZoneId.of("UTC"));

    @Test
    @DisplayName("renders the machine settings, the devices, and the consents as recorded")
    void machineSettings() throws Exception {
        var dialogue = TranscriptDialogues.consoleSettings(FIXED_CLOCK);
        assertTrue(dialogue.rendered().contains("last used 1 d ago"),
                            () -> "raw output should reflect fixed clock: " + dialogue.rendered());
        dialogue.assertRecorded();
    }

    @Test
    @DisplayName("the transcript test passes with the clock moved forward by 120 days")
    void machineSettingsClockMovedForward120Days() throws Exception {
        var dialogue = TranscriptDialogues.consoleSettings(FIXED_CLOCK_120_DAYS);
        assertTrue(dialogue.rendered().contains("last used 16 Oct 14:02"),
                            () -> "raw output should reflect absolute date with clock 120 days ahead: " + dialogue.rendered());
        dialogue.assertRecorded();
    }

    @Test
    @DisplayName("renders the German machine settings and the devices as recorded")
    void machineSettingsGerman() throws Exception {
        var dialogue = TranscriptDialogues.consoleSettingsGerman(FIXED_CLOCK_120_DAYS);
        assertTrue(dialogue.rendered().contains("zuletzt benutzt"),
                            () -> "the German row should carry the catalogue's words: " + dialogue.rendered());
        dialogue.assertRecorded();
    }

    @Test
    @DisplayName("the printed ages follow the injected clock, not the system clock")
    void machineSettingsFollowTheInjectedClock() throws Exception {
        var atBaseInstant = TranscriptDialogues.consoleSettings(FIXED_CLOCK);
        var oneHundredTwentyDaysLater = TranscriptDialogues.consoleSettings(FIXED_CLOCK_120_DAYS);

        assertNotEquals(atBaseInstant.withoutMachineRoot(), oneHundredTwentyDaysLater.withoutMachineRoot(),
                            "a console reading the system clock prints the same ages for both clocks");
        assertTrue(atBaseInstant.rendered().contains("Firefox on mbp-oliver")
                            && atBaseInstant.rendered().contains("last used 1 d ago"));
        assertTrue(atBaseInstant.rendered().contains("Safari on iPad")
                            && atBaseInstant.rendered().contains("last used 5 d ago"));
        assertTrue(atBaseInstant.rendered().contains("Chrome on build-box")
                            && atBaseInstant.rendered().contains("last used 5 weeks ago"));
        assertTrue(oneHundredTwentyDaysLater.rendered().contains("Firefox on mbp-oliver")
                            && oneHundredTwentyDaysLater.rendered().contains("last used 16 Oct 14:02"));
        assertTrue(oneHundredTwentyDaysLater.rendered().contains("Safari on iPad")
                            && oneHundredTwentyDaysLater.rendered().contains("last used 13 Oct 10:00"));
        assertTrue(oneHundredTwentyDaysLater.rendered().contains("Chrome on build-box")
                            && oneHundredTwentyDaysLater.rendered().contains("last used 8 Sep 10:00"));
    }
}
