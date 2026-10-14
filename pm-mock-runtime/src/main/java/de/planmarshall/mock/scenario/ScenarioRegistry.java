/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.scenario;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and cache for mock runtime scenarios, supporting name-based resolution and registration.
 */
public final class ScenarioRegistry {

    private final Map<String, Scenario> cache = new ConcurrentHashMap<>();

    /**
     * Creates an empty registry.
     */
    public ScenarioRegistry() {
        // Creates an empty scenario registry with an empty cache
    }

    /**
     * Registers a preloaded scenario.
     *
     * @param scenario the scenario
     * @return this registry
     */
    public ScenarioRegistry register(Scenario scenario) {
        Objects.requireNonNull(scenario, "scenario");
        cache.put(scenario.name(), scenario);
        return this;
    }

    /**
     * Resolves a scenario by name or file path, loading it if not cached.
     *
     * @param nameOrPath the scenario name (e.g. {@code onboarding-happy}) or path
     * @return the resolved scenario
     * @throws ScenarioValidationException if validation fails
     * @throws IllegalArgumentException    if the scenario cannot be found
     */
    public Scenario resolve(String nameOrPath) {
        Objects.requireNonNull(nameOrPath, "nameOrPath");
        var cached = cache.get(nameOrPath);
        if (cached != null) {
            return cached;
        }
        var loaded = ScenarioLoader.load(nameOrPath);
        cache.put(loaded.name(), loaded);
        cache.putIfAbsent(nameOrPath, loaded);
        return loaded;
    }

    /**
     * Finds a scenario if already loaded or available.
     *
     * @param name the scenario name
     * @return optional containing the scenario
     */
    public Optional<Scenario> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(resolve(name));
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Clears all cached scenarios.
     */
    public void clear() {
        cache.clear();
    }
}
