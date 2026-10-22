/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

@DisplayName("Event envelope and closed event catalogue")
class EventEnvelopeTest {

    private static final JsonFactory FACTORY = new JsonFactory();
    private static final Instant AT = Instant.parse("2026-10-18T09:30:00Z");

    @Test
    @DisplayName("the catalogue is the closed specification list of event names")
    void closedEventNameCatalogue() {
        var catalogue = List.of(
                // Stream control
                "reset", "stream.closed",
                // Runtime, system & cleanup
                "runtime.draining", "runtime.drain_withdrawn", "runtime.stopping", "runtime.upgrading",
                "runtime.upgraded", "runtime.upgrade_failed", "system.metrics", "cleanup.job_opened",
                "cleanup.decided", "cleanup.progress", "cleanup.done", "device.paired", "device.renamed",
                "device.revoked", "listener.changed", "settings.changed",
                // Overview
                "project.work_started", "project.work_ended", "project.changed", "scope.started", "scope.ended",
                "phase.changed", "activity.changed", "needs_operator.changed", "condition.raised",
                "condition.cleared", "hold.opened", "hold.changed", "hold.cleared", "epic.row_changed",
                "task.off_track",
                // States, process & decisions
                "state.entered", "state.left", "subworkflow.called", "subworkflow.returned", "step.opened",
                "step.closed", "instance.changed", "tokens.changed", "progress.changed", "check.result",
                "decision.opened", "decision.changed", "decision.answered", "decision.closed", "source.added",
                "source.removed", "source.flagged", "entry.started", "entry.abandoned", "entry.moved",
                "task.offered", "task.ended", "job.started", "job.ended", "worker.started", "worker.recycled",
                "worker.replaced", "epic.auto_changed", "epic.register_changed", "epic.decomposition_changed",
                // Reports & activity
                "report.sent", "report.read", "activity");

        assertEquals(catalogue, EventName.wireValues());
        assertEquals(catalogue.size(), EventName.values().length);
        assertEquals(catalogue.size(), EventName.wireValues().stream().distinct().count());
        assertTrue(EventName.wireValues().stream().allMatch(EventName::matchesGrammar));
    }

    @Test
    @DisplayName("an unknown or malformed event name is refused fail-closed")
    void unknownEventNameIsRefused() {
        assertEquals(EventName.CHECK_RESULT, EventName.fromWire("check.result"));
        assertEquals(EventName.STREAM_CLOSED, EventName.fromWire("stream.closed"));
        assertNull(EventName.fromWireOrNull("check"));
        assertFalse(EventName.isCatalogueMember("check"));
        assertTrue(EventName.isCatalogueMember("check.result"));

        assertTrue(EventName.matchesGrammar("runtime.upgrade_failed"));
        assertFalse(EventName.matchesGrammar("Check.Result"));
        assertFalse(EventName.matchesGrammar("check..result"));
        assertFalse(EventName.matchesGrammar(""));
        assertFalse(EventName.matchesGrammar(null));

        assertThrows(IllegalArgumentException.class, () -> EventName.fromWire("check"));
        assertThrows(IllegalArgumentException.class, () -> EventName.fromWire(null));
    }

    @Test
    @DisplayName("the stream.closed reasons are the closed specification catalogue")
    void closedStreamClosedReasons() {
        var reasons = List.of("device_revoked", "device_expired", "listener_closed", "runtime_stopping",
                "scope_ended");
        assertEquals(reasons, StreamClosedEvent.wireValues());
        assertEquals(reasons.size(), StreamClosedEvent.values().length);

        assertEquals(StreamClosedEvent.DEVICE_REVOKED, StreamClosedEvent.fromWire("device_revoked"));
        assertEquals(StreamClosedEvent.SCOPE_ENDED, StreamClosedEvent.fromWire("scope_ended"));
        assertNull(StreamClosedEvent.fromWireOrNull("normal"));
        assertThrows(IllegalArgumentException.class, () -> StreamClosedEvent.fromWire("normal"));
    }

    @Test
    @DisplayName("the event scope kinds are the closed catalogue with their identifier rule")
    void closedEventScopeKinds() {
        assertEquals(List.of("runtime", "plan", "epic", "workspace", "machine", "entry"),
                EventScopeKind.wireValues());
        assertEquals(EventScopeKind.ENTRY, EventScopeKind.fromWire("entry"));
        assertNull(EventScopeKind.fromWireOrNull("project"));
        assertTrue(EventScopeKind.PLAN.requiresIdentifier());
        assertFalse(EventScopeKind.MACHINE.requiresIdentifier());
        assertThrows(IllegalArgumentException.class, () -> EventScopeKind.fromWire("project"));
    }

    @Test
    @DisplayName("an event envelope round-trips through JSON, the map, and the streaming codec")
    void eventEnvelopeRoundTrip() throws Exception {
        var envelope = new EventEnvelope(EventName.CHECK_RESULT, 7L, AT,
                new EventScope(EventScopeKind.MACHINE, null, null),
                Map.of("check_id", "login", "result", "fail"));

        assertEquals(envelope, EventEnvelope.fromJson(envelope.toJson()));
        assertEquals(envelope, EventEnvelope.fromMap(envelope.toMap()));
        assertEquals(envelope, readStreaming(envelope.toJson()));
        assertEquals(envelope, writeStreaming(envelope));

        // the envelope carries the SSE event name and the data object with event, at, and scope
        var tree = JsonTree.asObject(JsonTree.parse(envelope.toJson()));
        assertEquals("check.result", JsonTree.string(tree, EventEnvelope.FIELD_EVENT));
        var data = JsonTree.object(tree, EventEnvelope.FIELD_DATA);
        assertEquals("check.result", JsonTree.string(data, EventEnvelope.FIELD_EVENT));
        assertEquals(AT.toString(), JsonTree.string(data, EventEnvelope.FIELD_AT));
        assertEquals("machine", JsonTree.string(JsonTree.object(data, EventEnvelope.FIELD_SCOPE), "kind"));
    }

    @Test
    @DisplayName("a scoped envelope carries the scope id and project")
    void scopedEnvelope() throws Exception {
        var envelope = new EventEnvelope(EventName.HOLD_OPENED, 12L, AT,
                new EventScope(EventScopeKind.PLAN, "plan-2026-042", "p-1"), Map.of("hold_id", "H-1", "kind", "landing_failed"));
        var parsed = EventEnvelope.fromJson(envelope.toJson());
        assertEquals(envelope, parsed);
        assertEquals("plan-2026-042", parsed.scope().id());
        assertEquals("p-1", parsed.scope().projectId());
    }

    @Test
    @DisplayName("a payload may carry a member whose value is null, and it survives a round-trip")
    void payloadMayCarryNullMembers() throws Exception {
        var json = "{\"event\":\"task.ended\",\"id\":3,\"data\":{\"event\":\"task.ended\","
                + "\"at\":\"2026-10-18T09:30:00Z\",\"scope\":{\"kind\":\"plan\",\"id\":\"p1\"},"
                + "\"queue_task_id\":\"qt-1\",\"outcome\":null}}";
        var parsed = EventEnvelope.fromJson(json);
        assertNull(parsed.payload().get("outcome"));
        assertEquals(parsed, EventEnvelope.fromJson(parsed.toJson()));
    }

    @Test
    @DisplayName("an unknown event, a mismatched data.event, or a bad id is refused")
    void malformedEnvelopeIsRefused() {
        assertThrows(JsonFormatException.class,
                () -> EventEnvelope.fromJson("{\"event\":\"check\",\"id\":1,\"data\":{}}"));
        assertThrows(JsonFormatException.class,
                () -> EventEnvelope.fromJson(
                        "{\"event\":\"check.result\",\"id\":1,\"data\":{\"event\":\"state.entered\","
                                + "\"at\":\"2026-10-18T09:30:00Z\",\"scope\":{\"kind\":\"machine\"}}}"));
        assertThrows(JsonFormatException.class,
                () -> EventEnvelope.fromJson(
                        "{\"event\":\"check.result\",\"id\":-1,\"data\":{\"event\":\"check.result\","
                                + "\"at\":\"2026-10-18T09:30:00Z\",\"scope\":{\"kind\":\"machine\"}}}"));
        assertThrows(JsonFormatException.class,
                () -> EventEnvelope.fromJson("{\"event\":\"check.result\",\"id\":1}"));
        assertThrows(JsonFormatException.class,
                () -> EventEnvelope.fromJson(
                        "{\"event\":\"check.result\",\"id\":1,\"data\":{\"event\":\"check.result\","
                                + "\"at\":\"not-a-time\",\"scope\":{\"kind\":\"machine\"}}}"));
        assertThrows(JsonFormatException.class, () -> EventEnvelope.fromJson("[]"));
        // an unknown scope kind is refused
        assertThrows(JsonFormatException.class,
                () -> EventEnvelope.fromJson(
                        "{\"event\":\"reset\",\"id\":1,\"data\":{\"event\":\"reset\","
                                + "\"at\":\"2026-10-18T09:30:00Z\",\"scope\":{\"kind\":\"project\"}}}"));
    }

    @Test
    @DisplayName("the constructors reject invalid values")
    void constructorsRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventEnvelope(EventName.RESET, -1L, AT, new EventScope(EventScopeKind.RUNTIME, null, null), Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new EventScope(EventScopeKind.PLAN, null, null));
        // a kind that carries no identifier refuses one
        assertThrows(IllegalArgumentException.class,
                () -> new EventScope(EventScopeKind.MACHINE, "m", null));
        // a payload never carries the envelope's own members
        assertThrows(IllegalArgumentException.class,
                () -> new EventEnvelope(EventName.RESET, 1L, AT, new EventScope(EventScopeKind.RUNTIME, null, null),
                        Map.of(EventEnvelope.FIELD_EVENT, "reset")));
    }

    private static EventEnvelope readStreaming(String json) throws IOException {
        try (var parser = FACTORY.createParser(json)) {
            return EventEnvelope.readFrom(parser);
        }
    }

    private static EventEnvelope writeStreaming(EventEnvelope envelope) throws IOException {
        var out = new StringWriter();
        try (var generator = FACTORY.createGenerator(out)) {
            envelope.writeTo(generator);
        }
        assertFalse(out.toString().isBlank());
        return EventEnvelope.fromJson(out.toString());
    }
}
