/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.transcript.TerminalTestDriver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * One accepted set per prompt: the line a prompt prints as offered is the collection of keys its
 * branches accept, so a key added to one and not the other turns this class red.
 * <p>
 * The offered line is read twice: from the dialogue's own builder (the set the branches use) and
 * from a rendered not-offered answer, so the builder cannot drift from what the operator sees.
 */
@DisplayName("EpicDecompositionDialogue: the offered keys")
class EpicDecompositionDialogueKeysTest {

    private static final String PROJECT = "pm-mcp-clients";
    private static final String EPIC = "ep-001";
    private static final String BASE = "/api/v1/projects/" + PROJECT + "/epics/" + EPIC + "/decomposition";
    private static final String EN = "en";
    private static final String DE = "de";

    private static final String THREE_PLANS = """
                        {"epic_id":"relay-hardening","status":"review","revision":3,
                         "workstreams":[{"id":"WS-1","name":"Reconnect","order":1,
                           "staged_plans":[
                             {"plan_id":"a-plan","title":"a","expected_surface":["a"],"dependencies":[],"status":"staged"},
                             {"plan_id":"b-plan","title":"b","expected_surface":["b"],"dependencies":[],"status":"staged"},
                             {"plan_id":"c-plan","title":"c","expected_surface":["c"],"dependencies":[],"status":"staged"}]}],
                         "findings":[]}
                        """;

    @TempDir
    Path tempDir;

    private RuntimeFixture fixture;

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            if (BASE.equals(request.target()) && "GET".equals(request.method())) {
                response.json(200, THREE_PLANS);
            } else if ((BASE + "/instruct").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, THREE_PLANS);
            } else if ((BASE + "/check").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, THREE_PLANS);
            } else if ((BASE + "/commit").equals(request.target()) && "POST".equals(request.method())) {
                response.json(200, "{\"status\":\"committed\",\"ledger_revision\":4,\"launchable_plan_count\":3}");
            } else {
                response.send(404, null, "");
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }

    @Test
    @DisplayName("the cut prompt offers exactly the keys its branches accept")
    void cutPromptOffersWhatItAccepts() throws Exception {
        var offered = offeredCut(EN);

        assertAll(
                            () -> assertEquals("1-3, +, r, q, ?", offered),
                            () -> assertEquals(offeredExpectation(EN, EpicDecompositionDialogue.CUT_KEYS, 3),
                                                offeredKeys(offered)),
                            () -> assertTrue(render(EN, "z\n").contains("z is not offered here: 1-3, +, r, q, ?"))
        );
    }

    @Test
    @DisplayName("the plan prompt offers exactly the keys its branches accept")
    void planPromptOffersWhatItAccepts() throws Exception {
        var offered = offeredLine(EN, EpicDecompositionDialogue.PLAN_KEYS);

        assertAll(
                            () -> assertEquals("k, e, b, q, ?", offered),
                            () -> assertEquals(offeredExpectation(EN, EpicDecompositionDialogue.PLAN_KEYS, 0),
                                                offeredKeys(offered)),
                            () -> assertTrue(render(EN, "1\nz\n").contains("z is not offered here: k, e, b, q, ?"))
        );
    }

    @Test
    @DisplayName("the read-back prompt offers exactly the keys its branches accept")
    void readBackPromptOffersWhatItAccepts() throws Exception {
        var offered = offeredLine(EN, EpicDecompositionDialogue.READ_BACK_KEYS);

        assertAll(
                            () -> assertEquals("i, x, q, ?", offered),
                            () -> assertEquals(offeredExpectation(EN, EpicDecompositionDialogue.READ_BACK_KEYS, 0),
                                                offeredKeys(offered)),
                            () -> assertTrue(render(EN, "1\ne\nsmaller\nz\n")
                                                .contains("z is not offered here: i, x, q, ?"))
        );
    }

    @Test
    @DisplayName("the prompts offer the German leave key, and nothing else moves")
    void germanOffersTheGermanLeaveKey() throws Exception {
        var cut = offeredCut(DE);
        var plan = offeredLine(DE, EpicDecompositionDialogue.PLAN_KEYS);
        var readBack = offeredLine(DE, EpicDecompositionDialogue.READ_BACK_KEYS);

        assertAll(
                            () -> assertEquals("1-3, +, r, v, ?", cut),
                            () -> assertEquals("k, e, b, v, ?", plan),
                            () -> assertEquals("i, x, v, ?", readBack),
                            () -> assertEquals(offeredExpectation(DE, EpicDecompositionDialogue.CUT_KEYS, 3),
                                                offeredKeys(cut)),
                            () -> assertEquals(offeredExpectation(DE, EpicDecompositionDialogue.PLAN_KEYS, 0),
                                                offeredKeys(plan)),
                            () -> assertEquals(offeredExpectation(DE, EpicDecompositionDialogue.READ_BACK_KEYS, 0),
                                                offeredKeys(readBack)),
                            () -> assertTrue(render(DE, "1\nz\n").contains("z wird hier nicht angeboten: k, e, b, v, ?"))
        );
    }

    /** @return the cut prompt's offered line, read from the dialogue's own builder */
    private String offeredCut(String locale) throws IOException {
        try (var driver = dialogue(locale, "")) {
            var api = new StartApi(driver.context());
            var dialogue = dialogue(api, driver, locale);
            return dialogue.offered(api.decomposition(PROJECT, EPIC));
        }
    }

    /** @return a prompt's offered line, read from the dialogue's own builder */
    private String offeredLine(String locale, List<String> keys) throws IOException {
        try (var driver = dialogue(locale, "")) {
            return dialogue(new StartApi(driver.context()), driver, locale).offeredLine(keys);
        }
    }

    /** @return everything one scripted dialogue run drew */
    private String render(String locale, String script) throws IOException {
        try (var driver = dialogue(locale, script)) {
            var dialogue = dialogue(new StartApi(driver.context()), driver, locale);
            try {
                dialogue.run();
            } catch (StartPrompt.StartLeftException _) {
                // The script ends after the not-offered answer; leaving is the expected end.
            }
            return driver.render();
        }
    }

    private TerminalTestDriver dialogue(String locale, String script) throws IOException {
        var driver = TerminalTestDriver.create();
        var environment = new HashMap<>(fixture.environment());
        environment.put("LANG", DE.equals(locale) ? "de_DE.UTF-8" : "en_US.UTF-8");
        driver.project(environment, tempDir);
        driver.type(script);
        driver.endOfInput();
        return driver;
    }

    private static EpicDecompositionDialogue dialogue(StartApi api, TerminalTestDriver driver, String locale) {
        var prompt = new StartPrompt(driver.context(), driver.writer(), locale, false);
        return new EpicDecompositionDialogue(api, prompt, PROJECT, EPIC, null);
    }

    /** The accepted set of one prompt: its keys, the leave key, Enter and every plan number. */
    private static Set<String> acceptedWith(String locale, List<String> keys, int planCount) {
        var accepted = new LinkedHashSet<>(keys);
        accepted.add(DE.equals(locale) ? "v" : "q");
        accepted.add("");
        for (var number = 1; number <= planCount; number++) {
            accepted.add(Integer.toString(number));
        }
        return accepted;
    }

    /**
     * The set the offered line must name: the accepted set minus <em>Enter</em> — which is never
     * listed — plus {@code ?}, which is listed and answered.
     */
    private static Set<String> offeredExpectation(String locale, List<String> keys, int planCount) {
        var offered = acceptedWith(locale, keys, planCount);
        offered.remove("");
        offered.add("?");
        return offered;
    }

    /** The keys a rendered offered line names: the {@code 1-N} range expanded, the {@code ?} kept. */
    private static Set<String> offeredKeys(String offered) {
        var keys = new LinkedHashSet<String>();
        for (var part : offered.split(", ")) {
            if (part.matches("\\d+-\\d+")) {
                var bounds = part.split("-");
                for (var number = Integer.parseInt(bounds[0]);
                     number <= Integer.parseInt(bounds[1]); number++) {
                    keys.add(Integer.toString(number));
                }
            } else {
                keys.add(part);
            }
        }
        return keys;
    }
}
