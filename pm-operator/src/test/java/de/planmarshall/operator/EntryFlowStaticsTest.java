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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.planmarshall.api.testsupport.RuntimeFixture;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The pieces of the entry that the transcript does not reach directly: the step names in either
 * locale, the onboarding detection of the several status shapes, the badge extraction, and the web
 * port fallback.
 */
@DisplayName("EntryFlow statics")
class EntryFlowStaticsTest {

    @Test
    @DisplayName("step names follow the interface locale and the unknown step keeps its number")
    void stepNames() {
        assertEquals("Harnesses", EntryFlow.getStepName("en", 2));
        assertEquals("Harnesses", EntryFlow.getStepName("de", 2));
        assertEquals("Step 8", EntryFlow.getStepName("en", 8));
        assertEquals("Schritt 8", EntryFlow.getStepName("de", 8));
    }

    @Test
    @DisplayName("the machine is onboarded from the flat flag, the nested flag, or the completed state")
    void onboardingDetection() {
        assertTrue(EntryFlow.isMachineOnboarded(Map.of("onboarded", true)));
        assertTrue(EntryFlow.isMachineOnboarded(Map.of("machine", Map.of("onboarded", true))));
        assertTrue(EntryFlow.isMachineOnboarded(Map.of("onboard", Map.of("state", "onboard.completed"))));
        assertTrue(EntryFlow.isMachineOnboarded(Map.of("onboard", Map.of("completed", true))));
        assertFalse(EntryFlow.isMachineOnboarded(Map.of("onboarded", false)));
        assertFalse(EntryFlow.isMachineOnboarded(Map.of()));
    }

    @Test
    @DisplayName("the completed step and the web port read the numbers and fall back")
    void numbers() {
        assertEquals(3, EntryFlow.getCompletedOnboardStep(Map.of("onboard", Map.of("step", 3))));
        assertEquals(2, EntryFlow.getCompletedOnboardStep(
                            Map.of("onboard", Map.of("completed_steps", 2))));
        assertEquals(0, EntryFlow.getCompletedOnboardStep(Map.of()));
        assertEquals(9000, EntryFlow.getWebPort(Map.of("web", Map.of("port", 9000))));
        assertEquals(EntryFlow.DEFAULT_WEB_PORT, EntryFlow.getWebPort(Map.of()));
    }

    @Test
    @DisplayName("badges come from the needs_operator summaries and drop the zero counts")
    void badges() {
        var badges = EntryFlow.extractWaitingBadges(Map.of("needs_operator", Map.of(
                            "question", 2, "waiver_request", 1, "stalled", 0)));
        assertTrue(badges.contains("2 questions"), badges.toString());
        assertTrue(badges.contains("1 waiver request"), badges.toString());
        assertEquals(2, badges.size());
    }

    @Test
    @DisplayName("an empty status yields no badge")
    void noBadges() {
        assertEquals(0, EntryFlow.extractWaitingBadges(Map.of()).size());
    }

    @Test
    @DisplayName("the entry reports the pair to the runtime fixture helper")
    void pairingFallback() throws Exception {
        try (var fixture = RuntimeFixture.create()) {
            PmOperatorTest.writeLiveRecord(fixture);
            fixture.start((request, response) -> response.send(500, null, ""));
            var context = new OperatorContext(fixture.environment(), fixture.base().getParent(), true);
            var pairing = new EntryFlow(context).issuePairingCode(context.runtime(true), 7420);
            assertEquals("KXQB-MRTN", pairing.code());
            assertEquals("http://127.0.0.1:7420/pair#code=KXQB-MRTN", pairing.url());
        }
    }
}
