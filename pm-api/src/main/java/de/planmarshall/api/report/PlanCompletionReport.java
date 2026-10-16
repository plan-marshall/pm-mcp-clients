/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.report;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * Immutable completion report of an ended plan ({@code GET /api/v1/plans/{plan_id}/completion}).
 *
 * @param planId            plan identifier
 * @param title             plan title
 * @param from              origin path (project, epic, plan)
 * @param outcome           terminal outcome ({@code "merged"}, {@code "no_change"}, etc.)
 * @param terminalReason    explanation of the outcome
 * @param pullRequestNumber optional pull request number
 * @param pullRequestUrl    optional pull request link URL
 * @param reason            reason given by plan or operator
 * @param startedAt         timestamp when plan started
 * @param endedAt           timestamp when plan ended
 * @param durationSeconds   wall clock runtime in seconds
 * @param pausedSeconds     seconds plan was paused
 * @param pausedBy          who paused the plan
 * @param deliverables      deliverables and their states
 * @param replanningRecords history of re-planning events
 * @param decisionRecords   decisions asked and answered
 * @param tokenUsage        token usage record
 */
public record PlanCompletionReport(
        String planId,
        String title,
        ReportFrom from,
        String outcome,
        @Nullable String terminalReason,
        @Nullable Integer pullRequestNumber,
        @Nullable String pullRequestUrl,
        @Nullable String reason,
        Instant startedAt,
        Instant endedAt,
        long durationSeconds,
        long pausedSeconds,
        @Nullable String pausedBy,
        List<DeliverableSummary> deliverables,
        List<ReplanningRecord> replanningRecords,
        List<DecisionRecord> decisionRecords,
        @Nullable TokenUsageRecord tokenUsage,
        @Nullable String rawEnded,
        @Nullable String rawRan
) {
    public PlanCompletionReport(
            String planId,
            String title,
            ReportFrom from,
            String outcome,
            @Nullable String terminalReason,
            @Nullable Integer pullRequestNumber,
            @Nullable String pullRequestUrl,
            @Nullable String reason,
            Instant startedAt,
            Instant endedAt,
            long durationSeconds,
            long pausedSeconds,
            @Nullable String pausedBy,
            List<DeliverableSummary> deliverables,
            List<ReplanningRecord> replanningRecords,
            List<DecisionRecord> decisionRecords,
            @Nullable TokenUsageRecord tokenUsage
    ) {
        this(planId, title, from, outcome, terminalReason, pullRequestNumber, pullRequestUrl, reason, startedAt, endedAt, durationSeconds, pausedSeconds, pausedBy, deliverables, replanningRecords, decisionRecords, tokenUsage, null, null);
    }

    public String ended() {
        return rawEnded != null ? rawEnded : ReportItem.formatTime(endedAt);
    }

    public @Nullable String ran() {
        return rawRan;
    }
    private static final String FIELD_PLAN_ID = "plan_id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_FROM = "from";
    private static final String FIELD_OUTCOME = "outcome";
    private static final String FIELD_TERMINAL_REASON = "terminal_reason";
    private static final String FIELD_PR_NUMBER = "pull_request_number";
    private static final String FIELD_PR_URL = "pull_request_url";
    private static final String FIELD_REASON = "reason";
    private static final String FIELD_STARTED_AT = "started_at";
    private static final String FIELD_ENDED_AT = "ended_at";
    private static final String FIELD_DURATION_SECONDS = "duration_seconds";
    private static final String FIELD_PAUSED_SECONDS = "paused_seconds";
    private static final String FIELD_PAUSED_BY = "paused_by";
    private static final String FIELD_DELIVERABLES = "deliverables";
    private static final String FIELD_REPLANNING = "replanning_records";
    private static final String FIELD_DECISIONS = "decision_records";
    private static final String FIELD_TOKEN_USAGE = "token_usage";

    public PlanCompletionReport {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(endedAt, "endedAt");
        deliverables = deliverables != null ? Collections.unmodifiableList(new ArrayList<>(deliverables)) : List.of();
        replanningRecords = replanningRecords != null ? Collections.unmodifiableList(new ArrayList<>(replanningRecords)) : List.of();
        decisionRecords = decisionRecords != null ? Collections.unmodifiableList(new ArrayList<>(decisionRecords)) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_PLAN_ID, planId);
        map.put(FIELD_TITLE, title);
        map.put(FIELD_FROM, from.toMap());
        map.put(FIELD_OUTCOME, outcome);
        if (terminalReason != null) map.put(FIELD_TERMINAL_REASON, terminalReason);
        if (pullRequestNumber != null) map.put(FIELD_PR_NUMBER, pullRequestNumber);
        if (pullRequestUrl != null) map.put(FIELD_PR_URL, pullRequestUrl);
        if (reason != null) map.put(FIELD_REASON, reason);
        map.put(FIELD_STARTED_AT, startedAt.toString());
        map.put(FIELD_ENDED_AT, endedAt.toString());
        map.put(FIELD_DURATION_SECONDS, durationSeconds);
        map.put(FIELD_PAUSED_SECONDS, pausedSeconds);
        if (pausedBy != null) map.put(FIELD_PAUSED_BY, pausedBy);
        map.put(FIELD_DELIVERABLES, deliverables.stream().map(DeliverableSummary::toMap).toList());
        map.put(FIELD_REPLANNING, replanningRecords.stream().map(ReplanningRecord::toMap).toList());
        map.put(FIELD_DECISIONS, decisionRecords.stream().map(DecisionRecord::toMap).toList());
        if (tokenUsage != null) {
            map.put(FIELD_TOKEN_USAGE, tokenUsage.toMap());
        }
        return map;
    }

    /**
     * Serializes to JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from a JSON map.
     *
     * @param map tree map
     * @return parsed completion report
     * @throws IOException on error
     */
    public static PlanCompletionReport fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, FIELD_PLAN_ID);
        if (planId == null) planId = JsonTree.string(map, "plan");
        if (planId == null) throw new JsonFormatException("Missing plan_id in PlanCompletionReport");

        var title = JsonTree.string(map, FIELD_TITLE);
        if (title == null) title = planId;

        var fromObj = map.get(FIELD_FROM);
        ReportFrom from;
        if (fromObj instanceof String path) {
            var parts = path.split("[>›]");
            var proj = parts[0].trim();
            String epic = null;
            String plan = null;
            if (parts.length >= 3) {
                epic = parts[1].trim();
                plan = parts[2].trim();
            } else if (parts.length == 2) {
                plan = parts[1].trim();
            }
            from = new ReportFrom(proj, epic, plan);
        } else if (fromObj != null && JsonTree.asObject(fromObj) != null) {
            from = ReportFrom.fromMap(JsonTree.asObject(fromObj));
        } else {
            from = new ReportFrom("project", null, planId);
        }

        var outcome = JsonTree.string(map, FIELD_OUTCOME);
        if (outcome == null) outcome = "completed";

        var termReason = JsonTree.string(map, FIELD_TERMINAL_REASON);
        if (termReason == null) termReason = JsonTree.string(map, "outcome_description");

        Integer prNumber = null;
        var prNumObj = map.get(FIELD_PR_NUMBER);
        if (prNumObj == null) prNumObj = map.get("pull_request");
        if (prNumObj instanceof JsonNumber jn) prNumber = (int) jn.longValue();
        else if (prNumObj instanceof Number n) prNumber = n.intValue();
        else if (prNumObj instanceof String s) {
            try {
                prNumber = Integer.parseInt(s.replace("#", "").trim());
            } catch (NumberFormatException _) {
            }
        }

        var prUrl = JsonTree.string(map, FIELD_PR_URL);
        var reason = JsonTree.string(map, FIELD_REASON);

        var startedStr = JsonTree.string(map, FIELD_STARTED_AT);
        if (startedStr == null) startedStr = JsonTree.string(map, "started");
        Instant startedAt;
        if (startedStr != null) {
            try {
                startedAt = Instant.parse(startedStr);
            } catch (DateTimeParseException _) {
                startedAt = Instant.now();
            }
        } else {
            startedAt = Instant.now();
        }

        var endedStr = JsonTree.string(map, FIELD_ENDED_AT);
        String rawEnded = null;
        if (endedStr == null) {
            endedStr = JsonTree.string(map, "ended");
            if (endedStr != null && !endedStr.contains("T")) {
                rawEnded = endedStr;
            }
        }
        Instant endedAt;
        if (endedStr != null) {
            try {
                endedAt = Instant.parse(endedStr);
            } catch (DateTimeParseException _) {
                rawEnded = endedStr;
                endedAt = Instant.now();
            }
        } else {
            endedAt = Instant.now();
        }

        long duration = getLong(map.get(FIELD_DURATION_SECONDS));
        if (duration == 0) duration = getLong(map.get("run_duration_seconds"));
        var rawRan = JsonTree.string(map, "ran");

        long paused = getLong(map.get(FIELD_PAUSED_SECONDS));
        var pausedBy = JsonTree.string(map, FIELD_PAUSED_BY);

        var deliverables = new ArrayList<DeliverableSummary>();
        var delivObj = map.get(FIELD_DELIVERABLES);
        if (delivObj instanceof List<?> dl) {
            for (var elem : dl) {
                var m = JsonTree.asObject(elem);
                if (m != null) deliverables.add(DeliverableSummary.fromMap(m));
            }
        }

        var replan = new ArrayList<ReplanningRecord>();
        var replanObj = map.get(FIELD_REPLANNING);
        if (replanObj == null) replanObj = map.get("replanning");
        if (replanObj instanceof List<?> rl) {
            for (var elem : rl) {
                var m = JsonTree.asObject(elem);
                if (m != null) replan.add(ReplanningRecord.fromMap(m));
            }
        }

        var decisions = new ArrayList<DecisionRecord>();
        var decObj = map.get(FIELD_DECISIONS);
        if (decObj == null) decObj = map.get("decisions");
        if (decObj instanceof List<?> dcl) {
            for (var elem : dcl) {
                var m = JsonTree.asObject(elem);
                if (m != null) decisions.add(DecisionRecord.fromMap(m));
            }
        }

        TokenUsageRecord tokenUsage = null;
        var tuObj = map.get(FIELD_TOKEN_USAGE);
        if (tuObj == null) tuObj = map.get("tokens");
        if (tuObj != null && JsonTree.asObject(tuObj) != null) {
            tokenUsage = TokenUsageRecord.fromMap(JsonTree.asObject(tuObj));
        }

        return new PlanCompletionReport(planId, title, from, outcome, termReason, prNumber, prUrl, reason, startedAt, endedAt, duration, paused, pausedBy, deliverables, replan, decisions, tokenUsage, rawEnded, rawRan);
    }

    /**
     * Parses from JSON string.
     *
     * @param json JSON string
     * @return parsed completion report
     * @throws IOException on error
     */
    public static PlanCompletionReport fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for PlanCompletionReport");
        }
        return fromMap(obj);
    }

    private static long getLong(Object obj) {
        if (obj instanceof JsonNumber jn) return jn.longValue();
        if (obj instanceof Number n) return n.longValue();
        return 0L;
    }
}
