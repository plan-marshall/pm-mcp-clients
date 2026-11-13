/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.transcript;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.json.JsonTree;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves how {@link DialogueHarness} reads the answer of {@code GET /mock/acts}: an act the mock
 * recorded without a body reads with the empty body, never {@code null}, as {@link DialogueHarness.Act}
 * and the mock's README state. The reading is tested on the answer itself, because the harness only
 * starts where a pseudo-terminal is available.
 */
@EnableGeneratorController
@DisplayName("DialogueHarness reads the recorded acts")
class DialogueHarnessActsTest {

    private static Map<String, Object> act(String path, String state) {
        var act = new LinkedHashMap<String, Object>();
        act.put("method", "POST");
        act.put("path", path);
        act.put("state", state);
        return act;
    }

    private static String answer(List<Map<String, Object>> acts) {
        var answer = new LinkedHashMap<String, Object>();
        answer.put("count", acts.size());
        answer.put("acts", new ArrayList<Object>(acts));
        return JsonTree.write(answer);
    }

    @Test
    @DisplayName("an act without a body member reads with the empty body, an act with one with its text")
    void absentBodyReadsEmpty() throws IOException {
        var path = "/api/v1/plans/" + Generators.letterStrings(4, 12).next() + "/pause";
        var decisionPath = "/api/v1/decisions/" + Generators.letterStrings(4, 12).next();
        var state = Generators.letterStrings(4, 12).next();
        var text = "{\"answer\":\"" + Generators.letterStrings(4, 24).next() + "\"}";
        var withBody = act(decisionPath, state);
        withBody.put("body", text);

        var acts = DialogueHarness.actsOf(answer(List.of(act(path, state), withBody)));

        assertEquals(List.of(new DialogueHarness.Act("POST", path, state, ""),
                            new DialogueHarness.Act("POST", decisionPath, state, text)), acts);
    }

    @Test
    @DisplayName("an act whose body member is null reads with the empty body")
    void nullBodyReadsEmpty() throws IOException {
        var path = "/api/v1/decisions/" + Generators.letterStrings(4, 12).next();
        var state = Generators.letterStrings(4, 12).next();
        var act = act(path, state);
        act.put("body", null);

        var acts = DialogueHarness.actsOf(answer(List.of(act)));

        assertEquals(List.of(new DialogueHarness.Act("POST", path, state, "")), acts);
    }

    @Test
    @DisplayName("an answer without acts reads as no act")
    void noActs() throws IOException {
        assertEquals(List.of(), DialogueHarness.actsOf(answer(List.of())));
    }
}
