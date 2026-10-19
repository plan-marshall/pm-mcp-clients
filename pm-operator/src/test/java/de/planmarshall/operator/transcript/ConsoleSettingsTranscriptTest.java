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
 * The machine settings in the console (UC-ONB-10): the settings screen, the paired devices, and
 * the remembered consents, typed at the prompt and compared with a recorded transcript.
 */
@DisplayName("pm-operator console settings transcript")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ConsoleSettingsTranscriptTest {

    @Test
    @DisplayName("renders the machine settings, the devices, and the consents as recorded")
    void machineSettings() throws Exception {
        TranscriptDialogues.consoleSettings().assertRecorded();
    }
}
