/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.message;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.control.EpicState;
import de.planmarshall.api.decision.GateKind;
import de.planmarshall.api.decision.Reversibility;
import de.planmarshall.api.onboarding.PlanDefaults;
import de.planmarshall.api.overview.LandingState;
import de.planmarshall.api.overview.PlanActivity;
import de.planmarshall.api.process.ActivityEntryKind;
import de.planmarshall.api.process.OffTrackSignal;
import de.planmarshall.api.process.TaskStatus;
import de.planmarshall.api.system.CleanupKind;
import de.planmarshall.api.system.ModelStatus;
import de.planmarshall.api.system.WorkerState;

/**
 * Guards the shared display vocabulary: every state value the clients can receive has a display
 * word in both locales, and every workflow unit id has a plain name.
 * <p>
 * The {@code state.<kind>.<value>} family is what both clients render a server value through
 * (design: Texts and Languages). A value without a key would reach the operator as a raw enum, a
 * state id or a placeholder, so the build fails here instead.
 * <p>
 * Each family is read from the {@code pm-api} enum that closes it, so an enum that gains a constant
 * fails this test until its word exists in every supported locale. The six primary mock scenarios
 * serve values from the same sets; the enums are their closed form, held here rather than read from
 * the mock, and the unit ids they serve are guarded where they live, in {@code pm-mock-runtime}.
 */
class StateWordsCompletenessTest {

    private static final String STATE_PREFIX = "state.";

    private static final String UNIT_PREFIX = "unit.";

    private static final List<String> LOCALES = MessageCatalogue.SUPPORTED_LOCALES.stream().sorted().toList();

    /**
     * Workflow unit ids the contract names in {@code PlanDefaults}. The ids the mock scenarios serve
     * are guarded where they live, in {@code pm-mock-runtime}.
     */
    private static final List<String> CONTRACT_UNIT_IDS = List.of(
            PlanDefaults.LANDING_PULL_REQUEST,
            PlanDefaults.LANDING_MAIN,
            PlanDefaults.LANDING_FILES,
            PlanDefaults.METHOD_TEST_FIRST,
            PlanDefaults.METHOD_TESTS_AFTER,
            PlanDefaults.ASK_EVERY_PHASE,
            PlanDefaults.ASK_KEY_POINTS,
            PlanDefaults.ASK_QUESTIONS_ONLY);

    @Test
    @DisplayName("should have a display word for every state enum value in both locales")
    void shouldHaveDisplayWordForEveryStateEnumValue() {
        var families = new LinkedHashMap<String, List<String>>();
        families.put("plan", wire(PlanActivity.values(), PlanActivity::value));
        families.put("epic", wire(EpicState.values(), EpicState::value));
        families.put("task", wire(TaskStatus.values(), TaskStatus::value));
        families.put("worker", wire(WorkerState.values(), WorkerState::value));
        families.put("model", wire(ModelStatus.values(), ModelStatus::value));
        families.put("landing", wire(LandingState.values(), LandingState::value));
        families.put("cleanup", wire(CleanupKind.values(), CleanupKind::value));
        families.put("activity", wire(ActivityEntryKind.values(), ActivityEntryKind::value));
        families.put("decision", wire(Reversibility.values(), Reversibility::value));
        families.put("gate", wire(GateKind.values(), GateKind::value));
        families.put("signal", wire(OffTrackSignal.values(), OffTrackSignal::value));

        var missing = missingStateWords(families);

        assertTrue(missing.isEmpty(),
                "Every state enum value needs a display word in en and de: " + missing);
    }

    @Test
    @DisplayName("should have a plain name for every workflow unit id the contract names")
    void shouldHavePlainNameForEveryWorkflowUnitId() {
        var missing = new ArrayList<String>();
        for (var locale : LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, "all");
            for (var id : CONTRACT_UNIT_IDS) {
                var key = UNIT_PREFIX + id;
                var text = messages.get(key);
                if (text == null || text.isBlank()) {
                    missing.add(locale + ":" + key);
                }
            }
        }

        assertTrue(missing.isEmpty(),
                "Every workflow unit id of the contract needs a plain name in every locale: " + missing);
    }

    /**
     * @param constants enum constants
     * @param value     the constant's wire value
     * @param <E>       the enum type
     * @return the wire values, in declaration order
     */
    private static <E extends Enum<E>> List<String> wire(E[] constants, Function<E, String> value) {
        return Arrays.stream(constants).map(value).toList();
    }

    /**
     * @param families kind to the values it covers
     * @return the missing keys, as {@code locale:key}, empty when every key exists and is not blank
     */
    private static List<String> missingStateWords(Map<String, List<String>> families) {
        var missing = new ArrayList<String>();
        for (var locale : LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, "all");
            for (var family : families.entrySet()) {
                for (var value : family.getValue()) {
                    var key = STATE_PREFIX + family.getKey() + "." + value;
                    var text = messages.get(key);
                    if (text == null || text.isBlank()) {
                        missing.add(locale + ":" + key);
                    }
                }
            }
        }
        return missing;
    }
}
