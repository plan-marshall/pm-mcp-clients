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

import java.nio.file.Path;
import java.util.UUID;


import lombok.Builder;
import lombok.Value;

import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;

/**
 * Configuration options for starting a {@link MockRuntime} instance.
 */
@Value
@Builder(toBuilder = true)
public class MockRuntimeConfig {

    /** The machine root directory {@code <PM_MCP_BASE>}, or {@code null} to create a temporary directory. */
    Path base;

    /** The initial scenario name (e.g. {@code status-only}) or file path. */
    @Builder.Default
    String scenario = StandardScenarios.STATUS_ONLY;

    /**
     * Port for the loopback web TCP listener: {@code -1} for disabled, {@code 0} for ephemeral, or
     * a specific positive port.
     */
    @Builder.Default
    int webPort = -1;

    /** The accepted runtime token for Unix socket auth, or {@code null} to generate one. */
    String runtimeToken;

    /** The accepted device secret for web auth, or {@code null} to generate one. */
    String deviceSecret;

    /** Whether to recursively delete the base directory on {@link MockRuntime#close()}. */
    Boolean cleanupBaseOnClose;

    /** An optional preconfigured scenario registry. If {@code null}, a new registry is instantiated. */
    ScenarioRegistry registry;

    /** An optional preloaded scenario instance. If specified, takes precedence as the initial scenario. */
    Scenario initialScenario;

    /**
     * @return the runtime token or a generated default
     */
    public String effectiveRuntimeToken() {
        return runtimeToken != null && !runtimeToken.isBlank() ? runtimeToken
                : "mock-token-" + UUID.randomUUID();
    }

    /**
     * @return the device secret or a generated default
     */
    public String effectiveDeviceSecret() {
        return deviceSecret != null && !deviceSecret.isBlank() ? deviceSecret
                : "pmw_mock_secret_" + UUID.randomUUID().toString().replace("-", "");
    }
}
