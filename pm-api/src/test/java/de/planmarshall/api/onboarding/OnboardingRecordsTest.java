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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            var consent = new ConsentRecord(ConsentKind.RUN_CHECKOUT_SCRIPTS, ConsentStatus.REMEMBERED,
                    true, "2026-10-12T09:00:00Z", "Firefox", "pm-mcp-clients", 3);

            var parsed = ConsentRecord.fromJson(consent.toJson());

            assertEquals(consent, parsed);
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
            var harness = new HarnessScanResult("opencode", "OpenCode", "/opt/homebrew/bin/opencode",
                    "1.9.0", true, true, true, TokenUsageCoverage.PARTIAL,
                    List.of("cache write"), true, null);

            var parsed = HarnessScanResult.fromJson(harness.toJson());

            assertEquals(harness, parsed);
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
            var settings = new MachineSettingsRecord(
                    7,
                    List.of(new MachineSettingsAspectSummary(OnboardingAspect.LANGUAGE, "Deutsch, models English", "de")),
                    "de", "en", DocumentsMode.ASK,
                    CleanupDurations.defaults(), PlanDefaults.shipped(),
                    List.of(new ConsentRecord(ConsentKind.SCAN_HARNESSES, ConsentStatus.REMEMBERED, true,
                            "2026-10-12T09:00:00Z", "terminal", null, null)),
                    List.of(new MachineCredentialStatus("github", "GitHub", "fine-grained", 9, false, null)));

            var parsed = MachineSettingsRecord.fromJson(settings.toJson());

            assertEquals(settings, parsed);
            assertEquals(1, parsed.aspects().size());
            assertTrue(parsed.credentials().getFirst().expiresSoon());
        }

        @Test
        @DisplayName("defaults an empty settings record and an empty set of plan defaults")
        void shouldDefaultAnEmptyRecord() throws Exception {
            var settings = MachineSettingsRecord.fromJson("{}");
            assertEquals(0, settings.revision());
            assertEquals(DocumentsMode.ASK, settings.documentsMode());
            assertTrue(settings.aspects().isEmpty());
            assertTrue(settings.consents().isEmpty());
            assertTrue(settings.credentials().isEmpty());

            var defaults = PlanDefaults.fromJson("{}");
            assertEquals(PlanDefaults.LANDING_PULL_REQUEST, defaults.landing());
            assertEquals(PlanDefaults.METHOD_TEST_FIRST, defaults.method());
            assertEquals(PlanDefaults.ASK_KEY_POINTS, defaults.asking());
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
            assertEquals("onboarding.step_completed", OnboardingStepCompletedEvent.EVENT_NAME);
            assertEquals(step, OnboardingStepCompletedEvent.fromJson(step.toJson()));

            var settings = new MachineSettingsChangedEvent("documents_mode", "copy", 8, "terminal");
            assertEquals("machine.settings_changed", MachineSettingsChangedEvent.EVENT_NAME);
            assertEquals(settings, MachineSettingsChangedEvent.fromJson(settings.toJson()));
        }
    }

    @Nested
    @DisplayName("Automatic onboarding result")
    class AutoResultTests {

        @Test
        @DisplayName("round-trips an item and its closed list of reasons")
        void shouldRoundTripItem() throws Exception {
            var item = new AutoResultItem(OnboardingAspect.HARNESSES,
                    "Claude Code 2.4.1 (default), OpenCode 1.9.0", AutoResultReason.ALL_SUPPORTED);

            var parsed = AutoResultItem.fromJson(item.toJson());

            assertEquals(item, parsed);
            assertEquals("onboard.auto.reason.all_supported", parsed.reason().titleKey());
            assertEquals(List.of(item), AutoResultItem.listFromJson("[" + item.toJson() + "]"));
        }

        @Test
        @DisplayName("is a closed list of four reasons and refuses an unknown one")
        void shouldBeClosedList() {
            assertEquals(4, AutoResultReason.values().length);
            assertEquals(AutoResultReason.DETECTED, AutoResultReason.fromWire("detected"));
            assertEquals(AutoResultReason.ASKED, AutoResultReason.fromWire("ASKED"));
            assertNull(AutoResultReason.fromWire("nope"));
            assertNull(AutoResultReason.fromWire(""));
            assertThrows(IllegalArgumentException.class, () -> AutoResultReason.parse("nope"));
        }
    }

    @Nested
    @DisplayName("HarnessUsageReport")
    class HarnessUsageReportTests {

        @Test
        @DisplayName("round-trips a complete report with a not-applicable figure")
        void shouldRoundTripComplete() throws Exception {
            var report = new HarnessUsageReport(TokenUsageCoverage.COMPLETE, Map.of(
                    UsageFigure.INPUT, UsageFigureStatus.REPORTED,
                    UsageFigure.CACHE_READ, UsageFigureStatus.REPORTED,
                    UsageFigure.CACHE_WRITE, UsageFigureStatus.NOT_APPLICABLE,
                    UsageFigure.OUTPUT, UsageFigureStatus.REPORTED));

            var parsed = HarnessUsageReport.fromJson(report.toJson());

            assertEquals(report, parsed);
            assertTrue(parsed.missingFigures().isEmpty());
            assertEquals(UsageFigureStatus.NOT_APPLICABLE, parsed.statusOf(UsageFigure.CACHE_WRITE));
            assertNull(parsed.statusOf(UsageFigure.REASONING));
        }

        @Test
        @DisplayName("round-trips a partial report that names what is missing, in catalogue order")
        void shouldRoundTripPartial() throws Exception {
            var report = new HarnessUsageReport(TokenUsageCoverage.PARTIAL, Map.of(
                    UsageFigure.INPUT, UsageFigureStatus.REPORTED,
                    UsageFigure.CACHE_READ, UsageFigureStatus.NOT_REPORTED,
                    UsageFigure.CACHE_WRITE, UsageFigureStatus.NOT_REPORTED,
                    UsageFigure.OUTPUT, UsageFigureStatus.REPORTED));

            var parsed = HarnessUsageReport.fromJson(report.toJson());

            assertEquals(report, parsed);
            assertEquals(List.of("cache_read", "cache_write"), parsed.missingFigures());
        }

        @Test
        @DisplayName("round-trips a not-reported report")
        void shouldRoundTripNotReported() throws Exception {
            var report = new HarnessUsageReport(TokenUsageCoverage.NOT_REPORTED, Map.of(
                    UsageFigure.INPUT, UsageFigureStatus.NOT_REPORTED,
                    UsageFigure.OUTPUT, UsageFigureStatus.NOT_REPORTED));

            assertEquals(report, HarnessUsageReport.fromJson(report.toJson()));
            assertFalse(report.missingFigures().isEmpty());
        }

        @Test
        @DisplayName("fails closed on an inconsistent summary and an unknown wire value")
        void shouldFailClosed() {
            assertThrows(IllegalArgumentException.class, () -> new HarnessUsageReport(
                    TokenUsageCoverage.COMPLETE, Map.of(UsageFigure.INPUT, UsageFigureStatus.NOT_REPORTED)));
            assertThrows(IllegalArgumentException.class, () -> new HarnessUsageReport(
                    TokenUsageCoverage.PARTIAL, Map.of(UsageFigure.INPUT, UsageFigureStatus.REPORTED)));
            assertThrows(IOException.class, () -> HarnessUsageReport.fromJson(
                    "{\"coverage\":\"complete\",\"figures\":{\"colour\":\"reported\"}}"));
            assertThrows(IOException.class, () -> HarnessUsageReport.fromJson(
                    "{\"coverage\":\"complete\",\"figures\":{\"input\":\"maybe\"}}"));
        }

        @Test
        @DisplayName("is a closed list of figures and statuses")
        void shouldBeClosedLists() {
            assertEquals(5, UsageFigure.values().length);
            assertEquals(3, UsageFigureStatus.values().length);
            assertEquals(UsageFigure.CACHE_WRITE, UsageFigure.fromWire("cache_write"));
            assertNull(UsageFigure.fromWire("colour"));
            assertEquals(UsageFigureStatus.NOT_APPLICABLE, UsageFigureStatus.fromWire("not_applicable"));
            assertNull(UsageFigureStatus.fromWire("maybe"));
            assertThrows(IllegalArgumentException.class, () -> UsageFigure.parse("colour"));
            assertThrows(IllegalArgumentException.class, () -> UsageFigureStatus.parse("maybe"));
        }
    }

    @Nested
    @DisplayName("HarnessVerification")
    class HarnessVerificationTests {

        @Test
        @DisplayName("round-trips the stages and the usage stage's result")
        void shouldRoundTrip() throws Exception {
            var verification = new HarnessVerification(Map.of(
                            HarnessVerificationStage.IDENTITY, VerificationStageStatus.PASSED,
                            HarnessVerificationStage.DETECTION, VerificationStageStatus.PASSED,
                            HarnessVerificationStage.LAUNCH, VerificationStageStatus.PASSED,
                            HarnessVerificationStage.ISOLATION, VerificationStageStatus.FAILED),
                    new HarnessUsageReport(TokenUsageCoverage.PARTIAL,
                            Map.of(UsageFigure.CACHE_WRITE, UsageFigureStatus.NOT_REPORTED)));

            var parsed = HarnessVerification.fromJson(verification.toJson());

            assertEquals(verification, parsed);
            assertEquals(Optional.of(HarnessVerificationStage.ISOLATION), parsed.firstFailure());
            assertFalse(parsed.passed());
            assertEquals(VerificationStageStatus.NOT_RUN, parsed.statusOf(HarnessVerificationStage.LIMITS));
        }

        @Test
        @DisplayName("reports a pass only when every stage of the catalogue passed")
        void shouldReportPass() {
            var stages = new EnumMap<HarnessVerificationStage, VerificationStageStatus>(
                    HarnessVerificationStage.class);
            for (var stage : HarnessVerificationStage.values()) {
                stages.put(stage, VerificationStageStatus.PASSED);
            }
            var verification = new HarnessVerification(stages, null);
            assertTrue(verification.passed());
            assertTrue(verification.firstFailure().isEmpty());

            var partial = new HarnessVerification(
                    Map.of(HarnessVerificationStage.IDENTITY, VerificationStageStatus.PASSED), null);
            assertFalse(partial.passed(), "a verification whose later stages did not run has not passed");
        }

        @Test
        @DisplayName("is a closed list of stages and statuses")
        void shouldBeClosedLists() {
            assertEquals(9, HarnessVerificationStage.values().length);
            assertEquals(3, VerificationStageStatus.values().length);
            assertEquals(HarnessVerificationStage.USAGE, HarnessVerificationStage.fromWire("usage"));
            assertNull(HarnessVerificationStage.fromWire("magic"));
            assertEquals(VerificationStageStatus.NOT_RUN, VerificationStageStatus.fromWire("not_run"));
            assertThrows(IllegalArgumentException.class, () -> HarnessVerificationStage.parse("magic"));
            assertThrows(IllegalArgumentException.class, () -> VerificationStageStatus.parse("maybe"));
        }
    }

    @Nested
    @DisplayName("HarnessScanResult with usage")
    class HarnessScanResultUsageTests {

        @Test
        @DisplayName("round-trips the per-figure usage report of a harness")
        void shouldRoundTripUsage() throws Exception {
            var usage = new HarnessUsageReport(TokenUsageCoverage.PARTIAL,
                    Map.of(UsageFigure.CACHE_WRITE, UsageFigureStatus.NOT_REPORTED));
            var harness = new HarnessScanResult("opencode", "OpenCode", "/opt/homebrew/bin/opencode",
                    "1.9.0", true, true, true, TokenUsageCoverage.PARTIAL, List.of("cache_write"), true, usage);

            var parsed = HarnessScanResult.fromJson(harness.toJson());

            assertEquals(harness, parsed);
            assertNotNull(parsed.usage());
            assertEquals(List.of("cache_write"), parsed.usage().missingFigures());
        }

        @Test
        @DisplayName("refuses a top-level token usage that disagrees with the usage report")
        void shouldRefuseConflictingUsage() {
            var usage = new HarnessUsageReport(TokenUsageCoverage.PARTIAL,
                    Map.of(UsageFigure.CACHE_WRITE, UsageFigureStatus.NOT_REPORTED));

            assertThrows(IllegalArgumentException.class, () -> new HarnessScanResult(
                    "claude", "Claude Code", null, null, true, true, true,
                    TokenUsageCoverage.COMPLETE, List.of(), true, usage));
            assertThrows(IllegalArgumentException.class, () -> new HarnessScanResult(
                    "claude", "Claude Code", null, null, true, true, true,
                    TokenUsageCoverage.PARTIAL, List.of("cache_read"), true, usage));
        }
    }

    @ParameterizedTest
    @EnumSource(OnboardingAspect.class)
    @DisplayName("every aspect has a distinct wire value")
    void aspectsHaveDistinctWireValues(OnboardingAspect aspect) {
        assertEquals(aspect, OnboardingAspect.fromWire(aspect.wireValue()));
        assertEquals(7, Set.of(OnboardingAspect.values()).size());
    }

    @Test
    @DisplayName("exposes the closed-list accessors of consent, documents, and token coverage")
    void shouldExposeClosedListAccessors() {
        assertEquals("stays", ConsentKind.NoBehavior.STAYS.wireValue());
        assertEquals(ConsentKind.NoBehavior.BACK_TO_MODE, ConsentKind.AUTO_RUN.noBehavior());
        assertEquals(ConsentStatus.GRANTED, ConsentStatus.fromWire("granted"));
        assertNull(ConsentStatus.fromWire("nope"));
        assertNull(ConsentStatus.fromWire(null));

        assertEquals("copy", DocumentsMode.COPY.wireValue());
        assertEquals(DocumentsMode.MOVE, DocumentsMode.fromWire("move"));
        assertNull(DocumentsMode.fromWire("nope"));
        assertNull(DocumentsMode.fromWire(""));

        assertEquals(TokenUsageCoverage.COMPLETE, TokenUsageCoverage.fromWire("complete"));
        assertNull(TokenUsageCoverage.fromWire(null));
        assertNull(ConsentKind.fromWire(""));
        assertEquals("onboard.step.plans", OnboardingAspect.PLANS.stepNameKey());
    }
}
