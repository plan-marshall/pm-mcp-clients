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
 * Starting a plan at the terminal (UC-STA-1 to UC-STA-5): the interactive source list, the ready
 * step, and the same start as one shell command with {@code --no-input}, both compared with their
 * recorded transcripts.
 */
@DisplayName("pm-operator starting transcript")
@EnabledOnOs({OS.MAC, OS.LINUX})
class StartTranscriptTest {

    @Test
    @DisplayName("an interactive plan start matches its transcript")
    void interactivePlanStart() throws Exception {
        TranscriptDialogues.startPlan().assertRecorded();
    }

    @Test
    @DisplayName("plan add --no-input matches its transcript")
    void shellPlanStart() throws Exception {
        TranscriptDialogues.startPlanNoInput().assertRecorded();
    }
}
