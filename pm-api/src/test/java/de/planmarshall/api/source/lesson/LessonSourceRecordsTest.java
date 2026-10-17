/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;

@DisplayName("Lesson source record contracts")
class LessonSourceRecordsTest {

    @Nested
    @DisplayName("PremiseEvidenceKind")
    class PremiseEvidenceKindTest {

        @Test
        @DisplayName("is the closed list of the two evidence kinds and resolves its wire values")
        void closedListAndWireValues() {
            var wireValues = Stream.of(PremiseEvidenceKind.values())
                    .map(PremiseEvidenceKind::wireValue)
                    .collect(Collectors.toCollection(TreeSet::new));

            assertEquals(Set.of("file", "symbol"), wireValues);
            assertEquals(PremiseEvidenceKind.FILE, PremiseEvidenceKind.fromWire("file"));
            assertEquals(PremiseEvidenceKind.SYMBOL, PremiseEvidenceKind.fromWire("SYMBOL"));
        }

        @Test
        @DisplayName("refuses a value that names no evidence kind")
        void refusesUnknownValue() {
            assertThrows(IllegalArgumentException.class, () -> PremiseEvidenceKind.fromWire("bogus"));
            assertThrows(IllegalArgumentException.class, () -> PremiseEvidenceKind.fromWire(null));
        }
    }

    @Test
    @DisplayName("LessonPremiseEvidence round-trips and omits absent optional fields")
    void premiseEvidenceRoundTrips() throws Exception {
        var full = new LessonPremiseEvidence(PremiseEvidenceKind.SYMBOL, "de.planmarshall.api.Start", null,
                "a1b2c3d");
        assertEquals(full, LessonPremiseEvidence.fromJson(full.toJson()));

        var minimal = new LessonPremiseEvidence(PremiseEvidenceKind.FILE, "doc/concepts/forgejo.adoc", null, null);
        assertFalse(minimal.toMap().containsKey("expected_path"));
        assertFalse(minimal.toMap().containsKey("last_verified_commit"));
        assertNull(LessonPremiseEvidence.fromJson(minimal.toJson()).expectedPath());
    }

    @Test
    @DisplayName("LessonPremiseCheckResult round-trips and reports whether the premise holds")
    void premiseCheckResultRoundTrips() throws Exception {
        var broken = new LessonPremiseCheckResult("les-0007", LessonPremiseCheckResult.STATUS_BROKEN,
                List.of(new LessonPremiseEvidence(PremiseEvidenceKind.FILE, "src/Old.java", "src/Old.java", null)),
                null);
        var roundTrip = LessonPremiseCheckResult.fromJson(broken.toJson());
        assertEquals(broken, roundTrip);
        assertFalse(roundTrip.holds());

        var holds = new LessonPremiseCheckResult("les-0007", LessonPremiseCheckResult.STATUS_HOLDS, null, null);
        assertTrue(holds.holds());
        assertTrue(holds.evidence().isEmpty());

        var unavailable = new LessonPremiseCheckResult("les-0007", LessonPremiseCheckResult.STATUS_UNAVAILABLE,
                List.of(), "the repository tree is not readable");
        assertEquals("the repository tree is not readable",
                LessonPremiseCheckResult.fromJson(unavailable.toJson()).cause());
    }

    @Test
    @DisplayName("UnclaimedLessonSummary round-trips with its citing plan and cited symbols")
    void unclaimedLessonSummaryRoundTrips() throws Exception {
        var full = new UnclaimedLessonSummary("les-0007", "Retire lessons on landing",
                "A landed plan retires the lessons it claimed.", Instant.parse("2026-10-12T14:05:00Z"),
                "fix-relay", List.of("src/A.java", "de.Old#m()"));
        assertEquals(full, UnclaimedLessonSummary.fromJson(full.toJson()));

        var anonymous = new UnclaimedLessonSummary("les-0008", "Keep the store append-only", null,
                Instant.parse("2026-10-11T09:00:00Z"), null, null);
        assertFalse(anonymous.toMap().containsKey("citing_plan_id"));
        assertTrue(anonymous.symbolsCited().isEmpty());
        assertNull(UnclaimedLessonSummary.fromJson(anonymous.toJson()).citingPlanId());
        assertEquals("", UnclaimedLessonSummary.fromJson(anonymous.toJson()).summary());
    }

    @Test
    @DisplayName("LessonClaimRequest round-trips and reports the use-anyway override")
    void lessonClaimRequestRoundTrips() throws Exception {
        var request = new LessonClaimRequest("pm-mcp-clients", "les-0007", Boolean.TRUE);
        var roundTrip = LessonClaimRequest.fromJson(request.toJson());
        assertEquals(request, roundTrip);
        assertTrue(roundTrip.isUseAnyway());

        var plain = new LessonClaimRequest("pm-mcp-clients", "les-0007", Boolean.FALSE);
        assertFalse(plain.isUseAnyway());
        assertFalse(LessonClaimRequest.fromJson(plain.toJson()).isUseAnyway());
    }

    @Test
    @DisplayName("LessonClaimResult round-trips and reports whether the claim succeeded")
    void lessonClaimResultRoundTrips() throws Exception {
        var claimed = new LessonClaimResult("les-0007", LessonClaimResult.STATUS_CLAIMED, "fix-relay");
        assertEquals(claimed, LessonClaimResult.fromJson(claimed.toJson()));
        assertTrue(LessonClaimResult.fromJson(claimed.toJson()).claimed());

        var refused = new LessonClaimResult("les-0007", LessonClaimResult.STATUS_REFUSED, null);
        assertFalse(refused.claimed());
        assertFalse(refused.toMap().containsKey("plan_id"));
    }

    @Test
    @DisplayName("LessonRetirementNotice round-trips and omits an absent reason")
    void lessonRetirementNoticeRoundTrips() throws Exception {
        var notice = new LessonRetirementNotice("les-0007", true, "premise broken");
        assertEquals(notice, LessonRetirementNotice.fromJson(notice.toJson()));

        var silent = new LessonRetirementNotice("les-0007", true, null);
        assertFalse(silent.toMap().containsKey("reason"));
        assertNull(LessonRetirementNotice.fromJson(silent.toJson()).reason());
    }

    @Nested
    @DisplayName("Negative parsing")
    class NegativeParsing {

        @Test
        @DisplayName("refuses a JSON value that is no object")
        void refusesNonObjectJson() {
            assertThrows(JsonFormatException.class, () -> LessonPremiseEvidence.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> LessonPremiseCheckResult.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> UnclaimedLessonSummary.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> LessonClaimRequest.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> LessonClaimResult.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> LessonRetirementNotice.fromJson("[]"));
        }

        @Test
        @DisplayName("refuses an object without the required fields")
        void refusesMissingFields() {
            assertThrows(JsonFormatException.class, () -> LessonPremiseEvidence.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> LessonPremiseCheckResult.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> UnclaimedLessonSummary.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> LessonClaimRequest.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> LessonClaimResult.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> LessonRetirementNotice.fromJson("{}"));
        }

        @Test
        @DisplayName("refuses an unknown evidence kind")
        void refusesUnknownEvidenceKind() {
            assertThrows(IllegalArgumentException.class,
                    () -> LessonPremiseEvidence.fromJson("{\"kind\":\"bogus\",\"name\":\"x\"}"));
        }
    }
}
