/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;

@DisplayName("Enrolment resource records")
@EnableGeneratorController
class EnrolRecordsTest {

    private static final String RECORDED_SHA =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String CURRENT_SHA =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    @Test
    @DisplayName("EnrolRequest round-trips and rejects a missing path")
    void enrolRequest() throws Exception {
        var projectPath = "/home/" + Generators.nonEmptyStrings().next();
        var aliasPath = "/" + Generators.nonEmptyStrings().next();
        var request = new EnrolRequest(projectPath, true);
        var roundTrip = EnrolRequest.fromJson(request.toJson());
        assertAll(
                () -> assertEquals(projectPath, roundTrip.projectPath()),
                () -> assertTrue(roundTrip.pilot())
        );

        var viaAlias = EnrolRequest.fromMap(Map.of("project_path", aliasPath));
        assertEquals(aliasPath, viaAlias.projectPath());
        assertFalse(viaAlias.pilot());

        assertThrows(JsonFormatException.class, () -> EnrolRequest.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> EnrolRequest.fromMap(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new EnrolRequest(" ", false));
    }

    @Test
    @DisplayName("EnrolResult round-trips and requires a project id")
    void enrolResult() throws Exception {
        var projectId = Generators.nonEmptyStrings().next();
        var result = new EnrolResult(EnrolResult.STATUS_ENROLLED, projectId, true);
        assertNotNull(result.toJson());

        var roundTrip = EnrolResult.fromJson(result.toJson());
        assertAll(
                () -> assertEquals(EnrolResult.STATUS_ENROLLED, roundTrip.status()),
                () -> assertEquals(projectId, roundTrip.projectId()),
                () -> assertTrue(roundTrip.pilot())
        );

        var minimalId = Generators.nonEmptyStrings().next();
        var minimal = EnrolResult.fromMap(Map.of("id", minimalId));
        assertEquals(EnrolResult.STATUS_ENROLLED, minimal.status());
        assertEquals(minimalId, minimal.projectId());

        assertThrows(JsonFormatException.class, () -> EnrolResult.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> EnrolResult.fromMap(Map.of()));
    }

    @Test
    @DisplayName("UnenrolRequest round-trips and rejects a missing project id")
    void unenrolRequest() throws Exception {
        var projectId = Generators.nonEmptyStrings().next();
        var roundTrip = UnenrolRequest.fromJson(new UnenrolRequest(projectId).toJson());
        assertEquals(projectId, roundTrip.projectId());

        assertThrows(JsonFormatException.class, () -> UnenrolRequest.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> UnenrolRequest.fromMap(Map.of()));
    }

    @Test
    @DisplayName("UnenrolResult defaults to unenrolled and round-trips")
    void unenrolResult() throws Exception {
        var result = new UnenrolResult(UnenrolResult.STATUS_UNENROLLED);
        var roundTrip = UnenrolResult.fromJson(result.toJson());
        assertEquals(UnenrolResult.STATUS_UNENROLLED, roundTrip.status());

        assertEquals(UnenrolResult.STATUS_UNENROLLED, UnenrolResult.fromMap(Map.of()).status());

        assertThrows(JsonFormatException.class, () -> UnenrolResult.fromJson("[]"));
    }

    @Test
    @DisplayName("StoreDigestComparison derives the mismatch and round-trips")
    void storeDigestComparison() throws Exception {
        var recordedSize = (long) Generators.integers(0, 10_000).next();
        var currentSize = recordedSize + Generators.integers(1, 1_000).next();
        var mismatchDetails = Generators.nonEmptyStrings().next();
        var mismatch = new StoreDigestComparison(".plan/marshal.json", recordedSize, RECORDED_SHA,
                currentSize, CURRENT_SHA, mismatchDetails);
        assertNotNull(mismatch.toJson());
        assertTrue(mismatch.mismatch());

        var roundTrip = StoreDigestComparison.fromJson(mismatch.toJson());
        assertAll(
                () -> assertEquals(".plan/marshal.json", roundTrip.storePath()),
                () -> assertEquals(recordedSize, roundTrip.recordedSize()),
                () -> assertEquals(RECORDED_SHA, roundTrip.recordedSha256()),
                () -> assertEquals(currentSize, roundTrip.currentSize()),
                () -> assertEquals(CURRENT_SHA, roundTrip.currentSha256()),
                () -> assertTrue(roundTrip.mismatch()),
                () -> assertEquals(mismatchDetails, roundTrip.mismatchDetails())
        );

        var matched = new StoreDigestComparison(".plan/marshal.json", recordedSize, RECORDED_SHA,
                recordedSize, RECORDED_SHA, null);
        assertFalse(matched.mismatch(), "mismatch is derived, never taken from input");

        var aliasSize = (long) Generators.integers(0, 10_000).next();
        var viaAlias = StoreDigestComparison.fromMap(Map.of(
                "path", ".plan/marshal.json",
                "recorded_size", aliasSize,
                "recorded_digest", RECORDED_SHA,
                "current_size", aliasSize,
                "current_digest", RECORDED_SHA));
        assertEquals(aliasSize, viaAlias.currentSize());
        assertFalse(viaAlias.mismatch());

        assertThrows(JsonFormatException.class, () -> StoreDigestComparison.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> StoreDigestComparison.fromMap(Map.of()));
    }

    @Test
    @DisplayName("AcceptEditRequest round-trips and requires both members")
    void acceptEditRequest() throws Exception {
        var request = new AcceptEditRequest(".plan/marshal.json", CURRENT_SHA);
        var roundTrip = AcceptEditRequest.fromJson(request.toJson());
        assertAll(
                () -> assertEquals(".plan/marshal.json", roundTrip.storePath()),
                () -> assertEquals(CURRENT_SHA, roundTrip.confirmedSha256())
        );

        assertThrows(JsonFormatException.class, () -> AcceptEditRequest.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> AcceptEditRequest.fromMap(Map.of()));
        assertThrows(JsonFormatException.class,
                () -> AcceptEditRequest.fromMap(Map.of("store_path", ".plan/marshal.json")));
    }

    @Test
    @DisplayName("AcceptEditResult round-trips and requires a new digest")
    void acceptEditResult() throws Exception {
        var result = new AcceptEditResult(AcceptEditResult.STATUS_ACCEPTED, CURRENT_SHA);
        var roundTrip = AcceptEditResult.fromJson(result.toJson());
        assertAll(
                () -> assertEquals(AcceptEditResult.STATUS_ACCEPTED, roundTrip.status()),
                () -> assertEquals(CURRENT_SHA, roundTrip.newSha256())
        );

        assertThrows(JsonFormatException.class, () -> AcceptEditResult.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> AcceptEditResult.fromMap(Map.of()));
    }
}
