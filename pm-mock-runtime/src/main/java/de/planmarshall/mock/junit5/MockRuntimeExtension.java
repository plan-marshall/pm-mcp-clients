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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;


import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

import de.cuioss.tools.logging.CuiLogger;
import de.cuioss.tools.string.Joiner;
import de.planmarshall.api.MachinePaths;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.mock.MockRuntime;
import de.planmarshall.mock.MockRuntimeConfig;
import de.planmarshall.mock.scenario.Scenario;

/**
 * JUnit 5 extension that manages the lifecycle of {@link MockRuntime} instances.
 * <p>
 * Supports:
 * <ul>
 *   <li>Automatic startup before each test and shutdown after each test</li>
 *   <li>Manual control via {@link EnableMockRuntime#manualStart()}</li>
 *   <li>Method-level overrides of class-level annotations</li>
 *   <li>Parameter resolution for {@link MockRuntime}, {@link RuntimeClient}, {@link MachinePaths},
 *       {@link Path}, {@link URI}, and {@link Scenario}</li>
 * </ul>
 *
 * @see EnableMockRuntime
 */
public class MockRuntimeExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final CuiLogger LOGGER = new CuiLogger(MockRuntimeExtension.class);

    /**
     * Identifies the JUnit 5 {@link Namespace} under which {@link MockRuntime} instances are stored.
     */
    public static final Namespace NAMESPACE = Namespace.create(MockRuntimeExtension.class);

    private static final String RUNTIME_KEY = MockRuntime.class.getName();
    private static final String CONFIG_KEY = MockRuntimeConfig.class.getName();

    private final Map<Class<?>, BiFunction<MockRuntime, ExtensionContext, Object>> parameterResolvers = Map.of(
            MockRuntime.class, (runtime, _) -> runtime,
            RuntimeClient.class, (runtime, _) -> runtime.client(),
            MachinePaths.class, (runtime, _) -> runtime.paths(),
            Path.class, (runtime, _) -> runtime.base(),
            URI.class, this::resolveUriParameter,
            Scenario.class, (runtime, _) -> runtime.currentScenario(),
            MockRuntimeConfig.class, this::resolveConfigParameter
    );

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        LOGGER.debug("Setting up MockRuntime for test: %s", context.getDisplayName());

        var annotation = findAnnotation(context);
        var config = createConfig(annotation.orElse(null));
        context.getStore(NAMESPACE).put(CONFIG_KEY, config);

        var manualStart = annotation.map(EnableMockRuntime::manualStart).orElse(false);
        if (manualStart) {
            LOGGER.debug("Manual start configured, MockRuntime not started automatically");
            return;
        }

        MockRuntime runtime = null;
        try {
            runtime = MockRuntime.start(config);
            put(runtime, context);
            LOGGER.debug("MockRuntime started at base: %s, socket: %s", runtime.base(), runtime.socketPath());
            runtime = null;
        }
        /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (IOException | RuntimeException e) {
            LOGGER.error(e, "Error during MockRuntime setup: %s", e.getMessage());
            throw e;
        } finally {
            if (runtime != null) {
                runtime.close();
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var optionalRuntime = get(context);
        if (optionalRuntime.isPresent()) {
            var runtime = optionalRuntime.get();
            try {
                LOGGER.debug("Closing MockRuntime at %s", runtime.base());
                runtime.close();
                LOGGER.debug("MockRuntime closed successfully");
            }
            /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
            catch (RuntimeException e) {
                LOGGER.error(e, "Failed to close MockRuntime: %s", e.getMessage());
                throw new IllegalStateException("Failed to properly close MockRuntime", e);
            } finally {
                remove(context);
            }
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        var type = parameterContext.getParameter().getType();
        var supported = parameterResolvers.containsKey(type);
        if (supported) {
            LOGGER.debug("Parameter type %s is supported", type.getName());
        }
        return supported;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        var type = parameterContext.getParameter().getType();
        var resolver = parameterResolvers.get(type);
        if (resolver == null) {
            throw new ParameterResolutionException("Unsupported parameter type: " + type.getName());
        }

        if (MockRuntimeConfig.class.equals(type)) {
            var config = (MockRuntimeConfig) extensionContext.getStore(NAMESPACE).get(CONFIG_KEY);
            if (config != null) {
                return config;
            }
        }

        var runtime = get(extensionContext).orElseThrow(() -> new ParameterResolutionException(
                "No MockRuntime instance available. Ensure @EnableMockRuntime is present and manualStart is false (or server was started)."));

        return resolver.apply(runtime, extensionContext);
    }

    private URI resolveUriParameter(MockRuntime runtime, ExtensionContext context) {
        if (runtime.webPort() < 0) {
            throw new ParameterResolutionException(
                    "Loopback web TCP listener is disabled (webPort < 0). Set @EnableMockRuntime(webPort = 0) to inject URI.");
        }
        return URI.create("http://127.0.0.1:" + runtime.webPort());
    }

    private Object resolveConfigParameter(MockRuntime runtime, ExtensionContext context) {
        var config = (MockRuntimeConfig) context.getStore(NAMESPACE).get(CONFIG_KEY);
        if (config != null) {
            return config;
        }
        throw new ParameterResolutionException("No MockRuntimeConfig available in extension context.");
    }

    /**
     * Stores a {@link MockRuntime} instance in the extension context.
     *
     * @param runtime the mock runtime instance
     * @param context the extension context
     */
    public static void put(MockRuntime runtime, ExtensionContext context) {
        context.getStore(NAMESPACE).put(RUNTIME_KEY, runtime);
    }

    /**
     * Retrieves the {@link MockRuntime} instance from the extension context if present.
     *
     * @param context the extension context
     * @return optional containing the runtime if present
     */
    public static Optional<MockRuntime> get(ExtensionContext context) {
        return Optional.ofNullable((MockRuntime) context.getStore(NAMESPACE).get(RUNTIME_KEY));
    }

    /**
     * Removes the {@link MockRuntime} from the extension context.
     *
     * @param context the extension context
     */
    public static void remove(ExtensionContext context) {
        context.getStore(NAMESPACE).remove(RUNTIME_KEY);
        context.getStore(NAMESPACE).remove(CONFIG_KEY);
    }

    private Optional<EnableMockRuntime> findAnnotation(ExtensionContext context) {
        // Method-level annotation takes precedence over class-level
        var methodAnnotation = context.getTestMethod()
                .flatMap(method -> AnnotationSupport.findAnnotation(method, EnableMockRuntime.class));
        if (methodAnnotation.isPresent()) {
            return methodAnnotation;
        }

        var testInstance = context.getTestInstance().orElse(null);
        if (testInstance != null) {
            var classHierarchy = extractClassHierarchy(testInstance);
            return classHierarchy.stream()
                    .map(clazz -> AnnotationSupport.findAnnotation(clazz, EnableMockRuntime.class))
                    .flatMap(Optional::stream)
                    .findFirst();
        }

        return context.getTestClass()
                .flatMap(clazz -> AnnotationSupport.findAnnotation(clazz, EnableMockRuntime.class));
    }

    private MockRuntimeConfig createConfig(@Nullable EnableMockRuntime annotation) {
        if (annotation == null) {
            return MockRuntimeConfig.builder().build();
        }

        var builder = MockRuntimeConfig.builder()
                .scenario(annotation.scenario())
                .webPort(annotation.webPort())
                .cleanupBaseOnClose(annotation.cleanupBaseOnClose());

        if (!annotation.token().isBlank()) {
            builder.runtimeToken(annotation.token());
        }
        if (!annotation.deviceSecret().isBlank()) {
            builder.deviceSecret(annotation.deviceSecret());
        }

        return builder.build();
    }

    private List<Class<?>> extractClassHierarchy(Object testInstance) {
        List<Class<?>> classHierarchy = new ArrayList<>();
        Class<?> enclosing = testInstance.getClass();
        while (enclosing != null) {
            Class<?> current = enclosing;
            while (current != null && !Object.class.equals(current)) {
                if (!classHierarchy.contains(current)) {
                    classHierarchy.add(current);
                }
                current = current.getSuperclass();
            }
            enclosing = enclosing.getEnclosingClass();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Extracted class hierarchy from %s: %s",
                    testInstance.getClass().getName(),
                    Joiner.on(", ").join(classHierarchy));
        }
        return classHierarchy;
    }
}
