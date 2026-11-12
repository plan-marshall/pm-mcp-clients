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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.planmarshall.api.chat.ChatMessageKeys;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.json.JsonTree;

/**
 * Every chat message key the scenario files seed, in a seeded chat or in a chat event a state plays, is
 * one {@link ChatMessageKeys} maps and carries the arguments its rendering needs, so a regression in a
 * seed fails the build instead of rendering a missing key in a client.
 */
@DisplayName("The chat message keys the scenario files seed")
class SeededChatMessageKeysTest {

    private static final String CHAT_KEY_PREFIX = "chat.";
    /** The keys whose rendering formats an argument the key does not carry in its name. */
    private static final Map<String, String> REQUIRED_ARGS = Map.of(
            "chat.applies.after_task", "task_id",
            "chat.knows.other", "outcome",
            "chat.step.read", "target");

    /** Every canonical scenario, as the catalogue of the standard scenarios lists them. */
    static Stream<String> scenarios() {
        return StandardScenarios.PRIMARY_SCENARIOS.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    @DisplayName("are mapped and carry their required arguments; a scenario may seed none")
    void seededKeysAreValid(String scenario) throws Exception {
        var seeded = seeded(scenario);

        var invalid = new ArrayList<String>();
        for (var text : seeded) {
            var key = (String) text.get(MessageText.FIELD_KEY);
            if (!ChatMessageKeys.maps(key)) {
                invalid.add(key + " is not mapped");
            }
            var required = REQUIRED_ARGS.get(key);
            if (required != null) {
                var args = JsonTree.object(text, MessageText.FIELD_ARGS);
                if (args == null || args.get(required) == null) {
                    invalid.add(key + " lacks the argument " + required);
                }
            }
        }
        assertTrue(invalid.isEmpty(), scenario + ": " + invalid);
    }

    @Test
    @DisplayName("are found across the canonical scenarios, so the check above inspects some")
    void someScenarioSeedsKeys() throws Exception {
        var count = 0;
        for (var scenario : StandardScenarios.PRIMARY_SCENARIOS) {
            count += seeded(scenario).size();
        }

        assertTrue(count > 0, "the canonical scenarios seed chat message keys");
    }

    private static List<Map<String, Object>> seeded(String scenario) throws IOException {
        var seeded = new ArrayList<Map<String, Object>>();
        collect(JsonTree.parse(read(scenario)), seeded);
        return seeded;
    }

    private static String read(String scenario) throws IOException {
        try (var in = SeededChatMessageKeysTest.class.getResourceAsStream("/scenarios/" + scenario + ".json")) {
            assertNotNull(in, scenario);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Collects every object of the tree whose {@code key} member is a chat message key.
     */
    @SuppressWarnings("unchecked")
    private static void collect(Object value, List<Map<String, Object>> seeded) {
        if (value instanceof Map<?, ?> map) {
            if (map.get(MessageText.FIELD_KEY) instanceof String key && key.startsWith(CHAT_KEY_PREFIX)) {
                seeded.add((Map<String, Object>) map);
            }
            map.values().forEach(member -> collect(member, seeded));
        } else if (value instanceof List<?> list) {
            list.forEach(member -> collect(member, seeded));
        }
    }
}
