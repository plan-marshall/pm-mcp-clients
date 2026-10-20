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
 * The {@code pm-operator plan fix-relay --all} dialogue against the mock runtime's plan-view
 * scenario (PM-TEST-7): the scrollback tree, its states, tasks, sub-workflows, the lane's pruned
 * phases, the tokens, and the worker activity are compared with {@code plan-view.transcript}.
 */
@DisplayName("pm-operator plan view dialogue transcript")
@EnabledOnOs({OS.MAC, OS.LINUX})
class PlanViewTranscriptTest {

    @Test
    @DisplayName("renders the plan view scenario exactly as recorded")
    void rendersPlanViewScenario() throws Exception {
        TranscriptDialogues.planView().assertRecorded();
    }
}
