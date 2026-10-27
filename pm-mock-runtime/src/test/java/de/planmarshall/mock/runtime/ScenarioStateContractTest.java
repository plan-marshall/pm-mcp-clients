/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.StandardScenarios;
import de.planmarshall.mock.http.MockHttpRequest;
import de.planmarshall.mock.http.MockHttpResponse;
import de.planmarshall.mock.scenario.Scenario;
import de.planmarshall.mock.scenario.ScenarioRegistry;

/**
 * The state contract of the three consolidated scenarios (UC-S-01, UC-V-01): every state of
 * {@code running-work}, {@code projects-and-start} and {@code reports-and-system} reaches the API
 * resource it concerns.
 * <p>
 * A state that stands for a domain fact carries that fact in its operator representation, and
 * {@code POST /mock/goto/<state>} applies it to the engine. The test presses the jump and reads the
 * resource the state concerns: its answer must differ from the scenario's start state, unless the
 * state is declared equal on purpose below.
 */
@DisplayName("ScenarioStateContractTest")
class ScenarioStateContractTest {

    private static final String AUTH = "Bearer test-token";

    /**
     * The resources of the {@code control.*} states, which the engine serves from its control
     * fixture rather than from a scenario route.
     */
    private static final Map<String, String> CONTROL_RESOURCES = controlResources();

    private static Map<String, String> controlResources() {
        var resources = new LinkedHashMap<String, String>();
        resources.put("control.ready", "/api/v1/plans/fix-relay/actions");
        resources.put("control.pause", "/api/v1/plans/fix-relay/actions");
        resources.put("control.pause-withdrawn", "/api/v1/plans/fix-relay/actions");
        resources.put("control.holds", "/api/v1/plans/fix-relay/holds");
        resources.put("control.settings", "/api/v1/plans/fix-relay/settings");
        resources.put("control.epic", "/api/v1/epics/relay-hardening");
        resources.put("control.decomposition", "/api/v1/epics/relay-hardening/decomposition");
        resources.put("control.chat", "/api/v1/plans/fix-relay/chat");
        resources.put("control.review", "/api/v1/epics/relay-hardening/review");
        resources.put("control.tokens", "/api/v1/epics/relay-hardening/tokens");
        resources.put("control.archive", "/api/v1/epics/relay-hardening/archive-check");
        return Map.copyOf(resources);
    }

    /**
     * The states whose concerned resource is equal to the start state on purpose. Each one either
     * declares no domain fact at all (the state is an anchor for a view that reads the scenario's
     * route), or its fixture is the scenario's base fixture, which the start state already shows.
     */
    private static final Set<String> DECLARED_EQUAL = Set.of(
            "running_work.active",
            "overview.running",
            "process.ended-task",
            "process.failed-tool",
            "process.off-track",
            "process.pruned-lane",
            "process.running",
            "process.tokens-partial",
            "enrol.scan",
            "enrol.digest-mismatch",
            "sources.git",
            "sources.lessons",
            "intent.ready",
            "control.ready",
            "control.pause-withdrawn",
            "control.epic",
            "control.decomposition",
            "control.chat",
            "control.review",
            "control.tokens",
            "control.archive",
            "cleanup.kept-items",
            "cleanup.residues",
            "projects.ready",
            "decisions.open",
            "reports.completion-landed",
            "reports.completion-nothing",
            "reports.completion-aborted",
            "reports.completion-failed");

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {StandardScenarios.RUNNING_WORK, StandardScenarios.PROJECTS_AND_START,
            StandardScenarios.REPORTS_AND_SYSTEM})
    @DisplayName("every state reaches the resource it concerns")
    void everyStateReachesTheResourceItConcerns(String scenarioName) throws Exception {
        var registry = new ScenarioRegistry();
        try (var engine = new MockRuntimeEngine(registry, registry.resolve(scenarioName), "test-token",
                     "test-secret")) {
            var scenario = engine.currentScenario();
            engine.goToState(scenario.start());
            var baseline = new HashMap<String, String>();
            for (var stateId : scenario.states().keySet()) {
                var path = concernedResource(scenario, stateId);
                if (path != null) {
                    baseline.putIfAbsent(path, answer(engine, path));
                }
            }

            for (var stateId : new TreeSet<>(scenario.states().keySet())) {
                if (stateId.equals(scenario.start())) {
                    continue;
                }
                var path = concernedResource(scenario, stateId);
                assertTrue(path != null || DECLARED_EQUAL.contains(stateId),
                        stateId + " names no concerned resource; declare it equal on purpose");

                engine.goToState(stateId);
                if (path == null) {
                    continue;
                }
                var after = answer(engine, path);
                if (DECLARED_EQUAL.contains(stateId)) {
                    assertEquals(baseline.get(path), after,
                            stateId + " is declared equal on purpose but changed " + path);
                } else {
                    assertNotEquals(baseline.get(path), after,
                            stateId + " must change " + path + " from the start state");
                }
            }
        }
    }

    /**
     * @param scenario the scenario
     * @param stateId  one of its states
     * @return the API resource the state stands for, or {@code null} for a state that declares no
     *         domain fact the engine applies
     */
    private static @Nullable String concernedResource(Scenario scenario, String stateId) {
        var control = CONTROL_RESOURCES.get(stateId);
        if (control != null) {
            return control;
        }
        if (stateId.startsWith("overview.")) {
            return switch (stateId) {
                case "overview.plan-ending" -> "/api/v1/projects/pm-mcp-clients";
                case "overview.running" -> null;
                default -> "/api/v1/decisions";
            };
        }
        if (stateId.startsWith("process.")) {
            return declaredRoute(scenario, stateId);
        }
        if ("intent.ready".equals(stateId)) {
            return "/api/v1/plans/fix-relay/intent";
        }
        if (stateId.startsWith("decisions.")) {
            return "/api/v1/decisions";
        }
        if (stateId.startsWith("projects.")) {
            return "/api/v1/projects";
        }
        if (stateId.startsWith("reports.completion-")) {
            var planId = fact(scenario, stateId, "plan_id");
            return planId == null ? null : "/api/v1/plans/" + planId + "/completion";
        }
        if (stateId.startsWith("reports.")) {
            return "/api/v1/reports";
        }
        if (stateId.startsWith("system.")) {
            return "system.running".equals(stateId) ? "/api/v1/system" : "/api/v1/status";
        }
        if (stateId.startsWith("cleanup.")) {
            return "/api/v1/system/cleanup";
        }
        if (stateId.startsWith("start.")) {
            var entryId = fixtureValue(scenario, stateId, "start", "entry_id");
            return entryId == null ? null : "/api/v1/entries/" + entryId;
        }
        return null;
    }

    /**
     * @param scenario the scenario
     * @param stateId  a state id
     * @return the path of the first route the state's runtime fixture declares, or {@code null}
     */
    private static @Nullable String declaredRoute(Scenario scenario, String stateId) {
        var state = scenario.state(stateId);
        if (state == null) {
            return null;
        }
        var representation = JsonTree.asObject(state.representation());
        var facts = representation == null ? null : JsonTree.asObject(representation.get("facts"));
        var runtime = facts == null ? null : JsonTree.asObject(facts.get("runtime"));
        var routes = runtime == null ? null : JsonTree.asObject(runtime.get("routes"));
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        var key = String.valueOf(routes.keySet().iterator().next());
        var space = key.indexOf(' ');
        return space < 0 ? key : key.substring(space + 1);
    }

    private static @Nullable String fact(Scenario scenario, String stateId, String member) {
        var state = scenario.state(stateId);
        if (state == null) {
            return null;
        }
        var representation = JsonTree.asObject(state.representation());
        var facts = representation == null ? null : JsonTree.asObject(representation.get("facts"));
        return facts == null ? null : asString(facts.get(member));
    }

    private static @Nullable String fixtureValue(Scenario scenario, String stateId, String fixture,
            String member) {
        var state = scenario.state(stateId);
        if (state == null) {
            return null;
        }
        var representation = JsonTree.asObject(state.representation());
        var facts = representation == null ? null : JsonTree.asObject(representation.get("facts"));
        var runtime = facts == null ? null : JsonTree.asObject(facts.get("runtime"));
        var block = runtime == null ? null : JsonTree.asObject(runtime.get(fixture));
        return block == null ? null : asString(block.get(member));
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    /**
     * @param engine the engine
     * @param path   the resource
     * @return the status and body of the answer, so a resource that does not exist differs from one
     *         that does
     * @throws IOException on write failure
     */
    private static String answer(MockRuntimeEngine engine, String path) throws IOException {
        var request = new MockHttpRequest("GET", path, Map.of("Authorization", AUTH), new byte[0],
                System.nanoTime());
        var out = new ByteArrayOutputStream();
        engine.dispatch(request, new MockHttpResponse(out), false);
        return out.toString(StandardCharsets.UTF_8);
    }
}
