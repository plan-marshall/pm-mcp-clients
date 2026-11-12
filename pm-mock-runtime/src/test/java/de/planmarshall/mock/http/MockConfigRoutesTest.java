/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.http;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

/**
 * The mock's configuration routes on their own: the three the transport table keeps, the level and
 * scope a write addresses, the aspect-owned refusal, and the value a read resolves.
 */
@EnableGeneratorController
@DisplayName("the mock's configuration routes")
class MockConfigRoutesTest {

    private static final String PATH = MockConfigRoutes.PATH;

    @Test
    @DisplayName("a path outside the route is not served")
    void anOutsidePathIsNotServed() throws Exception {
        var routes = new MockConfigRoutes();

        assertFalse(routes.handle(request("GET", "/api/v1/status"), response(), Map.of()),
                "an unrelated path is left to the other handlers");
    }

    @Test
    @DisplayName("GET serves the machine's values with their origin and revision")
    void getServesTheMachineValues() throws Exception {
        var routes = new MockConfigRoutes();
        var answer = query(routes, "GET", PATH, null, Map.of("web.port", 7420));

        assertAll(
                () -> assertEquals(200, answer.status()),
                () -> assertTrue(answer.body().contains("\"key\":\"web.port\""), answer.body()),
                () -> assertTrue(answer.body().contains("\"origin\":\"machine\""), answer.body()));
    }

    @Test
    @DisplayName("PUT records a value under {value} and serves it back")
    void putRecordsTheValueField() throws Exception {
        var routes = new MockConfigRoutes();
        var machine = new LinkedHashMap<String, Object>();
        machine.put("web.port", 7420);

        var written = query(routes, "PUT", PATH + "/machine/custom.timeout", "{\"value\":\"60\"}", machine);
        var read = query(routes, "GET", PATH, null, machine);

        assertAll(
                () -> assertEquals(200, written.status(), written.body()),
                () -> assertTrue(read.body().contains("\"key\":\"custom.timeout\""), read.body()),
                () -> assertTrue(read.body().contains("\"value\":\"60\""), read.body()));
    }

    @Test
    @DisplayName("PUT records a body that is the value itself")
    void putRecordsABareValue() throws Exception {
        var routes = new MockConfigRoutes();
        var written = query(routes, "PUT", PATH + "/machine/custom.flag", "true", Map.of());

        assertAll(
                () -> assertEquals(200, written.status(), written.body()),
                () -> assertTrue(written.body().contains("\"value\":true"), written.body()));
    }

    @Test
    @DisplayName("a nearer level overrides the machine's and names it")
    void aNearerLevelNamesTheLevelItShadows() throws Exception {
        var routes = new MockConfigRoutes();
        query(routes, "PUT", PATH + "/machine/custom.label", "{\"value\":\"machine\"}", Map.of());
        query(routes, "PUT", PATH + "/project/custom.label", "{\"value\":\"local\"}", Map.of());
        var read = query(routes, "GET", PATH, null, Map.of());

        assertAll(
                () -> assertTrue(read.body().contains("\"origin\":\"project\""), read.body()),
                () -> assertTrue(read.body().contains("\"overrides\":\"machine\""), read.body()));
    }

    @Test
    @DisplayName("a project's value is scoped to that project")
    void aProjectValueIsScoped() throws Exception {
        var routes = new MockConfigRoutes();
        query(routes, "PUT", PATH + "/project/custom.label?project=p-1", "{\"value\":\"one\"}", Map.of());
        var scoped = query(routes, "GET", PATH + "?project=p-1", null, Map.of());
        var other = query(routes, "GET", PATH + "?project=p-2", null, Map.of());

        assertAll(
                () -> assertTrue(scoped.body().contains("custom.label"), scoped.body()),
                () -> assertFalse(other.body().contains("custom.label"), other.body()));
    }

    @Test
    @DisplayName("PUT refuses an aspect-owned key and a level the operator may not write")
    void putRefusesTheKeysTheTableDoesNotOwn() throws Exception {
        var routes = new MockConfigRoutes();
        var aspectOwned = query(routes, "PUT", PATH + "/machine/language.interface", "{\"value\":\"de\"}",
                Map.of());
        var unknownLevel = query(routes, "PUT", PATH + "/team/custom.key", "{\"value\":1}", Map.of());
        var withoutKey = query(routes, "PUT", PATH + "/machine", "{\"value\":1}", Map.of());

        assertAll(
                () -> assertEquals(409, aspectOwned.status(), aspectOwned.body()),
                () -> assertEquals(400, unknownLevel.status(), unknownLevel.body()),
                () -> assertEquals(400, withoutKey.status(), withoutKey.body()));
    }

    @Test
    @DisplayName("DELETE removes one level's value and refuses an unknown one")
    void deleteRemovesOneLevel() throws Exception {
        var routes = new MockConfigRoutes();
        query(routes, "PUT", PATH + "/machine/custom.flag", "{\"value\":\"on\"}", Map.of());
        var deleted = query(routes, "DELETE", PATH + "/machine/custom.flag", null, Map.of());
        var unknown = query(routes, "DELETE", PATH + "/machine/custom.flag", null, Map.of());
        var read = query(routes, "GET", PATH, null, Map.of());

        assertAll(
                () -> assertEquals(204, deleted.status(), deleted.body()),
                () -> assertEquals(404, unknown.status(), unknown.body()),
                () -> assertFalse(read.body().contains("custom.flag"), read.body()));
    }

    @Test
    @DisplayName("an empty body records no value")
    void anEmptyBodyRecordsNoValue() throws Exception {
        var routes = new MockConfigRoutes();
        var written = query(routes, "PUT", PATH + "/machine/custom.none", "", Map.of());

        assertAll(
                () -> assertEquals(200, written.status(), written.body()),
                () -> assertTrue(written.body().contains("\"value\":null"), written.body()));
    }

    @Test
    @DisplayName("a stored null at a nearer level overrides the machine's value")
    void aStoredNullOverridesTheMachine() throws Exception {
        var routes = new MockConfigRoutes();
        query(routes, "PUT", PATH + "/machine/custom.label", "{\"value\":\"machine\"}", Map.of());
        query(routes, "PUT", PATH + "/project/custom.label", "{\"value\":null}", Map.of());
        var read = query(routes, "GET", PATH, null, Map.of());

        assertAll(
                            () -> assertTrue(read.body().contains("\"origin\":\"project\""), read.body()),
                            () -> assertTrue(read.body().contains("\"value\":null"), read.body()));
    }

    @Test
    @DisplayName("the key an aspect owns is known by its prefix")
    void aspectOwnershipIsPrefixBased() {
        var key = Generators.letterStrings(4, 8).next();

        assertAll(
                () -> assertTrue(MockConfigRoutes.isAspectOwned("language." + key)),
                () -> assertTrue(MockConfigRoutes.isAspectOwned("harness." + key)),
                () -> assertTrue(MockConfigRoutes.isAspectOwned("roles")),
                () -> assertFalse(MockConfigRoutes.isAspectOwned("custom." + key)));
    }

    private record Answer(int status, String body) {
    }

    private static Answer query(MockConfigRoutes routes, String method, String target, String body,
            Map<String, Object> machineValues) throws IOException {
        var out = new ByteArrayOutputStream();
        var served = routes.handle(request(method, target, body), new MockHttpResponse(out), machineValues);
        assertTrue(served, method + " " + target + " is one of the configuration routes");
        var raw = out.toString(StandardCharsets.UTF_8);
        var statusLine = raw.substring(0, raw.indexOf('\r'));
        var status = Integer.parseInt(statusLine.split(" ")[1]);
        var payload = raw.substring(raw.indexOf("\r\n\r\n") + 4);
        return new Answer(status, payload);
    }

    private static MockHttpRequest request(String method, String target) {
        return request(method, target, null);
    }

    private static MockHttpRequest request(String method, String target, String body) {
        var bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        return new MockHttpRequest(method, target, Map.of(), bytes, System.nanoTime());
    }

    private static MockHttpResponse response() {
        return new MockHttpResponse(new ByteArrayOutputStream());
    }
}
