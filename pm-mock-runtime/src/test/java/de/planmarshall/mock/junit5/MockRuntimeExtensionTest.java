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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.scenario.Scenario;

@DisplayName("MockRuntimeExtension")
@EnabledOnOs({OS.MAC, OS.LINUX})
class MockRuntimeExtensionTest {

    @Nested
    @DisplayName("with default configuration")
    @EnableMockRuntime
    class DefaultConfigurationTest {

        @Test
        @DisplayName("injects MockRuntime, RuntimeClient, MachinePaths, Path, and Scenario")
        void shouldInjectStandardParameters(
                MockRuntime runtime,
                RuntimeClient client,
                MachinePaths paths,
                Path base,
                Scenario scenario,
                MockRuntimeConfig config) throws Exception {

            assertNotNull(runtime);
            assertNotNull(client);
            assertNotNull(paths);
            assertNotNull(base);
            assertNotNull(scenario);
            assertNotNull(config);

            assertEquals(StandardScenarios.STATUS_ONLY, runtime.currentScenarioName());
            assertEquals("runtime.idle", runtime.currentStateName());
            assertEquals(base, runtime.base());
            assertEquals(paths.socket(), runtime.socketPath());
            assertEquals(StandardScenarios.STATUS_ONLY, scenario.name());
            assertEquals(StandardScenarios.STATUS_ONLY, config.getScenario());

            assertTrue(Files.exists(base), "Base directory must exist");
            assertTrue(Files.exists(paths.socket()), "Unix socket must exist");

            // Client roundtrip
            var response = client.send(HttpRequest.get("/api/v1/status"));
            assertEquals(200, response.status());
        }

        @Test
        @DisplayName("overrides class-level scenario at method level")
        @EnableMockRuntime(scenario = StandardScenarios.ONBOARDING_HAPPY)
        void shouldSupportMethodLevelScenarioOverride(MockRuntime runtime) {
            assertEquals(StandardScenarios.ONBOARDING_HAPPY, runtime.currentScenarioName());
            assertEquals("onboard.mode", runtime.currentStateName());
        }
    }

    @Nested
    @DisplayName("with web TCP listener enabled")
    @EnableMockRuntime(scenario = StandardScenarios.WEB_PAIRING, webPort = 0)
    class WebPortTest {

        @Test
        @DisplayName("injects URI pointing to loopback web listener")
        void shouldInjectLoopbackUri(MockRuntime runtime, URI loopbackUrl) {
            assertNotNull(loopbackUrl);
            assertTrue(runtime.webPort() > 0);
            assertEquals("http", loopbackUrl.getScheme());
            assertEquals("127.0.0.1", loopbackUrl.getHost());
            assertEquals(runtime.webPort(), loopbackUrl.getPort());
        }
    }

    @Nested
    @DisplayName("with manual start")
    @EnableMockRuntime(manualStart = true)
    class ManualStartTest {

        @Test
        @DisplayName("does not start automatically when manualStart is true")
        void shouldSupportManualStart(MockRuntimeConfig config) throws Exception {
            assertNotNull(config);
            // Server was not started automatically
            try (var runtime = MockRuntime.start(config)) {
                assertNotNull(runtime.base());
                assertTrue(Files.exists(runtime.base()));
                assertEquals(StandardScenarios.STATUS_ONLY, runtime.currentScenarioName());
            }
        }
    }

    @Nested
    @DisplayName("parameter resolution failure modes")
    @EnableMockRuntime(webPort = -1)
    class FailureModesTest {

        @Test
        @DisplayName("throws ParameterResolutionException when injecting URI with webPort disabled")
        void shouldRejectUriWhenWebPortDisabled(MockRuntime runtime) {
            var extension = new MockRuntimeExtension();
            var ctx = createMockExtensionContext(runtime);
            assertThrows(ParameterResolutionException.class, () ->
                    extension.resolveParameter(new DummyParameterContext(URI.class), ctx));
        }

        @Test
        @DisplayName("rejects unsupported parameter types")
        void shouldRejectUnsupportedParameter(MockRuntime runtime) {
            var extension = new MockRuntimeExtension();
            var ctx = createMockExtensionContext(runtime);
            assertFalse(extension.supportsParameter(new DummyParameterContext(String.class), ctx));
            assertThrows(ParameterResolutionException.class, () ->
                    extension.resolveParameter(new DummyParameterContext(String.class), ctx));
        }

        private ExtensionContext createMockExtensionContext(MockRuntime runtime) {
            var map = new HashMap<Object, Object>();
            map.put(MockRuntime.class.getName(), runtime);

            var store = (ExtensionContext.Store) java.lang.reflect.Proxy.newProxyInstance(
                    ExtensionContext.Store.class.getClassLoader(),
                    new Class<?>[]{ExtensionContext.Store.class},
                    (_, method, args) -> {
                        if ("get".equals(method.getName()) && args != null && args.length == 1) {
                            return map.get(args[0]);
                        }
                        if ("put".equals(method.getName()) && args != null && args.length == 2) {
                            map.put(args[0], args[1]);
                            return null;
                        }
                        return null;
                    }
            );

            return (ExtensionContext) java.lang.reflect.Proxy.newProxyInstance(
                    ExtensionContext.class.getClassLoader(),
                    new Class<?>[]{ExtensionContext.class},
                    (_, method, _) -> {
                        if ("getStore".equals(method.getName())) {
                            return store;
                        }
                        return null;
                    }
            );
        }
    }

    private static class DummyParameterContext implements ParameterContext {
        private final Class<?> type;

        DummyParameterContext(Class<?> type) {
            this.type = type;
        }

        @Override
        public java.lang.reflect.Parameter getParameter() {
            for (var m : DummyMethods.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(type)) {
                    return m.getParameters()[0];
                }
            }
            throw new IllegalArgumentException("No dummy parameter found for: " + type);
        }

        @Override
        public int getIndex() {
            return 0;
        }

        @Override
        public Optional<Object> getTarget() {
            return Optional.empty();
        }

        @Override
        public boolean isAnnotated(Class<? extends java.lang.annotation.Annotation> a) {
            return false;
        }

        @Override
        public <A extends java.lang.annotation.Annotation> Optional<A> findAnnotation(Class<A> a) {
            return Optional.empty();
        }

        @Override
        public <A extends java.lang.annotation.Annotation> List<A> findRepeatableAnnotations(Class<A> a) {
            return List.of();
        }
    }

    private static class DummyMethods {
        void dummyUri(URI uri) {
        }

        void dummyString(String str) {
        }
    }
}
