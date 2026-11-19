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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.transcript.TerminalTestDriver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * One accepted set per prompt: the line a prompt prints as offered is built from the same
 * collection of (key, act) records its branches read, so a key added to one and not the other turns
 * this class red.
 * <p>
 * The offered line is read from the dialogue's own builder, and each key of each set is driven
 * through the rendered dialogue: the transcript must not answer "not offered", and the act the key
 * is paired with must reach the runtime — the instruct request with its action, the reload, or no
 * request at all. A key paired with the wrong act therefore fails, which a check for acceptance
 * alone could not see. The refused key shows the line the operator reads back.
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
    /** Every request the dialogue sent, as {@code METHOD target}. */
    private final List<String> requests = new CopyOnWriteArrayList<>();
    /** The body of every instruct request, in the order it was sent. */
    private final List<String> instructs = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        fixture = RuntimeFixture.create();
        PmOperatorTest.writeLiveRecord(fixture);
        fixture.start((request, response) -> {
            requests.add(request.method() + " " + request.target());
            if ((BASE + "/instruct").equals(request.target()) && "POST".equals(request.method())) {
                instructs.add(request.bodyText());
                response.json(200, THREE_PLANS);
            } else if (BASE.equals(request.target()) && "GET".equals(request.method())) {
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
                            () -> assertEquals(offeredExpectation(EN, keysOf(EpicDecompositionDialogue.CUT_KEYS), 3),
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
                            () -> assertEquals(offeredExpectation(EN, keysOf(EpicDecompositionDialogue.PLAN_KEYS), 0),
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
                            () -> assertEquals(offeredExpectation(EN, keysOf(EpicDecompositionDialogue.READ_BACK_KEYS), 0),
                                                offeredKeys(offered)),
                            () -> assertTrue(render(EN, "1\ne\nsmaller\nz\n")
                                                .contains("z is not offered here: i, x, q, ?"))
        );
    }

    /** @return each cut key with the instruct action it must send */
    static Stream<Arguments> cutKeys() {
        var cases = new ArrayList<Arguments>();
        for (var key : EpicDecompositionDialogue.CUT_KEYS) {
            cases.add(Arguments.of(key.key(), key.act().name().toLowerCase(Locale.ROOT),
                                key.key() + "\nsplit the work\n"));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "the cut prompt runs {1} for {0}")
    @MethodSource("cutKeys")
    @DisplayName("every key the cut prompt offers reaches the runtime with its own act")
    void theCutPromptRunsTheActOfEveryKeyItOffers(String key, String action, String script) throws Exception {
        var rendered = render(EN, script);

        assertAll(
                            () -> assertFalse(rendered.contains("is not offered here"), key),
                            () -> assertEquals(1, instructs.size(), instructs.toString()),
                            () -> assertTrue(instructs.getFirst().contains("\"" + action + "\""),
                                                instructs.getFirst())
        );
    }

    /** @return each plan key with the number of edit requests its act must send, and its script */
    static Stream<Arguments> planKeys() {
        var cases = new ArrayList<Arguments>();
        for (var key : EpicDecompositionDialogue.PLAN_KEYS) {
            var edits = key.act() == EpicDecompositionDialogue.PlanAct.EDIT;
            cases.add(Arguments.of(key.key(), edits ? 1 : 0,
                                "1\n" + key.key() + "\n" + (edits ? "smaller\n" : "")));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "the plan prompt runs {0} and sends {1} edit")
    @MethodSource("planKeys")
    @DisplayName("every key the plan prompt offers reaches the runtime with its own act")
    void thePlanPromptRunsTheActOfEveryKeyItOffers(String key, int edits, String script) throws Exception {
        var rendered = render(EN, script);

        assertAll(
                            () -> assertFalse(rendered.contains("is not offered here"), key),
                            () -> assertEquals(edits, instructs.size(), instructs.toString()),
                            () -> assertEquals(edits == 1, !instructs.isEmpty() && instructs.stream()
                                                .allMatch(body -> body.contains("\"edit\"")
                                                                    && body.contains("a-plan")),
                                                instructs.toString())
        );
    }

    /** @return each read-back key with its edit count, its read count, and its script */
    static Stream<Arguments> readBackKeys() {
        var cases = new ArrayList<Arguments>();
        for (var key : EpicDecompositionDialogue.READ_BACK_KEYS) {
            var instructAgain =
                                key.act() == EpicDecompositionDialogue.ReadBackAct.INSTRUCT_AGAIN;
            cases.add(Arguments.of(key.key(), instructAgain ? 2 : 1, instructAgain ? 1 : 2,
                                "1\ne\nsmaller\n" + key.key() + "\n" + (instructAgain ? "second\n" : "")));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "the read-back prompt runs {0}")
    @MethodSource("readBackKeys")
    @DisplayName("every key the read-back prompt offers reaches the runtime with its own decision")
    void theReadBackPromptTakesTheDecisionOfEveryKeyItOffers(String key, int edits, int reads,
                        String script) throws Exception {
        var rendered = render(EN, script);

        assertAll(
                            () -> assertFalse(rendered.contains("is not offered here"), key),
                            () -> assertEquals(edits, instructs.size(), instructs.toString()),
                            () -> assertEquals(reads, readsOf(BASE), requests.toString())
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
                            () -> assertEquals(offeredExpectation(DE, keysOf(EpicDecompositionDialogue.CUT_KEYS), 3),
                                                offeredKeys(cut)),
                            () -> assertEquals(offeredExpectation(DE, keysOf(EpicDecompositionDialogue.PLAN_KEYS), 0),
                                                offeredKeys(plan)),
                            () -> assertEquals(offeredExpectation(DE, keysOf(EpicDecompositionDialogue.READ_BACK_KEYS), 0),
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
    private String offeredLine(String locale, List<? extends EpicDecompositionDialogue.KeyAct> keys)
                        throws IOException {
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
                // The script ends after the answer under test; leaving is the expected end.
            }
            return driver.render();
        }
    }

    /** @return how often the dialogue read the cut resource */
    private long readsOf(String target) {
        return requests.stream().filter(("GET " + target)::equals).count();
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

    private static List<String> keysOf(List<? extends EpicDecompositionDialogue.KeyAct> keys) {
        var names = new ArrayList<String>();
        for (var key : keys) {
            names.add(key.key());
        }
        return names;
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
