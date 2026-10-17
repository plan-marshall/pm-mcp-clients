/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.source.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;

@DisplayName("Git source record contracts")
class GitSourceRecordsTest {

    private static List<ChangedFile> changedFiles() {
        return List.of(
                new ChangedFile("doc/concepts/forgejo.adoc", 12, 3, true, false),
                new ChangedFile("pm-api/src/main/java/de/planmarshall/api/start/StartPlanRequest.java", 4, 1,
                        false, true));
    }

    @Nested
    @DisplayName("SourceLane")
    class SourceLaneTest {

        @Test
        @DisplayName("is the two-lane closed list and resolves its wire values")
        void closedListAndWireValues() {
            var wireValues = Stream.of(SourceLane.values())
                    .map(SourceLane::wireValue)
                    .collect(Collectors.toCollection(TreeSet::new));

            assertEquals(Set.of("adopt", "land"), wireValues);
            assertEquals(SourceLane.ADOPT, SourceLane.fromWire("adopt"));
            assertEquals(SourceLane.LAND, SourceLane.fromWire("LAND"));
        }

        @Test
        @DisplayName("refuses a value that names no lane")
        void refusesUnknownValue() {
            assertThrows(IllegalArgumentException.class, () -> SourceLane.fromWire("bogus"));
            assertThrows(IllegalArgumentException.class, () -> SourceLane.fromWire(null));
        }
    }

    @Nested
    @DisplayName("ScreeningOutcome")
    class ScreeningOutcomeTest {

        @Test
        @DisplayName("is the three-outcome closed list and resolves its wire values")
        void closedListAndWireValues() {
            var wireValues = Stream.of(ScreeningOutcome.values())
                    .map(ScreeningOutcome::wireValue)
                    .collect(Collectors.toCollection(TreeSet::new));

            assertEquals(Set.of("clean", "flagged", "unscreened"), wireValues);
            assertEquals(ScreeningOutcome.FLAGGED, ScreeningOutcome.fromWire("flagged"));
            assertEquals(ScreeningOutcome.UNSCREENED, ScreeningOutcome.fromWire("UNSCREENED"));
        }

        @Test
        @DisplayName("needs an acceptance for every outcome except clean")
        void needsAcceptance() {
            assertFalse(ScreeningOutcome.CLEAN.needsAcceptance());
            assertTrue(ScreeningOutcome.FLAGGED.needsAcceptance());
            assertTrue(ScreeningOutcome.UNSCREENED.needsAcceptance());
        }

        @Test
        @DisplayName("refuses a value that names no outcome")
        void refusesUnknownValue() {
            assertThrows(IllegalArgumentException.class, () -> ScreeningOutcome.fromWire("bogus"));
            assertThrows(IllegalArgumentException.class, () -> ScreeningOutcome.fromWire(null));
        }
    }

    @Test
    @DisplayName("ChangedFile round-trips and clamps negative line counts")
    void changedFileRoundTrips() throws Exception {
        var file = new ChangedFile("doc/concepts/forgejo.adoc", 12, 3, true, false);
        assertEquals(file, ChangedFile.fromJson(file.toJson()));
        assertEquals(15, file.changedLines());

        var clamped = new ChangedFile("src/Main.java", -5, -1, false, false);
        assertEquals(0, clamped.additions());
        assertEquals(0, clamped.deletions());
    }

    @Test
    @DisplayName("RateLimitStatus round-trips with its reset instant and clamps negatives")
    void rateLimitRoundTrips() throws Exception {
        var limited = new RateLimitStatus(60, 0, Instant.parse("2026-10-12T14:05:00Z"), true);
        assertEquals(limited, RateLimitStatus.fromJson(limited.toJson()));

        var unlimited = RateLimitStatus.unlimited();
        assertFalse(unlimited.limited());
        assertNull(unlimited.resetAt());
        assertEquals(unlimited, RateLimitStatus.fromJson(unlimited.toJson()));

        var clamped = new RateLimitStatus(-1, -2, null, false);
        assertEquals(0, clamped.limit());
        assertEquals(0, clamped.remaining());
    }

    @Test
    @DisplayName("PullRequestObservation round-trips with its changed files and screening")
    void pullRequestObservationRoundTrips() throws Exception {
        var observation = new PullRequestObservation(
                "https://codeberg.org/forgejo/forgejo/pulls/7001", 7001,
                "Add a Codeberg provider for issues", "octocat", "codeberg-provider",
                PullRequestObservation.STATE_OPEN, true, changedFiles(),
                ScreeningOutcome.CLEAN, RateLimitStatus.unlimited());

        var roundTrip = PullRequestObservation.fromJson(observation.toJson());

        assertEquals(observation, roundTrip);
        assertEquals(1, roundTrip.untrustedCount());
        assertTrue(roundTrip.open());
    }

    @Test
    @DisplayName("PullRequestObservation defaults the state to open and the screening to clean")
    void pullRequestObservationDefaults() throws Exception {
        var observation = new PullRequestObservation(
                "https://codeberg.org/forgejo/forgejo/pulls/7001", 7001, null, null, null, null, false,
                null, null, null);

        assertEquals(PullRequestObservation.STATE_OPEN, observation.state());
        assertEquals(ScreeningOutcome.CLEAN, observation.screening());
        assertTrue(observation.changedFiles().isEmpty());
        assertFalse(observation.toMap().containsKey("reset_at"));
    }

    @Test
    @DisplayName("PrAdoptRequest round-trips with and without a screening bypass reason")
    void prAdoptRequestRoundTrips() throws Exception {
        var clean = new PrAdoptRequest("pm-mcp-clients", "7001", null);
        assertFalse(clean.toMap().containsKey("screening_bypass_reason"));
        assertEquals(clean, PrAdoptRequest.fromJson(clean.toJson()));

        var flagged = new PrAdoptRequest("pm-mcp-clients", "7001", "I read the flagged text");
        assertEquals(flagged, PrAdoptRequest.fromJson(flagged.toJson()));
    }

    @Test
    @DisplayName("PrAdoptResult round-trips and defaults to the adopt lane")
    void prAdoptResultRoundTrips() throws Exception {
        var result = new PrAdoptResult("adopt-7001", null, "codeberg-provider", null);

        var roundTrip = PrAdoptResult.fromJson(result.toJson());

        assertEquals(SourceLane.ADOPT, roundTrip.lane());
        assertEquals(PrAdoptResult.STATUS_ADOPTED, roundTrip.status());
    }

    @Test
    @DisplayName("LocalChangesPreview round-trips and sums its line counts")
    void localChangesPreviewRoundTrips() throws Exception {
        var preview = new LocalChangesPreview(LocalChangesPreview.STATUS_PREVIEWED, "main", changedFiles(),
                "pr", true, true, null);

        var roundTrip = LocalChangesPreview.fromJson(preview.toJson());

        assertEquals(preview, roundTrip);
        assertEquals(16, roundTrip.totalAdditions());
        assertEquals(4, roundTrip.totalDeletions());
    }

    @Test
    @DisplayName("LocalChangesPreview carries the PM-WF-25 refusal when the landing does not commit")
    void localChangesPreviewRefusal() throws Exception {
        var preview = new LocalChangesPreview(null, "main", changedFiles(), "landing.files", false, false,
                "landing.files");

        assertTrue(preview.toMap().containsKey("reason"));
        assertEquals("landing.files", LocalChangesPreview.fromJson(preview.toJson()).reason());
        assertFalse(LocalChangesPreview.fromJson(preview.toJson()).eligible());
    }

    @Test
    @DisplayName("LocalChangesCaptureRequest round-trips with its confirmation flag")
    void localChangesCaptureRequestRoundTrips() throws Exception {
        var unconfirmed = new LocalChangesCaptureRequest("pm-mcp-clients", false);
        assertEquals(unconfirmed, LocalChangesCaptureRequest.fromJson(unconfirmed.toJson()));

        var confirmed = new LocalChangesCaptureRequest("pm-mcp-clients", true);
        assertEquals(Boolean.TRUE, confirmed.toMap().get("confirmed"));
        assertEquals(confirmed, LocalChangesCaptureRequest.fromJson(confirmed.toJson()));
    }

    @Test
    @DisplayName("LocalChangesCaptureResult round-trips and defaults to the land lane")
    void localChangesCaptureResultRoundTrips() throws Exception {
        var result = new LocalChangesCaptureResult("capture-local-changes",
                SourceLane.LAND, "a1b2c3d4e5f6", null);

        var roundTrip = LocalChangesCaptureResult.fromJson(result.toJson());

        assertEquals(SourceLane.LAND, roundTrip.lane());
        assertEquals(LocalChangesCaptureResult.STATUS_CAPTURED, roundTrip.status());
        assertEquals("a1b2c3d4e5f6", roundTrip.snapshotCommit());
    }

    @Nested
    @DisplayName("Negative parsing")
    class NegativeParsing {

        @Test
        @DisplayName("refuses a JSON value that is no object")
        void refusesNonObjectJson() {
            assertThrows(JsonFormatException.class, () -> ChangedFile.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> RateLimitStatus.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> PullRequestObservation.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> PrAdoptRequest.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> PrAdoptResult.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> LocalChangesPreview.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> LocalChangesCaptureRequest.fromJson("[]"));
            assertThrows(JsonFormatException.class, () -> LocalChangesCaptureResult.fromJson("[]"));
        }

        @Test
        @DisplayName("refuses an object without the required fields")
        void refusesMissingFields() {
            assertThrows(JsonFormatException.class, () -> ChangedFile.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> PullRequestObservation.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> PrAdoptRequest.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> PrAdoptResult.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> LocalChangesCaptureRequest.fromJson("{}"));
            assertThrows(JsonFormatException.class, () -> LocalChangesCaptureResult.fromJson("{}"));
        }

        @Test
        @DisplayName("refuses a malformed reset instant and an unknown lane")
        void refusesMalformedValues() {
            assertThrows(JsonFormatException.class,
                    () -> RateLimitStatus.fromJson("{\"reset_at\":\"not-a-time\"}"));
            assertThrows(IllegalArgumentException.class,
                    () -> PrAdoptResult.fromJson("{\"plan_id\":\"p\",\"lane\":\"bogus\"}"));
            assertThrows(IllegalArgumentException.class,
                    () -> LocalChangesCaptureResult.fromJson(
                            "{\"plan_id\":\"p\",\"snapshot_commit\":\"s\",\"lane\":\"bogus\"}"));
            assertThrows(IllegalArgumentException.class,
                    () -> PullRequestObservation.fromJson("{\"url\":\"u\",\"screening\":\"bogus\"}"));
        }

        @Test
        @DisplayName("refuses null required values in the compact constructors")
        void refusesNullRequiredValues() {
            assertThrows(NullPointerException.class,
                    () -> new ChangedFile(null, 0, 0, false, false));
            assertThrows(NullPointerException.class,
                    () -> new PullRequestObservation(null, 0, null, null, null, null, false, null, null, null));
            assertThrows(NullPointerException.class, () -> new PrAdoptRequest("p", null, null));
            assertThrows(NullPointerException.class, () -> new LocalChangesCaptureRequest(null, true));
            assertThrows(NullPointerException.class,
                    () -> new LocalChangesCaptureResult("p", SourceLane.LAND, null, null));
        }

        @Test
        @DisplayName("keeps an empty map's changed files empty rather than null")
        void defaultsTheChangedFiles() throws Exception {
            assertTrue(LocalChangesPreview.fromMap(Map.of()).changedFiles().isEmpty());
            assertTrue(PullRequestObservation.fromMap(Map.of("url", "u")).changedFiles().isEmpty());
        }
    }
}
