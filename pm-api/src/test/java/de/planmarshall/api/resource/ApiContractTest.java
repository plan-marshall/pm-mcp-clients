/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;

@DisplayName("API contract of /api/v1")
class ApiContractTest {

    private static final JsonFactory FACTORY = new JsonFactory();
    private static final Instant STARTED_AT = Instant.parse("2026-03-01T08:30:00Z");

    private static final List<String> ERROR_CODES = List.of(
            // 400
            "invalid_arguments", "invalid_answer", "dependency_unanswered", "pairing_invalid",
            "unit_name_invalid",
            // 403
            "cli_only", "loopback_only",
            // 404
            "project_not_enrolled", "scope_not_found", "target_not_found",
            // 409
            "act_not_available", "conflict", "already_answered", "onboarding_incomplete",
            "aspect_owned", "lifecycle_closed", "scope_active", "release_landing_pending",
            "axis_bound", "team_owned", "plan_ended", "already_consumed", "not_waivable",
            "raise_out_of_range", "position_not_contained", "enrolment_root_conflict",
            "project_store_root_mismatch", "enrolment_tampered", "enrolment_insecure",
            "enrolment_invalid", "harness_version_unsupported", "web_listener_conflict",
            "not_withdrawable", "upgrade_not_possible", "credential_store_unavailable",
            "unit_not_designable", "unit_invalid", "no_project_to_compose",
            // 423, 500, 503
            "credential_store_locked", "internal_fault", "credential_store_failed",
            "runtime_recovering");

    private static final List<String> LINK_RELATIONS =
            List.of("self", "status", "stop", "web", "credentials", "events");

    @Test
    @DisplayName("the error codes are the closed specification catalogue with their HTTP statuses")
    void closedErrorCodeCatalogue() {
        assertEquals(ERROR_CODES, ApiErrorCode.wireValues());
        assertEquals(ERROR_CODES.size(), ApiErrorCode.values().length);
        assertEquals(ERROR_CODES.size(), ApiErrorCode.wireValues().stream().distinct().count());
        assertEquals(List.of(400, 403, 404, 409, 423, 500, 503), ApiErrorCode.statuses());

        assertAll(
                () -> assertEquals(400, ApiErrorCode.INVALID_ARGUMENTS.status()),
                () -> assertEquals(403, ApiErrorCode.CLI_ONLY.status()),
                () -> assertEquals(404, ApiErrorCode.SCOPE_NOT_FOUND.status()),
                () -> assertEquals(409, ApiErrorCode.ACT_NOT_AVAILABLE.status()),
                () -> assertEquals(423, ApiErrorCode.CREDENTIAL_STORE_LOCKED.status()),
                () -> assertEquals(500, ApiErrorCode.INTERNAL_FAULT.status()),
                () -> assertEquals(503, ApiErrorCode.RUNTIME_RECOVERING.status()));

        // no member carries a bare 401: an unauthenticated request is answered without a code
        assertTrue(ApiErrorCode.wireValues().stream()
                .map(ApiErrorCode::fromWire)
                .noneMatch(code -> code.status() == 401));
        assertTrue(Arrays.stream(ApiErrorCode.values())
                .allMatch(code -> ApiErrorCode.statuses().contains(code.status())));
    }

    @Test
    @DisplayName("an unknown error code read from the wire is refused fail-closed")
    void unknownErrorCodeIsRefused() {
        assertEquals(ApiErrorCode.SCOPE_NOT_FOUND, ApiErrorCode.fromWire("scope_not_found"));
        assertEquals(ApiErrorCode.SCOPE_NOT_FOUND, ApiErrorCode.fromWire("SCOPE_NOT_FOUND"));
        assertNull(ApiErrorCode.fromWireOrNull("not_a_code"));

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> ApiErrorCode.fromWire("nope")),
                () -> assertThrows(IllegalArgumentException.class, () -> ApiErrorCode.fromWire(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> ApiErrorCode.fromWire("")));
    }

    @Test
    @DisplayName("the link relations are the closed catalogue")
    void closedLinkRelationCatalogue() {
        assertEquals(LINK_RELATIONS, LinkRelation.wireValues());
        assertEquals(LINK_RELATIONS.size(), LinkRelation.values().length);

        assertEquals(LinkRelation.SELF, LinkRelation.fromWire("self"));
        assertEquals(LinkRelation.EVENTS, LinkRelation.requireWire("EVENTS"));
        assertNull(LinkRelation.fromWire("not_a_relation"));
        assertNull(LinkRelation.fromWire(null));
        assertThrows(IllegalArgumentException.class, () -> LinkRelation.requireWire("not_a_relation"));
    }

    @Test
    @DisplayName("a resource link round-trips through JSON and the streaming codec")
    void resourceLinkRoundTrip() throws Exception {
        var link = new ResourceLink("/api/v1/status", LinkRelation.STATUS, ResourceLink.METHOD_GET, false);

        assertEquals(link, ResourceLink.fromJson(link.toJson()));
        assertEquals(link, ResourceLink.fromMap(link.toMap()));
        assertEquals(link, readStreaming(link.toJson(), ResourceLink::readFrom));
        assertEquals(link, writeStreaming(link, ResourceLink::fromJson));

        // a missing method defaults to GET, a templated href carries the flag
        var templated = ResourceLink.fromJson(
                "{\"href\":\"/api/v1/plans/{plan_id}\",\"rel\":\"self\",\"templated\":true}");
        assertAll(
                () -> assertEquals(ResourceLink.METHOD_GET, templated.method()),
                () -> assertTrue(templated.templated()));
    }

    @Test
    @DisplayName("an unknown or missing link relation is refused")
    void unknownLinkRelationIsRefused() {
        assertThrows(JsonFormatException.class,
                () -> ResourceLink.fromJson("{\"href\":\"/x\",\"rel\":\"nope\"}"));
        assertThrows(JsonFormatException.class,
                () -> ResourceLink.fromJson("{\"href\":\"/x\"}"));
        assertThrows(JsonFormatException.class,
                () -> ResourceLink.fromJson("{\"rel\":\"self\"}"));
    }

    @Test
    @DisplayName("the runtime status resource round-trips")
    void runtimeStatusRoundTrip() throws Exception {
        var resource = new RuntimeStatusResource("running", "0.1.0-SNAPSHOT", 4321L, STARTED_AT,
                "/home/oliver/.marshall/run/runtime.sock",
                List.of(new ResourceLink("/api/v1/status", LinkRelation.SELF,
                        ResourceLink.METHOD_GET, false)));

        assertEquals(resource, RuntimeStatusResource.fromJson(resource.toJson()));
        assertEquals(resource, RuntimeStatusResource.fromMap(resource.toMap()));
        assertEquals(resource, readStreaming(resource.toJson(), RuntimeStatusResource::readFrom));
        assertEquals(resource, writeStreaming(resource, RuntimeStatusResource::fromJson));
    }

    @Test
    @DisplayName("the web access resource round-trips")
    void webAccessRoundTrip() throws Exception {
        var resource = new WebAccessResource(WebAccessResource.STATUS_ON, 7420, false, 2,
                List.of(new ResourceLink("/api/v1/web/devices", LinkRelation.WEB,
                        ResourceLink.METHOD_GET, false)));

        assertEquals(resource, WebAccessResource.fromJson(resource.toJson()));
        assertEquals(resource, WebAccessResource.fromMap(resource.toMap()));
        assertEquals(resource, readStreaming(resource.toJson(), WebAccessResource::readFrom));
        assertEquals(resource, writeStreaming(resource, WebAccessResource::fromJson));
    }

    @Test
    @DisplayName("the runtime stop resource round-trips both modes")
    void runtimeStopRoundTrip() throws Exception {
        for (var mode : RuntimeStopResource.StopMode.values()) {
            var resource = new RuntimeStopResource(mode, 30L, List.of());
            assertEquals(resource, RuntimeStopResource.fromJson(resource.toJson()));
            assertEquals(resource, readStreaming(resource.toJson(), RuntimeStopResource::readFrom));
        }
        assertEquals(List.of("immediate", "drain"), RuntimeStopResource.StopMode.wireValues());
        assertThrows(JsonFormatException.class,
                () -> RuntimeStopResource.fromJson("{\"mode\":\"later\",\"timeout_seconds\":1}"));
        assertThrows(IllegalArgumentException.class,
                () -> RuntimeStopResource.StopMode.requireWire("later"));
    }

    @Test
    @DisplayName("the credential status resource round-trips present and absent")
    void credentialStatusRoundTrip() throws Exception {
        var present = new CredentialStatusResource(CredentialStatusResource.Availability.PRESENT,
                "/home/oliver/.config/gh/hosts.yml", STARTED_AT, List.of());
        var absent = new CredentialStatusResource(CredentialStatusResource.Availability.NOT_FOUND,
                null, null, List.of());

        assertEquals(present, CredentialStatusResource.fromJson(present.toJson()));
        assertEquals(present, readStreaming(present.toJson(), CredentialStatusResource::readFrom));
        assertEquals(absent, CredentialStatusResource.fromJson(absent.toJson()));
        assertEquals(absent, CredentialStatusResource.fromMap(absent.toMap()));
        assertNull(CredentialStatusResource.fromJson(absent.toJson()).tokenPath());
        assertThrows(JsonFormatException.class,
                () -> CredentialStatusResource.fromJson("{\"status\":\"maybe\"}"));
    }

    @Test
    @DisplayName("the runtime event stream resource round-trips")
    void runtimeEventStreamRoundTrip() throws Exception {
        var resource = new RuntimeEventStreamResource("open", 3, "evt-42",
                List.of(new ResourceLink("/api/v1/events", LinkRelation.EVENTS,
                        ResourceLink.METHOD_GET, false)));

        assertEquals(resource, RuntimeEventStreamResource.fromJson(resource.toJson()));
        assertEquals(resource, RuntimeEventStreamResource.fromMap(resource.toMap()));
        assertEquals(resource, readStreaming(resource.toJson(), RuntimeEventStreamResource::readFrom));
        assertEquals(resource, writeStreaming(resource, RuntimeEventStreamResource::fromJson));
    }

    @Test
    @DisplayName("malformed JSON and missing required fields are refused cleanly")
    void malformedJsonIsRefused() {
        assertThrows(IOException.class, () -> RuntimeStatusResource.fromJson("{not json"));
        assertThrows(JsonFormatException.class, () -> RuntimeStatusResource.fromJson("[]"));
        assertThrows(JsonFormatException.class,
                () -> RuntimeStatusResource.fromJson("{\"state\":\"running\"}"));
        assertThrows(JsonFormatException.class,
                () -> WebAccessResource.fromJson("{\"status\":\"on\"}"));
        assertThrows(JsonFormatException.class,
                () -> RuntimeEventStreamResource.fromJson("{\"status\":\"open\"}"));
        assertThrows(JsonFormatException.class,
                () -> RuntimeStatusResource.fromJson(
                        "{\"state\":\"running\",\"version\":\"1\",\"pid\":1,"
                                + "\"started_at\":\"not-a-time\",\"socket_path\":\"/s\"}"));
    }

    @Test
    @DisplayName("the constructors reject invalid values")
    void constructorsRejectInvalidValues() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ResourceLink(" ", LinkRelation.SELF, "GET", false)),
                () -> assertThrows(NullPointerException.class,
                        () -> new ResourceLink("/x", null, "GET", false)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new WebAccessResource("on", 70000, false, 0, List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new WebAccessResource("on", 7420, false, -1, List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RuntimeStopResource(RuntimeStopResource.StopMode.DRAIN, -1, List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RuntimeEventStreamResource("open", -1, null, List.of())),
                () -> assertThrows(NullPointerException.class,
                        () -> new RuntimeStatusResource("running", "1", 1, null, "/s", List.of())));
    }

    @Test
    @DisplayName("the streaming reader leaves trailing values readable")
    void streamingReaderLeavesTrailingValues() throws Exception {
        var resource = new RuntimeStopResource(RuntimeStopResource.StopMode.DRAIN, 45L, List.of());
        var stream = resource.toJson() + resource.toJson();

        try (var parser = FACTORY.createParser(stream)) {
            assertEquals(resource, RuntimeStopResource.readFrom(parser));
            assertEquals(resource, RuntimeStopResource.readFrom(parser));
        }
    }

    @Test
    @DisplayName("jackson-databind is not on the classpath of pm-api")
    void noJacksonDatabind() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.fasterxml.jackson.databind.ObjectMapper"));
    }

    @Test
    @DisplayName("the error body of a refusal carries a code of the closed list")
    void refusalCarriesAClosedCode() throws Exception {
        var body = Map.of("status", 404, "code", ApiErrorCode.SCOPE_NOT_FOUND.wire());
        var code = (String) body.get("code");
        assertEquals(404, ApiErrorCode.fromWire(code).status());
        assertNotEquals(ApiErrorCode.SCOPE_NOT_FOUND, ApiErrorCode.fromWire(ApiErrorCode.CONFLICT.wire()));
    }

    private static <T> T readStreaming(String json, ParserReader<T> reader) throws IOException {
        try (var parser = FACTORY.createParser(json)) {
            return reader.read(parser);
        }
    }

    private static <T> T writeStreaming(T resource, JsonReader<T> reader) throws IOException {
        var out = new StringWriter();
        try (var generator = FACTORY.createGenerator(out)) {
            write(resource, generator);
        }
        assertFalse(out.toString().isBlank());
        return reader.read(out.toString());
    }

    private static void write(Object resource, JsonGenerator generator) throws IOException {
        switch (resource) {
            case ResourceLink link -> link.writeTo(generator);
            case RuntimeStatusResource status -> status.writeTo(generator);
            case WebAccessResource web -> web.writeTo(generator);
            case RuntimeStopResource stop -> stop.writeTo(generator);
            case CredentialStatusResource credential -> credential.writeTo(generator);
            case RuntimeEventStreamResource events -> events.writeTo(generator);
            default -> throw new IllegalArgumentException("Not a resource: " + resource);
        }
    }

    @FunctionalInterface
    private interface ParserReader<T> {
        T read(JsonParser parser) throws IOException;
    }

    @FunctionalInterface
    private interface JsonReader<T> {
        T read(String json) throws IOException;
    }
}
