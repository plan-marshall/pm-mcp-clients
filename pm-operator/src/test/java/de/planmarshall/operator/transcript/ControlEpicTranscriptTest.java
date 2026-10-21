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
 * The {@code pm-operator epic relay-hardening} dialogue against the mock runtime's running-work
 * scenario: the epic's head, workstreams and plans with their waits, sources, landings,
 * registers, tokens, and events are compared with {@code control-epic.transcript}.
 */
@DisplayName("pm-operator epic view dialogue transcript")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ControlEpicTranscriptTest {

    @Test
    @DisplayName("renders the epic resource exactly as recorded")
    void rendersControlEpic() throws Exception {
        TranscriptDialogues.controlEpic().assertRecorded();
    }
}
