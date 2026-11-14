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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.intent.EpicIntentResource.PlanDoneWhen;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * The rules an epic's intent holds by construction: three to six done-when checks, at least one non-goal,
 * no blank entry, and one criterion per plan.
 */
@DisplayName("EpicIntentResource rules")
@EnableGeneratorController
class EpicIntentResourceTest {

    private static DoneWhenCheck pending() {
        return new DoneWhenCheck(Generators.nonBlankStrings().next(), DoneWhenCheck.Verdict.PENDING, List.of());
    }

    private static DoneWhenCheck met() {
        return new DoneWhenCheck(Generators.nonBlankStrings().next(), DoneWhenCheck.Verdict.MET,
                List.of(Generators.nonBlankStrings().next()));
    }

    private static List<DoneWhenCheck> checks(int count) {
        return IntStream.range(0, count).mapToObj(index -> pending()).toList();
    }

    private static PlanDoneWhen plan(String planId) {
        return new PlanDoneWhen(planId, Generators.nonBlankStrings().next(),
                List.of(Generators.nonBlankStrings().next()));
    }

    private static EpicIntentResource epic(List<DoneWhenCheck> doneWhen, List<String> nonGoals,
                                           List<String> notYetSpecified, List<PlanDoneWhen> plans) {
        return new EpicIntentResource(Generators.nonBlankStrings().next(), Generators.nonBlankStrings().next(),
                doneWhen, nonGoals, notYetSpecified, plans);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 7})
    @DisplayName("should refuse a count of done-when checks outside three to six")
    void shouldRefuseCheckCountOutsideRange(int count) {
        var checks = checks(count);
        var nonGoals = List.of(Generators.nonBlankStrings().next());

        assertThrows(IllegalArgumentException.class, () -> epic(checks, nonGoals, null, null));
    }

    @Test
    @DisplayName("should refuse absent checks, absent or blank non-goals, a blank entry, and blank ids")
    void shouldRefuseMissingOrBlankMembers() {
        var checks = checks(3);
        var nonGoals = List.of(Generators.nonBlankStrings().next());
        var text = Generators.nonBlankStrings().next();
        var blankEntries = List.of(" ");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> epic(null, nonGoals, null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> epic(checks, null, null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> epic(checks, blankEntries, null, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> epic(checks, nonGoals, blankEntries, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new EpicIntentResource(" ", text, checks, nonGoals, null, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new EpicIntentResource(text, " ", checks, nonGoals, null, null)));
    }

    @Test
    @DisplayName("should refuse two criteria of the same plan and keep absent lists empty")
    void shouldRefuseDuplicatePlanAndKeepAbsentListsEmpty() {
        var planId = Generators.nonBlankStrings().next();
        var checks = checks(3);
        var nonGoals = List.of(Generators.nonBlankStrings().next());
        var plans = List.of(plan(planId), plan(planId));

        var epic = epic(checks, nonGoals, null, null);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> epic(checks, nonGoals, null, plans)),
                () -> assertTrue(epic.notYetSpecified().isEmpty()),
                () -> assertTrue(epic.plans().isEmpty()));
    }

    @Test
    @DisplayName("should refuse a plan criterion with a blank id or title, or without a criterion")
    void shouldRefuseInvalidPlanCriterion() {
        var planId = Generators.nonBlankStrings().next();
        var title = Generators.nonBlankStrings().next();
        var criterion = List.of(Generators.nonBlankStrings().next());
        var blankCriterion = List.of(" ");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new PlanDoneWhen(" ", title, criterion)),
                () -> assertThrows(IllegalArgumentException.class, () -> new PlanDoneWhen(planId, " ", criterion)),
                () -> assertThrows(IllegalArgumentException.class, () -> new PlanDoneWhen(planId, title, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PlanDoneWhen(planId, title, Collections.emptyList())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PlanDoneWhen(planId, title, blankCriterion)));
    }

    @Test
    @DisplayName("should be all met only when every check is met")
    void shouldBeAllMetOnlyWhenEveryCheckIsMet() {
        var nonGoals = List.of(Generators.nonBlankStrings().next());

        var allMet = epic(List.of(met(), met(), met()), nonGoals, null, null);
        var oneOpen = epic(List.of(met(), met(), pending()), nonGoals, null, null);

        assertAll(
                () -> assertTrue(allMet.allMet()),
                () -> assertFalse(oneOpen.allMet()));
    }

    @Test
    @DisplayName("should refuse JSON that is no object")
    void shouldRefuseJsonThatIsNoObject() {
        var json = JsonTree.write(List.of(Generators.nonBlankStrings().next()));

        assertThrows(JsonFormatException.class, () -> EpicIntentResource.fromJson(json));
    }
}
