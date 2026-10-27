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

import de.planmarshall.api.decision.Reversibility;
import de.planmarshall.api.designer.UnitPlacingResult;
import de.planmarshall.api.onboarding.PlanDefaults;
import de.planmarshall.api.overview.PlanActivity;
import de.planmarshall.api.process.ActivityEntryKind;
import de.planmarshall.api.process.OffTrackSignal;
import de.planmarshall.api.process.TaskStatus;
import de.planmarshall.api.system.WorkerState;

/**
 * Guards the shared display vocabulary: every state value a client can receive has a display word
 * in both locales, and every workflow unit id has a plain name.
 * <p>
 * The {@code state.<kind>.<value>} family is what both clients render a server value through
 * (design: Texts and Languages). A value without a key would reach the operator as a raw enum, a
 * state id or a placeholder, so the build fails here instead.
 */
class StateWordsCompletenessTest {

    private static final String STATE_PREFIX = "state.";

    private static final String UNIT_PREFIX = "unit.";

    private static final List<String> LOCALES = List.of("en", "de");

    /**
     * Workflow unit ids that the mock scenarios serve but no {@code pm-api} constant names: the
     * project workflows of {@code designer.json} (lines 53, 82, 237, 279) and the custom landing of
     * its placing result (line 483).
     */
    private static final List<String> SCENARIO_UNIT_IDS = List.of(
            "git.commit_and_push",
            "epic",
            "phase-6-finalize",
            "remediation.ci-failure",
            "landing.custom");

    @Test
    @DisplayName("should have a display word for every contract enum value in both locales")
    void shouldHaveDisplayWordForEveryContractEnumValue() {
        var families = new LinkedHashMap<String, List<String>>();
        families.put("plan", wire(PlanActivity.values(), PlanActivity::value));
        families.put("task", wire(TaskStatus.values(), TaskStatus::value));
        families.put("worker", wire(WorkerState.values(), WorkerState::value));
        families.put("activity", wire(ActivityEntryKind.values(), ActivityEntryKind::value));
        families.put("decision", wire(Reversibility.values(), Reversibility::value));
        families.put("signal", wire(OffTrackSignal.values(), OffTrackSignal::value));

        var missing = missingKeys(families, STATE_PREFIX);

        assertTrue(missing.isEmpty(),
                "Every contract enum value needs a state word in en and de: " + missing);
    }

    @Test
    @DisplayName("should have a display word for every scenario state value in both locales")
    void shouldHaveDisplayWordForEveryScenarioStateValue() {
        var families = new LinkedHashMap<String, List<String>>();
        // `phase_state` values the primary scenarios carry that PlanActivity does not name
        // (running-work.json lines 823 and 2753).
        families.put("plan", List.of("staged", "ended"));
        // The closed set of the epic's state (EpicResource, `state`), normalised to the wire
        // spelling the scenarios use (running-work.json line 1727, "epics_by_state").
        families.put("epic", List.of("running", "waiting_for_you", "held", "landing", "ended"));
        // The status column the system view derives from a model's counts: `ok` with none,
        // otherwise the retry and error badges, and "not measured" when no call was measured
        // (SystemView.svelte lines 1082-1098).
        families.put("model", List.of("ok", "retry", "error", "not_measured"));
        // The landing's own state (AlongsideLanding, `state`) and the terminal status of a placing
        // (UnitPlacingResult) — running-work.json line 238, designer.json line 485.
        families.put("landing", List.of("running", "failed",
                UnitPlacingResult.STATUS_WRITTEN, UnitPlacingResult.STATUS_COMMITTED,
                UnitPlacingResult.STATUS_LANDED));
        // The item kinds of a cleanup run (CleanupItemEntry, `kind`, and CleanupKeptItem, `kind`),
        // as reports-and-system.json serves them (lines 738, 3249, 3257, 3265).
        families.put("cleanup", List.of("plan", "epic", "read_reports", "unenrolled_project"));
        // The gate kinds of a waiver request, from the `waiver_gate` values of running-work.json
        // (lines 2309 and 2333), which carry the kind before the scope separator.
        families.put("gate", List.of("ci_check", "coverage"));

        var missing = missingKeys(families, STATE_PREFIX);

        assertTrue(missing.isEmpty(),
                "Every state value of the scenarios needs a display word in en and de: " + missing);
    }

    @Test
    @DisplayName("should have a plain name for every workflow unit id in both locales")
    void shouldHavePlainNameForEveryWorkflowUnitId() {
        var ids = new ArrayList<String>(List.of(
                PlanDefaults.LANDING_PULL_REQUEST,
                PlanDefaults.LANDING_MAIN,
                PlanDefaults.LANDING_FILES,
                PlanDefaults.METHOD_TEST_FIRST,
                PlanDefaults.METHOD_TESTS_AFTER,
                PlanDefaults.ASK_EVERY_PHASE,
                PlanDefaults.ASK_KEY_POINTS,
                PlanDefaults.ASK_QUESTIONS_ONLY));
        ids.addAll(SCENARIO_UNIT_IDS);

        var families = new LinkedHashMap<String, List<String>>();
        for (var id : ids) {
            families.put(id, List.of(""));
        }

        var missing = missingKeys(families, UNIT_PREFIX, true);

        assertTrue(missing.isEmpty(),
                "Every workflow unit id needs a plain name in en and de: " + missing);
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
     * @param families kind (or unit id) to its values
     * @param prefix   the key prefix, {@code state.} or {@code unit.}
     * @return the missing keys, as {@code locale:key}, empty when every key exists and is not blank
     */
    private static List<String> missingKeys(Map<String, List<String>> families, String prefix) {
        return missingKeys(families, prefix, false);
    }

    /**
     * @param families   kind (or unit id) to its values
     * @param prefix     the key prefix, {@code state.} or {@code unit.}
     * @param idOnly     whether a family key is the whole key rather than a {@code <kind>.<value>}
     * @return the missing keys, as {@code locale:key}, empty when every key exists and is not blank
     */
    private static List<String> missingKeys(Map<String, List<String>> families, String prefix, boolean idOnly) {
        var missing = new ArrayList<String>();
        for (var locale : LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, "all");
            for (var family : families.entrySet()) {
                for (var value : family.getValue()) {
                    var key = idOnly
                            ? prefix + family.getKey()
                            : prefix + family.getKey() + "." + value;
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
