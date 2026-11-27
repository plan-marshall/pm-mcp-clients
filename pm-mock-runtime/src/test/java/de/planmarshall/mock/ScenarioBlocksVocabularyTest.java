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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonTree;

/**
 * The one vocabulary of what a decision or a needs-operator item blocks, carried by every scenario
 * of the mock runtime (`needs-and-find.adoc`, What It Blocks; `decisions-texts.adoc`).
 */
@DisplayName("ScenarioBlocksVocabularyTest")
class ScenarioBlocksVocabularyTest {

    private static final Path SCENARIOS = Path.of("src/main/resources/scenarios");

    /** The closed list a scenario may carry, and the only values the clients word. */
    private static final Set<String> VOCABULARY = Set.of("plan", "epic", "setup", "entry", "task", "nothing");

    private static final String ROUTE = "GET /api/v1/machine/needs-operator";

    private static final String FIELD_ITEMS = "items";

    private static final String FIELD_BLOCKS = "blocks";

    private static final String FIELD_SCOPE = "scope";

    private static final String FIELD_REVERSIBILITY = "reversibility";

    private static final String FIELD_KIND = "kind";

    private static final String FIELD_ID = "id";

    @Test
    @DisplayName("every decision of every scenario carries a value of the vocabulary")
    void decisionsCarryTheVocabulary() throws Exception {
        var offenders = new ArrayList<String>();
        var read = new ArrayList<String>();
        for (var file : scenarioFiles()) {
            var name = file.getFileName().toString();
            for (var decision : find(load(file), ScenarioBlocksVocabularyTest::isDecision)) {
                read.add(name + " " + decision.get(FIELD_ID));
                var blocks = JsonTree.string(decision, FIELD_BLOCKS);
                if (blocks == null) {
                    offenders.add(name + " " + decision.get(FIELD_ID) + ": absent");
                } else if (!VOCABULARY.contains(blocks)) {
                    offenders.add(name + " " + decision.get(FIELD_ID) + ": " + blocks);
                }
            }
        }

        assertFalse(read.isEmpty(), "no decision was read at all");
        assertEquals(List.of(), offenders, "a decision blocks a value outside the vocabulary");
    }

    @Test
    @DisplayName("every needs-operator item of every scenario carries a value of the vocabulary")
    void needsOperatorItemsCarryTheVocabulary() throws Exception {
        var offenders = new ArrayList<String>();
        var read = new ArrayList<String>();
        for (var file : scenarioFiles()) {
            var name = file.getFileName().toString();
            for (var route : find(load(file), node -> node.containsKey(ROUTE))) {
                for (var item : itemsOf(route.get(ROUTE))) {
                    var blocks = JsonTree.string(item, FIELD_BLOCKS);
                    read.add(name + " " + item.get(FIELD_KIND));
                    if (blocks == null) {
                        offenders.add(name + " " + item.get(FIELD_KIND) + ": absent");
                    } else if (!VOCABULARY.contains(blocks)) {
                        offenders.add(name + " " + item.get(FIELD_KIND) + ": " + blocks);
                    }
                }
            }
        }

        assertFalse(read.isEmpty(), "no needs-operator item was read at all");
        assertEquals(List.of(), offenders,
                "a needs-operator item blocks a value outside the vocabulary, or carries none");
    }

    @Test
    @DisplayName("reads the scenarios the mock serves")
    void scenariosAreRead() throws Exception {
        if (scenarioFiles().isEmpty()) {
            fail("no scenario was found below " + SCENARIOS.toAbsolutePath());
        }
    }

    /**
     * @return the scenario files the mock runtime serves
     * @throws IOException if the directory cannot be read
     */
    private static List<Path> scenarioFiles() throws IOException {
        try (Stream<Path> files = Files.list(SCENARIOS)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".json")).sorted().toList();
        }
    }

    /**
     * @param file a scenario file
     * @return its parsed document
     * @throws IOException on a read or format failure
     */
    private static Map<String, Object> load(Path file) throws IOException {
        var map = JsonTree.asObject(JsonTree.parse(Files.readString(file)));
        if (map == null) {
            throw new IOException("Expected a JSON object in " + file);
        }
        return map;
    }

    /**
     * @param root a parsed document
     * @param predicate the shape to collect
     * @return every object of the document the predicate selects
     */
    private static List<Map<String, Object>> find(Map<String, Object> root,
            Predicate<Map<String, Object>> predicate) {
        var found = new ArrayList<Map<String, Object>>();
        collect(root, predicate, found);
        return found;
    }

    private static void collect(Object node, Predicate<Map<String, Object>> predicate,
            List<Map<String, Object>> found) {
        if (node instanceof Map<?, ?> map) {
            var copy = stringKeys(map);
            if (predicate.test(copy)) {
                found.add(copy);
            }
            for (var value : copy.values()) {
                collect(value, predicate, found);
            }
        } else if (node instanceof List<?> list) {
            for (var value : list) {
                collect(value, predicate, found);
            }
        }
    }

    /**
     * @param map a parsed object
     * @return the same entries under string keys
     */
    private static Map<String, Object> stringKeys(Map<?, ?> map) {
        var copy = new LinkedHashMap<String, Object>();
        for (var entry : map.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }

    /**
     * @param decision an object
     * @return whether it is a decision envelope, whether or not it carries {@code blocks}: a missing
     *         value is one of the offenders the caller reports
     */
    private static boolean isDecision(Map<String, Object> decision) {
        return decision.get(FIELD_SCOPE) instanceof String
                && decision.get(FIELD_REVERSIBILITY) instanceof String;
    }

    /**
     * @param route a served needs-operator route
     * @return the items it serves, whether it carries them directly or under {@code representation}
     */
    private static List<Map<String, Object>> itemsOf(Object route) {
        var found = new ArrayList<Map<String, Object>>();
        if (route instanceof Map<?, ?> map) {
            Object items = map.get(FIELD_ITEMS);
            if (items == null && map.get("representation") instanceof Map<?, ?> nested) {
                items = nested.get(FIELD_ITEMS);
            }
            if (items instanceof List<?> list) {
                for (var item : list) {
                    if (item instanceof Map<?, ?> itemMap) {
                        found.add(stringKeys(itemMap));
                    }
                }
            }
        }
        return found;
    }
}
