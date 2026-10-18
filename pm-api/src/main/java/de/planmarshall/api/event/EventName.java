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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The closed catalogue of the events the runtime streams to the operator's clients, one name of the
 * Event Name grammar ({@code ^[a-z][a-z_]*(\.[a-z][a-z_]*)*$}, at most 64 characters). It is the
 * closed list of the Runtime Model specification (Event Catalogue) and the identifier registry
 * (Event Name): a new event, a new payload field, or a new stream is a change of {@code pm-api}.
 * <p>
 * The list is closed and fails closed: an event name read from the wire that is not a member is
 * refused by {@link #fromWire(String)}, never silently accepted. The name written into the SSE
 * {@code event} field and the {@code event} member of the envelope's {@code data} is {@link #wire()}.
 */
public enum EventName {

    // Stream control.

    /** The events after the client's {@code Last-Event-ID} are no longer held, or the runtime started since. */
    RESET("reset"),

    /** The stream ends after it, with its reason. */
    STREAM_CLOSED("stream.closed"),

    // Runtime, system & cleanup.

    /** The runtime was asked to drain. */
    RUNTIME_DRAINING("runtime.draining"),

    /** A drain was withdrawn before it started. */
    RUNTIME_DRAIN_WITHDRAWN("runtime.drain_withdrawn"),

    /** The runtime is stopping. */
    RUNTIME_STOPPING("runtime.stopping"),

    /** The runtime is upgrading. */
    RUNTIME_UPGRADING("runtime.upgrading"),

    /** The successor runtime reports the upgrade done. */
    RUNTIME_UPGRADED("runtime.upgraded"),

    /** An upgrade failed. */
    RUNTIME_UPGRADE_FAILED("runtime.upgrade_failed"),

    /** The metrics that changed. */
    SYSTEM_METRICS("system.metrics"),

    /** A cleanup job was opened. */
    CLEANUP_JOB_OPENED("cleanup.job_opened"),

    /** The operator decided for a cleanup job. */
    CLEANUP_DECIDED("cleanup.decided"),

    /** A cleanup job made progress. */
    CLEANUP_PROGRESS("cleanup.progress"),

    /** A cleanup job is done. */
    CLEANUP_DONE("cleanup.done"),

    /** A device was paired. */
    DEVICE_PAIRED("device.paired"),

    /** A device was renamed. */
    DEVICE_RENAMED("device.renamed"),

    /** A device was revoked. */
    DEVICE_REVOKED("device.revoked"),

    /** A web listener changed. */
    LISTENER_CHANGED("listener.changed"),

    /** Settings changed at a level. */
    SETTINGS_CHANGED("settings.changed"),

    // Overview.

    /** A project gained running work. */
    PROJECT_WORK_STARTED("project.work_started"),

    /** A project lost running work. */
    PROJECT_WORK_ENDED("project.work_ended"),

    /** A project's fields changed. */
    PROJECT_CHANGED("project.changed"),

    /** A scope started. */
    SCOPE_STARTED("scope.started"),

    /** A scope ended. */
    SCOPE_ENDED("scope.ended"),

    /** A plan's phase changed. */
    PHASE_CHANGED("phase.changed"),

    /** A plan's activity changed. */
    ACTIVITY_CHANGED("activity.changed"),

    /** A {@code needs_operator} summary changed. */
    NEEDS_OPERATOR_CHANGED("needs_operator.changed"),

    /** A condition was raised. */
    CONDITION_RAISED("condition.raised"),

    /** A condition was cleared. */
    CONDITION_CLEARED("condition.cleared"),

    /** A hold opened. */
    HOLD_OPENED("hold.opened"),

    /** A hold changed. */
    HOLD_CHANGED("hold.changed"),

    /** A hold was cleared. */
    HOLD_CLEARED("hold.cleared"),

    /** An epic's queue row changed. */
    EPIC_ROW_CHANGED("epic.row_changed"),

    /** A worker went off track. */
    TASK_OFF_TRACK("task.off_track"),

    // States, process & decisions.

    /** A state was entered. */
    STATE_ENTERED("state.entered"),

    /** A state was left, with its outcome. */
    STATE_LEFT("state.left"),

    /** A sub-workflow was called. */
    SUBWORKFLOW_CALLED("subworkflow.called"),

    /** A sub-workflow returned, with its outcome. */
    SUBWORKFLOW_RETURNED("subworkflow.returned"),

    /** A step firing opened. */
    STEP_OPENED("step.opened"),

    /** A step firing closed. */
    STEP_CLOSED("step.closed"),

    /** A new instance version. */
    INSTANCE_CHANGED("instance.changed"),

    /** The tokens used so far changed. */
    TOKENS_CHANGED("tokens.changed"),

    /** Progress changed. */
    PROGRESS_CHANGED("progress.changed"),

    /** A check's result changed. */
    CHECK_RESULT("check.result"),

    /** A decision opened. */
    DECISION_OPENED("decision.opened"),

    /** A decision changed. */
    DECISION_CHANGED("decision.changed"),

    /** A decision was answered. */
    DECISION_ANSWERED("decision.answered"),

    /** A decision closed. */
    DECISION_CLOSED("decision.closed"),

    /** A source was added. */
    SOURCE_ADDED("source.added"),

    /** A source was removed. */
    SOURCE_REMOVED("source.removed"),

    /** A source was flagged. */
    SOURCE_FLAGGED("source.flagged"),

    /** An entry started its scope. */
    ENTRY_STARTED("entry.started"),

    /** An entry was abandoned. */
    ENTRY_ABANDONED("entry.abandoned"),

    /** An onboarding's entry moved into the project's workspace scope. */
    ENTRY_MOVED("entry.moved"),

    /** A task was offered. */
    TASK_OFFERED("task.offered"),

    /** A task ended, with its outcome. */
    TASK_ENDED("task.ended"),

    /** A job started. */
    JOB_STARTED("job.started"),

    /** A job ended, with its fate. */
    JOB_ENDED("job.ended"),

    /** A worker started. */
    WORKER_STARTED("worker.started"),

    /** A worker was recycled. */
    WORKER_RECYCLED("worker.recycled"),

    /** A worker was replaced. */
    WORKER_REPLACED("worker.replaced"),

    /** An epic's automatic mode changed. */
    EPIC_AUTO_CHANGED("epic.auto_changed"),

    /** An epic's register changed. */
    EPIC_REGISTER_CHANGED("epic.register_changed"),

    /** An epic's decomposition changed. */
    EPIC_DECOMPOSITION_CHANGED("epic.decomposition_changed"),

    // Reports & activity.

    /** A report was sent. */
    REPORT_SENT("report.sent"),

    /** Reports were read. */
    REPORT_READ("report.read"),

    /** One entry of a task's activity journal. */
    ACTIVITY("activity");

    private static final Pattern GRAMMAR = Pattern.compile("^[a-z][a-z_]*(\\.[a-z][a-z_]*)*$");
    private static final int MAX_LENGTH = 64;

    private final String wire;

    EventName(String wire) {
        this.wire = wire;
    }

    /**
     * @return the wire representation of this event name
     */
    public String wire() {
        return wire;
    }

    /**
     * @param wire the wire representation
     * @return {@code true} if the value satisfies the event name grammar
     */
    public static boolean matchesGrammar(String wire) {
        return wire != null && wire.length() <= MAX_LENGTH && GRAMMAR.matcher(wire).matches();
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
