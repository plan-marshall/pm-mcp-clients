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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.process.LeafTask;
import de.planmarshall.api.process.NodeState;
import de.planmarshall.api.process.OffTrackAction;
import de.planmarshall.api.process.ProcessTokens;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.operator.OutputSafety;
import de.planmarshall.operator.tui.TuiLayout;

/**
 * Pure formatting helpers of the full-screen plan view: every value the runtime supplies is
 * stripped of control sequences and collapsed to one line before it becomes a cell.
 */
final class PlanViewFormatting {

    static final String DOT_SEPARATOR = " \u00b7 ";
    static final String PATH_SEPARATOR = " \u203a ";

    private PlanViewFormatting() {
    }

    /**
     * @param text raw text, possibly {@code null}
     * @return the text without control sequences or line breaks
     */
    static String clean(@Nullable String text) {
        return TuiLayout.sanitizeCell(OutputSafety.stripControlSequences(text)).strip();
    }

    /**
     * @param seconds duration in seconds
     * @return a compact duration ({@code 45 s}, {@code 5 min}, {@code 1 h 5 min}), or empty
     */
    static String duration(long seconds) {
        if (seconds <= 0) {
            return "";
        }
        if (seconds < 60) {
            return seconds + " s";
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + " h " + minutes + " min";
        }
        return minutes + " min";
    }

    /**
     * @param locale the interface locale
     * @param state  the node state
     * @param node   run counts
     * @return the localized state label ({@code done ×2}, {@code running · run 3}, ...)
     */
    static String nodeState(String locale, NodeState state, int runCount, int currentRun) {
        return switch (state) {
            case DONE -> runCount > 1
                                ? MessageCatalogue.format(locale, "plan.node.state.done_times",
                                Map.of("count", runCount))
                                : MessageCatalogue.format(locale, "plan.node.state.done", Map.of());
            case RUNNING -> currentRun > 1
                                ? MessageCatalogue.format(locale, "plan.node.state.running_run",
                                Map.of("run", currentRun))
                                : MessageCatalogue.format(locale, "plan.node.state.running", Map.of());
            case WAITING_FOR_OPERATOR -> MessageCatalogue.format(locale, "plan.node.state.waiting", Map.of());
            case FAILED -> MessageCatalogue.format(locale, "plan.node.state.failed", Map.of());
            case NOT_TAKEN -> MessageCatalogue.format(locale, "plan.node.state.not_taken", Map.of());
            case NOT_IN_PLAN -> MessageCatalogue.format(locale, "plan.node.state.not_in_plan", Map.of());
            case PLANNED -> MessageCatalogue.format(locale, "plan.node.state.planned", Map.of());
        };
    }

    /**
     * @param locale the interface locale
     * @param state  the state value the runtime reported
     * @return the localized state label
     */
    static String phaseState(String locale, @Nullable String state) {
        var clean = state != null ? state : "running";
        return switch (clean.toLowerCase(Locale.ROOT)) {
            case "done" -> MessageCatalogue.format(locale, "plan.node.state.done", Map.of());
            case "running" -> MessageCatalogue.format(locale, "plan.node.state.running", Map.of());
            case "waiting", "waiting_for_you", "waiting_for_operator" ->
                MessageCatalogue.format(locale, "plan.node.state.waiting", Map.of());
            case "failed" -> MessageCatalogue.format(locale, "plan.node.state.failed", Map.of());
            case "planned" -> MessageCatalogue.format(locale, "plan.node.state.planned", Map.of());
            case "paused" -> MessageCatalogue.format(locale, "overview.state.paused", Map.of());
            case "held" -> MessageCatalogue.format(locale, "overview.state.held", Map.of());
            case "pausing" -> MessageCatalogue.format(locale, "overview.state.pausing", Map.of());
            default -> clean;
        };
    }

    /**
     * @param locale the interface locale
     * @param tokens the plan's tokens so far
     * @return the total with its split, a floor with {@code ≥}, or {@code none yet}
     */
    static String tokens(String locale, ProcessTokens tokens) {
        if (tokens.noneYet()) {
            return MessageCatalogue.format(locale, "plan.view.tokens_none", Map.of());
        }
        return MessageCatalogue.format(locale, "operator.plan.tokens.split", Map.of(
                            "total", TokenFormat.formatTokens(tokens.total(), tokens.floor(), locale),
                            "input", TokenFormat.formatTokens(tokens.input(), tokens.floor(), locale),
                            "output", TokenFormat.formatTokens(tokens.output(), tokens.floor(), locale)));
    }

    /**
     * @param locale the interface locale
     * @param task   the leaf task
     * @return the task's one-line detail
     */
    static String taskLine(String locale, LeafTask task) {
        var outcome = task.outcome() != null ? task.outcome() : "-";
        var line = clean(task.taskId()) + DOT_SEPARATOR
                            + MessageCatalogue.format(locale, "plan.leaf.task", Map.of(
                            "role", clean(task.role()),
                            "harness", clean(task.harness()),
                            "model", clean(task.model()),
                            "attempt", task.attempt(),
                            "status", clean(task.status()),
                            "outcome", clean(outcome)));
        var duration = duration(task.durationSeconds());
        return duration.isEmpty() ? line : line + DOT_SEPARATOR + duration;
    }

    /**
     * @param glyph the state glyph of a task
     * @param line  the task line
     * @return the row line with the glyph in front
     */
    static String withGlyph(String glyph, String line) {
        return glyph + " " + line;
    }

    /**
     * @param locale the interface locale
     * @param signal the off-track signal
     * @param action the action the server took
     * @return the localized banner text
     */
    static String offTrack(String locale, String signal, @Nullable String action,
                        @Nullable Integer value, @Nullable Integer threshold) {
        var signalKey = "plan.signal." + signal;
        var signalText = MessageCatalogue.hasKey(signalKey)
                            ? MessageCatalogue.format(locale, signalKey, Map.of())
                            : clean(signal);
        var actionKey = "plan.signal.action." + (action != null ? action : OffTrackAction.SHOW.value());
        var actionText = MessageCatalogue.hasKey(actionKey)
                            ? MessageCatalogue.format(locale, actionKey, Map.of())
                            : clean(action);
        var params = new LinkedHashMap<String, Object>();
        params.put("signal", signalText);
        params.put("action", actionText);
        params.put("value", value != null ? value : 0);
        params.put("threshold", threshold != null ? threshold : 0);
        if (value != null && threshold != null) {
            return MessageCatalogue.format(locale, "operator.plan.activity.off_track_banner", params);
        }
        return MessageCatalogue.format(locale, "plan.activity.off_track", params);
    }

    /**
     * @param signal the signal value
     * @return its localized name, or the cleaned value when the closed list has no key
     */
    static String signal(String locale, @Nullable String signal) {
        if (signal == null) {
            return "";
        }
        var key = "plan.signal." + signal;
        return MessageCatalogue.hasKey(key)
                            ? MessageCatalogue.format(locale, key, Map.of())
                            : clean(signal);
    }
}
