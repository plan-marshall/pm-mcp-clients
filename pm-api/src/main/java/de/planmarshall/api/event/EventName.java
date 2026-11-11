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

import static de.planmarshall.api.event.EventStream.ENTRY;
import static de.planmarshall.api.event.EventStream.EPIC;
import static de.planmarshall.api.event.EventStream.MACHINE;
import static de.planmarshall.api.event.EventStream.PLAN;
import static de.planmarshall.api.event.EventStream.REPORTS;
import static de.planmarshall.api.event.EventStream.RUNTIME;
import static de.planmarshall.api.event.EventStream.WORKSPACE;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import de.planmarshall.api.id.DottedIdentifier;

/**
 * The closed catalogue of the events the runtime streams to the operator's clients, one name of the
 * Event Name grammar ({@code ^[a-z][a-z_]*(\.[a-z][a-z_]*)*$}, at most 64 characters). It is the
 * closed list of the Runtime Model specification (Event Catalogue, {@code 06-event-catalogue.adoc})
 * and the identifier registry (Event Name): a new event, a new payload field, or a new stream is a
 * change of {@code pm-api}.
 * <p>
 * Every member declares the streams it is sent on ({@link #streams()}), taken from the catalogue. A
 * constructor takes either at least one stream or the set of all streams, so no member can lack a
 * declaration and a missing stream is never read as none. The stream-control events {@link #RESET} and
 * {@link #STREAM_CLOSED} are sent on every stream and take the set of all of them, so a stream added
 * later is never left out.
 * <p>
 * The catalogue's {@code plan.change_captured} is not a member yet: it comes with the package for a
 * plan's changes.
 * <p>
 * The list is closed and fails closed: an event name read from the wire that is not a member is
 * refused by {@link #fromWire(String)}, never silently accepted. The name written into the SSE
 * {@code event} field and the {@code event} member of the envelope's {@code data} is {@link #wire()}.
 */
public enum EventName {

    // Stream control.

    /**
     * The events after the client's {@code Last-Event-ID} are no longer held, or the runtime started since.
     * Sent on every stream.
     */
    RESET("reset", EnumSet.allOf(EventStream.class)),

    /** The stream ends after it, with its reason. Sent on every stream. */
    STREAM_CLOSED("stream.closed", EnumSet.allOf(EventStream.class)),

    // Runtime, system & cleanup.

    /** The runtime was asked to drain. */
    RUNTIME_DRAINING("runtime.draining", RUNTIME),

    /** A drain was withdrawn before it started. */
    RUNTIME_DRAIN_WITHDRAWN("runtime.drain_withdrawn", RUNTIME),

    /** The runtime is stopping. */
    RUNTIME_STOPPING("runtime.stopping", RUNTIME),

    /** The runtime is upgrading. */
    RUNTIME_UPGRADING("runtime.upgrading", RUNTIME),

    /** The successor runtime reports the upgrade done. */
    RUNTIME_UPGRADED("runtime.upgraded", RUNTIME),

    /** An upgrade failed. */
    RUNTIME_UPGRADE_FAILED("runtime.upgrade_failed", RUNTIME),

    /** The metrics that changed. */
    SYSTEM_METRICS("system.metrics", RUNTIME),

    /** A cleanup job was opened. */
    CLEANUP_JOB_OPENED("cleanup.job_opened", RUNTIME),

    /** The operator decided for a cleanup job. */
    CLEANUP_DECIDED("cleanup.decided", RUNTIME),

    /** A cleanup job made progress. */
    CLEANUP_PROGRESS("cleanup.progress", RUNTIME),

    /** A cleanup job is done. */
    CLEANUP_DONE("cleanup.done", RUNTIME),

    /** A device was paired. */
    DEVICE_PAIRED("device.paired", RUNTIME),

    /** A device was renamed. */
    DEVICE_RENAMED("device.renamed", RUNTIME),

    /** A device was revoked. */
    DEVICE_REVOKED("device.revoked", RUNTIME),

    /** A web listener changed. */
    LISTENER_CHANGED("listener.changed", RUNTIME),

    /** Settings changed at a level. */
    SETTINGS_CHANGED("settings.changed", RUNTIME, MACHINE, WORKSPACE, PLAN),

    // Overview.

    /** A project gained running work. */
    PROJECT_WORK_STARTED("project.work_started", RUNTIME),

    /** A project lost running work. */
    PROJECT_WORK_ENDED("project.work_ended", RUNTIME),

    /** A project's fields changed. */
    PROJECT_CHANGED("project.changed", RUNTIME),

    /** A scope started. */
    SCOPE_STARTED("scope.started", RUNTIME, EPIC),

    /** A scope ended. */
    SCOPE_ENDED("scope.ended", RUNTIME, EPIC),

    /** A plan's phase changed. */
    PHASE_CHANGED("phase.changed", RUNTIME, PLAN, EPIC),

    /** A plan's activity changed. */
    ACTIVITY_CHANGED("activity.changed", RUNTIME, PLAN, EPIC),

    /** A plan's {@code queued_revision} changed: queued, its chat ended, or it left the queue. */
    PLAN_REVISION_QUEUED("plan.revision_queued", RUNTIME, PLAN, EPIC),

    /** A {@code needs_operator} summary changed. */
    NEEDS_OPERATOR_CHANGED("needs_operator.changed", RUNTIME, PLAN, EPIC, WORKSPACE),

    /** A condition was raised. */
    CONDITION_RAISED("condition.raised", RUNTIME, PLAN, EPIC, WORKSPACE),

    /** A condition was cleared. */
    CONDITION_CLEARED("condition.cleared", RUNTIME, PLAN, EPIC, WORKSPACE),

    /** A hold opened. */
    HOLD_OPENED("hold.opened", RUNTIME, PLAN, EPIC, WORKSPACE, ENTRY),

    /** A hold changed. */
    HOLD_CHANGED("hold.changed", RUNTIME, PLAN, EPIC, WORKSPACE, ENTRY),

    /** A hold was cleared. */
    HOLD_CLEARED("hold.cleared", RUNTIME, PLAN, EPIC, WORKSPACE, ENTRY),

    /** An epic's queue row changed. */
    EPIC_ROW_CHANGED("epic.row_changed", RUNTIME, EPIC),

    /** A worker went off track. */
    TASK_OFF_TRACK("task.off_track", RUNTIME, PLAN, EPIC),

    // States, process & decisions.

    /** A state was entered. */
    STATE_ENTERED("state.entered", PLAN, EPIC, WORKSPACE, MACHINE, ENTRY),

    /** A state was left, with its outcome. */
    STATE_LEFT("state.left", PLAN, EPIC, WORKSPACE, MACHINE, ENTRY),

    /** A sub-workflow was called. */
    SUBWORKFLOW_CALLED("subworkflow.called", PLAN, EPIC, WORKSPACE, MACHINE, ENTRY),

    /** A sub-workflow returned, with its outcome. */
    SUBWORKFLOW_RETURNED("subworkflow.returned", PLAN, EPIC, WORKSPACE, MACHINE, ENTRY),

    /** A step firing opened. */
    STEP_OPENED("step.opened", PLAN),

    /** A step firing closed. */
    STEP_CLOSED("step.closed", PLAN),

    /** A new instance version. */
    INSTANCE_CHANGED("instance.changed", PLAN),

    /** The tokens used so far changed. */
    TOKENS_CHANGED("tokens.changed", PLAN, EPIC),

    /** Progress changed. */
    PROGRESS_CHANGED("progress.changed", PLAN, MACHINE, WORKSPACE, ENTRY),

    /** A check's result changed. */
    CHECK_RESULT("check.result", MACHINE, WORKSPACE, ENTRY),

    /** A decision opened. */
    DECISION_OPENED("decision.opened", PLAN, EPIC, WORKSPACE, MACHINE, ENTRY),

    /** A decision changed. */
    DECISION_CHANGED("decision.changed", PLAN, EPIC, WORKSPACE, MACHINE, ENTRY),

    /** A decision was answered. */
    DECISION_ANSWERED("decision.answered", PLAN, EPIC, WORKSPACE, MACHINE, ENTRY),

    /** A decision closed. */
    DECISION_CLOSED("decision.closed", PLAN, EPIC, WORKSPACE, MACHINE, ENTRY),

    /** A source was added. */
    SOURCE_ADDED("source.added", ENTRY, EPIC),

    /** A source was removed. */
    SOURCE_REMOVED("source.removed", ENTRY, EPIC),

    /** A source was flagged. */
    SOURCE_FLAGGED("source.flagged", ENTRY, EPIC),

    /** An entry started its scope. */
    ENTRY_STARTED("entry.started", ENTRY),

    /** An entry was abandoned. */
    ENTRY_ABANDONED("entry.abandoned", ENTRY),

    /** An onboarding's entry moved into the project's workspace scope. */
    ENTRY_MOVED("entry.moved", ENTRY),

    /** A task was offered. */
    TASK_OFFERED("task.offered", PLAN, EPIC, WORKSPACE, MACHINE),

    /** A task ended, with its outcome. */
    TASK_ENDED("task.ended", PLAN, EPIC, WORKSPACE, MACHINE),

    /** A job started. */
    JOB_STARTED("job.started", PLAN, EPIC, WORKSPACE),

    /** A job ended, with its fate. */
    JOB_ENDED("job.ended", PLAN, EPIC, WORKSPACE),

    /** A worker started. */
    WORKER_STARTED("worker.started", RUNTIME),

    /** A worker was recycled. */
    WORKER_RECYCLED("worker.recycled", RUNTIME),

    /** A worker was replaced. */
    WORKER_REPLACED("worker.replaced", RUNTIME),

    /** An epic's automatic mode changed. */
    EPIC_AUTO_CHANGED("epic.auto_changed", EPIC),

    /** An epic's register changed. */
    EPIC_REGISTER_CHANGED("epic.register_changed", EPIC),

    /** An epic's decomposition changed. */
    EPIC_DECOMPOSITION_CHANGED("epic.decomposition_changed", EPIC),

    // Chat.

    /** A turn of a chat started: the operator's message, or a role's reply. */
    CHAT_TURN_STARTED("chat.turn.started", PLAN, EPIC, WORKSPACE, ENTRY),

    /** The next piece of a reply being written. */
    CHAT_TURN_DELTA("chat.turn.delta", PLAN, EPIC, WORKSPACE, ENTRY),

    /** A step of the role's work on a reply, as a message key. */
    CHAT_TURN_PROGRESS("chat.turn.progress", PLAN, EPIC, WORKSPACE, ENTRY),

    /** A turn completed, stopped or failed, or a notice was written. */
    CHAT_TURN_COMPLETED("chat.turn.completed", PLAN, EPIC, WORKSPACE, ENTRY),

    /** A proposal of a chat changed its state. */
    CHAT_PROPOSAL("chat.proposal", PLAN, EPIC, WORKSPACE, ENTRY),

    /** A chat ended. */
    CHAT_ENDED("chat.ended", PLAN, EPIC, WORKSPACE, ENTRY),

    // Reports & activity.

    /** A report was sent. */
    REPORT_SENT("report.sent", REPORTS),

    /** Reports were read. */
    REPORT_READ("report.read", REPORTS),

    /** One entry of a task's activity journal. */
    ACTIVITY("activity", EventStream.ACTIVITY);

    private final String wire;
    private final Set<EventStream> streams;

    EventName(String wire, EventStream first, EventStream... rest) {
        this(wire, EnumSet.of(first, rest));
    }

    /**
     * For an event sent on a set of streams derived from {@link EventStream} rather than listed, such as
     * all of them, so a stream added later is included without a change here.
     */
    EventName(String wire, EnumSet<EventStream> streams) {
        this.wire = wire;
        this.streams = Collections.unmodifiableSet(streams);
    }

    /**
     * @return the wire representation of this event name
     */
    public String wire() {
        return wire;
    }

    /**
     * @return the streams the catalogue sends this event on, never empty, in the order of
     *         {@link EventStream}
     */
    public Set<EventStream> streams() {
        return streams;
    }

    /**
     * @param wire the wire representation
     * @return {@code true} if the value satisfies the event name grammar
     */
    public static boolean matchesGrammar(String wire) {
        return DottedIdentifier.eventName(wire);
    }

    /**
     * Resolves a wire value to its catalogue member.
     *
     * @param wire the wire representation
     * @return the matching event name, or {@code null} if none matches
     */
    public static EventName fromWireOrNull(String wire) {
        if (wire == null) {
            return null;
        }
        for (var name : values()) {
            if (name.wire.equals(wire)) {
                return name;
            }
        }
        return null;
    }

    /**
     * Resolves a wire value to its catalogue member.
     *
     * @param wire the wire representation
     * @return the matching event name
     * @throws IllegalArgumentException if no catalogue member matches (fail-closed)
     */
    public static EventName fromWire(String wire) {
        var name = fromWireOrNull(wire);
        if (name == null) {
            throw new IllegalArgumentException("Unknown event name: " + wire);
        }
        return name;
    }

    /**
     * @param wire the wire representation
     * @return {@code true} if the value is a member of the closed catalogue
     */
    public static boolean isCatalogueMember(String wire) {
        return fromWireOrNull(wire) != null;
    }

    /**
     * @return the wire representations of every event name, in catalogue order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(EventName::wire).toList();
    }
}
