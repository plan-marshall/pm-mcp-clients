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
 * The {@code pm-operator status} dialogue against the mock runtime's overview scenario
 * (PM-TEST-7): the verb runs with the terminal a transcript test drives, and every rendered
 * character is compared with {@code status.transcript}.
 */
@DisplayName("pm-operator status dialogue transcript")
@EnabledOnOs({OS.MAC, OS.LINUX})
class StatusDialogueTranscriptTest {

    @Test
    @DisplayName("renders the status overview exactly as recorded")
    void rendersStatusScenario() throws Exception {
        TranscriptDialogues.status().assertRecorded();
    }
}
