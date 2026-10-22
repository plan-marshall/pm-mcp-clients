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
 * @param ownCalls          calls the plan took on its own, apart from the operator's decisions
 * @param changeMap         the change map the runtime rendered, or {@code null}
 * @param rawEnded          the end time as the runtime wrote it, when it is not an instant
 * @param rawRan            the run duration as the runtime wrote it, or {@code null}
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
        List<OwnCallRecord> ownCalls,
        @Nullable ChangeMapRecord changeMap,
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
        this(planId, title, from, outcome, terminalReason, pullRequestNumber, pullRequestUrl, reason, startedAt, endedAt, durationSeconds, pausedSeconds, pausedBy, deliverables, replanningRecords, decisionRecords, tokenUsage, List.of(), null, null, null);
    }

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
            @Nullable TokenUsageRecord tokenUsage,
            List<OwnCallRecord> ownCalls,
            @Nullable ChangeMapRecord changeMap
    ) {
        this(planId, title, from, outcome, terminalReason, pullRequestNumber, pullRequestUrl, reason, startedAt, endedAt, durationSeconds, pausedSeconds, pausedBy, deliverables, replanningRecords, decisionRecords, tokenUsage, ownCalls, changeMap, null, null);
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
    private static final String FIELD_OWN_CALLS = "own_calls";
    private static final String FIELD_CHANGE_MAP = "change_map";

    public PlanCompletionReport {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(title, FIELD_TITLE);
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(outcome, FIELD_OUTCOME);
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(endedAt, "endedAt");
        deliverables = deliverables != null ? Collections.unmodifiableList(new ArrayList<>(deliverables)) : List.of();
        replanningRecords = replanningRecords != null ? Collections.unmodifiableList(new ArrayList<>(replanningRecords)) : List.of();
        decisionRecords = decisionRecords != null ? Collections.unmodifiableList(new ArrayList<>(decisionRecords)) : List.of();
        ownCalls = ownCalls != null ? Collections.unmodifiableList(new ArrayList<>(ownCalls)) : List.of();
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
        map.put(FIELD_OWN_CALLS, ownCalls.stream().map(OwnCallRecord::toMap).toList());
        if (changeMap != null) {
            map.put(FIELD_CHANGE_MAP, changeMap.toMap());
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

        var from = parseFrom(map.get(FIELD_FROM), planId);
        var outcome = JsonTree.string(map, FIELD_OUTCOME);
        if (outcome == null) outcome = "completed";

        var termReason = JsonTree.string(map, FIELD_TERMINAL_REASON);
        if (termReason == null) termReason = JsonTree.string(map, "outcome_description");

        var prNumber = parsePrNumber(map);
        var prUrl = JsonTree.string(map, FIELD_PR_URL);
        var reason = JsonTree.string(map, FIELD_REASON);
        var startedAt = parseStartedAt(map);
        var ended = parseEndedAt(map);

        long duration = parseDuration(map);
        var rawRan = JsonTree.string(map, "ran");
        long paused = getLong(map.get(FIELD_PAUSED_SECONDS));
        var pausedBy = JsonTree.string(map, FIELD_PAUSED_BY);

        var deliverables = parseDeliverables(map);
        var replan = parseReplanning(map);
        var decisions = parseDecisions(map);
        var tokenUsage = parseTokenUsage(map);
        var ownCalls = parseOwnCalls(map);
        var changeMap = parseChangeMap(map);

        return new PlanCompletionReport(planId, title, from, outcome, termReason, prNumber, prUrl, reason, startedAt, ended.instant(), duration, paused, pausedBy, deliverables, replan, decisions, tokenUsage, ownCalls, changeMap, ended.raw(), rawRan);
    }

    private static ReportFrom parseFrom(Object fromObj, String planId) throws IOException {
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
            return new ReportFrom(proj, epic, plan);
        } else if (fromObj != null && JsonTree.asObject(fromObj) != null) {
            return ReportFrom.fromMap(JsonTree.asObject(fromObj));
        }
        return new ReportFrom("project", null, planId);
    }

    private static @Nullable Integer parsePrNumber(Map<String, Object> map) {
        var prNumObj = map.get(FIELD_PR_NUMBER);
        if (prNumObj == null) prNumObj = map.get("pull_request");
        if (prNumObj instanceof JsonNumber jn) return (int) jn.longValue();
        if (prNumObj instanceof Number n) return n.intValue();
        if (prNumObj instanceof String s) {
            try {
                return Integer.parseInt(s.replace("#", "").trim());
            } catch (NumberFormatException _) {
                // Ignore unparseable PR numbers gracefully
            }
        }
        return null;
    }

    private static Instant parseStartedAt(Map<String, Object> map) {
        var startedStr = JsonTree.string(map, FIELD_STARTED_AT);
        if (startedStr == null) startedStr = JsonTree.string(map, "started");
        if (startedStr != null) {
            try {
                return Instant.parse(startedStr);
            } catch (DateTimeParseException _) {
                return Instant.now();
            }
        }
        return Instant.now();
    }

    private record ParsedEnded(Instant instant, @Nullable String raw) {
    }

    private static ParsedEnded parseEndedAt(Map<String, Object> map) {
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
        return new ParsedEnded(endedAt, rawEnded);
    }

    private static long parseDuration(Map<String, Object> map) {
        long duration = getLong(map.get(FIELD_DURATION_SECONDS));
        if (duration == 0) duration = getLong(map.get("run_duration_seconds"));
        return duration;
    }

    private static List<DeliverableSummary> parseDeliverables(Map<String, Object> map) {
        var list = new ArrayList<DeliverableSummary>();
        var delivObj = map.get(FIELD_DELIVERABLES);
        if (delivObj instanceof List<?> dl) {
            for (var elem : dl) {
                var m = JsonTree.asObject(elem);
                if (m != null) list.add(DeliverableSummary.fromMap(m));
            }
        }
        return list;
    }

    private static List<ReplanningRecord> parseReplanning(Map<String, Object> map) {
        var list = new ArrayList<ReplanningRecord>();
        var replanObj = map.get(FIELD_REPLANNING);
        if (replanObj == null) replanObj = map.get("replanning");
        if (replanObj instanceof List<?> rl) {
            for (var elem : rl) {
                var m = JsonTree.asObject(elem);
                if (m != null) list.add(ReplanningRecord.fromMap(m));
            }
        }
        return list;
    }

    private static List<DecisionRecord> parseDecisions(Map<String, Object> map) {
        var list = new ArrayList<DecisionRecord>();
        var decObj = map.get(FIELD_DECISIONS);
        if (decObj == null) decObj = map.get("decisions");
        if (decObj instanceof List<?> dcl) {
            for (var elem : dcl) {
                var m = JsonTree.asObject(elem);
                if (m != null) list.add(DecisionRecord.fromMap(m));
            }
        }
        return list;
    }

    private static List<OwnCallRecord> parseOwnCalls(Map<String, Object> map) throws IOException {
        if (!map.containsKey(FIELD_OWN_CALLS)) {
            return List.of();
        }
        var ownCallsObj = map.get(FIELD_OWN_CALLS);
        if (!(ownCallsObj instanceof List<?> ocl)) {
            throw new JsonFormatException("Expected an array for " + FIELD_OWN_CALLS);
        }
        var list = new ArrayList<OwnCallRecord>();
        for (var elem : ocl) {
            var m = JsonTree.asObject(elem);
            if (m == null) {
                throw new JsonFormatException("Expected an object in " + FIELD_OWN_CALLS);
            }
            list.add(OwnCallRecord.fromMap(m));
        }
        return list;
    }

    private static @Nullable ChangeMapRecord parseChangeMap(Map<String, Object> map) throws IOException {
        if (!map.containsKey(FIELD_CHANGE_MAP)) {
            return null;
        }
        var changeMap = JsonTree.asObject(map.get(FIELD_CHANGE_MAP));
        if (changeMap == null) {
            throw new JsonFormatException("Expected an object for " + FIELD_CHANGE_MAP);
        }
        return ChangeMapRecord.fromMap(changeMap);
    }

    private static @Nullable TokenUsageRecord parseTokenUsage(Map<String, Object> map) {
        var tuObj = map.get(FIELD_TOKEN_USAGE);
        if (tuObj == null) tuObj = map.get("tokens");
        if (tuObj != null && JsonTree.asObject(tuObj) != null) {
            return TokenUsageRecord.fromMap(JsonTree.asObject(tuObj));
        }
        return null;
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
