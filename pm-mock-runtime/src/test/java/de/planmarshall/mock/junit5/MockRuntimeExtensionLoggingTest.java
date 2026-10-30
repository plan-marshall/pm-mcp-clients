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

import static de.planmarshall.mock.junit5.MockRuntimeLogMessages.ERROR;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Optional;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import de.cuioss.test.juli.LogAsserts;
import de.cuioss.test.juli.TestLogLevel;
import de.cuioss.test.juli.junit5.EnableTestLogger;

/**
 * Contract of the production log messages the extension writes: each {@code ERROR} message reaches
 * the log with its identifier, which is what the log search and the documentation key on.
 */
@DisplayName("MockRuntimeExtension logging")
@EnableTestLogger
class MockRuntimeExtensionLoggingTest {

    @Test
    @DisplayName("logs a failed setup as the MOCK-200 record")
    void shouldLogSetupFailureRecord() {
        var extension = new MockRuntimeExtension();
        var context = contextOf(UnresolvableScenario.class);

        assertThrows(IllegalArgumentException.class, () -> extension.beforeEach(context));

        LogAsserts.assertSingleLogMessagePresentContaining(TestLogLevel.ERROR,
                ERROR.SETUP_FAILED.resolveIdentifierString());
    }

    @Test
    @DisplayName("logs a failed close as the MOCK-201 record")
    void shouldLogCloseFailureRecord() {
        MockRuntimeExtension.logCloseFailure(new IllegalStateException("simulated close failure"));

        LogAsserts.assertSingleLogMessagePresentContaining(TestLogLevel.ERROR,
                ERROR.CLOSE_FAILED.resolveIdentifierString());
    }

    private static ExtensionContext contextOf(Class<?> testClass) {
        var values = new HashMap<Object, Object>();
        var store = (ExtensionContext.Store) Proxy.newProxyInstance(
                ExtensionContext.Store.class.getClassLoader(),
                new Class<?>[]{ExtensionContext.Store.class},
                (_, method, args) -> switch (method.getName()) {
                    case "get" -> values.get(args[0]);
                    case "put" -> {
                        values.put(args[0], args[1]);
                        yield null;
                    }
                    case "remove" -> {
                        values.remove(args[0]);
                        yield null;
                    }
                    default -> null;
                });
        return (ExtensionContext) Proxy.newProxyInstance(
                ExtensionContext.class.getClassLoader(),
                new Class<?>[]{ExtensionContext.class},
                (_, method, args) -> switch (method.getName()) {
                    case "getStore" -> store;
                    case "getTestClass" -> Optional.of(testClass);
                    case "getTestMethod", "getTestInstance" -> Optional.empty();
                    case "getDisplayName", "getUniqueId" -> "stub";
                    default -> null;
                });
    }

    /**
     * A test class whose declared scenario does not resolve, so that the extension fails in
     * {@code beforeEach}.
     */
    @EnableMockRuntime(scenario = "scenario-that-does-not-exist")
    static class UnresolvableScenario {
    }
}
