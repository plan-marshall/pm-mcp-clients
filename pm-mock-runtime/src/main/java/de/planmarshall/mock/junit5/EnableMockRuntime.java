/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.junit5;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


import org.junit.jupiter.api.extension.ExtendWith;

import de.planmarshall.mock.StandardScenarios;

/**
 * JUnit 5 annotation that enables, configures, and manages the lifecycle of a
 * {@link de.planmarshall.mock.MockRuntime} instance for testing.
 *
 * <h2>Basic Usage</h2>
 * <pre>
 * &#64;EnableMockRuntime
 * class SimpleClientTest {
 *
 *     &#64;Test
 *     void shouldConnectToRuntime(MockRuntime runtime, RuntimeClient client) throws Exception {
 *         var status = client.status();
 *         assertNotNull(status);
 *     }
 * }
 * </pre>
 *
 * <h2>Custom Scenario Selection</h2>
 * <pre>
 * &#64;EnableMockRuntime(scenario = StandardScenarios.ONBOARDING_HAPPY)
 * class OnboardingTest {
 *
 *     &#64;Test
 *     void shouldBeInOnboardingMode(MockRuntime runtime) {
 *         assertEquals("onboard.mode", runtime.currentStateName());
 *     }
 *
 *     &#64;Test
 *     &#64;EnableMockRuntime(scenario = StandardScenarios.WEB_PAIRING, webPort = 0)
 *     void shouldSupportWebPairing(MockRuntime runtime, URI loopbackUrl) {
 *         assertTrue(runtime.webPort() &gt; 0);
 *     }
 * }
 * </pre>
 *
 * <h2>Parameter Injection</h2>
 * The extension supports injecting the following parameter types into test methods:
 * <ul>
 *   <li>{@link de.planmarshall.mock.MockRuntime}: the running mock runtime server</li>
 *   <li>{@link de.planmarshall.api.runtime.RuntimeClient}: a pre-authenticated client targeting the Unix domain socket</li>
 *   <li>{@link de.planmarshall.api.MachinePaths}: machine path layout of the running runtime</li>
 *   <li>{@link java.nio.file.Path}: the base machine directory ({@code PM_MCP_BASE})</li>
 *   <li>{@link java.net.URI}: loopback web URL when {@link #webPort()} &ge; 0 (e.g. {@code http://127.0.0.1:port})</li>
 *   <li>{@link de.planmarshall.mock.scenario.Scenario}: active scenario definition</li>
 * </ul>
 *
 * @see MockRuntimeExtension
 * @see StandardScenarios
 */
@Documented
@Inherited
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@ExtendWith(MockRuntimeExtension.class)
public @interface EnableMockRuntime {

    /**
     * Initial scenario name or file path to activate.
     * Defaults to {@link StandardScenarios#STATUS_ONLY}.
     *
     * @return the initial scenario identifier
     */
    String scenario() default StandardScenarios.STATUS_ONLY;

    /**
     * Port for the loopback web TCP listener: {@code -1} for disabled (default),
     * {@code 0} for ephemeral, or a specific positive port number.
     *
     * @return the loopback TCP port
     */
    int webPort() default -1;

    /**
     * Fixed runtime token for Unix domain socket authentication. If empty, a secure mock token is generated.
     *
     * @return fixed token or empty string for generated default
     */
    String token() default "";

    /**
     * Fixed device secret for web pairing authentication. If empty, a secure mock secret is generated.
     *
     * @return fixed device secret or empty string for generated default
     */
    String deviceSecret() default "";

    /**
     * Controls the automatic startup behavior.
     * When {@code true}, the server is not started automatically before each test, allowing
     * manual startup within the test method.
     *
     * @return {@code true} for manual startup, {@code false} for automatic startup (default)
     */
    boolean manualStart() default false;

    /**
     * Whether to clean up the temporary {@code PM_MCP_BASE} directory when closing the runtime.
     * Defaults to {@code true}.
     *
     * @return {@code true} if base directory should be removed on shutdown
     */
    boolean cleanupBaseOnClose() default true;
}
