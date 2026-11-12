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
import java.util.concurrent.ScheduledExecutorService;


import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import de.planmarshall.mock.runtime.MockTiming;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;

/**
 * Configuration options for starting a {@link MockRuntime} instance.
 */
@Value
@Builder(toBuilder = true)
public class MockRuntimeConfig {

    /** The machine root directory {@code <PM_MCP_BASE>}, or {@code null} to create a temporary directory. */
    @Nullable
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
    @Nullable
    String runtimeToken;

    /** The accepted device secret for web auth, or {@code null} to generate one. */
    @Nullable
    String deviceSecret;

    /** Whether to recursively delete the base directory on {@link MockRuntime#close()}. */
    @Nullable
    Boolean cleanupBaseOnClose;

    /**
     * Whether {@code GET /mock/acts} records the request body of decision acts. Off by default, so a
     * developer's mock keeps no free-text answer, note or waiver reason in a control surface; the
     * dialogue tests turn it on to assert what the operator sent.
     */
    @Builder.Default
    boolean recordActBodies = false;

    /**
     * How scenario events and a chat's reply are played: {@link MockTiming#REALISTIC} (the default)
     * each after its delay, {@link MockTiming#ORDERED} at once and in order, for a test that asserts a
     * sequence without waiting on time.
     */
    @Builder.Default
    MockTiming timing = MockTiming.REALISTIC;

    /** An optional preconfigured scenario registry. If {@code null}, a new registry is instantiated. */
    @Nullable
    ScenarioRegistry registry;

    /** An optional preloaded scenario instance. If specified, takes precedence as the initial scenario. */
    @Nullable
    Scenario initialScenario;

    /**
     * An optional single-threaded scheduler for the states' timed events, their auto transitions and
     * the drain's steps; {@code null} for the engine's own. A test supplies its own to hold the timed
     * events until it lets them run, instead of waiting for their delay. The runtime shuts it down on
     * {@link MockRuntime#close()}.
     */
    @Nullable
    ScheduledExecutorService scheduler;

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
