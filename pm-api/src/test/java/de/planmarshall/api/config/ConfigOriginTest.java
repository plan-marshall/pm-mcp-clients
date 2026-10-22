/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

@DisplayName("Configuration levels and origins")
class ConfigOriginTest {

    private static final JsonFactory FACTORY = new JsonFactory();

    private static final List<String> ORIGINS =
            List.of("default", "derived", "machine", "team", "project", "plan");

    @Test
    @DisplayName("the origins are the closed specification catalogue in resolution order")
    void closedOriginCatalogue() {
        assertEquals(ORIGINS, ConfigOrigin.wireValues());
        assertEquals(ORIGINS.size(), ConfigOrigin.values().length);
        assertEquals(ORIGINS.size(), ConfigOrigin.wireValues().stream().distinct().count());
        assertEquals(List.of("machine", "project", "plan"), ConfigOrigin.operatorLevels());

        assertAll(
                () -> assertEquals(0, ConfigOrigin.DEFAULT.rank()),
                () -> assertEquals(1, ConfigOrigin.DERIVED.rank()),
                () -> assertEquals(2, ConfigOrigin.MACHINE.rank()),
                () -> assertEquals(3, ConfigOrigin.TEAM.rank()),
                () -> assertEquals(4, ConfigOrigin.PROJECT.rank()),
                () -> assertEquals(5, ConfigOrigin.PLAN.rank()));
    }

    @Test
    @DisplayName("the nearest level wins; an unknown origin is refused fail-closed")
    void resolutionOrderAndFailClosed() {
        assertTrue(ConfigOrigin.PLAN.overrides(ConfigOrigin.PROJECT));
        assertTrue(ConfigOrigin.PROJECT.overrides(ConfigOrigin.MACHINE));
        assertFalse(ConfigOrigin.MACHINE.overrides(ConfigOrigin.PROJECT));
        assertFalse(ConfigOrigin.DEFAULT.overrides(null));

        assertEquals(ConfigOrigin.DERIVED, ConfigOrigin.fromWire("derived"));
        assertEquals(ConfigOrigin.TEAM, ConfigOrigin.fromWire("TEAM"));
        assertNull(ConfigOrigin.fromWireOrNull("nope"));
        assertNull(ConfigOrigin.fromWireOrNull(null));
        assertThrows(IllegalArgumentException.class, () -> ConfigOrigin.fromWire("nope"));
        assertThrows(IllegalArgumentException.class, () -> ConfigOrigin.fromWire(null));
    }

    @Test
    @DisplayName("an effective value round-trips through JSON, the map, and the streaming codec")
    void configValueRoundTrip() throws Exception {
        var resource = new ConfigValueResource("workflow.landing", "pr", ConfigOrigin.PROJECT, ConfigOrigin.MACHINE, 7L);

        assertEquals(resource, ConfigValueResource.fromJson(resource.toJson()));
        assertEquals(resource, ConfigValueResource.fromMap(resource.toMap()));
        assertEquals(resource, readStreaming(resource.toJson()));
        assertEquals(resource, writeStreaming(resource));
    }

    @Test
    @DisplayName("a value may be any JSON tree value; an absent value reads back null")
    void valueMayBeAnyTreeValue() throws Exception {
        var value = JsonTree.parse("{\"nested\":[1,2,{\"k\":true}]}");
        var resource = new ConfigValueResource("derived.push_remote", value, ConfigOrigin.DERIVED, null, 0L);
        assertEquals(resource, ConfigValueResource.fromJson(resource.toJson()));

        var absent = ConfigValueResource.fromJson("{\"key\":\"k.a\",\"origin\":\"default\",\"revision\":0}");
        assertNull(absent.value());
        assertNull(absent.overrides());

        var numeric = new ConfigValueResource("cleanup.plans_after_days", new JsonNumber("30"), ConfigOrigin.MACHINE,
                ConfigOrigin.DEFAULT, 2L);
        assertEquals(numeric, ConfigValueResource.fromJson(numeric.toJson()));
    }

    @Test
    @DisplayName("an origin may only override a level farther from the plan")
    void overridesMustBeALowerLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigValueResource("k.a", "v", ConfigOrigin.TEAM, ConfigOrigin.PROJECT, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigValueResource("k.a", "v", ConfigOrigin.MACHINE, ConfigOrigin.MACHINE, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigValueResource(" ", "v", ConfigOrigin.DEFAULT, null, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigValueResource("k.a", "v", ConfigOrigin.DEFAULT, null, -1L));
    }

    @Test
    @DisplayName("an unknown origin, a missing key, or a bad revision is refused")
    void malformedValueIsRefused() {
        assertThrows(JsonFormatException.class,
                () -> ConfigValueResource.fromJson("{\"key\":\"k.a\",\"origin\":\"nope\",\"revision\":0}"));
        assertThrows(JsonFormatException.class,
                () -> ConfigValueResource.fromJson("{\"origin\":\"default\",\"revision\":0}"));
        assertThrows(JsonFormatException.class,
                () -> ConfigValueResource.fromJson("{\"key\":\"k.a\",\"origin\":\"default\",\"revision\":-1}"));
        assertThrows(JsonFormatException.class,
                () -> ConfigValueResource.fromJson("{\"key\":\"k.a\",\"origin\":\"default\",\"revision\":1.5}"));
        assertThrows(JsonFormatException.class,
                () -> ConfigValueResource.fromJson("[]"));
    }

    @Test
    @DisplayName("an explicit null value is serialized; an invalid override order is a format error")
    void explicitNullAndOverrideOrder() throws Exception {
        var nullValue = new ConfigValueResource("k.a", null, ConfigOrigin.DEFAULT, null, 0L);
        assertTrue(nullValue.toJson().contains("\"value\":null"));
        assertEquals(nullValue, ConfigValueResource.fromJson(nullValue.toJson()));

        assertThrows(JsonFormatException.class,
                () -> ConfigValueResource.fromJson(
                        "{\"key\":\"k.a\",\"value\":\"v\",\"origin\":\"machine\",\"overrides\":\"plan\","
                                + "\"revision\":1}"));
    }

    private static ConfigValueResource readStreaming(String json) throws IOException {
        try (var parser = FACTORY.createParser(json)) {
            return ConfigValueResource.readFrom(parser);
        }
    }

    private static ConfigValueResource writeStreaming(ConfigValueResource resource) throws IOException {
        var out = new StringWriter();
        try (var generator = FACTORY.createGenerator(out)) {
            resource.writeTo(generator);
        }
        assertFalse(out.toString().isBlank());
        return ConfigValueResource.fromJson(out.toString());
    }
}
