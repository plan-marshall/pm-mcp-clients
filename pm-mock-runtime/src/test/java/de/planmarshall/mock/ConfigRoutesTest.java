/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.UnixSocketHttpClient;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.mock.scenario.ScenarioLoader;

/**
 * The configuration routes the transport table keeps ({@code GET /api/v1/config}, {@code PUT} and
 * {@code DELETE /api/v1/config/{level}/{key}}): the operator's {@code config show|set|unset} read and
 * write them, so the mock serves them beside the machine settings.
 */
@DisplayName("the configuration routes of the mock runtime")
@EnabledOnOs({OS.MAC, OS.LINUX})
class ConfigRoutesTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String ORIGIN = "origin";
    private static final String VALUE = "value";

    @Test
    @DisplayName("GET /api/v1/config serves the machine's values with their origin")
    void listsTheMachineValues() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
            var values = values(runtime);

            assertAll(
                    () -> assertTrue(values.containsKey("language.interface"), values.toString()),
                    () -> assertEquals("machine", values.get("language.interface").get(ORIGIN),
                            values.toString()),
                    () -> assertEquals("en", values.get("language.interface").get(VALUE),
                            values.toString()),
                    () -> assertTrue(values.containsKey("sources.documents_mode"), values.toString()));
        }
    }

    @Test
    @DisplayName("PUT /api/v1/config/{level}/{key} records a value that GET serves back")
    void putRecordsAValue() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
            var written = client(runtime).send(authorized(HttpRequest.json("PUT",
                    "/api/v1/config/machine/custom.timeout", "{\"value\":\"60\"}"), runtime));
            var values = values(runtime);

            assertAll(
                    () -> assertEquals(200, written.status(), written.bodyText()),
                    () -> assertEquals("60", values.get("custom.timeout").get(VALUE), values.toString()),
                    () -> assertEquals("machine", values.get("custom.timeout").get(ORIGIN),
                            values.toString()));
        }
    }

    @Test
    @DisplayName("a value of a nearer level overrides the machine's and names it")
    void nearerLevelOverridesTheMachine() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
            var written = client(runtime).send(authorized(HttpRequest.json("PUT",
                    "/api/v1/config/project/custom.label", "{\"value\":\"local\"}"), runtime));
            client(runtime).send(authorized(HttpRequest.json("PUT",
                    "/api/v1/config/machine/custom.label", "{\"value\":\"machine\"}"), runtime));

            var value = values(runtime).get("custom.label");
            assertAll(
                    () -> assertEquals(200, written.status(), written.bodyText()),
                    () -> assertEquals("local", value.get(VALUE), value.toString()),
                    () -> assertEquals("project", value.get(ORIGIN), value.toString()),
                    () -> assertEquals("machine", value.get("overrides"), value.toString()));
        }
    }

    @Test
    @DisplayName("PUT refuses a key an aspect owns with 409 aspect_owned")
    void putRefusesAnAspectOwnedKey() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
            var refused = client(runtime).send(authorized(HttpRequest.json("PUT",
                    "/api/v1/config/machine/language.interface", "{\"value\":\"de\"}"), runtime));

            assertEquals(409, refused.status(), refused.bodyText());
            assertEquals("aspect_owned", JsonTree.string(JsonTree.asObject(JsonTree.parse(refused.bodyText())),
                    "error"));
        }
    }

    @Test
    @DisplayName("DELETE removes the value of one level and refuses an unknown key")
    void deleteRemovesOneLevel() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.FULLY_ONBOARDED)) {
            client(runtime).send(authorized(HttpRequest.json("PUT",
                    "/api/v1/config/machine/custom.flag", "{\"value\":\"on\"}"), runtime));
            var deleted = client(runtime).send(authorized(new HttpRequest("DELETE",
                    "/api/v1/config/machine/custom.flag", Map.of(), null), runtime));
            var unknown = client(runtime).send(authorized(new HttpRequest("DELETE",
                    "/api/v1/config/machine/custom.flag", Map.of(), null), runtime));

            assertAll(
                    () -> assertEquals(204, deleted.status(), deleted.bodyText()),
                    () -> assertEquals(404, unknown.status(), unknown.bodyText()));
        }
    }

    @Test
    @DisplayName("the machine settings offer the later run of every aspect")
    void settingsOfferTheLaterRuns() {
        var scenario = ScenarioLoader.load(StandardScenarios.ONBOARDING);
        var settings = scenario.state("settings.choose");

        assertNotNull(settings, "settings.choose");
        assertAll(
                () -> assertTrue(settings.links().containsKey("reconfigure:language"),
                        settings.links().keySet().toString()),
                () -> assertTrue(settings.links().containsKey("reconfigure:plan-defaults"),
                        settings.links().keySet().toString()));
    }

    @Test
    @DisplayName("a later run of one aspect starts at its own state and returns to the settings")
    void laterRunReturnsToTheSettings() {
        var scenario = ScenarioLoader.load(StandardScenarios.ONBOARDING);
        var settings = scenario.state("settings.choose");
        assertNotNull(settings, "settings.choose");

        var aspects = List.of(
                "language", "harnesses", "models", "roles", "credentials", "providers", "plan-defaults");
        for (var aspect : aspects) {
            var link = settings.links().get("reconfigure:" + aspect);
            assertNotNull(link, aspect);
            var start = scenario.state(link.next());
            assertNotNull(start, link.next());
            var back = start.links().get("set");
            assertNotNull(back, link.next());
            assertEquals("settings.choose", back.next(), link.next());
        }
    }

    @Test
    @DisplayName("a later run records the answer it submits and the read serves it")
    void aLaterRunRecordsItsAnswer() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING_SETTINGS)) {
            client(runtime).send(authorized(HttpRequest.json("POST", "/api/v1/onboarding/step",
                    "{\"link\":\"reconfigure:language\"}"), runtime));
            client(runtime).send(authorized(HttpRequest.json("POST", "/api/v1/onboarding/step",
                    "{\"link\":\"set\",\"interface\":\"de\",\"model\":\"same\"}"), runtime));
            var language = values(runtime).get("language.interface");

            assertAll(
                    () -> assertEquals("de", language.get(VALUE), language.toString()),
                    () -> assertEquals("machine", language.get(ORIGIN), language.toString()));
        }
    }

    @Test
    @DisplayName("a later run of the store and of the plan defaults records what they submit")
    void aLaterRunRecordsTheStoreAndTheDefaults() throws Exception {
        try (var runtime = MockRuntime.start(StandardScenarios.ONBOARDING_SETTINGS)) {
            client(runtime).send(authorized(HttpRequest.json("POST", "/api/v1/onboarding/step",
                    "{\"link\":\"reconfigure:credentials\"}"), runtime));
            client(runtime).send(authorized(HttpRequest.json("POST", "/api/v1/onboarding/step",
                    "{\"link\":\"set\",\"current\":\"file\"}"), runtime));
            client(runtime).send(authorized(HttpRequest.json("POST", "/api/v1/onboarding/step",
                    "{\"link\":\"reconfigure:plan-defaults\"}"), runtime));
            client(runtime).send(authorized(HttpRequest.json("POST", "/api/v1/onboarding/step",
                    "{\"link\":\"set\",\"landing\":\"landing.files\"}"), runtime));
            client(runtime).send(authorized(HttpRequest.json("POST", "/api/v1/onboarding/step",
                    "{\"link\":\"reconfigure:providers\"}"), runtime));
            client(runtime).send(authorized(HttpRequest.json("POST", "/api/v1/onboarding/step",
                    "{\"link\":\"set\",\"verify\":\"true\"}"), runtime));
            var values = values(runtime);

            assertAll(
                    () -> assertEquals("file", values.get("credentials.store").get(VALUE),
                            values.toString()),
                    () -> assertEquals("landing.files", values.get("plan_defaults.landing").get(VALUE),
                            values.toString()),
                    () -> assertTrue(String.valueOf(values.get("providers.enabled").get(VALUE))
                            .contains("verified"), values.toString()));
        }
    }

    private static Map<String, Map<String, Object>> values(MockRuntime runtime) throws Exception {
        var response = client(runtime).send(authorized(HttpRequest.get("/api/v1/config"), runtime));
        assertEquals(200, response.status(), response.bodyText());
        var values = new LinkedHashMap<String, Map<String, Object>>();
        var elements = JsonTree.asArray(JsonTree.parse(response.bodyText()));
        assertNotNull(elements, response.bodyText());
        for (var element : elements) {
            var map = JsonTree.asObject(element);
            assertNotNull(map, response.bodyText());
            values.put(JsonTree.string(map, "key"), map);
        }
        return values;
    }

    private static HttpRequest authorized(HttpRequest request, MockRuntime runtime) {
        return request.withHeader(AUTHORIZATION, "Bearer " + runtime.token());
    }

    private static UnixSocketHttpClient client(MockRuntime runtime) {
        return new UnixSocketHttpClient(runtime.socketPath());
    }
}
