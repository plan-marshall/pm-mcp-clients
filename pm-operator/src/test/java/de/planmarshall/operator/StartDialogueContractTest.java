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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.EntryResource;
import de.planmarshall.api.start.RecipeRef;
import de.planmarshall.api.start.ScreeningVerdict;
import de.planmarshall.api.start.SourceFailure;
import de.planmarshall.api.start.SourceFailureCause;
import de.planmarshall.api.start.SourceKind;
import de.planmarshall.api.start.SourceKindOffer;
import de.planmarshall.api.start.SourceResource;
import de.planmarshall.api.start.SourceState;
import de.planmarshall.api.start.SourceTrust;
import de.planmarshall.api.start.StartReadyResource;
import de.planmarshall.api.start.StartState;
import de.planmarshall.api.start.StartStateResource;
import de.planmarshall.api.testsupport.FakeRuntimeServer;
import de.planmarshall.api.testsupport.RuntimeFixture;
import de.planmarshall.operator.transcript.TerminalTestDriver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The starting dialogue against answers of the start contract the mock runtime does not stage: a
 * source whose cause allows no retry, an unscreened source with and without a cause, a recipe step
 * without recipes, and an answer to {@code create} that names no work. The runtime is a fake that
 * serves {@link StartStateResource} built from the contract's records.
 */
@EnableGeneratorController
@DisplayName("starting dialogue: answers of the start contract the mock does not stage")
class StartDialogueContractTest {

    private static final String ENTRY = "ent-1";
    private static final String ENTRY_TARGET = "/api/v1/entries/" + ENTRY;
    private static final String ISSUE = "https://github.com/plan-marshall/pm-mcp-clients/issues/212";

    static Stream<Arguments> pendingSources() {
        return Stream.of(
                            Arguments.of("a failed source whose cause allows no retry offers only its removal",
                                                new SourceResource("src-1", SourceKind.LOCAL_CHANGES, "2 files changed",
                                                                    null, SourceState.FAILED, SourceTrust.TRUSTED, null,
                                                                    SourceFailure.of(SourceFailureCause.NO_LOCAL_CHANGES),
                                                                    List.of(), null, null, List.of(), null),
                                                "u", "r, q"),
                            Arguments.of("an unscreened source without a cause is not tried again",
                                                unscreened(null), "t", "a, r, q"),
                            Arguments.of("an unscreened source whose cause allows it is tried again",
                                                unscreened(SourceFailure.of(SourceFailureCause.NO_SCREENER)), "u",
                                                "t, a, r, q"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pendingSources")
    @DisplayName("a source waiting for the operator offers only the acts the runtime takes")
    void pendingSourceOffersWhatTheRuntimeTakes(String name, SourceResource source, String typed, String offered)
                        throws Exception {
        var sources = state(entry(StartState.SOURCES, null, List.of(source)), null, List.of());
        var outcome = run((request, response) -> response.json(200, sources.toJson()),
                            typed + "\nq\n", "plan", "add", "--project", project(), "--entry", ENTRY);
        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains(typed + " is not offered here: " + offered),
                                                outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("Left the start"), outcome.rendered()));
    }

    @Test
    @DisplayName("a recipe step without recipes says the project offers none and never offers the machine's")
    void recipeStepWithoutRecipesOffersNone() throws Exception {
        var project = project();
        var machineRecipe = Generators.letterStrings(4, 12).next();
        var machineAsked = new AtomicBoolean();
        var choose = state(entry(StartState.RECIPE_CHOOSE, null, List.of()), null, List.of());
        var outcome = run((request, response) -> {
            if ("/api/v1/recipes".equals(request.target())) {
                machineAsked.set(true);
                response.json(200, "{\"recipes\":[{\"key\":\"" + machineRecipe + "\"}]}");
            } else {
                response.json(200, choose.toJson());
            }
        }, "1\nq\n", "recipe", "run", "--project", project, "--entry", ENTRY);
        assertAll(
                            () -> assertEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("No recipe is offered in " + project),
                                                outcome.rendered()),
                            () -> assertFalse(machineAsked.get(), "the machine's recipes must not be asked"),
                            () -> assertFalse(outcome.rendered().contains(machineRecipe), outcome.rendered()));
    }

    @ParameterizedTest(name = "an answer to create with the id \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("an answer to create that names no work, or a blank one, is refused, never announced under a guessed name")
    void createAnswerWithoutAnIdIsRefused(@Nullable String answeredId) throws Exception {
        var requested = Generators.letterStrings(4, 12).next();
        var title = Generators.letterStrings(4, 12).next();
        var ready = state(entry(StartState.READY, requested, List.of(described())),
                            StartReadyResource.ofPlan(title, requested, null), List.of());
        var nameless = state(entry(StartState.READY, answeredId, List.of(described())), null, List.of());
        var outcome = run((request, response) -> response.json(200,
                                                (ENTRY_TARGET + "/links/create").equals(request.target())
                                                                    ? nameless.toJson() : ready.toJson()),
                            null, "plan", "add", "--project", project(), "--entry", ENTRY, "--id", requested,
                            "--no-input");
        assertAll(
                            () -> assertNotEquals(0, outcome.code(), outcome.rendered()),
                            () -> assertTrue(outcome.rendered().contains("names no plan id"), outcome.rendered()),
                            () -> assertFalse(outcome.rendered().contains("has started"), outcome.rendered()));
    }

    private static String project() {
        return Generators.letterStrings(4, 12).next();
    }

    private static SourceResource unscreened(@Nullable SourceFailure cause) {
        return new SourceResource("src-1", SourceKind.ISSUE, ISSUE, null, SourceState.UNSCREENED,
                            SourceTrust.UNTRUSTED, ScreeningVerdict.UNSCREENED, cause, List.of(), null, null, List.of(),
                            null);
    }

    private static SourceResource described() {
        return SourceResource.local("src-1", SourceKind.DESCRIPTION, Generators.letterStrings(4, 12).next());
    }

    private static EntryResource entry(StartState state, @Nullable String planId, List<SourceResource> sources) {
        return new EntryResource(ENTRY, EntryKind.PLAN, "project", state, null, planId, sources,
                            List.of(SourceKindOffer.available(SourceKind.DESCRIPTION)), false, false,
                            "start.chat.waits", null, List.of(), null);
    }

    private static StartStateResource state(EntryResource entry, @Nullable StartReadyResource ready,
                        List<RecipeRef> recipes) {
        return StartStateResource.of(entry, ready, null, null, null, recipes);
    }

    private static StartTestSupport.Outcome run(FakeRuntimeServer.Handler handler, @Nullable String input,
                        String... args)
                        throws Exception {
        try (var fixture = RuntimeFixture.create(); var driver = TerminalTestDriver.create()) {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start(handler);
            var environment = new HashMap<>(fixture.environment());
            environment.put("LANG", "en_US.UTF-8");
            environment.put("USER", "operator");
            driver.project(environment, fixture.base().getParent());
            if (input != null) {
                driver.type(input);
            }
            driver.endOfInput();
            var code = TranscriptVerbRunner.run(driver.context(), driver.writer(), driver.writer(), args);
            return new StartTestSupport.Outcome(code, driver.render());
        }
    }
}
