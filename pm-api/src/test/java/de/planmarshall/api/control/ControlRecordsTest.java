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

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

@DisplayName("ControlRecordsTest")
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
        var availability = new ActionAvailability("pause", false, "the runtime is reconnecting");
        var parsedAvailability = ActionAvailability.fromJson(availability.toJson());
        assertEquals("pause", parsedAvailability.action());
        assertFalse(parsedAvailability.available());
        assertEquals("the runtime is reconnecting", parsedAvailability.reason());

        var enabled = ActionAvailability.enabled("settings");
        assertTrue(enabled.available());
        assertNull(enabled.reason());

        var actions = new WorkActions("fix-relay", "plan", "paused", "task", null, "relay-hardening",
                List.of(ActionAvailability.enabled("start"), ActionAvailability.enabled("settings")));
        var parsedActions = WorkActions.fromJson(actions.toJson());
        assertEquals("fix-relay", parsedActions.subjectId());
        assertEquals("plan", parsedActions.subjectKind());
        assertEquals("paused", parsedActions.state());
        assertEquals("task", parsedActions.pausingPoint());
        assertEquals("relay-hardening", parsedActions.epicId());
        assertEquals(2, parsedActions.actions().size());
        assertNotNull(parsedActions.action("start"));
        assertNull(parsedActions.action("cancel"));
    }

    @Test
    @DisplayName("round-trip serializes HoldRecord and its resolution request and result")
    void holds() throws Exception {
        var hold = new HoldRecord(
                "H-0001", "relay-cursor-order", "plan", "integrity_pause",
                "the store file changed outside the runtime", 0, null,
                "references.json", 4812L, "3f9a", 5034L, "b70d",
                List.of("retry", "accept", "abort"), null
        );
        var parsedHold = HoldRecord.fromJson(hold.toJson());
        assertEquals("H-0001", parsedHold.holdId());
        assertEquals("integrity_pause", parsedHold.kind());
        assertEquals(4812L, parsedHold.recordedSize());
        assertEquals("b70d", parsedHold.currentDigest());
        assertTrue(parsedHold.offers("accept"));
        assertFalse(parsedHold.offers("cancel"));

        var request = new HoldResolutionRequest("accept", "checked by hand", Map.of("force", true));
        var parsedRequest = HoldResolutionRequest.fromJson(request.toJson());
        assertEquals("accept", parsedRequest.act());
        assertEquals("checked by hand", parsedRequest.reason());
        assertEquals(Boolean.TRUE, parsedRequest.params().get("force"));

        var released = new HoldResolutionResult("H-0001", true, null, "relay-cursor-order");
        var parsedReleased = HoldResolutionResult.fromJson(released.toJson());
        assertTrue(parsedReleased.released());
        assertNull(parsedReleased.cause());

        var stillHeld = new HoldResolutionResult("H-0001", false, "the plan reads cui-http", "fix-relay");
        var parsedStillHeld = HoldResolutionResult.fromJson(stillHeld.toJson());
        assertFalse(parsedStillHeld.released());
        assertEquals("the plan reads cui-http", parsedStillHeld.cause());
    }

    @Test
    @DisplayName("round-trip serializes plan settings records")
    void planSettings() throws Exception {
        var value = new PlanSettingsValue("landing", "landing.pull-request", "project",
                List.of("landing.pull-request", "landing.main", "landing.files"));
        var record = new PlanSettingsRecord("fix-relay", 7, List.of(value,
                new PlanSettingsValue("role.implement", "Claude Code \u00b7 sonnet-5.5", "plan", List.of())));
        var parsedRecord = PlanSettingsRecord.fromJson(record.toJson());
        assertEquals("fix-relay", parsedRecord.planId());
        assertEquals(7, parsedRecord.revision());
        assertEquals("project", parsedRecord.value("landing").origin());
        assertEquals(2, parsedRecord.values().size());

        var update = new PlanSettingsUpdateRequest("landing", "landing.main", 6);
        var parsedUpdate = PlanSettingsUpdateRequest.fromJson(update.toJson());
        assertEquals("landing", parsedUpdate.aspect());
        assertEquals("landing.main", parsedUpdate.value());
        assertEquals(6, parsedUpdate.revision());

        var applied = PlanSettingsUpdateResult.applied(8);
        var parsedApplied = PlanSettingsUpdateResult.fromJson(applied.toJson());
        assertTrue(parsedApplied.applied());
        assertFalse(parsedApplied.conflict());
        assertEquals(8, parsedApplied.revision());

        var conflict = PlanSettingsUpdateResult.conflict(8, "landing.main");
        var parsedConflict = PlanSettingsUpdateResult.fromJson(conflict.toJson());
        assertTrue(parsedConflict.conflict());
        assertEquals("landing.main", parsedConflict.currentValue());

        var tooLate = PlanSettingsUpdateResult.tooLate("landing");
        assertEquals("landing", PlanSettingsUpdateResult.fromJson(tooLate.toJson()).restoreState());
    }

    @Test
    @DisplayName("round-trip serializes the hold and plan control events")
    void controlEvents() throws Exception {
        var opened = new HoldChangedEvent(HoldChangedEvent.EVENT_OPENED, "H-0001", "fix-relay", "plan",
                "condition", "the plan reads cui-http, which this project may not read");
        assertEquals("hold.opened", opened.event());
        var parsedOpened = HoldChangedEvent.fromJson(HoldChangedEvent.EVENT_OPENED, opened.toJson());
        assertEquals("H-0001", parsedOpened.holdId());
        assertEquals("condition", parsedOpened.kind());

        var cleared = new HoldChangedEvent(HoldChangedEvent.EVENT_CLEARED, "H-0001", "fix-relay", "plan",
                null, null);
        var parsedCleared = HoldChangedEvent.fromJson(HoldChangedEvent.EVENT_CLEARED, cleared.toJson());
        assertNull(parsedCleared.kind());
        assertEquals("plan", parsedCleared.subjectKind());

        var paused = new PlanControlEvent(PlanControlEvent.EVENT_PAUSED, "fix-relay", "paused", "task", null);
        assertEquals("plan.paused", paused.event());
        var parsedPaused = PlanControlEvent.fromJson(PlanControlEvent.EVENT_PAUSED, paused.toJson());
        assertEquals("fix-relay", parsedPaused.planId());
        assertEquals("task", parsedPaused.point());

        var cancelled = new PlanControlEvent(PlanControlEvent.EVENT_CANCELLED, "fix-relay", "ended", null,
                "replaced by relay-v2");
        var parsedCancelled = PlanControlEvent.fromJson(PlanControlEvent.EVENT_CANCELLED, cancelled.toJson());
        assertEquals("replaced by relay-v2", parsedCancelled.reason());
    }

    @Test
    @DisplayName("round-trip serializes PlanActionRequest and its factory methods")
    void planActionRequest() throws Exception {
        var pause = PlanActionRequest.pauseAt("phase");
        assertEquals("phase", pause.point());
        assertNull(pause.reason());

        var ending = PlanActionRequest.ending("replaced by relay-v2", true);
        var parsedEnding = PlanActionRequest.fromJson(ending.toJson());
        assertEquals("replaced by relay-v2", parsedEnding.reason());
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
        var actions = WorkActions.fromMap(Map.of(
                "plan_id", "prj-alias",
                "state", "running",
                "actions", List.of(Map.of("name", "pause", "available", true))));
        assertEquals("prj-alias", actions.subjectId());
        assertEquals("pause", actions.actions().getFirst().action());

        var hold = HoldRecord.fromMap(Map.of(
                "id", "H-X",
                "plan_id", "prj-hold",
                "kind", "stalled",
                "cause", "the worker stopped",
                "acts", List.of("retry")));
        assertEquals("H-X", hold.holdId());
        assertEquals("prj-hold", hold.subjectId());
        assertEquals(List.of("retry"), hold.acts());

        var settings = PlanSettingsRecord.fromMap(Map.of(
                "id", "prj-set",
                "revision", 2,
                "values", List.of(Map.of("key", "method", "value", "method.test-first", "origin", "machine"))));
        assertEquals("prj-set", settings.planId());
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
        var at = Instant.parse("2026-10-16T14:10:00Z");
        var hold = new HoldRecord("H-1", "fix-relay", "plan", "condition", "reads cui-http", 3, at,
                null, null, null, null, null, List.of("retry", "grant", "end_plan"), null);
        var parsed = HoldRecord.fromJson(hold.toJson());
        assertEquals(at, parsed.lastTriedAt());
        assertEquals(3, parsed.attempts());
    }
}
