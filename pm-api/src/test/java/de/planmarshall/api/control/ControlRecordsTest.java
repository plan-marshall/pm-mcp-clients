/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

@DisplayName("ControlRecordsTest")
@EnableGeneratorController
class ControlRecordsTest {

    @Test
    @DisplayName("validates the closed enumerations and their wire parsing")
    void closedEnumerations() {
        assertAll(
                () -> assertEquals(PlanAction.PAUSE, PlanAction.parse("pause")),
                () -> assertEquals(PlanAction.SETTINGS, PlanAction.parse("SETTINGS")),
                () -> assertNull(PlanAction.fromValue("unknown")),
                () -> assertNull(PlanAction.fromValue(null)),
                () -> assertEquals(HoldKind.STALLED, HoldKind.parse("stalled")),
                () -> assertEquals(HoldKind.INTEGRITY_PAUSE, HoldKind.parse("integrity_pause")),
                () -> assertNull(HoldKind.fromValue("nope")),
                () -> assertThrows(IllegalArgumentException.class, () -> PlanAction.parse("x")),
                () -> assertThrows(IllegalArgumentException.class, () -> HoldKind.parse("x"))
        );
    }

    @Test
    @DisplayName("every enumeration value round-trips through its wire value")
    void allEnumerationValues() {
        for (PlanAction action : PlanAction.values()) {
            assertNotNull(action.value());
            assertEquals(action, PlanAction.fromValue(action.value()));
            assertEquals(action, PlanAction.parse(action.value()));
        }
        for (HoldKind kind : HoldKind.values()) {
            assertNotNull(kind.value());
            assertEquals(kind, HoldKind.fromValue(kind.value()));
            assertEquals(kind, HoldKind.parse(kind.value()));
        }
    }

    @Test
    @DisplayName("round-trip serializes ActionAvailability and WorkActions")
    void workActions() throws Exception {
        var reason = Generators.nonEmptyStrings().next();
        var availability = new ActionAvailability("pause", false, reason);
        var parsedAvailability = ActionAvailability.fromJson(availability.toJson());
        assertEquals("pause", parsedAvailability.action());
        assertFalse(parsedAvailability.available());
        assertEquals(reason, parsedAvailability.reason());

        var enabled = ActionAvailability.enabled("settings");
        assertTrue(enabled.available());
        assertNull(enabled.reason());

        var subjectId = Generators.nonEmptyStrings().next();
        var epicId = Generators.nonEmptyStrings().next();
        var actions = new WorkActions(subjectId, "plan", "paused", "task", null, epicId,
                List.of(ActionAvailability.enabled("start"), ActionAvailability.enabled("settings")));
        var parsedActions = WorkActions.fromJson(actions.toJson());
        assertEquals(subjectId, parsedActions.subjectId());
        assertEquals("plan", parsedActions.subjectKind());
        assertEquals("paused", parsedActions.state());
        assertEquals("task", parsedActions.pausingPoint());
        assertEquals(epicId, parsedActions.epicId());
        assertEquals(2, parsedActions.actions().size());
        assertNotNull(parsedActions.action("start"));
        assertNull(parsedActions.action("cancel"));
    }

    @Test
    @DisplayName("round-trip serializes HoldRecord and its resolution request and result")
    void holds() throws Exception {
        var holdId = Generators.nonEmptyStrings().next();
        var cause = Generators.nonEmptyStrings().next();
        var recordedSize = (long) Generators.integers(0, 1_000_000).next();
        var previousDigest = Generators.nonEmptyStrings().next();
        var currentDigest = Generators.nonEmptyStrings().next();
        var hold = new HoldRecord(
                holdId, "relay-cursor-order", "plan", "integrity_pause",
                cause, 0, null,
                "references.json", recordedSize, previousDigest, 5034L, currentDigest,
                List.of("retry", "accept", "abort"), null
        );
        var parsedHold = HoldRecord.fromJson(hold.toJson());
        assertEquals(holdId, parsedHold.holdId());
        assertEquals("integrity_pause", parsedHold.kind());
        assertEquals(recordedSize, parsedHold.recordedSize());
        assertEquals(currentDigest, parsedHold.currentDigest());
        assertTrue(parsedHold.offers("accept"));
        assertFalse(parsedHold.offers("cancel"));

        var requestReason = Generators.nonEmptyStrings().next();
        var request = new HoldResolutionRequest("accept", requestReason, Map.of("force", true));
        var parsedRequest = HoldResolutionRequest.fromJson(request.toJson());
        assertEquals("accept", parsedRequest.act());
        assertEquals(requestReason, parsedRequest.reason());
        assertEquals(Boolean.TRUE, parsedRequest.params().get("force"));

        var released = new HoldResolutionResult(holdId, true, null, "relay-cursor-order");
        var parsedReleased = HoldResolutionResult.fromJson(released.toJson());
        assertTrue(parsedReleased.released());
        assertNull(parsedReleased.cause());

        var stillHeldCause = Generators.nonEmptyStrings().next();
        var stillHeld = new HoldResolutionResult(holdId, false, stillHeldCause, "fix-relay");
        var parsedStillHeld = HoldResolutionResult.fromJson(stillHeld.toJson());
        assertFalse(parsedStillHeld.released());
        assertEquals(stillHeldCause, parsedStillHeld.cause());
    }

    @Test
    @DisplayName("round-trip serializes plan settings records")
    void planSettings() throws Exception {
        var planId = Generators.nonEmptyStrings().next();
        var revision = Generators.integers(0, 100).next();
        var value = new PlanSettingsValue("landing", "landing.pull-request", "project",
                List.of("landing.pull-request", "landing.main", "landing.files"));
        var settingsRecord = new PlanSettingsRecord(planId, revision, List.of(value,
                new PlanSettingsValue("role.implement", "Claude Code \u00b7 sonnet-5.5", "plan", List.of())));
        var parsedRecord = PlanSettingsRecord.fromJson(settingsRecord.toJson());
        assertEquals(planId, parsedRecord.planId());
        assertEquals(revision, parsedRecord.revision());
        assertEquals("project", parsedRecord.value("landing").origin());
        assertEquals(2, parsedRecord.values().size());

        var updateRevision = Generators.integers(0, 100).next();
        var update = new PlanSettingsUpdateRequest("landing", "landing.main", updateRevision);
        var parsedUpdate = PlanSettingsUpdateRequest.fromJson(update.toJson());
        assertEquals("landing", parsedUpdate.aspect());
        assertEquals("landing.main", parsedUpdate.value());
        assertEquals(updateRevision, parsedUpdate.revision());

        var appliedRevision = Generators.integers(0, 100).next();
        var applied = PlanSettingsUpdateResult.applied(appliedRevision);
        var parsedApplied = PlanSettingsUpdateResult.fromJson(applied.toJson());
        assertTrue(parsedApplied.applied());
        assertFalse(parsedApplied.conflict());
        assertEquals(appliedRevision, parsedApplied.revision());

        var conflictRevision = Generators.integers(0, 100).next();
        var conflict = PlanSettingsUpdateResult.conflict(conflictRevision, "landing.main");
        var parsedConflict = PlanSettingsUpdateResult.fromJson(conflict.toJson());
        assertTrue(parsedConflict.conflict());
        assertEquals("landing.main", parsedConflict.currentValue());

        var tooLate = PlanSettingsUpdateResult.tooLate("landing");
        assertEquals("landing", PlanSettingsUpdateResult.fromJson(tooLate.toJson()).restoreState());
    }

    @Test
    @DisplayName("round-trip serializes the hold and plan control events")
    void controlEvents() throws Exception {
        var holdId = Generators.nonEmptyStrings().next();
        var planId = Generators.nonEmptyStrings().next();
        var description = Generators.nonEmptyStrings().next();
        var opened = new HoldChangedEvent(HoldChangedEvent.EVENT_OPENED, holdId, planId, "plan",
                "condition", description);
        assertEquals("hold.opened", opened.event());
        var parsedOpened = HoldChangedEvent.fromJson(HoldChangedEvent.EVENT_OPENED, opened.toJson());
        assertEquals(holdId, parsedOpened.holdId());
        assertEquals("condition", parsedOpened.kind());

        var cleared = new HoldChangedEvent(HoldChangedEvent.EVENT_CLEARED, holdId, planId, "plan",
                null, null);
        var parsedCleared = HoldChangedEvent.fromJson(HoldChangedEvent.EVENT_CLEARED, cleared.toJson());
        assertNull(parsedCleared.kind());
        assertEquals("plan", parsedCleared.subjectKind());

        var paused = new PlanControlEvent(PlanControlEvent.EVENT_PAUSED, planId, "paused", "task", null);
        assertEquals("plan.paused", paused.event());
        var parsedPaused = PlanControlEvent.fromJson(PlanControlEvent.EVENT_PAUSED, paused.toJson());
        assertEquals(planId, parsedPaused.planId());
        assertEquals("task", parsedPaused.point());

        var reason = Generators.nonEmptyStrings().next();
        var cancelled = new PlanControlEvent(PlanControlEvent.EVENT_CANCELLED, planId, "ended", null,
                reason);
        var parsedCancelled = PlanControlEvent.fromJson(PlanControlEvent.EVENT_CANCELLED, cancelled.toJson());
        assertEquals(reason, parsedCancelled.reason());
    }

    @Test
    @DisplayName("round-trip serializes PlanActionRequest and its factory methods")
    void planActionRequest() throws Exception {
        var pause = PlanActionRequest.pauseAt("phase");
        assertEquals("phase", pause.point());
        assertNull(pause.reason());

        var reason = Generators.nonEmptyStrings().next();
        var ending = PlanActionRequest.ending(reason, true);
        var parsedEnding = PlanActionRequest.fromJson(ending.toJson());
        assertEquals(reason, parsedEnding.reason());
        assertEquals(Boolean.TRUE, parsedEnding.closePr());

        var empty = PlanActionRequest.empty();
        assertEquals(0, empty.toMap().size());

        var epicRequest = PlanActionRequest.fromJson("{\"raise_scope\":true,\"with_plans\":false,\"custom\":7}");
        assertEquals(Boolean.TRUE, epicRequest.raiseScope());
        assertEquals(Boolean.FALSE, epicRequest.withPlans());
        assertEquals(7L, JsonTree.number(epicRequest.extra(), "custom").longValue());
    }

    @Test
    @DisplayName("parses aliased field names and rejects malformed input")
    void aliasesAndErrors() throws Exception {
        var subjectId = Generators.nonEmptyStrings().next();
        var actions = WorkActions.fromMap(Map.of(
                "plan_id", subjectId,
                "state", "running",
                "actions", List.of(Map.of("name", "pause", "available", true))));
        assertEquals(subjectId, actions.subjectId());
        assertEquals("pause", actions.actions().getFirst().action());

        var holdId = Generators.nonEmptyStrings().next();
        var holdSubjectId = Generators.nonEmptyStrings().next();
        var holdCause = Generators.nonEmptyStrings().next();
        var hold = HoldRecord.fromMap(Map.of(
                "id", holdId,
                "plan_id", holdSubjectId,
                "kind", "stalled",
                "cause", holdCause,
                "acts", List.of("retry")));
        assertEquals(holdId, hold.holdId());
        assertEquals(holdSubjectId, hold.subjectId());
        assertEquals(List.of("retry"), hold.acts());

        var settingsPlanId = Generators.nonEmptyStrings().next();
        var settings = PlanSettingsRecord.fromMap(Map.of(
                "id", settingsPlanId,
                "revision", 2,
                "values", List.of(Map.of("key", "method", "value", "method.test-first", "origin", "machine"))));
        assertEquals(settingsPlanId, settings.planId());
        assertEquals("machine", settings.value("method").origin());

        assertThrows(NullPointerException.class,
                () -> new ActionAvailability(null, true, null));
        assertThrows(NullPointerException.class,
                () -> new HoldRecord(null, "p", "plan", "stalled", "c", 0, null, null, null, null, null, null, null, null));
        assertThrows(NullPointerException.class,
                () -> new PlanSettingsRecord(null, 0, null));
        assertThrows(JsonFormatException.class, () -> WorkActions.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> HoldRecord.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> PlanSettingsRecord.fromJson("[]"));
        assertThrows(JsonFormatException.class, () -> PlanSettingsUpdateRequest.fromJson("{}"));
        assertThrows(NullPointerException.class, () -> HoldResolutionResult.fromMap(null));
    }

    @Test
    @DisplayName("uses the recorded instant of a hold's last attempt")
    void holdLastTried() throws Exception {
        var at = Instant.ofEpochMilli(Generators.integers(0, 1_000_000).next());
        var attempts = Generators.integers(1, 10).next();
        var hold = new HoldRecord("H-1", "fix-relay", "plan", "condition", "reads cui-http", attempts, at,
                null, null, null, null, null, List.of("retry", "grant", "end_plan"), null);
        var parsed = HoldRecord.fromJson(hold.toJson());
        assertEquals(at, parsed.lastTriedAt());
        assertEquals(attempts, parsed.attempts());
    }
}
