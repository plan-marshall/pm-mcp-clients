/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StandardScenarios")
class StandardScenariosTest {

    @Test
    @DisplayName("defines all standard scenario constants correctly")
    void shouldDefineStandardConstants() {
        assertEquals("status-only", StandardScenarios.STATUS_ONLY);
        assertEquals("onboarding-happy", StandardScenarios.ONBOARDING_HAPPY);
        assertEquals("onboarding-harness-not-logged-in", StandardScenarios.ONBOARDING_HARNESS_NOT_LOGGED_IN);
        assertEquals("web-pairing", StandardScenarios.WEB_PAIRING);
        assertEquals("paired-devices", StandardScenarios.PAIRED_DEVICES);

        assertEquals(6, StandardScenarios.ALL.size());
        assertTrue(StandardScenarios.ALL.contains(StandardScenarios.STATUS_ONLY));
        assertTrue(StandardScenarios.ALL.contains(StandardScenarios.ONBOARDING_HAPPY));
        assertTrue(StandardScenarios.ALL.contains(StandardScenarios.ONBOARDING_HARNESS_NOT_LOGGED_IN));
        assertTrue(StandardScenarios.ALL.contains(StandardScenarios.WEB_PAIRING));
        assertTrue(StandardScenarios.ALL.contains(StandardScenarios.PAIRED_DEVICES));
        assertTrue(StandardScenarios.ALL.contains(StandardScenarios.RUNNING_WORK));
    }

    @Test
    @DisplayName("utility class constructor throws UnsupportedOperationException")
    void shouldRefuseInstantiation() throws Exception {
        var constructor = StandardScenarios.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, ex.getCause());
    }
}
