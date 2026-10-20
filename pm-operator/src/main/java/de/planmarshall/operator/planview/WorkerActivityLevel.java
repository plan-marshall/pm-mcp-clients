/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.planview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.process.ActivityEntry;
import de.planmarshall.api.process.ActivityEntryKind;
import de.planmarshall.api.process.TaskActivity;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.operator.tui.TuiRow;

/**
 * The live level of one worker at work: the task's head and the activity stream as plain text
 * ({@code doc/design/terminal-client/16-plan-view.adoc#activity}).
 *
 * <p>Nothing of a stream entry is markup or a terminal sequence: the harness writes untrusted
 * text, so every timestamp, tool target, file name, question, and content cell is stripped of
 * control sequences before it becomes a row.
 */
public final class WorkerActivityLevel {

    /** The row id of the task's head. */
    public static final String SUBHEADER_ROW_ID = "activity:subheader";
    private static final String ENTRY_PREFIX = "activity:entry:";
    private static final String INDENT = "  ";
    private static final String KEY_SEPARATOR = "  ";

    private TaskActivity activity;
    private final String planId;
    private final String nodeId;

    /**
     * @param planId   the plan id
     * @param nodeId   the leaf's node id
     * @param activity the task's activity
     */
    public WorkerActivityLevel(String planId, String nodeId, TaskActivity activity) {
        this.planId = Objects.requireNonNull(planId, "planId");
        this.nodeId = nodeId != null ? nodeId : "";
        this.activity = Objects.requireNonNull(activity, "activity");
    }

    /** @return the task's activity as last read */
    public TaskActivity activity() {
        return activity;
    }

    /** @return the plan id */
    public String planId() {
        return planId;
    }

    /** @return the task id */
    public String taskId() {
        return activity.taskId();
    }

    /** @return the leaf node id */
    public String nodeId() {
        return nodeId;
    }

    /**
     * Replaces the activity after a live update.
     *
     * @param fresh the fresh activity
     */
    public void update(TaskActivity fresh) {
        this.activity = Objects.requireNonNull(fresh, "fresh");
    }

    /**
     * @return the header path from the plan through the leaf to the task
     */
    public String breadcrumb() {
        return PlanViewFormatting.clean(planId) + PlanViewFormatting.PATH_SEPARATOR
                            + PlanViewFormatting.clean(nodeId) + PlanViewFormatting.PATH_SEPARATOR
                            + PlanViewFormatting.clean(activity.taskId());
    }

    /**
     * @param locale the interface locale
     * @return the head, the off-track banner, the gap, the server-only note, and the entries
     */
    public List<TuiRow> rows(String locale) {
        var rows = new ArrayList<TuiRow>();
        rows.add(TuiRow.leaf(SUBHEADER_ROW_ID, head(locale)));
        var offTrack = activity.offTrack();
        if (offTrack != null) {
            rows.add(TuiRow.leaf("activity:off_track",
                                PlanViewFormatting.offTrack(locale, offTrack.signal(), offTrack.action(),
                                                    offTrack.value(), offTrack.threshold())));
        }
        if (activity.gapCount() > 0) {
            rows.add(TuiRow.leaf("activity:gap", MessageCatalogue.format(locale, "plan.activity.gap",
                                Map.of("count", activity.gapCount()))));
        }
        if (activity.serverOnly()) {
            rows.add(TuiRow.leaf("activity:server_only",
                                MessageCatalogue.format(locale, "plan.activity.server_only", Map.of())));
        }
        if (activity.entries().isEmpty()) {
            rows.add(TuiRow.leaf("activity:empty",
                                MessageCatalogue.format(locale, "plan.activity.empty", Map.of())));
        }
        int index = 0;
        for (var entry : activity.entries()) {
            rows.add(TuiRow.leaf(ENTRY_PREFIX + index++, entryLine(locale, entry)));
        }
        return rows;
    }

    /**
     * @return the row a live update should keep at the bottom
     */
    public String lastRowId() {
        var rows = rows("en");
        return rows.isEmpty() ? SUBHEADER_ROW_ID : rows.getLast().id();
    }

    private String head(String locale) {
        var duration = PlanViewFormatting.duration(activity.durationSeconds());
        var tokens = TokenFormat.formatTokens(activity.tokensSoFar(), false, locale) + " tokens";
        return MessageCatalogue.format(locale, "operator.plan.activity.subheader", Map.of(
                            "role", PlanViewFormatting.clean(activity.role()),
                            "harness", PlanViewFormatting.clean(activity.harness()),
                            "model", PlanViewFormatting.clean(activity.model()),
                            "attempt", activity.attempt(),
                            "duration", duration.isEmpty() ? "-" : duration,
                            "tokens", tokens));
    }

    private static String entryLine(String locale, ActivityEntry entry) {
        var timestamp = PlanViewFormatting.clean(entry.timestamp());
        var kind = ActivityEntryKind.fromValue(entry.kind());
        var body = switch (kind) {
            case MODEL_TEXT -> MessageCatalogue.format(locale, "plan.activity.kind.text", Map.of())
                                + KEY_SEPARATOR + safe(entry.content());
            case TOOL_CALL -> toolCall(locale, entry);
            case FILES_CHANGED -> MessageCatalogue.format(locale, "plan.activity.kind.file_changed", Map.of())
                                + KEY_SEPARATOR + String.join(", ", entry.files().stream()
                                .map(PlanViewFormatting::clean).toList());
            case QUESTIONS_ASKED -> asked(locale, entry);
            case LINK -> MessageCatalogue.format(locale, "plan.activity.kind.link", Map.of())
                                + KEY_SEPARATOR + safe(entry.toolTarget());
            case ANSWER_ACK -> MessageCatalogue.format(locale, "plan.activity.kind.answer_ack", Map.of())
                                + KEY_SEPARATOR + safe(entry.content());
        };
        return INDENT + timestamp + KEY_SEPARATOR + body;
    }

    private static String toolCall(String locale, ActivityEntry entry) {
        var name = safe(entry.toolName());
        var target = safe(entry.toolTarget());
        var call = MessageCatalogue.format(locale, "plan.activity.kind.tool_call", Map.of())
                            + KEY_SEPARATOR + (target.isEmpty() ? name : name + " " + target);
        if (!entry.toolFailed()) {
            return call;
        }
        var failed = MessageCatalogue.format(locale, "plan.activity.kind.tool_failed",
                            Map.of("size", safe(entry.size())));
        return call + INDENT + failed;
    }

    private static String asked(String locale, ActivityEntry entry) {
        var text = entry.content() != null && !entry.content().isBlank()
                            ? entry.content()
                            : String.join(" \u00b7 ", entry.questions());
        return MessageCatalogue.format(locale, "plan.activity.kind.question", Map.of())
                            + KEY_SEPARATOR + safe(text);
    }

    private static String safe(@Nullable String text) {
        return PlanViewFormatting.clean(text);
    }
}
