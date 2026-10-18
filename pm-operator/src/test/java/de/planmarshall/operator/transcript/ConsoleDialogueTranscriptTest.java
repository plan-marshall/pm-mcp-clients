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
 * The console at work (PM-TEST-7): the test types {@code status} and {@code exit} at the
 * console's prompt, JLine reads them from the scripted terminal, and the whole dialogue, prompt,
 * echoed input, and rendered status, is compared with {@code console-status.transcript}.
 */
@DisplayName("pm-operator console dialogue transcript")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ConsoleDialogueTranscriptTest {

    @Test
    @DisplayName("runs a typed status dialogue exactly as recorded")
    void runsTypedStatusDialogue() throws Exception {
        TranscriptDialogues.consoleStatus().assertRecorded();
    }
}
