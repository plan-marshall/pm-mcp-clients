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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.mock.StandardScenarios;

@DisplayName("ScenarioLoader")
@EnableGeneratorController
class ScenarioLoaderTest {

    static Stream<String> standardScenarioResources() {
        return StandardScenarios.PRIMARY_SCENARIOS.stream()
                .map(name -> "/scenarios/" + name + ".json");
    }

    @ParameterizedTest
    @MethodSource("standardScenarioResources")
    @DisplayName("loads and validates all standard scenarios from classpath")
    void loadsStandardScenarios(String resourcePath) throws Exception {
        try (var in = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(in, "Resource must exist: " + resourcePath);
            var scenario = ScenarioLoader.load(in, resourcePath);
            assertNotNull(scenario);
            assertEquals(1, scenario.formatVersion());
            assertFalse(scenario.name().isBlank());
            assertNotNull(scenario.start());
            assertTrue(scenario.states().containsKey(scenario.start()));
        }
    }

    @Test
    @DisplayName("refuses a non-object runtime fixture in a state's facts")
    void refusesNonObjectRuntimeFixture() {
        var json = """
                {"format_version":1,"name":"bad-runtime","start":"a.b",
                 "states":{"a.b":{"representation":{"scope":{"kind":"machine"},
                 "state":{"state_id":"a.b"},"facts":{"runtime":"not-an-object"},"links":[]}}}}
                """;
        var refused = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "bad-runtime"));
        assertEquals("representation.facts.runtime", refused.field());
    }

    @Test
    @DisplayName("parses complete scenario details including routes, events, and auto-transitions")
    void parsesScenarioDetails() {
        var scenarioName = Generators.letterStrings(4, 12).next().toLowerCase();
        var description = Generators.letterStrings(4, 12).next();
        var token = Generators.letterStrings(4, 10).next().toLowerCase();
        var startState = token + ".start";
        var secondState = token + ".second";
        var linkId = Generators.letterStrings(4, 10).next().toLowerCase();
        var pairingCode = Generators.integers(1000, 9999).next() + "-" + Generators.integers(1000, 9999).next();
        var linkDelay = Generators.integers(1, 100).next().intValue();
        var eventDelay = Generators.integers(1, 100).next().intValue();
        var autoDelay = Generators.integers(1, 500).next().intValue();
        var json = """
                {
                  "format_version": 1,
                  "name": "%1$s",
                  "description": "%2$s",
                  "start": "%3$s",
                  "pairing_code": "%6$s",
                  "routes": {
                    "GET /api/v1/ping": {
                      "status": 200,
                      "representation": { "pong": true }
                    }
                  },
                  "states": {
                    "%3$s": {
                      "representation": {
                        "scope": { "kind": "machine" },
                        "state": { "state_id": "%3$s", "kind": "operator_wait" },
                        "links": [
                          { "id": "%5$s", "rel": "submit", "method": "POST",
                            "href": "/api/v1/advance", "params": {} }
                        ]
                      },
                      "links": {
                        "%5$s": {
                          "method": "POST",
                          "href": "/api/v1/advance",
                          "next": "%4$s",
                          "delay_ms": %7$d
                        }
                      },
                      "events": [
                        { "after_ms": %8$d, "event": "check.result",
                          "data": { "event": "check.result", "at": "2026-10-16T12:00:00Z",
                                    "scope": { "kind": "machine" },
                                    "id": "version", "result": "pass" } }
                      ],
                      "auto": {
                        "after_ms": %9$d,
                        "next": "%4$s"
                      }
                    },
                    "%4$s": {
                      "representation": {
                        "scope": { "kind": "machine" },
                        "state": { "state_id": "%4$s", "kind": "operator_wait" },
                        "links": []
                      }
                    }
                  }
                }
                """.formatted(scenarioName, description, startState, secondState, linkId, pairingCode,
                linkDelay, eventDelay, autoDelay);

        var scenario = ScenarioLoader.load(json, "test.json");
        assertEquals(1, scenario.formatVersion());
        assertEquals(scenarioName, scenario.name());
        assertEquals(description, scenario.description());
        assertEquals(startState, scenario.start());
        assertEquals(pairingCode, scenario.pairingCode());

        // Routes
        var route = scenario.route("GET", "/api/v1/ping");
        assertNotNull(route);
        assertEquals(200, route.status());

        // States
        var initial = scenario.state(startState);
        assertNotNull(initial);
        assertEquals(1, initial.links().size());
        var link = initial.links().get(linkId);
        assertNotNull(link);
        assertEquals("POST", link.method());
        assertEquals("/api/v1/advance", link.href());
        assertEquals(secondState, link.next());
        assertEquals(linkDelay, link.delayMs());

        // Events & Auto
        assertEquals(1, initial.events().size());
        assertEquals("check.result", initial.events().getFirst().event());
        assertEquals(eventDelay, initial.events().getFirst().afterMs());
        assertNotNull(initial.auto());
        assertEquals(secondState, initial.auto().next());
        assertEquals(autoDelay, initial.auto().afterMs());

        var second = scenario.state(secondState);
        assertNotNull(second);
        assertNull(second.auto());
        assertTrue(second.links().isEmpty());
    }

    @Test
    @DisplayName("rejects invalid format_version")
    void rejectsUnsupportedFormatVersion() {
        var json = """
                {
                  "format_version": 2,
                  "name": "invalid-version",
                  "start": "initial",
                  "states": { "initial": { "representation": {} } }
                }
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "test.json"));
        assertEquals("format_version", ex.field());
        assertTrue(ex.getMessage().contains("expected 1"));
    }

    @Test
    @DisplayName("rejects missing start state")
    void rejectsMissingStartState() {
        var json = """
                {
                  "format_version": 1,
                  "name": "missing-start",
                  "start": "non_existent",
                  "states": { "initial": { "representation": {} } }
                }
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "test.json"));
        assertEquals("start", ex.field());
        assertTrue(ex.getMessage().contains("non_existent"));
    }

    @Test
    @DisplayName("reads the custom status of a route and of a link")
    void readsCustomRouteAndLinkStatus() {
        var routeStatus = Generators.integers(201, 299).next();
        var linkStatus = Generators.integers(201, 299).next();
        var json = """
                {
                  "format_version": 1,
                  "name": "custom-status",
                  "start": "initial",
                  "routes": { "GET /api/v1/ping": { "status": %1$d, "representation": { "pong": true } } },
                  "states": {
                    "initial": {
                      "representation": {
                        "scope": { "kind": "machine" },
                        "state": { "state_id": "initial", "kind": "operator_wait" },
                        "links": [
                          { "id": "go", "rel": "submit", "method": "POST",
                            "href": "/api/v1/go", "params": {} }
                        ]
                      },
                      "links": {
                        "go": { "method": "POST", "href": "/api/v1/go", "next": "initial", "status": %2$d }
                      }
                    }
                  }
                }
                """.formatted(routeStatus, linkStatus);

        var scenario = ScenarioLoader.load(json, "custom-status.json");

        var route = scenario.route("GET", "/api/v1/ping");
        assertNotNull(route);
        assertEquals(routeStatus, route.status());
        var initial = scenario.state("initial");
        assertNotNull(initial);
        var link = initial.links().get("go");
        assertNotNull(link);
        assertEquals(linkStatus, link.status());
    }

    @Test
    @DisplayName("names no state for a file-level violation and the state for a state-level one")
    void namesStateOnlyForStateLevelViolation() {
        var fileLevel = """
                {"format_version":1,"name":"file-level","start":"absent",
                 "states":{"initial":{"representation":{}}}}
                """;
        var stateLevel = """
                {"format_version":1,"name":"state-level","start":"a",
                 "states":{"a":{"representation":{},"retry":true}}}
                """;

        var fileEx = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(fileLevel, "file-level.json"));
        var stateEx = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(stateLevel, "state-level.json"));

        assertNull(fileEx.state());
        assertEquals("start", fileEx.field());
        assertEquals("a", stateEx.state());
        assertEquals("retry", stateEx.field());
    }

    @Test
    @DisplayName("rejects dangling next in links")
    void rejectsDanglingLinkNext() {
        var json = """
                {
                  "format_version": 1,
                  "name": "dangling-link",
                  "start": "initial",
                  "states": {
                    "initial": {
                      "representation": {
                        "scope": { "kind": "machine" },
                        "state": { "state_id": "initial", "kind": "operator_wait" },
                        "links": [
                          { "id": "go", "rel": "submit", "method": "POST",
                            "href": "/api/v1/go", "params": {} }
                        ]
                      },
                      "links": {
                        "go": { "method": "POST", "href": "/api/v1/go", "next": "does_not_exist" }
                      }
                    }
                  }
                }
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "test.json"));
        assertEquals("initial", ex.state());
        assertEquals("links.go.next", ex.field());
        assertTrue(ex.getMessage().contains("dangling next"));
    }

    @Test
    @DisplayName("rejects dangling next in auto transition")
    void rejectsDanglingAutoNext() {
        var json = """
                {
                  "format_version": 1,
                  "name": "dangling-auto",
                  "start": "initial",
                  "states": {
                    "initial": {
                      "representation": {
                        "scope": { "kind": "machine" },
                        "state": { "state_id": "initial", "kind": "operator_wait" },
                        "links": []
                      },
                      "auto": { "after_ms": 100, "next": "does_not_exist" }
                    }
                  }
                }
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "test.json"));
        assertEquals("initial", ex.state());
        assertEquals("auto.next", ex.field());
        assertTrue(ex.getMessage().contains("dangling next"));
    }

    @Test
    @DisplayName("rejects non-object representation")
    void rejectsInvalidRepresentation() {
        var json = """
                {
                  "format_version": 1,
                  "name": "bad-rep",
                  "start": "initial",
                  "states": {
                    "initial": {
                      "representation": "not an object"
                    }
                  }
                }
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "test.json"));
        assertEquals("representation", ex.field());
    }

    @Test
    @DisplayName("rejects invalid JSON content")
    void rejectsInvalidJson() {
        var json = "{ not valid json";
        assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "bad.json"));
    }

    @Test
    @DisplayName("rejects an unknown top-level field, naming the file and the field")
    void rejectsUnknownRootField() {
        var json = """
                {"format_version":1,"name":"x","start":"a","unknown_root":true,
                 "states":{"a":{"representation":{}}}}
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "closed-root.json"));
        assertEquals("closed-root.json", ex.scenario());
        assertEquals("unknown_root", ex.field());
        assertTrue(ex.getMessage().contains("unknown field 'unknown_root'"));
    }

    @Test
    @DisplayName("rejects an unknown state field, naming the state and the field")
    void rejectsUnknownStateField() {
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{},"retry":2}}}
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "closed-state.json"));
        assertEquals("a", ex.state());
        assertEquals("retry", ex.field());
    }

    @Test
    @DisplayName("rejects an unknown link, event, route, and auto field")
    void rejectsUnknownFieldsAtEveryLevel() {
        var link = """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{},"links":{"go":{"next":"a","visit":true}}}}}
                """;
        var linkEx = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(link, "closed-link.json"));
        assertEquals("links.go.visit", linkEx.field());

        var event = """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{},"events":[{"after_ms":1,"event":"check.result","data":{},"repeat":2}]}}}
                """;
        var eventEx = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(event, "closed-event.json"));
        assertEquals("events[0].repeat", eventEx.field());

        var route = """
                {"format_version":1,"name":"x","start":"a",
                 "routes":{"GET /api/v1/status":{"representation":{},"cache":"no"}},
                 "states":{"a":{"representation":{}}}}
                """;
        var routeEx = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(route, "closed-route.json"));
        assertEquals("routes.GET /api/v1/status.cache", routeEx.field());

        var auto = """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{},"auto":{"after_ms":1,"next":"a","repeat":true}}}}
                """;
        var autoEx = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(auto, "closed-auto.json"));
        assertEquals("auto.repeat", autoEx.field());
    }

    @Test
    @DisplayName("rejects a link.error that is not a pm-api error code")
    void rejectsUnknownLinkError() {
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{},"links":{"go":{"next":"a","status":409,"error":"not_a_code"}}}}}
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "closed-error.json"));
        assertEquals("links.go.error", ex.field());
        assertTrue(ex.getMessage().contains("not_a_code"));
    }

    @Test
    @DisplayName("loads a state naming chat failures, in order and with their members")
    void loadsChatFailures() {
        var status = Generators.integers(400, 599).next();
        var delayMs = Generators.integers(1, 5_000).next();
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{"scope":{"kind":"machine"},
                  "state":{"state_id":"a","kind":"operator_wait"},"links":[]},"chat_failures":[
                  {"failure":"lost"},
                  {"failure":"http_error","status":%d,"code":"scope_not_found"},
                  {"failure":"slow","delay_ms":%d},
                  {"failure":"slow"}]}}}
                """.formatted(status, delayMs);

        var failures = Objects.requireNonNull(ScenarioLoader.load(json, "chat-failures.json").state("a"))
                .chatFailures();

        assertEquals(List.of(
                StateDefinition.ChatFailure.of(StateDefinition.ChatFailure.Kind.LOST),
                new StateDefinition.ChatFailure(StateDefinition.ChatFailure.Kind.HTTP_ERROR, status,
                        "scope_not_found", 0),
                new StateDefinition.ChatFailure(StateDefinition.ChatFailure.Kind.SLOW, 0, null, delayMs),
                new StateDefinition.ChatFailure(StateDefinition.ChatFailure.Kind.SLOW, 0, null,
                        StateDefinition.ChatFailure.DEFAULT_DELAY_MS)), failures);
    }

    @Test
    @DisplayName("a state without chat_failures arms none")
    void noChatFailures() {
        var json = """
                {"format_version":1,"name":"x","start":"a","states":{"a":{"representation":{"scope":{"kind":"machine"},
                 "state":{"state_id":"a","kind":"operator_wait"},"links":[]}}}}
                """;
        assertTrue(Objects.requireNonNull(ScenarioLoader.load(json, "no-failures.json").state("a"))
                .chatFailures().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[{\"failure\":\"no-%s\"}]",
            "[{\"failure\":\"lost\",\"%s\":1}]",
            "[{\"failure\":\"http_error\",\"status\":503}]",
            "[\"%s\"]"})
    @DisplayName("refuses an unknown chat failure or one a member does not fit, naming the entry")
    void rejectsUnknownChatFailure(String failures) {
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{},"chat_failures":%s}}}
                """.formatted(failures.formatted(Generators.letterStrings(4, 10).next()));

        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "bad-failure.json"));
        assertEquals("a", ex.state());
        assertEquals("chat_failures[0]", ex.field());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(StateDefinition.ChatFailure.Kind.class)
    @DisplayName("refuses a chat failure whose code is present but not a string, for every kind, naming the entry")
    void rejectsNonStringChatFailureCode(StateDefinition.ChatFailure.Kind kind) {
        var status = kind == StateDefinition.ChatFailure.Kind.HTTP_ERROR
                ? ",\"status\":" + Generators.integers(400, 599).next()
                : "";
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{},"chat_failures":[
                  {"failure":"%s"%s,"code":%d}]}}}
                """.formatted(kind.wireValue(), status, Generators.integers(1, 999).next());

        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "non-string-code.json"));

        assertEquals("a", ex.state());
        assertEquals("chat_failures[0]", ex.field());
        assertTrue(ex.getMessage().contains("code is a string"), ex.getMessage());
    }

    @Test
    @DisplayName("refuses chat_failures that are not an array")
    void rejectsChatFailuresNotAnArray() {
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{},"chat_failures":{"failure":"lost"}}}}
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "bad-failures.json"));
        assertEquals("chat_failures", ex.field());
    }

    @Test
    @DisplayName("refuses an http_error status that would wrap into 400-599 when narrowed to an int")
    void rejectsWrappingChatFailureStatus() {
        var status = Generators.integers(400, 599).next();
        // 2^32 above the status: the low 32 bits are the status itself.
        var wrapping = (1L << 32) + status;
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{},"chat_failures":[
                  {"failure":"http_error","status":%d,"code":"scope_not_found"}]}}}
                """.formatted(wrapping);

        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "wrapping-status.json"));
        assertEquals("a", ex.state());
        assertEquals("chat_failures[0]", ex.field());
    }

    /**
     * A scenario whose one state plays one event, its data the envelope's own members followed by the
     * given payload members.
     */
    private static String stateWithEvent(String event, String payloadMembers) {
        return """
                {"format_version":1,"name":"x","start":"a",
                 "states":{"a":{"representation":{"scope":{"kind":"machine"},
                  "state":{"state_id":"a","kind":"operator_wait"},"links":[]},
                  "events":[{"after_ms":1,"event":"%1$s","data":{"event":"%1$s","at":"2026-11-10T18:00:00Z",
                   "scope":{"kind":"machine"},%2$s}}]}}}
                """.formatted(event, payloadMembers.strip());
    }

    private static String lowerLetters() {
        return Generators.letterStrings(4, 10).next().toLowerCase();
    }

    @Test
    @DisplayName("loads a state that plays a chat event of the catalogue with a mapped message key")
    void loadsChatEvent() {
        var chatId = lowerLetters();
        var turnId = lowerLetters();
        var target = lowerLetters();
        var json = stateWithEvent("chat.turn.progress", """
                "chat_id":"%s","turn_id":"%s","step":{"key":"chat.step.read","args":{"target":"%s"}}
                """.formatted(chatId, turnId, target));

        var events = Objects.requireNonNull(ScenarioLoader.load(json, "chat-event.json").state("a")).events();

        assertEquals(1, events.size());
        assertEquals("chat.turn.progress", events.getFirst().event());
    }

    @Test
    @DisplayName("loads a chat event whose message key takes an argument object with a member named key")
    void loadsChatEventWithKeyNamedArgument() {
        var json = stateWithEvent("chat.turn.progress", """
                "chat_id":"%s","turn_id":"%s","step":{"key":"chat.step.read","args":{"detail":{"key":"%s"}}}
                """.formatted(lowerLetters(), lowerLetters(), lowerLetters()));

        var events = Objects.requireNonNull(ScenarioLoader.load(json, "key-named-argument.json").state("a"))
                .events();

        assertEquals(1, events.size());
        assertEquals("chat.turn.progress", events.getFirst().event());
    }

    @Test
    @DisplayName("refuses a chat event name the catalogue does not hold before its payload is read")
    void rejectsUnknownChatEvent() {
        var json = stateWithEvent("chat.turn." + lowerLetters(), """
                "chat_id":"%s","turn_id":"%s","text":"%s"
                """.formatted(lowerLetters(), lowerLetters(), lowerLetters()));

        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "unknown-chat-event.json"));

        assertEquals("a", ex.state());
        assertEquals("events[0].event", ex.field());
    }

    @Test
    @DisplayName("refuses a chat event whose payload does not read through its pm-api record")
    void rejectsMalformedChatEventPayload() {
        var json = stateWithEvent("chat.turn.delta", """
                "chat_id":"%s","turn_id":"%s"
                """.formatted(lowerLetters(), lowerLetters()));

        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "malformed-chat-event.json"));

        assertEquals("a", ex.state());
        assertEquals("events[0].data", ex.field());
    }

    @Test
    @DisplayName("refuses a chat event carrying a message key that maps to no rendering key")
    void rejectsUnmappedChatMessageKey() {
        var unmapped = "chat.step." + lowerLetters();
        var json = stateWithEvent("chat.turn.progress", """
                "chat_id":"%s","turn_id":"%s","step":{"key":"%s"}
                """.formatted(lowerLetters(), lowerLetters(), unmapped));

        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "unmapped-chat-key.json"));

        assertEquals("a", ex.state());
        assertEquals("events[0].data", ex.field());
        assertTrue(ex.getMessage().contains(unmapped), ex.getMessage());
    }

    @Test
    @DisplayName("rejects a static route representation the pm-api codec refuses")
    void rejectsMalformedRouteRepresentation() {
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "routes":{"GET /api/v1/projects":{"representation":{"all_projects":[{"project_id":""}]}}},
                 "states":{"a":{"representation":{}}}}
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "closed-codec.json"));
        assertEquals("routes.GET /api/v1/projects.representation", ex.field());
    }

    @Test
    @DisplayName("rejects a parameterized plan process route the ProcessTree codec refuses")
    void rejectsMalformedPlanProcessRoute() {
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "routes":{"GET /api/v1/plans/fix-relay/process":{"representation":{"title":"no id"}}},
                 "states":{"a":{"representation":{}}}}
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "plan-process.json"));
        assertEquals("routes.GET /api/v1/plans/fix-relay/process.representation", ex.field());
        assertTrue(ex.getMessage().contains("plan_id"), ex.getMessage());
    }

    @Test
    @DisplayName("rejects a parameterized epic detail route the EpicResource codec refuses")
    void rejectsMalformedEpicDetailRoute() {
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "routes":{"GET /api/v1/epics/relay-hardening":{"representation":{"epic_id":"relay-hardening"}}},
                 "states":{"a":{"representation":{}}}}
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "epic-detail.json"));
        assertEquals("routes.GET /api/v1/epics/relay-hardening.representation", ex.field());
        assertTrue(ex.getMessage().contains("project_id"), ex.getMessage());
    }

    @Test
    @DisplayName("keeps the generic fallback for a parameterized plan route no codec registers")
    void acceptsParameterizedRouteWithoutCodec() {
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "routes":{"GET /api/v1/plans/fix-relay/unregistered":{"representation":{"anything":true}}},
                 "states":{"a":{"representation":{"scope":{"kind":"machine"},
                  "state":{"state_id":"a","kind":"operator_wait"},"links":[]}}}}
                """;
        assertNotNull(ScenarioLoader.load(json, "plan-unregistered.json"));
    }

    @Test
    @DisplayName("accepts an error-status plan route whose body is the error, not the resource")
    void acceptsErrorStatusRoute() {
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "routes":{"GET /api/v1/plans/fix-relay/process":{"status":404,
                  "representation":{"error":{"code":"not_found"}}}},
                 "states":{"a":{"representation":{"scope":{"kind":"machine"},
                  "state":{"state_id":"a","kind":"operator_wait"},"links":[]}}}}
                """;
        var scenario = ScenarioLoader.load(json, "plan-error.json");
        assertEquals(404, scenario.route("GET", "/api/v1/plans/fix-relay/process").status());
    }

    private static final String REP_A = """
            {"scope":{"kind":"machine"},"state":{"state_id":"a","kind":"operator_wait"},"links":[]}""";

    private static String repWithLink(String href) {
        return """
                {"scope":{"kind":"machine"},"state":{"state_id":"a","kind":"operator_wait"},
                 "links":[{"id":"go","rel":"submit","method":"POST","href":"%s","params":{}}]}""".formatted(href);
    }

    private static String doc(String states) {
        return """
                {"format_version":1,"name":"x","start":"a","states":%s}""".formatted(states);
    }

    private static String docWithRoutes(String routes) {
        return """
                {"format_version":1,"name":"x","start":"a","routes":%s,
                 "states":{"a":{"representation":%s}}}""".formatted(routes, REP_A);
    }

    static Stream<Arguments> invalidScenarios() {
        return Stream.of(
                Arguments.of("a root that is no object", "[]", null),
                Arguments.of("no format_version", """
                        {"name":"x","start":"a","states":{"a":{"representation":{}}}}""", "format_version"),
                Arguments.of("a blank name", """
                        {"format_version":1,"name":" ","start":"a","states":{"a":{"representation":{}}}}""", "name"),
                Arguments.of("no start", """
                        {"format_version":1,"name":"x","states":{"a":{"representation":{}}}}""", "start"),
                Arguments.of("no state", doc("{}"), "states"),
                Arguments.of("states that are no object", doc("[]"), "states"),
                Arguments.of("a blank state id",
                        doc("{\"a\":{\"representation\":" + REP_A + "},\" \":{\"representation\":{}}}"), "states"),
                Arguments.of("a state that is no object", doc("{\"a\":1}"), null),
                Arguments.of("a blank link name",
                        doc("{\"a\":{\"representation\":{},\"links\":{\" \":{\"next\":\"a\"}}}}"), "links"),
                Arguments.of("a link that is no object",
                        doc("{\"a\":{\"representation\":{},\"links\":{\"go\":1}}}"), "links.go"),
                Arguments.of("a link without next",
                        doc("{\"a\":{\"representation\":{},\"links\":{\"go\":{\"method\":\"POST\"}}}}"), "links.go.next"),
                Arguments.of("a negative link delay",
                        doc("{\"a\":{\"representation\":{},\"links\":{\"go\":{\"next\":\"a\",\"delay_ms\":-1}}}}"),
                        "links.go.delay_ms"),
                Arguments.of("an event that is no object",
                        doc("{\"a\":{\"representation\":{},\"events\":[1]}}"), "events[0]"),
                Arguments.of("an event without after_ms",
                        doc("{\"a\":{\"representation\":{},\"events\":[{\"event\":\"check.result\",\"data\":{}}]}}"),
                        "events[0].after_ms"),
                Arguments.of("an event without a name",
                        doc("{\"a\":{\"representation\":{},\"events\":[{\"after_ms\":1,\"event\":\" \",\"data\":{}}]}}"),
                        "events[0].event"),
                Arguments.of("an event without data",
                        doc("{\"a\":{\"representation\":{},\"events\":[{\"after_ms\":1,\"event\":\"check.result\"}]}}"),
                        "events[0].data"),
                Arguments.of("an auto transition that is no object",
                        doc("{\"a\":{\"representation\":{},\"auto\":5}}"), "auto"),
                Arguments.of("an auto transition without after_ms",
                        doc("{\"a\":{\"representation\":{},\"auto\":{\"next\":\"a\"}}}"), "auto.after_ms"),
                Arguments.of("an auto transition without next",
                        doc("{\"a\":{\"representation\":{},\"auto\":{\"after_ms\":1}}}"), "auto.next"),
                Arguments.of("a route that is no object", docWithRoutes("{\"GET /x\":1}"), "routes.GET /x"),
                Arguments.of("a route whose representation is a scalar",
                        docWithRoutes("{\"GET /x\":{\"representation\":\"s\"}}"), "routes.GET /x.representation"),
                Arguments.of("a representation without a state_id",
                        doc("{\"a\":{\"representation\":{\"scope\":{\"kind\":\"machine\"},\"state\":{}}}}"),
                        "representation.state.state_id"),
                Arguments.of("a scenario link the representation does not offer",
                        doc("{\"a\":{\"representation\":" + REP_A
                                + ",\"links\":{\"go\":{\"next\":\"a\",\"method\":\"POST\"}}}}"), "links.go"),
                Arguments.of("a scenario method that disagrees with the offered link",
                        doc("{\"a\":{\"representation\":" + repWithLink("/api/v1/go")
                                + ",\"links\":{\"go\":{\"next\":\"a\",\"method\":\"GET\",\"href\":\"/api/v1/go\"}}}}"),
                        "links.go.method"),
                Arguments.of("a scenario href that disagrees with the offered link",
                        doc("{\"a\":{\"representation\":" + repWithLink("/api/v1/go")
                                + ",\"links\":{\"go\":{\"next\":\"a\",\"method\":\"POST\",\"href\":\"/api/v1/other\"}}}}"),
                        "links.go.href"),
                Arguments.of("an offered href that does not end in the link id",
                        doc("{\"a\":{\"representation\":" + repWithLink("/api/v1/go-else")
                                + ",\"links\":{\"go\":{\"next\":\"a\",\"method\":\"POST\"}}}}"),
                        "links.go.href"),
                Arguments.of("an offered link without a scenario link",
                        doc("{\"a\":{\"representation\":" + repWithLink("/api/v1/go") + "}}"),
                        "representation.links"),
                Arguments.of("an event name outside the catalogue",
                        doc("{\"a\":{\"representation\":" + REP_A
                                + ",\"events\":[{\"after_ms\":1,\"event\":\"no.such_event\",\"data\":{}}]}}"),
                        "events[0].event"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidScenarios")
    @DisplayName("refuses a scenario that breaks the closed schema, naming the field")
    void refusesAnInvalidScenario(String breach, String json, String field) {
        var identifier = Generators.letterStrings(4, 10).next() + ".json";

        var refused = assertThrows(ScenarioValidationException.class, () -> ScenarioLoader.load(json, identifier));

        assertEquals(identifier, refused.scenario(), breach);
        assertEquals(field, refused.field(), breach);
    }

    @Test
    @DisplayName("names a scenario without an identifier unnamed")
    void namesItUnnamed() {
        var refused = assertThrows(ScenarioValidationException.class, () -> ScenarioLoader.load("[]", " "));

        assertEquals("unnamed", refused.scenario());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET /api/v1/epics/relay-hardening/process", "GET /api/v1/plans/fix-relay/intent",
            "GET /api/v1/epics/relay-hardening/intent", "GET /api/v1/plans/fix-relay/outline",
            "GET /api/v1/plans/fix-relay/completion", "GET /api/v1/plans/fix-relay/tasks/TASK-1/activity",
            "GET /api/v1/epics/relay-hardening"})
    @DisplayName("decodes a plan or epic route with its resource's codec")
    void decodesAPlanOrEpicRoute(String routeKey) {
        var json = docWithRoutes("{\"" + routeKey + "\":{\"representation\":{}}}");

        var refused = assertThrows(ScenarioValidationException.class, () -> ScenarioLoader.load(json, "codec.json"));

        assertEquals("routes." + routeKey + ".representation", refused.field());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST /api/v1/plans/fix-relay/process", "GET /api/v1/plans/fix-relay",
            "GET /api/v1/epics/relay-hardening/unregistered", "GET /api/v1/epics//process",
            "GET /api/v1/plans/fix-relay/tasks/TASK-1/unregistered", "GET /api/v1/epics/",
            "GET /api/v1/epics/relay-hardening/tasks/TASK-1/activity",
            "GET /api/v1/plans/fix-relay/steps/TASK-1/activity", "GET /api/v1/plans//tasks/TASK-1/activity",
            "GET /api/v1/plans/fix-relay/tasks//activity"})
    @DisplayName("keeps a route of another shape without a codec")
    void keepsARouteWithoutCodec(String routeKey) {
        var json = docWithRoutes("{\"" + routeKey + "\":{\"representation\":{\"anything\":true}}}");

        var scenario = ScenarioLoader.load(json, "no-codec.json");

        assertNotNull(scenario.routes().get(routeKey));
    }

    @Nested
    @DisplayName("Loading by name or path")
    class ByNameOrPath {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"", ".json"})
        @DisplayName("finds a standard scenario by its name, with or without the suffix and the directory")
        void findsByName(String suffix) {
            var name = StandardScenarios.PROJECTS_AND_START;

            var byName = ScenarioLoader.load(name + suffix);
            var byResource = ScenarioLoader.load(ScenarioLoader.SCENARIOS_RESOURCE_DIR + name + suffix);

            assertEquals(byName.name(), byResource.name());
            assertEquals(byName.states().keySet(), byResource.states().keySet());
        }

        @Test
        @DisplayName("refuses a name no resource and no file carries")
        void refusesAnUnknownName() {
            var unknown = "unknown-" + Generators.letterStrings(4, 10).next();

            assertThrows(IllegalArgumentException.class, () -> ScenarioLoader.load(unknown));
        }

        @Test
        @DisplayName("loads a scenario file named by its path")
        void loadsAFile(@TempDir Path directory) throws Exception {
            var file = directory.resolve(Generators.letterStrings(4, 10).next() + ".json");
            Files.writeString(file, doc("{\"a\":{\"representation\":" + REP_A + "}}"));

            var scenario = ScenarioLoader.load(file.toString());

            assertEquals("x", scenario.name());
            assertNotNull(scenario.state("a"));
        }

        @Test
        @DisplayName("reports a path that cannot be read as such")
        void reportsAnUnreadablePath(@TempDir Path directory) {
            var path = directory.toString();

            assertThrows(UncheckedIOException.class, () -> ScenarioLoader.load(path));
        }
    }

    @Test
    @DisplayName("rejects a list representation for a recognized plan resource route")
    void rejectsListRepresentationForRegisteredRoute() {
        var json = """
                {"format_version":1,"name":"x","start":"a",
                 "routes":{"GET /api/v1/plans/fix-relay/process":{"representation":[]}},
                 "states":{"a":{"representation":{"scope":{"kind":"machine"},
                  "state":{"state_id":"a","kind":"operator_wait"},"links":[]}}}}
                """;
        var ex = assertThrows(ScenarioValidationException.class,
                () -> ScenarioLoader.load(json, "plan-list.json"));
        assertEquals("routes.GET /api/v1/plans/fix-relay/process.representation", ex.field());
        assertTrue(ex.getMessage().contains("not a list"), ex.getMessage());
    }
}
