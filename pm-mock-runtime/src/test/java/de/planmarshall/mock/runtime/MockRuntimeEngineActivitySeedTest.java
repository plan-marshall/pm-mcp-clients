/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.StandardScenarios;

/**
 * The activity seed of the mock runtime: a task whose explicit activity route is empty must not
 * receive the synthetic live event, so the web app can show the empty feed.
 */
@DisplayName("MockRuntimeEngine activity seeding")
@EnabledOnOs({OS.MAC, OS.LINUX})
class MockRuntimeEngineActivitySeedTest {

    @Test
    @DisplayName("recognises an explicit empty activity route and leaves a populated one alone")
    void explicitEmptyRoute() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.RUNNING_WORK)) {
            var engine = runtime.engine();

            assertTrue(engine.hasEmptyActivityRoute("session-log", "TASK-002"));
            assertTrue(engine.hasEmptyActivityRoute("fix-relay", "TASK-002"));
            assertFalse(engine.hasEmptyActivityRoute("session-log", "TASK-003"));
            assertFalse(engine.hasEmptyActivityRoute("session-log", "TASK-UNKNOWN"));
        }
    }
}
