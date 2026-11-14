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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import de.planmarshall.api.intent.DeliverableRecord.AffectedFile;
import de.planmarshall.api.intent.DeliverableRecord.FileIntent;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * A deliverable of an outline: the invariants its record holds and the wire shapes its codec refuses.
 */
@DisplayName("DeliverableRecord")
@EnableGeneratorController
class DeliverableRecordTest {

    private static String text() {
        return Generators.nonBlankStrings().next();
    }

    @Test
    @DisplayName("should replace absent lists of a constructed deliverable by empty lists")
    void shouldReplaceAbsentLists() {
        var deliverable = new DeliverableRecord(Generators.integers(1, 99).next(), text(), text(), null, null, null,
                null);

        assertAll(
                () -> assertTrue(deliverable.affectedFiles().isEmpty()),
                () -> assertTrue(deliverable.dependsOn().isEmpty()),
                () -> assertTrue(deliverable.satisfies().isEmpty()),
                () -> assertTrue(deliverable.writtenFiles().isEmpty()));
    }

    static Stream<Arguments> brokenDeliverables() {
        var blank = " ".repeat(Generators.integers(1, 3).next());
        return Stream.of(
                Arguments.of("a number below 1", Generators.integers(-50, 0).next(), text(), text(), List.of(),
                        List.of()),
                Arguments.of("a number above 99", Generators.integers(100, 500).next(), text(), text(), List.of(),
                        List.of()),
                Arguments.of("a blank title", 1, blank, text(), List.of(), List.of()),
                Arguments.of("a blank intent gloss", 1, text(), blank, List.of(), List.of()),
                Arguments.of("a dependency below 1", 2, text(), text(), List.of(0), List.of()),
                Arguments.of("a dependency above 99", 2, text(), text(), List.of(100), List.of()),
                Arguments.of("a blank criterion", 2, text(), text(), List.of(), List.of(blank)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenDeliverables")
    @DisplayName("should refuse a deliverable that breaks an invariant")
    void shouldRefuseABrokenDeliverable(String violation, int number, String title, String gloss,
                                        List<Integer> dependsOn, List<String> satisfies) {
        assertThrows(IllegalArgumentException.class,
                () -> new DeliverableRecord(number, title, gloss, List.of(), dependsOn, satisfies, null), violation);
    }

    @Test
    @DisplayName("should refuse an affected file without a path")
    void shouldRefuseABlankPath() {
        var blank = " ".repeat(Generators.integers(0, 3).next());

        assertThrows(IllegalArgumentException.class, () -> new AffectedFile(blank, FileIntent.READ));
    }

    static Stream<Arguments> malformedMembers() {
        var file = Map.<String, Object>of("path", text(), "intent", "rename");
        return Stream.of(
                Arguments.of("an affected file of an unknown intent", "affected_files", List.of(file)),
                Arguments.of("a blank criterion", "satisfies", List.of(" ")),
                Arguments.of("a present null dependency list", "depends_on", null),
                Arguments.of("a present null criterion list", "satisfies", null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedMembers")
    @DisplayName("should refuse a deliverable whose members break its rules as a format error")
    void shouldRefuseMalformedMembers(String violation, String member, Object value) {
        var map = new HashMap<String, Object>(Map.of("number", 1, "title", text(), "intent_gloss", text()));
        map.put(member, value);

        assertThrows(JsonFormatException.class, () -> DeliverableRecord.fromMap(map), violation);
    }

    @Test
    @DisplayName("should refuse JSON that is no object")
    void shouldRefuseJsonThatIsNoObject() {
        var json = JsonTree.write(List.of(text()));

        assertThrows(JsonFormatException.class, () -> DeliverableRecord.fromJson(json));
    }

    @Test
    @DisplayName("should read the files a deliverable writes and the criteria it satisfies")
    void shouldReadWrittenFilesAndCriteria() throws Exception {
        var read = new AffectedFile(text(), FileIntent.READ);
        var written = new AffectedFile(text(), FileIntent.WRITE_NEW);
        var criterion = text();
        var deliverable = new DeliverableRecord(2, text(), text(), List.of(read, written), List.of(1),
                List.of(criterion), null);

        var parsed = DeliverableRecord.fromJson(deliverable.toJson());

        assertAll(
                () -> assertEquals(deliverable, parsed),
                () -> assertEquals(List.of(written), parsed.writtenFiles()),
                () -> assertTrue(parsed.satisfies(criterion)));
    }
}
