/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.intent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;

/**
 * The member readers of the intent codec: what each one refuses, and what an absent or {@code null}
 * member yields.
 */
@DisplayName("IntentJson member readers")
@EnableGeneratorController
class IntentJsonTest {

    private static final String KEY = "value";
    private static final String TYPE = "Fixture";

    /** A reader of one member of the fixture object. */
    @FunctionalInterface
    private interface MemberReader {
        Object read(Map<String, Object> object) throws Exception;
    }

    private static Map<String, Object> member(Object value) {
        var object = new HashMap<String, Object>();
        object.put(KEY, value);
        return object;
    }

    /** Each case gives a reader and a member value the reader refuses. */
    static Stream<Arguments> refusedMembers() {
        MemberReader requireText = object -> IntentJson.requireText(object, KEY, TYPE);
        MemberReader optionalText = object -> IntentJson.optionalText(object, KEY, TYPE);
        MemberReader optionalRawText = object -> IntentJson.optionalRawText(object, KEY, TYPE);
        MemberReader requireDouble = object -> IntentJson.requireDouble(object, KEY, TYPE);
        MemberReader requireBoolean = object -> IntentJson.requireBoolean(object, KEY, TYPE);
        MemberReader readStrings = object -> IntentJson.readStrings(object, KEY);
        MemberReader readInts = object -> IntentJson.readInts(object, KEY, 0, 10);
        MemberReader readObjects = object -> IntentJson.readObjects(object, KEY);
        return Stream.of(
                Arguments.of("required text absent", requireText, null),
                Arguments.of("required text no string", requireText, JsonNumber.of(1)),
                Arguments.of("required text blank", requireText, " "),
                Arguments.of("optional text no string", optionalText, Boolean.TRUE),
                Arguments.of("optional raw text no string", optionalRawText, List.of()),
                Arguments.of("required number absent", requireDouble, null),
                Arguments.of("required number no number", requireDouble, "1.5"),
                Arguments.of("required number unreadable", requireDouble, new JsonNumber("1.5.5")),
                Arguments.of("required number not finite", requireDouble, new JsonNumber("1e999")),
                Arguments.of("required number NaN", requireDouble, Double.NaN),
                Arguments.of("required boolean no boolean", requireBoolean, "true"),
                Arguments.of("strings present null", readStrings, null),
                Arguments.of("strings no array", readStrings, "a"),
                Arguments.of("strings holding a number", readStrings, List.of(JsonNumber.of(1))),
                Arguments.of("numbers no array", readInts, JsonNumber.of(1)),
                Arguments.of("numbers out of range", readInts, List.of(JsonNumber.of(11))),
                Arguments.of("objects present null", readObjects, null),
                Arguments.of("objects no array", readObjects, Map.of()),
                Arguments.of("objects holding a string", readObjects, List.of("a")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("refusedMembers")
    @DisplayName("should refuse a member outside the reader's form")
    void shouldRefuseMember(String violation, MemberReader reader, Object value) {
        var object = member(value);

        assertThrows(JsonFormatException.class, () -> reader.read(object), violation);
    }

    @Test
    @DisplayName("should yield nothing for an absent optional member or list, a null optional member, and keep a blank raw text")
    void shouldYieldNothingForAbsentOptionalMembers() throws Exception {
        var blank = " ".repeat(Generators.integers(1, 5).next());
        var absent = Map.<String, Object>of();
        var nullMember = member(null);

        assertAll(
                () -> assertNull(IntentJson.optionalText(absent, KEY, TYPE)),
                () -> assertNull(IntentJson.optionalText(member(blank), KEY, TYPE)),
                () -> assertNull(IntentJson.optionalRawText(nullMember, KEY, TYPE)),
                () -> assertEquals(blank, IntentJson.optionalRawText(member(blank), KEY, TYPE)),
                () -> assertNull(IntentJson.optionalDouble(nullMember, KEY, TYPE)),
                () -> assertTrue(IntentJson.readStrings(absent, KEY).isEmpty()),
                () -> assertTrue(IntentJson.readInts(absent, KEY, 0, 1).isEmpty()),
                () -> assertTrue(IntentJson.readObjects(absent, KEY).isEmpty()));
    }

    @Test
    @DisplayName("should read numbers of a map built in memory")
    void shouldReadNumbersOfAMapInMemory() throws Exception {
        var value = Generators.integers(0, 10).next();

        assertAll(
                () -> assertEquals(value.doubleValue(), IntentJson.requireDouble(member(value), KEY, TYPE)),
                () -> assertEquals(List.of(value),
                        IntentJson.readInts(member(List.of(JsonNumber.of(value))), KEY, 0, 10)));
    }

    @Test
    @DisplayName("should refuse a streamed value that is no object")
    void shouldRefuseStreamedValueThatIsNoObject() throws Exception {
        var text = Generators.integers(0, 1000).next().toString();

        try (var parser = new JsonFactory().createParser("[" + text + "]")) {
            assertThrows(JsonFormatException.class, () -> IntentJson.readObject(parser, TYPE));
        }
    }
}
