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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.TreeSet;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StandardScenarios")
class StandardScenariosTest {

    @Test
    @DisplayName("defines all standard scenario constants correctly")
    void shouldDefineStandardConstants() throws Exception {
        assertEquals("status-only", StandardScenarios.STATUS_ONLY);
        assertEquals("onboarding-happy", StandardScenarios.ONBOARDING_HAPPY);
        assertEquals("onboarding-harness-not-logged-in", StandardScenarios.ONBOARDING_HARNESS_NOT_LOGGED_IN);
        assertEquals("web-pairing", StandardScenarios.WEB_PAIRING);
        assertEquals("entry", StandardScenarios.ENTRY);
        assertEquals("paired-devices", StandardScenarios.PAIRED_DEVICES);
        assertEquals("running-work", StandardScenarios.RUNNING_WORK);
        assertEquals("decisions", StandardScenarios.DECISIONS);
        assertEquals("system", StandardScenarios.SYSTEM);
        assertEquals("projects", StandardScenarios.PROJECTS);
        assertEquals("plan-view", StandardScenarios.PLAN_VIEW);

        // ALL lists exactly the scenario constants declared here. Reading them by reflection keeps
        // the test correct when a scenario is added, so it does not break on a sibling change.
        var expected = new TreeSet<String>();
        for (var field : StandardScenarios.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                expected.add((String) field.get(null));
            }
        }
        assertEquals(expected, new TreeSet<>(StandardScenarios.ALL));
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
