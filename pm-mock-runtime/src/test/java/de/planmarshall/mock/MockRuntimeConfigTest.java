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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;
import de.planmarshall.mock.scenario.StateDefinition;

@DisplayName("MockRuntimeConfig")
@EnableGeneratorController
class MockRuntimeConfigTest {

    @Test
    @DisplayName("builder provides sensible defaults")
    void defaults() {
        var config = MockRuntimeConfig.builder().build();

        assertEquals("status-only", config.getScenario());
        assertNull(config.getBase());
        assertEquals(-1, config.getWebPort());
        assertNull(config.getRuntimeToken());
        assertNull(config.getDeviceSecret());
        assertNull(config.getCleanupBaseOnClose());

        assertNotNull(config.effectiveRuntimeToken());
        assertFalse(config.effectiveRuntimeToken().isBlank());
        assertTrue(config.effectiveDeviceSecret().startsWith("pmw_mock_secret_"));
    }

    @Test
    @DisplayName("builder applies custom values")
    void customValues() {
        var base = Path.of("/tmp", Generators.letterStrings(4, 10).next());
        var registry = new ScenarioRegistry();
        var stateName = Generators.letterStrings(4, 10).next();
        var scenario = new Scenario(1, "in-mem", "desc", stateName, null, Map.of(),
                Map.of(stateName, new StateDefinition(Map.of("ok", true), Map.of(), List.of(), null)));
        var scenarioName = Generators.letterStrings(4, 12).next();
        var webPort = Generators.integers(1024, 65535).next().intValue();
        var token = Generators.letterStrings(8, 20).next();
        var secret = Generators.letterStrings(8, 20).next();
        var config = MockRuntimeConfig.builder()
                .scenario(scenarioName)
                .base(base)
                .webPort(webPort)
                .runtimeToken(token)
                .deviceSecret(secret)
                .cleanupBaseOnClose(false)
                .registry(registry)
                .initialScenario(scenario)
                .build();

        assertEquals(scenarioName, config.getScenario());
        assertEquals(base, config.getBase());
        assertEquals(webPort, config.getWebPort());
        assertEquals(token, config.getRuntimeToken());
        assertEquals(token, config.effectiveRuntimeToken());
        assertEquals(secret, config.getDeviceSecret());
        assertEquals(secret, config.effectiveDeviceSecret());
        assertFalse(config.getCleanupBaseOnClose());
        assertSame(registry, config.getRegistry());
        assertSame(scenario, config.getInitialScenario());
    }

    @Test
    @DisplayName("verifies equals, hashCode, and toString")
    void equalsHashCodeToString() {
        var scenarioName = Generators.letterStrings(4, 12).next();
        var webPort = Generators.integers(1024, 65535).next().intValue();
        var c1 = MockRuntimeConfig.builder().scenario(scenarioName).webPort(webPort).build();
        var c2 = MockRuntimeConfig.builder().scenario(scenarioName).webPort(webPort).build();

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertTrue(c1.toString().contains("scenario=" + scenarioName));
    }
}
