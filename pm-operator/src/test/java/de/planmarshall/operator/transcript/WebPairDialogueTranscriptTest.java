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
 * The {@code pm-operator web pair} dialogue against the mock runtime's pairing scenario
 * (PM-TEST-7): the verb issues a pairing code and its QR code, and the rendered terminal is
 * compared with {@code web-pair.transcript}.
 */
@DisplayName("pm-operator web pair dialogue transcript")
@EnabledOnOs({OS.MAC, OS.LINUX})
class WebPairDialogueTranscriptTest {

    @Test
    @DisplayName("renders the pairing dialogue exactly as recorded")
    void rendersPairingScenario() throws Exception {
        TranscriptDialogues.webPair().assertRecorded();
    }
}
