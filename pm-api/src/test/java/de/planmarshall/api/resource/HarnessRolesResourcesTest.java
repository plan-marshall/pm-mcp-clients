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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

@DisplayName("Harness enrolment and role resource records")
@EnableGeneratorController
class HarnessRolesResourcesTest {

    private static final JsonFactory FACTORY = new JsonFactory();
    private static final String SHA =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Test
    @DisplayName("HarnessEnrolmentResource round-trips through JSON and the streaming codec")
    void harnessEnrolmentRoundTrip() throws Exception {
        var resource = new HarnessEnrolmentResource("claude", "/usr/local/bin/claude", "2.4.1", SHA,
                HarnessEnrolmentResource.Status.ENROLLED);

        assertEquals(resource, HarnessEnrolmentResource.fromJson(resource.toJson()));
        assertEquals(resource, HarnessEnrolmentResource.fromMap(resource.toMap()));
        assertEquals(resource, readStreaming(resource.toJson(), HarnessEnrolmentResource::readFrom));
        assertEquals(resource, writeStreaming(resource, HarnessEnrolmentResource::fromJson));
        assertEquals("enrolled", resource.status().wire());
        assertEquals(List.of("detected", "enrolled", "updated"),
                HarnessEnrolmentResource.Status.wireValues());
        assertEquals(HarnessEnrolmentResource.Status.UPDATED,
                HarnessEnrolmentResource.Status.requireWire("UPDATED"));
        assertNull(HarnessEnrolmentResource.Status.fromWire("unknown"));
    }

    @Test
    @DisplayName("a harness enrolment normalizes its absolute path and refuses invalid members")
    void harnessEnrolmentValidation() {
        var upper = SHA.toUpperCase();
        assertAll(
                () -> assertEquals("/usr/local/bin/claude",
                        new HarnessEnrolmentResource("claude", "/usr/local/./bin/claude", "2.4.1", SHA,
                                HarnessEnrolmentResource.Status.ENROLLED).executablePath()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new HarnessEnrolmentResource("Claude", "/bin/claude", "2.4.1", SHA,
                                HarnessEnrolmentResource.Status.ENROLLED)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new HarnessEnrolmentResource("claude", "claude", "2.4.1", SHA,
                                HarnessEnrolmentResource.Status.ENROLLED)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new HarnessEnrolmentResource("claude", "/bin/claude", " ", SHA,
                                HarnessEnrolmentResource.Status.ENROLLED)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new HarnessEnrolmentResource("claude", "/bin/claude", "2.4.1", upper,
                                HarnessEnrolmentResource.Status.ENROLLED)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new HarnessEnrolmentResource("claude", "/bin/claude", "2.4.1", "abc",
                                HarnessEnrolmentResource.Status.ENROLLED)),
                () -> assertThrows(NullPointerException.class,
                        () -> new HarnessEnrolmentResource("claude", "/bin/claude", "2.4.1", SHA, null)));
    }

    @Test
    @DisplayName("a harness enrolment is refused fail-closed at the wire boundary")
    void harnessEnrolmentWireRefusals() {
        assertAll(
                () -> assertThrows(JsonFormatException.class,
                        () -> HarnessEnrolmentResource.fromJson("[]")),
                () -> assertThrows(JsonFormatException.class,
                        () -> HarnessEnrolmentResource.fromMap(Map.of("client", "claude"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> HarnessEnrolmentResource.fromMap(Map.of(
                                "client", "claude",
                                "executable_path", "/bin/claude",
                                "version", "2.4.1",
                                "sha256_digest", SHA,
                                "status", "unknown"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> HarnessEnrolmentResource.fromMap(Map.of(
                                "client", "claude",
                                "executable_path", "relative",
                                "version", "2.4.1",
                                "sha256_digest", SHA,
                                "status", "enrolled"))));
    }

    @Test
    @DisplayName("RoleSummaryResource round-trips and defaults an absent member")
    void roleSummaryRoundTrip() throws Exception {
        var displayName = Generators.nonBlankStrings().next();
        var description = Generators.nonBlankStrings().next();
        var resource = new RoleSummaryResource("implementer", displayName, description,
                List.of("pm_wait", "pm_do", "write"), "shipped", "writer", "fresh", "claude",
                "opus-4.5", "high", "project", "machine", "shipped",
                RoleQualificationState.QUALIFIED, false);

        assertEquals(resource, RoleSummaryResource.fromJson(resource.toJson()));
        assertEquals(resource, RoleSummaryResource.fromMap(resource.toMap()));
        assertEquals(resource, readStreaming(resource.toJson(), RoleSummaryResource::readFrom));
        assertEquals(resource, writeStreaming(resource, RoleSummaryResource::fromJson));

        var minimal = RoleSummaryResource.fromMap(Map.of(
                "role", "judge",
                "start_failure_guard_held", false));
        assertAll(
                () -> assertEquals("judge", minimal.displayName()),
                () -> assertEquals("", minimal.description()),
                () -> assertTrue(minimal.capabilities().isEmpty()),
                () -> assertNull(minimal.sourceSet()),
                () -> assertEquals(RoleQualificationState.UNQUALIFIED, minimal.qualificationState()),
                () -> assertFalse(minimal.startFailureGuardHeld()));
    }

    @Test
    @DisplayName("RoleSummaryResource refuses an invalid role and an unknown qualification state")
    void roleSummaryRefusals() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new RoleSummaryResource("Implementer", null, null, List.of(), null, null,
                                null, null, null, null, null, null, null,
                                RoleQualificationState.QUALIFIED, false)),
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleSummaryResource.fromMap(Map.of("role", "judge", "qualification_state", "maybe"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleSummaryResource.fromJson("[]")),
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleSummaryResource.fromMap(Map.of())));
    }

    @Test
    @DisplayName("a malformed guard or tool array is refused instead of coerced")
    void malformedRoleArraysAreRefused() {
        assertAll(
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleSummaryResource.fromMap(Map.of("role", "judge"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleSummaryResource.fromMap(Map.of(
                                "role", "judge",
                                "start_failure_guard_held", "yes"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleSummaryResource.fromMap(Map.of(
                                "role", "judge",
                                "start_failure_guard_held", false,
                                "capabilities", "pm_wait"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleSummaryResource.fromMap(Map.of(
                                "role", "judge",
                                "start_failure_guard_held", false,
                                "capabilities", List.of(1)))),
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleDetailResource.fromMap(Map.of(
                                "role", "judge",
                                "advisory_tool_set", List.of(1)))));
    }

    @Test
    @DisplayName("a role list is read from a bare array or an object with a roles member")
    void roleSummaryList() throws Exception {
        var first = new RoleSummaryResource("implementer", null, null, List.of(),
                "shipped", "writer", "fresh", "claude", "opus-4.5", "high",
                "project", "machine", "shipped", RoleQualificationState.QUALIFIED, false);
        var second = new RoleSummaryResource("judge", null, null, List.of(),
                "shipped", "read_only", "fresh", "claude", "opus-4.5", null,
                "project", "shipped", null, RoleQualificationState.UNQUALIFIED, false);

        assertEquals(List.of(first, second), RoleSummaryResource.listFromJson(
                "[" + first.toJson() + "," + second.toJson() + "]"));
        assertEquals(List.of(first, second), RoleSummaryResource.listFromJson(
                "{\"roles\":[" + first.toJson() + "," + second.toJson() + "]}"));
        assertEquals(List.of(), RoleSummaryResource.listFromJson("{\"roles\":[]}"));
        assertThrows(JsonFormatException.class, () -> RoleSummaryResource.listFromJson("{}"));
        assertThrows(JsonFormatException.class, () -> RoleSummaryResource.listFromJson("[1]"));
    }

    @Test
    @DisplayName("RoleDetailResource round-trips its advisory tools, effort, and parameters")
    void roleDetailRoundTrip() throws Exception {
        var displayName = Generators.nonBlankStrings().next();
        var description = Generators.nonBlankStrings().next();
        var count = Generators.integers(0, 100).next();
        var parameters = Map.<String, Object>of(
                "source_set", "shipped",
                "form", "fresh",
                "count", count,
                "corpus_topics", List.of("java", "testing"));
        var resource = new RoleDetailResource("implementer", displayName, description,
                List.of("pm_wait", "pm_do", "write"),
                new RoleDetailResource.NarrowedEffort("high", "The harness narrows high to medium"),
                new RoleDetailResource.Qualification(RoleQualificationState.QUALIFIED_FRESH_ONLY,
                        "warm gap above the project level", List.of("correctness", "tool use")),
                parameters);

        assertEquals(resource, RoleDetailResource.fromJson(resource.toJson()));
        assertEquals(resource, RoleDetailResource.fromMap(resource.toMap()));
        assertEquals(resource, readStreaming(resource.toJson(), RoleDetailResource::readFrom));
        assertEquals(resource, writeStreaming(resource, RoleDetailResource::fromJson));
        assertEquals(JsonNumber.of(count), resource.configurationParameters().get("count"));
        var configuration = resource.configurationParameters();
        assertThrows(UnsupportedOperationException.class,
                () -> configuration.put("x", "y"));
    }

    @Test
    @DisplayName("RoleDetailResource reads shorthand forms and fails closed on an unknown state")
    void roleDetailShorthand() throws Exception {
        var detail = RoleDetailResource.fromMap(Map.of(
                "role", "judge",
                "narrowed_effort", "medium",
                "qualification", "unqualified",
                "tools", List.of("pm_wait"),
                "harness", "claude",
                "source_set", "machine"));

        assertAll(
                () -> assertEquals("medium", detail.narrowedEffort().category()),
                () -> assertNull(detail.narrowedEffort().description()),
                () -> assertEquals(RoleQualificationState.UNQUALIFIED, detail.qualification().state()),
                () -> assertEquals(List.of("pm_wait"), detail.advisoryToolSet()),
                () -> assertEquals("claude", detail.configurationParameters().get("harness")),
                () -> assertEquals("machine", detail.configurationParameters().get("source_set")));

        assertAll(
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleDetailResource.fromMap(Map.of(
                                "role", "judge",
                                "qualification", "maybe"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleDetailResource.fromMap(Map.of("role", "Judge"))),
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleDetailResource.fromJson("[]")),
                () -> assertThrows(JsonFormatException.class,
                        () -> RoleDetailResource.fromMap(Map.of("role", "judge", "qualification", Map.of(
                                "state", "unknown")))));
    }

    @Test
    @DisplayName("RoleDetailResource refuses a parameter value that is no JSON tree value")
    void roleDetailParameterValidation() {
        var qualification = new RoleDetailResource.Qualification(RoleQualificationState.UNQUALIFIED, null,
                List.of());
        var parameters = Map.of("bad", new Object());
        assertThrows(IllegalArgumentException.class,
                () -> new RoleDetailResource("judge", null, null, List.of(), null,
                        qualification, parameters));
    }

    @Test
    @DisplayName("a qualification state has the closed wire catalogue")
    void qualificationStateCatalogue() {
        assertEquals(List.of("qualified", "qualified_fresh_only", "unqualified", "invalid"),
                RoleQualificationState.wireValues());
        assertEquals(RoleQualificationState.QUALIFIED,
                RoleQualificationState.requireWire("QUALIFIED"));
        assertNull(RoleQualificationState.fromWire("maybe"));
        assertThrows(IllegalArgumentException.class,
                () -> RoleQualificationState.requireWire("maybe"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("a narrowed effort refuses a blank category")
    void narrowedEffortRefusesBlankCategory(String category) {
        assertThrows(IllegalArgumentException.class, () -> new RoleDetailResource.NarrowedEffort(category, null));
    }

    @Test
    @DisplayName("a narrowed effort and a qualification drop a blank text and default absent criteria")
    void blankTextsAreDropped() {
        var category = Generators.nonBlankStrings().next();

        var effort = new RoleDetailResource.NarrowedEffort(category, " ");
        var qualification = new RoleDetailResource.Qualification(RoleQualificationState.INVALID, " ", null);

        assertAll(
                () -> assertNull(effort.description()),
                () -> assertEquals(Map.of("category", category), effort.toMap()),
                () -> assertNull(qualification.reason()),
                () -> assertTrue(qualification.criteria().isEmpty()),
                () -> assertFalse(qualification.toMap().containsKey("reason")));
    }

    @Test
    @DisplayName("RoleDetailResource defaults its display name to the role and an absent member to none")
    void roleDetailDefaults() throws Exception {
        var qualification = new RoleDetailResource.Qualification(RoleQualificationState.UNQUALIFIED, null, null);

        var detail = new RoleDetailResource("judge", " ", null, null, null, qualification, null);

        assertAll(
                () -> assertEquals("judge", detail.displayName()),
                () -> assertEquals("", detail.description()),
                () -> assertTrue(detail.advisoryToolSet().isEmpty()),
                () -> assertTrue(detail.configurationParameters().isEmpty()),
                () -> assertFalse(detail.toMap().containsKey("narrowed_effort")),
                () -> assertEquals(detail, RoleDetailResource.fromJson(detail.toJson())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("javaNumbers")
    @DisplayName("a Java number given as a parameter becomes its JSON number and survives the wire")
    void javaNumbersNormalize(String type, Object value, String expected) throws Exception {
        var qualification = new RoleDetailResource.Qualification(RoleQualificationState.QUALIFIED, null, null);

        var detail = new RoleDetailResource("judge", null, null, List.of(), null, qualification,
                Map.of("value", value));

        assertEquals(new JsonNumber(expected), detail.configurationParameters().get("value"), type);
        assertEquals(detail, RoleDetailResource.fromJson(detail.toJson()), type);
    }

    static Stream<Arguments> javaNumbers() {
        var small = Generators.integers(1, 100).next();
        return Stream.of(
                Arguments.of("byte", (byte) (int) small, small.toString()),
                Arguments.of("short", (short) (int) small, small.toString()),
                Arguments.of("long", (long) small, small.toString()),
                Arguments.of("float", (float) small, small + ".0"),
                Arguments.of("double", (double) small, small + ".0"),
                Arguments.of("big decimal", new BigDecimal(BigInteger.valueOf(small), -2), small + "00"),
                Arguments.of("big integer", BigInteger.valueOf(small), small.toString()));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("a parameter that is no finite number is refused, as a double and as a float")
    void nonFiniteNumbersRefused(double value) {
        var qualification = new RoleDetailResource.Qualification(RoleQualificationState.QUALIFIED, null, null);
        var asDouble = Map.<String, Object>of("value", value);
        var asFloat = Map.<String, Object>of("value", (float) value);

        assertThrows(IllegalArgumentException.class, () -> new RoleDetailResource("judge", null, null,
                List.of(), null, qualification, asDouble));
        assertThrows(IllegalArgumentException.class, () -> new RoleDetailResource("judge", null, null,
                List.of(), null, qualification, asFloat));
    }

    @Test
    @DisplayName("nested parameters are normalized throughout and a null parameter stays JSON null")
    void nestedParametersNormalize() throws Exception {
        var count = Generators.integers(0, 100).next();
        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("limits", Map.of("count", count));
        parameters.put("sizes", List.of(count, (long) count));
        parameters.put("enabled", Boolean.TRUE);
        parameters.put("unset", null);
        var qualification = new RoleDetailResource.Qualification(RoleQualificationState.QUALIFIED, null, null);

        var detail = new RoleDetailResource("judge", null, null, List.of(), null, qualification, parameters);

        var number = JsonNumber.of(count);
        assertAll(
                () -> assertEquals(Map.of("count", number), detail.configurationParameters().get("limits")),
                () -> assertEquals(List.of(number, number), detail.configurationParameters().get("sizes")),
                () -> assertEquals(Boolean.TRUE, detail.configurationParameters().get("enabled")),
                () -> assertTrue(detail.configurationParameters().containsKey("unset")),
                () -> assertNull(detail.configurationParameters().get("unset")),
                () -> assertTrue(detail.toJson().contains("\"unset\":null")));
    }

    @Test
    @DisplayName("RoleDetailResource reads a narrowed effort object by its category or its effort alias")
    void narrowedEffortObjectForms() throws Exception {
        var description = Generators.nonBlankStrings().next();

        var byCategory = RoleDetailResource.fromMap(Map.of("role", "judge",
                "narrowed_effort", Map.of("category", "high", "description", description)));
        var byAlias = RoleDetailResource.fromMap(Map.of("role", "judge",
                "narrowed_effort", Map.of("effort", "low")));

        assertAll(
                () -> assertEquals(new RoleDetailResource.NarrowedEffort("high", description),
                        byCategory.narrowedEffort()),
                () -> assertEquals(new RoleDetailResource.NarrowedEffort("low", null), byAlias.narrowedEffort()));
    }

    @Test
    @DisplayName("RoleDetailResource reads a blank qualification as unqualified and refuses malformed members")
    void roleDetailWireForms() throws Exception {
        var blank = RoleDetailResource.fromMap(Map.of("role", "judge", "qualification", ""));

        assertEquals(RoleQualificationState.UNQUALIFIED, blank.qualification().state());
        assertAll(
                () -> assertThrows(JsonFormatException.class, () -> RoleDetailResource.fromMap(Map.of(
                        "role", "judge", "narrowed_effort", Map.of("description", "x")))),
                () -> assertThrows(JsonFormatException.class, () -> RoleDetailResource.fromMap(Map.of(
                        "role", "judge", "narrowed_effort", 3))),
                () -> assertThrows(JsonFormatException.class, () -> RoleDetailResource.fromMap(Map.of(
                        "role", "judge", "qualification", 3))),
                () -> assertThrows(JsonFormatException.class, () -> RoleDetailResource.fromMap(Map.of())));
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
        var json = out.toString();
        assertFalse(json.isBlank());
        assertEquals(JsonTree.write(JsonTree.parse(json)), json);
        return reader.read(json);
    }

    private static void write(Object resource, JsonGenerator generator) throws IOException {
        switch (resource) {
            case HarnessEnrolmentResource harness -> harness.writeTo(generator);
            case RoleSummaryResource summary -> summary.writeTo(generator);
            case RoleDetailResource detail -> detail.writeTo(generator);
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
