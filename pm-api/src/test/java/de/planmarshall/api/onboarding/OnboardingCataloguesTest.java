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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Compares every closed list of the onboarding contract verbatim with the specification it is cut
 * from: the consent kinds (Consents, The Kinds), the token-usage values (Client Profiles, Tokens,
 * Not Money), the reasons of {@code auto.result} (State Catalogue, Automatic Onboarding), the
 * landing variants (Plan Behaviour Axes), the seven aspects, the verification stages, and the
 * documents modes. A change of a list in the specification that the code does not follow fails here.
 */
@DisplayName("Onboarding closed catalogues")
class OnboardingCataloguesTest {

    @Test
    @DisplayName("the consent kinds are the fourteen of the consents table, in catalogue order")
    void consentKindsMatchSpecification() {
        assertEquals(List.of(
                        "scan_harnesses", "fetch_icons", "probe_harness_login", "verify_harness_profile",
                        "probe_harness_models", "model_advice", "check_credential_store", "scan_providers",
                        "verify_providers", "inspect_checkout", "run_checkout_scripts", "model_cost",
                        "check_updates", "auto_run"),
                Arrays.stream(ConsentKind.values()).map(ConsentKind::wireValue).toList());
    }

    @Test
    @DisplayName("the token-usage values are the closed three of Tokens, Not Money, and the figures of a run")
    void tokenUsageValuesMatchSpecification() {
        assertEquals(List.of("complete", "partial", "not_reported"),
                Arrays.stream(TokenUsageCoverage.values()).map(TokenUsageCoverage::wireValue).toList());
        assertEquals(List.of("reported", "not_applicable", "not_reported"),
                Arrays.stream(UsageFigureStatus.values()).map(UsageFigureStatus::wireValue).toList());
        assertEquals(List.of("input", "cache_read", "cache_write", "output", "reasoning"),
                Arrays.stream(UsageFigure.values()).map(UsageFigure::wireValue).toList());
    }

    @Test
    @DisplayName("the automatic run's reasons are the closed four of Automatic Onboarding")
    void autoResultReasonsMatchSpecification() {
        assertEquals(List.of("detected", "all_supported", "default", "asked"),
                Arrays.stream(AutoResultReason.values()).map(AutoResultReason::wireValue).toList());
    }

    @Test
    @DisplayName("the landing variants are the three of the plan behaviour axes")
    void landingVariantsMatchSpecification() {
        assertEquals(List.of("landing.pull-request", "landing.main", "landing.files"),
                List.of(PlanDefaults.LANDING_PULL_REQUEST, PlanDefaults.LANDING_MAIN, PlanDefaults.LANDING_FILES));
    }

    @Test
    @DisplayName("the seven aspects keep the order and the wire values of the first run")
    void aspectsMatchSpecification() {
        assertEquals(List.of("language", "harness", "catalogue", "roles", "store", "providers", "defaults"),
                Arrays.stream(OnboardingAspect.values()).map(OnboardingAspect::wireValue).toList());
    }

    @Test
    @DisplayName("the verification stages are the nine of the draft profile, in order")
    void verificationStagesMatchSpecification() {
        assertEquals(List.of("identity", "detection", "launch", "isolation", "classes", "limits", "paths",
                        "faults", "usage"),
                Arrays.stream(HarnessVerificationStage.values()).map(HarnessVerificationStage::wireValue).toList());
    }

    @Test
    @DisplayName("the documents modes are the closed three of the documents source")
    void documentsModesMatchSpecification() {
        assertEquals(List.of("copy", "move", "ask"),
                Arrays.stream(DocumentsMode.values()).map(DocumentsMode::wireValue).toList());
    }
}
