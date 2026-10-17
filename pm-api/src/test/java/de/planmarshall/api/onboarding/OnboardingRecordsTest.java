/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.planmarshall.api.system.CleanupDurations;

@DisplayName("Onboarding records")
class OnboardingRecordsTest {

    @Nested
    @DisplayName("OnboardingAspect")
    class AspectTests {

        @Test
        @DisplayName("carries the seven aspects in the fixed order of the first run")
        void shouldCarrySevenAspectsInOrder() {
            var aspects = OnboardingAspect.values();
            assertEquals(7, aspects.length);
            for (int i = 0; i < aspects.length; i++) {
                assertEquals(i + 1, aspects[i].step(), "aspect " + aspects[i] + " must keep its step");
                assertTrue(aspects[i].stepNameKey().startsWith("onboard.step."));
            }
            assertEquals(OnboardingAspect.HARNESSES, OnboardingAspect.parse("harness"));
            assertEquals(OnboardingAspect.PLANS, OnboardingAspect.parse("PLANS"));
            assertNull(OnboardingAspect.fromWire("nope"));
            assertThrows(IllegalArgumentException.class, () -> OnboardingAspect.parse("nope"));
        }
    }

    @Nested
    @DisplayName("ConsentKind")
    class ConsentKindTests {

        @Test
        @DisplayName("is a closed list whose titles all reside in the common area")
        void shouldBeClosedList() {
            assertEquals(14, ConsentKind.values().length);
            for (var kind : ConsentKind.values()) {
                assertTrue(kind.titleKey().startsWith("common.consent.kind.")
                        || kind.titleKey().startsWith("onboard.auto."));
            }
            assertEquals("common.consent.kind.scan_harnesses", ConsentKind.SCAN_HARNESSES.titleKey());
            assertEquals("onboard.auto.consent.title", ConsentKind.AUTO_RUN.titleKey());
        }

        @Test
        @DisplayName("names the kinds that may never be remembered")
        void shouldNameOnceOnlyKinds() {
            assertFalse(ConsentKind.AUTO_RUN.rememberable());
            assertFalse(ConsentKind.VERIFY_HARNESS_PROFILE.rememberable());
            assertTrue(ConsentKind.SCAN_HARNESSES.rememberable());
        }
    }

    @Nested
    @DisplayName("ConsentRecord")
    class ConsentRecordTests {

        @Test
        @DisplayName("round-trips through JSON with its channel and script count")
        void shouldRoundTrip() throws Exception {
            var record = new ConsentRecord(ConsentKind.RUN_CHECKOUT_SCRIPTS, ConsentStatus.REMEMBERED,
                    true, "2026-10-12T09:00:00Z", "Firefox", "pm-mcp-clients", 3);

            var parsed = ConsentRecord.fromJson(record.toJson());

            assertEquals(record, parsed);
            assertEquals("common.consent.kind.run_checkout_scripts", parsed.titleKey());
        }

        @Test
        @DisplayName("defaults a missing status to remembered")
        void shouldDefaultStatus() throws Exception {
            var parsed = ConsentRecord.fromJson("{\"kind\":\"scan_harnesses\"}");
            assertEquals(ConsentStatus.REMEMBERED, parsed.status());
            assertFalse(parsed.remember());
        }
    }

    @Nested
    @DisplayName("HarnessScanResult")
    class HarnessScanResultTests {

        @Test
        @DisplayName("round-trips a partial report with its missing figures and default mark")
        void shouldRoundTripPartial() throws Exception {
            var record = new HarnessScanResult("opencode", "OpenCode", "/opt/homebrew/bin/opencode",
                    "1.9.0", true, true, true, TokenUsageCoverage.PARTIAL,
                    List.of("cache write"), true);

            var parsed = HarnessScanResult.fromJson(record.toJson());

            assertEquals(record, parsed);
            assertTrue(parsed.isDefault());
            assertEquals(List.of("cache write"), parsed.missingFigures());
        }

        @Test
        @DisplayName("tolerates a harness that was not found")
        void shouldTolerateNotFound() throws Exception {
            var parsed = HarnessScanResult.fromJson(
                    "{\"harness_id\":\"agy\",\"name\":\"Antigravity\",\"found\":false}");
            assertFalse(parsed.found());
            assertEquals(TokenUsageCoverage.NOT_REPORTED, parsed.tokenUsage());
        }
    }

    @Nested
    @DisplayName("ModelCatalogueEntry")
    class ModelCatalogueEntryTests {

        @Test
        @DisplayName("round-trips and compares by harness and model")
        void shouldRoundTrip() throws Exception {
            var entry = new ModelCatalogueEntry("claude", "opus-5.5", "probe");

            var parsed = ModelCatalogueEntry.fromJson(entry.toJson());

            assertEquals(entry, parsed);
            assertTrue(parsed.sameModel(new ModelCatalogueEntry("claude", "opus-5.5", "operator")));
            var list = ModelCatalogueEntry.listFromJson("[" + entry.toJson() + "]");
            assertEquals(1, list.size());
        }
    }

    @Nested
    @DisplayName("RoleAssignment")
    class RoleAssignmentTests {

        @Test
        @DisplayName("reports an unassigned role")
        void shouldReportUnassigned() throws Exception {
            var assigned = new RoleAssignment("outline", "claude", "opus-5.5", "high", "steward");
            var parsed = RoleAssignment.fromJson(assigned.toJson());
            assertEquals(assigned, parsed);
            assertTrue(parsed.assigned());

            var open = new RoleAssignment("screener", null, null, null, "default");
            assertFalse(open.assigned());
            assertEquals(open, RoleAssignment.fromJson(open.toJson()));
        }
    }

    @Nested
    @DisplayName("PlanDefaults")
    class PlanDefaultsTests {

        @Test
        @DisplayName("ships a pull request, tests first, and the key points with no posture")
        void shouldShipDefaults() throws Exception {
            var shipped = PlanDefaults.shipped();
            assertEquals(PlanDefaults.LANDING_PULL_REQUEST, shipped.landing());
            assertEquals(PlanDefaults.METHOD_TEST_FIRST, shipped.method());
            assertEquals(PlanDefaults.ASK_KEY_POINTS, shipped.asking());
            assertNull(shipped.posture());
            assertEquals(shipped, PlanDefaults.fromJson(shipped.toJson()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"landing.pull-request", "landing.main", "landing.files"})
        @DisplayName("accepts every landing variant")
        void shouldAcceptLandingVariants(String landing) throws Exception {
            var defaults = new PlanDefaults(landing, PlanDefaults.METHOD_TESTS_AFTER,
                    PlanDefaults.ASK_EVERY_PHASE, "full");
            assertEquals(defaults, PlanDefaults.fromJson(defaults.toJson()));
        }
    }

    @Nested
    @DisplayName("MachineSettingsRecord")
    class MachineSettingsRecordTests {

        @Test
        @DisplayName("round-trips the whole machine settings record")
        void shouldRoundTrip() throws Exception {
            var record = new MachineSettingsRecord(
                    7,
                    List.of(new MachineSettingsAspectSummary(OnboardingAspect.LANGUAGE, "Deutsch, models English", "de")),
                    "de", "en", DocumentsMode.ASK,
                    CleanupDurations.defaults(), PlanDefaults.shipped(),
                    List.of(new ConsentRecord(ConsentKind.SCAN_HARNESSES, ConsentStatus.REMEMBERED, true,
                            "2026-10-12T09:00:00Z", "terminal", null, null)),
                    List.of(new MachineCredentialStatus("github", "GitHub", "fine-grained", 9, false, null)));

            var parsed = MachineSettingsRecord.fromJson(record.toJson());

            assertEquals(record, parsed);
            assertEquals(1, parsed.aspects().size());
            assertTrue(parsed.credentials().getFirst().expiresSoon());
        }
    }

    @Nested
    @DisplayName("MachineSettingsUpdate")
    class MachineSettingsUpdateTests {

        @Test
        @DisplayName("round-trips a request and a conflict result")
        void shouldRoundTripUpdate() throws Exception {
            var request = MachineSettingsUpdateRequest.withdrawConsent(ConsentKind.SCAN_HARNESSES, 4);
            assertEquals("consent:scan_harnesses", request.key());
            assertNull(request.value());
            assertEquals(request, MachineSettingsUpdateRequest.fromJson(request.toJson()));

            var applied = MachineSettingsUpdateResult.applied(5);
            assertTrue(applied.applied());
            assertEquals(applied, MachineSettingsUpdateResult.fromJson(applied.toJson()));

            var conflict = MachineSettingsUpdateResult.conflict(9, "copy");
            assertTrue(conflict.conflict());
            assertEquals("copy", conflict.currentValue());
            assertEquals(conflict, MachineSettingsUpdateResult.fromJson(conflict.toJson()));
        }
    }

    @Nested
    @DisplayName("MachineOnboardingState")
    class MachineOnboardingStateTests {

        @Test
        @DisplayName("round-trips the state with the completed aspects")
        void shouldRoundTrip() throws Exception {
            var state = new MachineOnboardingState("providers.select", OnboardingAspect.PROVIDERS,
                    List.of(OnboardingAspect.LANGUAGE, OnboardingAspect.HARNESSES), false, 11);

            var parsed = MachineOnboardingState.fromJson(state.toJson());

            assertEquals(state, parsed);
            assertEquals(2, parsed.completedSteps());
            assertFalse(parsed.completed());
        }

        @Test
        @DisplayName("derives the aspect from a state id and recognizes the resting state")
        void shouldDeriveAspect() {
            assertEquals(OnboardingAspect.CREDENTIALS, MachineOnboardingState.aspectOf("store.choose"));
            assertEquals(OnboardingAspect.PLANS, MachineOnboardingState.aspectOf("defaults.review"));
            assertNull(MachineOnboardingState.aspectOf("onboard.completed"));
            assertTrue(new MachineOnboardingState("onboard.completed", null, List.of(), false, 1).completed());
        }
    }

    @Nested
    @DisplayName("Events")
    class EventTests {

        @Test
        @DisplayName("round-trip the step-completed and settings-changed events with their names")
        void shouldRoundTripEvents() throws Exception {
            var step = new OnboardingStepCompletedEvent(OnboardingAspect.MODELS, "roles.review", 3);
            assertEquals("onboarding.step_completed", OnboardingStepCompletedEvent.eventName());
            assertEquals(step, OnboardingStepCompletedEvent.fromJson(step.toJson()));

            var settings = new MachineSettingsChangedEvent("documents_mode", "copy", 8, "terminal");
            assertEquals("machine.settings_changed", MachineSettingsChangedEvent.eventName());
            assertEquals(settings, MachineSettingsChangedEvent.fromJson(settings.toJson()));
        }
    }

    @ParameterizedTest
    @EnumSource(OnboardingAspect.class)
    @DisplayName("every aspect has a distinct wire value")
    void aspectsHaveDistinctWireValues(OnboardingAspect aspect) {
        assertEquals(aspect, OnboardingAspect.fromWire(aspect.wireValue()));
        assertEquals(7, Set.of(OnboardingAspect.values()).size());
    }
}
