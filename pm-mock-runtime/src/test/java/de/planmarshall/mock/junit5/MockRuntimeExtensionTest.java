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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
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

            assertEquals(StandardScenarios.STATUS_ONLY, scenario.name());
            assertEquals(StandardScenarios.STATUS_ONLY, runtime.currentScenarioName());
            assertEquals(base, runtime.base());
            assertEquals(paths.socket(), runtime.socketPath());

            assertTrue(client.isLive());

            try (var exchange = client.open(HttpRequest.get("/api/v1/status"))) {
                assertEquals(200, exchange.status());
            }
        }
    }

    @Nested
    @DisplayName("with custom scenario and loopback web port")
    @EnableMockRuntime(scenario = StandardScenarios.ONBOARDING_HAPPY, webPort = 0)
    class CustomScenarioAndPortTest {

        @Test
        @DisplayName("injects loopback URI and custom scenario")
        void shouldInjectLoopbackUri(
                MockRuntime runtime,
                Scenario scenario,
                URI webUri) throws Exception {

            assertNotNull(runtime);
            assertNotNull(scenario);
            assertNotNull(webUri);

            assertEquals(StandardScenarios.ONBOARDING_HAPPY, scenario.name());
            assertEquals(StandardScenarios.ONBOARDING_HAPPY, runtime.currentScenarioName());

            assertTrue(runtime.webPort() > 0);
            assertEquals("127.0.0.1", webUri.getHost());
            assertEquals(runtime.webPort(), webUri.getPort());

            var httpClient = HttpClient.newHttpClient();
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(webUri.toString() + "/mock/state"))
                    .GET()
                    .build();
            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, res.statusCode());
        }

        @Test
        @DisplayName("method-level @EnableMockRuntime overrides class-level configuration")
        @EnableMockRuntime(scenario = StandardScenarios.WEB_PAIRING, webPort = 0)
        void shouldSupportMethodLevelOverride(MockRuntime runtime, Scenario scenario) {
            assertEquals(StandardScenarios.WEB_PAIRING, scenario.name());
            assertEquals(StandardScenarios.WEB_PAIRING, runtime.currentScenarioName());
        }
    }

    @Nested
    @DisplayName("with custom token and device secret")
    @EnableMockRuntime(webPort = 0, token = "test-token", deviceSecret = "test-secret")
    class CustomTokenAndSecretTest {

        @Test
        @DisplayName("configures token and device secret from annotation")
        void shouldApplyCustomTokenAndSecret(MockRuntime runtime, MockRuntimeConfig config) {
            assertEquals("test-token", runtime.token());
            assertEquals("test-token", config.getRuntimeToken());
            assertEquals("test-secret", config.getDeviceSecret());
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
            var paramContext = new DummyParameterContext(URI.class);
            assertThrows(ParameterResolutionException.class, () ->
                    extension.resolveParameter(paramContext, ctx));
        }

        @Test
        @DisplayName("rejects unsupported parameter types")
        void shouldRejectUnsupportedParameter(MockRuntime runtime) {
            var extension = new MockRuntimeExtension();
            var ctx = createMockExtensionContext(runtime);
            var paramContext = new DummyParameterContext(String.class);
            assertFalse(extension.supportsParameter(paramContext, ctx));
            assertThrows(ParameterResolutionException.class, () ->
                    extension.resolveParameter(paramContext, ctx));
        }

        @Test
        @DisplayName("throws ParameterResolutionException when runtime is absent from context")
        void shouldThrowWhenRuntimeAbsent() {
            var extension = new MockRuntimeExtension();
            var ctx = createMockExtensionContext(null);
            var paramContext = new DummyParameterContext(URI.class);
            assertThrows(ParameterResolutionException.class, () ->
                    extension.resolveParameter(paramContext, ctx));
        }

        @Test
        @DisplayName("resolves and manages MockRuntimeConfig directly")
        void shouldResolveConfigDirectly() {
            var extension = new MockRuntimeExtension();
            var config = MockRuntimeConfig.builder().build();
            var ctx = createMockExtensionContext(null, config);
            var paramContext = new DummyParameterContext(MockRuntimeConfig.class);
            assertTrue(extension.supportsParameter(paramContext, ctx));
            assertSame(config, extension.resolveParameter(paramContext, ctx));
        }

        @Test
        @DisplayName("supports all standard parameter types")
        void shouldSupportAllStandardParameterTypes(MockRuntime runtime) {
            var extension = new MockRuntimeExtension();
            var ctx = createMockExtensionContext(runtime);
            assertTrue(extension.supportsParameter(new DummyParameterContext(MockRuntime.class), ctx));
            assertTrue(extension.supportsParameter(new DummyParameterContext(RuntimeClient.class), ctx));
            assertTrue(extension.supportsParameter(new DummyParameterContext(MachinePaths.class), ctx));
            assertTrue(extension.supportsParameter(new DummyParameterContext(Path.class), ctx));
            assertTrue(extension.supportsParameter(new DummyParameterContext(Scenario.class), ctx));
            assertTrue(extension.supportsParameter(new DummyParameterContext(MockRuntimeConfig.class), ctx));
            assertTrue(extension.supportsParameter(new DummyParameterContext(URI.class), ctx));

            assertNotNull(extension.resolveParameter(new DummyParameterContext(MockRuntime.class), ctx));
            assertNotNull(extension.resolveParameter(new DummyParameterContext(RuntimeClient.class), ctx));
            assertNotNull(extension.resolveParameter(new DummyParameterContext(MachinePaths.class), ctx));
            assertNotNull(extension.resolveParameter(new DummyParameterContext(Path.class), ctx));
            assertNotNull(extension.resolveParameter(new DummyParameterContext(Scenario.class), ctx));
        }

        @Test
        @DisplayName("afterEach handles absent runtime cleanly")
        void shouldHandleAbsentRuntimeInAfterEach() {
            var extension = new MockRuntimeExtension();
            var ctx = createMockExtensionContext(null);
            assertDoesNotThrow(() -> extension.afterEach(ctx));
        }

        private ExtensionContext createMockExtensionContext(MockRuntime runtime) {
            return createMockExtensionContext(runtime, null);
        }

        private ExtensionContext createMockExtensionContext(MockRuntime runtime, MockRuntimeConfig config) {
            var map = new HashMap<Object, Object>();
            if (runtime != null) {
                map.put(MockRuntime.class.getName(), runtime);
            }
            if (config != null) {
                map.put(MockRuntimeConfig.class.getName(), config);
            }

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
                        if ("remove".equals(method.getName()) && args != null && args.length == 1) {
                            map.remove(args[0]);
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
            // Intentionally empty: used only as reflection target for ParameterContext
        }

        void dummyString(String str) {
            // Intentionally empty: used only as reflection target for ParameterContext
        }

        void dummyRuntime(MockRuntime runtime) {
            // Intentionally empty: used only as reflection target for ParameterContext
        }

        void dummyClient(RuntimeClient client) {
            // Intentionally empty: used only as reflection target for ParameterContext
        }

        void dummyPaths(MachinePaths paths) {
            // Intentionally empty: used only as reflection target for ParameterContext
        }

        void dummyPath(Path path) {
            // Intentionally empty: used only as reflection target for ParameterContext
        }

        void dummyScenario(Scenario scenario) {
            // Intentionally empty: used only as reflection target for ParameterContext
        }

        void dummyConfig(MockRuntimeConfig config) {
            // Intentionally empty: used only as reflection target for ParameterContext
        }
    }
}
