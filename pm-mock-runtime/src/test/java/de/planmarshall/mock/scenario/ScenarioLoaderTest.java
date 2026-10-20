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


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ScenarioLoader")
class ScenarioLoaderTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/scenarios/status-only.json",
            "/scenarios/onboarding-happy.json",
            "/scenarios/onboarding-harness-not-logged-in.json",
            "/scenarios/web-pairing.json"
    })
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
    @DisplayName("parses complete scenario details including routes, events, and auto-transitions")
    void parsesScenarioDetails() {
        var json = """
                {
                  "format_version": 1,
                  "name": "full-test",
                  "description": "A full scenario test",
                  "start": "initial",
                  "pairing_code": "4321-8765",
                  "routes": {
                    "GET /api/v1/ping": {
                      "status": 200,
                      "representation": { "pong": true }
                    }
                  },
                  "states": {
                    "initial": {
                      "representation": {
                        "status": "idle",
                        "links": {
                          "advance": { "href": "/api/v1/advance", "method": "POST" }
                        }
                      },
                      "links": {
                        "advance": {
                          "method": "POST",
                          "href": "/api/v1/advance",
                          "next": "second",
                          "delay_ms": 10
                        }
                      },
                      "events": [
                        { "after_ms": 50, "event": "alert", "data": { "msg": "hello" } }
                      ],
                      "auto": {
                        "after_ms": 200,
                        "next": "second"
                      }
                    },
                    "second": {
                      "representation": { "status": "completed" }
                    }
                  }
                }
                """;

        var scenario = ScenarioLoader.load(json, "test.json");
        assertEquals(1, scenario.formatVersion());
        assertEquals("full-test", scenario.name());
        assertEquals("A full scenario test", scenario.description());
        assertEquals("initial", scenario.start());
        assertEquals("4321-8765", scenario.pairingCode());

        // Routes
        var route = scenario.route("GET", "/api/v1/ping");
        assertNotNull(route);
        assertEquals(200, route.status());

        // States
        var initial = scenario.state("initial");
        assertNotNull(initial);
        assertEquals(1, initial.links().size());
        var link = initial.links().get("advance");
        assertNotNull(link);
        assertEquals("POST", link.method());
        assertEquals("/api/v1/advance", link.href());
        assertEquals("second", link.next());
        assertEquals(10, link.delayMs());

        // Events & Auto
        assertEquals(1, initial.events().size());
        assertEquals("alert", initial.events().getFirst().event());
        assertEquals(50, initial.events().getFirst().afterMs());
        assertNotNull(initial.auto());
        assertEquals("second", initial.auto().next());
        assertEquals(200, initial.auto().afterMs());

        var second = scenario.state("second");
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
    @DisplayName("rejects dangling next in links")
    void rejectsDanglingLinkNext() {
        var json = """
                {
                  "format_version": 1,
                  "name": "dangling-link",
                  "start": "initial",
                  "states": {
                    "initial": {
                      "representation": {},
                      "links": {
                        "go": { "method": "POST", "next": "does_not_exist" }
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
                      "representation": {},
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
}
