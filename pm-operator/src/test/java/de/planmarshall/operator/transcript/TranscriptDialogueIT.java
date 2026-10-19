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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * The transcript dialogues under the integration-test profile (PM-TEST-7). The scenarios are the
 * same as those of the unit-level transcript tests, so {@code -Pintegration-tests} covers the
 * recorded transcripts as well.
 */
@DisplayName("pm-operator transcript dialogues")
@EnabledOnOs({OS.MAC, OS.LINUX})
class TranscriptDialogueIT {

    @Test
    @DisplayName("status dialogue matches its transcript")
    void statusDialogue() throws Exception {
        TranscriptDialogues.status().assertRecorded();
    }

    @Test
    @DisplayName("web pair dialogue matches its transcript")
    void webPairDialogue() throws Exception {
        TranscriptDialogues.webPair().assertRecorded();
    }

    @Test
    @DisplayName("console dialogue matches its transcript")
    void consoleDialogue() throws Exception {
        TranscriptDialogues.consoleStatus().assertRecorded();
    }

    @Test
    @DisplayName("console settings dialogue matches its transcript")
    void consoleSettingsDialogue() throws Exception {
        TranscriptDialogues.consoleSettings().assertRecorded();
    }
}
