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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and cache for mock runtime scenarios.
 * <p>
 * Two resolution notations are supported:
 * <ul>
 *   <li>{@code <name>} resolves a scenario file as before;</li>
 *   <li>{@code <name>#<state>} resolves the scenario and starts it directly at {@code state}, so a
 *       test or a developer can jump to one case inside a rich scenario (for example
 *       {@code running-work#plan.paused}) without a separate JSON file.</li>
 * </ul>
 * The registry also keeps a legacy alias table mapping the fine-grained scenario names of the
 * pre-consolidation catalog to their primary scenario and initial state. The table is used as a
 * fallback only: while a legacy file still exists the file wins, so the migration can delete the
 * files one wave at a time without breaking a single caller.
 */
public final class ScenarioRegistry {

    /** Separates the scenario name from the state in the composite notation. */
    public static final char STATE_SEPARATOR = '#';

    private final Map<String, Scenario> cache = new ConcurrentHashMap<>();
    private final Map<String, String> legacyAliases = new ConcurrentHashMap<>();

    /**
     * Creates an empty registry with the standard legacy alias table.
     */
    public ScenarioRegistry() {
        StandardAliases.registerInto(this);
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
     * Registers a legacy scenario name as an alias for a composite {@code <primary>#<state>}
     * target. A directly resolvable scenario of the same name always wins over its alias.
     *
     * @param legacyName the fine-grained scenario name
     * @param target     the composite target, e.g. {@code onboarding#harness.blocked}
     * @return this registry
     */
    public ScenarioRegistry registerLegacyAlias(String legacyName, String target) {
        Objects.requireNonNull(legacyName, "legacyName");
        Objects.requireNonNull(target, "target");
        legacyAliases.put(legacyName, target);
        return this;
    }

    /**
     * @param legacyName a fine-grained scenario name
     * @return the composite target this name migrates to, or {@code null}
     */
    public String legacyAlias(String legacyName) {
        return legacyName == null ? null : legacyAliases.get(legacyName);
    }

    /**
     * @return an unmodifiable snapshot of the legacy alias table
     */
    public Map<String, String> legacyAliases() {
        return Map.copyOf(new LinkedHashMap<>(legacyAliases));
    }

    /**
     * Resolves a scenario by name, legacy alias, composite {@code name#state}, or file path,
     * loading it if not cached.
     *
     * @param nameOrPath the scenario name (e.g. {@code onboarding-happy}), composite reference, or path
     * @return the resolved scenario, its start set to the requested state where one was named
     * @throws ScenarioValidationException if validation fails
     * @throws IllegalArgumentException    if the scenario or the named state cannot be found
     */
    public Scenario resolve(String nameOrPath) {
        Objects.requireNonNull(nameOrPath, "nameOrPath");

        var cached = cache.get(nameOrPath);
        if (cached != null) {
            return cached;
        }

        var separator = nameOrPath.lastIndexOf(STATE_SEPARATOR);
        if (separator > 0 && separator < nameOrPath.length() - 1) {
            var scenarioName = nameOrPath.substring(0, separator);
            var stateId = nameOrPath.substring(separator + 1);
            var base = resolve(scenarioName);
            var resolved = atState(base, stateId, nameOrPath);
            cache.put(nameOrPath, resolved);
            return resolved;
        }

        var loaded = loadDirectOrAliased(nameOrPath);
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
        } catch (RuntimeException _) {
            return Optional.empty();
        }
    }

    /**
     * Clears all cached scenarios, keeping the legacy alias table.
     */
    public void clear() {
        cache.clear();
    }

    private Scenario loadDirectOrAliased(String nameOrPath) {
        try {
            return ScenarioLoader.load(nameOrPath);
            // cui-rewrite:disable InvalidExceptionUsageRecipe
        } catch (IllegalArgumentException missing) {
            var target = legacyAliases.get(nameOrPath);
            if (target == null) {
                throw missing;
            }
            return resolve(target);
        }
    }

    private static Scenario atState(Scenario base, String stateId, String reference) {
        if (!base.states().containsKey(stateId)) {
            throw new IllegalArgumentException(
                    "State '" + stateId + "' does not exist in scenario '" + base.name() + "' (" + reference + ")");
        }
        return new Scenario(
                base.formatVersion(),
                base.name(),
                base.description(),
                stateId,
                base.pairingCode(),
                base.routes(),
                base.states(),
                base.designer(),
                base.startCase());
    }

    /**
     * Splits a composite reference into its scenario name and optional state.
     *
     * @param reference a {@code name} or {@code name#state} reference
     * @return the scenario name
     */
    public static String scenarioNameOf(String reference) {
        Objects.requireNonNull(reference, "reference");
        var separator = reference.lastIndexOf(STATE_SEPARATOR);
        return separator > 0 ? reference.substring(0, separator) : reference;
    }

    /**
     * Splits a composite reference into its scenario name and optional state.
     *
     * @param reference a {@code name} or {@code name#state} reference
     * @return the requested state, or {@code null} when none was named
     */
    public static String stateNameOf(String reference) {
        Objects.requireNonNull(reference, "reference");
        var separator = reference.lastIndexOf(STATE_SEPARATOR);
        return separator > 0 && separator < reference.length() - 1
                ? reference.substring(separator + 1)
                : null;
    }

    /**
     * The standard legacy alias table of the five-scenario consolidation.
     */
    private static final class StandardAliases {

        private StandardAliases() {
        }

        private static void registerInto(ScenarioRegistry registry) {
            // Scenario 1: onboarding and entry.
            registry.registerLegacyAlias("status-only", "onboarding#onboard.mode");
            registry.registerLegacyAlias("onboarding-happy", "onboarding#onboard.mode");
            registry.registerLegacyAlias("onboarding-harness-not-logged-in", "onboarding#harness.blocked");
            registry.registerLegacyAlias("onboarding-harness-tokens-complete", "onboarding#harness.enrol");
            registry.registerLegacyAlias("onboarding-harness-tokens-partial", "onboarding#harness.enrol");
            registry.registerLegacyAlias("onboarding-harness-tokens-not-reported", "onboarding#harness.enrol");
            registry.registerLegacyAlias("onboarding-harness-all-failed", "onboarding#harness.blocked");
            registry.registerLegacyAlias("onboarding-refusals", "onboarding#harness.consent");
            registry.registerLegacyAlias("onboarding-unknown-harness", "onboarding#harness.add");
            registry.registerLegacyAlias("onboarding-settings-conflict", "onboarding#settings.choose");
            registry.registerLegacyAlias("web-pairing", "onboarding#onboard.completed");
            registry.registerLegacyAlias("entry", "onboarding#onboard.mode");
            registry.registerLegacyAlias("entry-unonboarded", "onboarding#onboard.mode");
            registry.registerLegacyAlias("entry-onboarding", "onboarding#onboard.mode");
            registry.registerLegacyAlias("entry-paired-browser", "onboarding#onboard.completed");
            registry.registerLegacyAlias("entry-ssh-session", "onboarding#onboard.completed");
            registry.registerLegacyAlias("entry-fresh-code", "onboarding#onboard.completed");
            registry.registerLegacyAlias("entry-lan-conflict", "onboarding#onboard.completed");
            registry.registerLegacyAlias("entry-lan-credentials", "onboarding#onboard.completed");
            registry.registerLegacyAlias("entry-device-lifecycle", "onboarding#onboard.completed");
            registry.registerLegacyAlias("entry-stream-close", "onboarding#onboard.completed");
            registry.registerLegacyAlias("paired-devices", "onboarding#onboard.completed");
            registry.registerLegacyAlias("operator-interaction", "onboarding#onboard.mode");

            // Scenario 2: running work, process view, decisions, and control.
            registry.registerLegacyAlias("plan-view", "running-work#running_work.active");
            registry.registerLegacyAlias("intent-plan", "running-work#running_work.active");
            registry.registerLegacyAlias("intent-outline", "running-work#running_work.active");
            registry.registerLegacyAlias("intent-epic", "running-work#running_work.active");
            registry.registerLegacyAlias("intent-oversize", "running-work#running_work.active");
            registry.registerLegacyAlias("decisions", "running-work#running_work.active");
            registry.registerLegacyAlias("decomposition", "running-work#running_work.active");
            registry.registerLegacyAlias("control", "running-work#running_work.active");
            registry.registerLegacyAlias("control-pause", "running-work#running_work.active");
            registry.registerLegacyAlias("control-holds", "running-work#running_work.active");
            registry.registerLegacyAlias("control-settings", "running-work#running_work.active");
            registry.registerLegacyAlias("control-epic", "running-work#running_work.active");
            registry.registerLegacyAlias("control-decomposition", "running-work#running_work.active");
            registry.registerLegacyAlias("control-chat", "running-work#running_work.active");
            registry.registerLegacyAlias("control-review", "running-work#running_work.active");
            registry.registerLegacyAlias("control-tokens", "running-work#running_work.active");

            // Scenario 3: projects and start.
            registry.registerLegacyAlias("projects", "projects-and-start#projects.explorer");
            registry.registerLegacyAlias("enrolment", "projects-and-start#projects.explorer");
            registry.registerLegacyAlias("git_sources", "projects-and-start#start.sources");
            registry.registerLegacyAlias("lesson_source", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-chat", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-documents", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-entry-continue", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-epic", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-epic-adopt", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-follow-up", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-issue-failed", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-lesson", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-local-changes", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-pull-request", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-rate-limited", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-recipe", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-refused", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-sources", "projects-and-start#start.sources");
            registry.registerLegacyAlias("start-unscreened", "projects-and-start#start.sources");

            // Scenario 4: reports and system.
            registry.registerLegacyAlias("reports", "reports-and-system#reports.list");
            registry.registerLegacyAlias("system", "reports-and-system#system.ready");

            // Scenario 5: designer.
            registry.registerLegacyAlias("designer", "designer#designer.ready");
        }
    }
}
