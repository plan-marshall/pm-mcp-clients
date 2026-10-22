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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonNumber;
import de.planmarshall.api.json.JsonTree;

/**
 * Token usage report for an ended plan (tokens, not money).
 *
 * @param totalTokens       total token figure
 * @param inputTokens       total input/cache tokens
 * @param outputTokens      total output/reasoning tokens
 * @param durationSeconds   total plan duration seconds
 * @param workersCount      number of workers used
 * @param modelsCount       number of models used
 * @param tasksMeasured     count of tasks whose usage was measured
 * @param tasksTotal        total count of executed tasks
 * @param isFloor           true if token count is a lower-bound floor (≥)
 * @param unmeasuredReason  explanation when tasks were not measured
 * @param byPhase           breakdown rows by phase
 * @param byRole            breakdown rows by role
 * @param byModel           breakdown rows by model
 * @param wasteRecords      breakdown of wasted tokens
 * @param totalWasteTokens  total wasted tokens
 * @param wasteSharePercent share of waste in total or measured tokens
 */
public record TokenUsageRecord(
        long totalTokens,
        long inputTokens,
        long outputTokens,
        long durationSeconds,
        int workersCount,
        int modelsCount,
        int tasksMeasured,
        int tasksTotal,
        boolean isFloor,
        @Nullable String unmeasuredReason,
        List<TokenBreakdownItem> byPhase,
        List<TokenBreakdownItem> byRole,
        List<TokenBreakdownItem> byModel,
        List<TokenWasteRecord> wasteRecords,
        long totalWasteTokens,
        int wasteSharePercent
) {
    public long total() {
        return totalTokens;
    }

    public long input() {
        return inputTokens;
    }

    public long output() {
        return outputTokens;
    }

    public boolean floor() {
        return isFloor;
    }

    private static final String FIELD_TOTAL_TOKENS = "total_tokens";
    private static final String FIELD_INPUT_TOKENS = "input_tokens";
    private static final String FIELD_OUTPUT_TOKENS = "output_tokens";
    private static final String FIELD_DURATION_SECONDS = "duration_seconds";
    private static final String FIELD_WORKERS_COUNT = "workers_count";
    private static final String FIELD_MODELS_COUNT = "models_count";
    private static final String FIELD_TASKS_MEASURED = "tasks_measured";
    private static final String FIELD_TASKS_TOTAL = "tasks_total";
    private static final String FIELD_IS_FLOOR = "is_floor";
    private static final String FIELD_UNMEASURED_REASON = "unmeasured_reason";
    private static final String FIELD_BY_PHASE = "by_phase";
    private static final String FIELD_BY_ROLE = "by_role";
    private static final String FIELD_BY_MODEL = "by_model";
    private static final String FIELD_WASTE_RECORDS = "waste_records";
    private static final String FIELD_TOTAL_WASTE_TOKENS = "total_waste_tokens";
    private static final String FIELD_WASTE_SHARE_PERCENT = "waste_share_percent";

    public TokenUsageRecord {
        byPhase = byPhase != null ? Collections.unmodifiableList(new ArrayList<>(byPhase)) : List.of();
        byRole = byRole != null ? Collections.unmodifiableList(new ArrayList<>(byRole)) : List.of();
        byModel = byModel != null ? Collections.unmodifiableList(new ArrayList<>(byModel)) : List.of();
        wasteRecords = wasteRecords != null ? Collections.unmodifiableList(new ArrayList<>(wasteRecords)) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_TOTAL_TOKENS, totalTokens);
        map.put(FIELD_INPUT_TOKENS, inputTokens);
        map.put(FIELD_OUTPUT_TOKENS, outputTokens);
        map.put(FIELD_DURATION_SECONDS, durationSeconds);
        map.put(FIELD_WORKERS_COUNT, workersCount);
        map.put(FIELD_MODELS_COUNT, modelsCount);
        map.put(FIELD_TASKS_MEASURED, tasksMeasured);
        map.put(FIELD_TASKS_TOTAL, tasksTotal);
        map.put(FIELD_IS_FLOOR, isFloor);
        if (unmeasuredReason != null) {
            map.put(FIELD_UNMEASURED_REASON, unmeasuredReason);
        }
        map.put(FIELD_BY_PHASE, byPhase.stream().map(TokenBreakdownItem::toMap).toList());
        map.put(FIELD_BY_ROLE, byRole.stream().map(TokenBreakdownItem::toMap).toList());
        map.put(FIELD_BY_MODEL, byModel.stream().map(TokenBreakdownItem::toMap).toList());
        map.put(FIELD_WASTE_RECORDS, wasteRecords.stream().map(TokenWasteRecord::toMap).toList());
        map.put(FIELD_TOTAL_WASTE_TOKENS, totalWasteTokens);
        map.put(FIELD_WASTE_SHARE_PERCENT, wasteSharePercent);
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed token usage record
     */
    public static TokenUsageRecord fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        long total = getLong(map.get(FIELD_TOTAL_TOKENS));
        if (total == 0L) total = getLong(map.get("total"));
        long input = getLong(map.get(FIELD_INPUT_TOKENS));
        if (input == 0L) input = getLong(map.get("input"));
        long output = getLong(map.get(FIELD_OUTPUT_TOKENS));
        if (output == 0L) output = getLong(map.get("output"));
        long duration = getLong(map.get(FIELD_DURATION_SECONDS));
        if (duration == 0L) duration = getLong(map.get("duration"));
        int workers = (int) getLong(map.get(FIELD_WORKERS_COUNT));
        int models = (int) getLong(map.get(FIELD_MODELS_COUNT));
        int measured = (int) getLong(map.get(FIELD_TASKS_MEASURED));
        int tasksTotal = (int) getLong(map.get(FIELD_TASKS_TOTAL));
        boolean floor = Boolean.TRUE.equals(map.get(FIELD_IS_FLOOR)) || Boolean.TRUE.equals(map.get("floor"));
        var reason = JsonTree.string(map, FIELD_UNMEASURED_REASON);

        var cov = parseCoverage(map, workers, models, tasksTotal, measured, reason);

        var byPhase = parseBreakdownList(map.get(FIELD_BY_PHASE));
        var byRole = parseBreakdownList(map.get(FIELD_BY_ROLE));
        var byModel = parseBreakdownList(map.get(FIELD_BY_MODEL));

        var waste = parseWaste(map);

        return new TokenUsageRecord(total, input, output, duration, cov.workers(), cov.models(), cov.measured(), cov.tasksTotal(), floor, cov.reason(), byPhase, byRole, byModel, waste.list(), waste.totalWaste(), waste.wasteShare());
    }

    private record CoverageInfo(int workers, int models, int tasksTotal, int measured, @Nullable String reason) {
    }

    private static CoverageInfo parseCoverage(Map<String, Object> map, int workers, int models, int tasksTotal, int measured, @Nullable String reason) {
        if (map.get("coverage") instanceof Map<?, ?> cm) {
            if (workers == 0) workers = (int) getLong(cm.get("workers"));
            if (models == 0) models = (int) getLong(cm.get("models"));
            if (tasksTotal == 0) tasksTotal = (int) getLong(cm.get("total_tasks"));
            int unmeasured = (int) getLong(cm.get("unmeasured"));
            if (measured == 0 && tasksTotal > 0) measured = tasksTotal - unmeasured;
            if (reason == null) reason = JsonTree.string(JsonTree.asObject(cm), "reason");
        }
        return new CoverageInfo(workers, models, tasksTotal, measured, reason);
    }

    private record WasteInfo(List<TokenWasteRecord> list, long totalWaste, int wasteShare) {
    }

    private static WasteInfo parseWaste(Map<String, Object> map) {
        var wasteList = parseWasteRecords(map.get(FIELD_WASTE_RECORDS));
        long totalWaste = getLong(map.get(FIELD_TOTAL_WASTE_TOKENS));
        int wasteShare = (int) getLong(map.get(FIELD_WASTE_SHARE_PERCENT));
        return applyLegacyWaste(map.get("waste"), wasteList, totalWaste, wasteShare);
    }

    private static List<TokenWasteRecord> parseWasteRecords(Object wasteObj) {
        if (!(wasteObj instanceof List<?> wl)) {
            return new ArrayList<>();
        }
        var list = new ArrayList<TokenWasteRecord>(wl.size());
        for (var elem : wl) {
            var m = JsonTree.asObject(elem);
            if (m != null) {
                list.add(TokenWasteRecord.fromMap(m));
            }
        }
        return list;
    }

    private static WasteInfo applyLegacyWaste(Object wasteObj, List<TokenWasteRecord> wasteList, long totalWaste, int wasteShare) {
        long effectiveTotal = totalWaste;
        int effectiveShare = wasteShare;
        if (wasteObj instanceof Map<?, ?> wm) {
            var wasteMap = JsonTree.asObject(wm);
            if (wasteMap != null) {
                if (effectiveTotal == 0L) effectiveTotal = getLong(wasteMap.get("total"));
                if (effectiveShare == 0) effectiveShare = (int) getLong(wasteMap.get("share_percent"));
                if (wasteList.isEmpty()) {
                    populateLegacyWaste(wasteMap, wasteList);
                }
            }
        }
        return new WasteInfo(wasteList, effectiveTotal, effectiveShare);
    }

    private static void populateLegacyWaste(Map<String, Object> wasteMap, List<TokenWasteRecord> wasteList) {
        long noProductTokens = getLong(wasteMap.get("no_product_tokens"));
        int noProductTasks = (int) getLong(wasteMap.get("no_product_tasks"));
        if (noProductTokens > 0 || noProductTasks > 0) {
            wasteList.add(new TokenWasteRecord("no_product", noProductTokens, 0, noProductTasks, null));
        }
        long retriesTokens = getLong(wasteMap.get("retries_tokens"));
        var retriesCauses = wasteMap.get("retries_causes");
        var retriesCausesStr = retriesCauses instanceof List<?> rcl ? String.join(" · ", rcl.stream().map(Object::toString).toList()) : null;
        if (retriesTokens > 0) {
            wasteList.add(new TokenWasteRecord("retries", retriesTokens, 0, 1, retriesCausesStr));
        }
        long remediationTokens = getLong(wasteMap.get("remediation_tokens"));
        var remediationCauses = wasteMap.get("remediation_causes");
        var remediationCausesStr = remediationCauses instanceof List<?> rcl ? String.join(" · ", rcl.stream().map(Object::toString).toList()) : null;
        if (remediationTokens > 0) {
            wasteList.add(new TokenWasteRecord("remediation", remediationTokens, 0, 1, remediationCausesStr));
        }
    }


    private static List<TokenBreakdownItem> parseBreakdownList(Object listObj) {
        if (!(listObj instanceof List<?> list)) return List.of();
        var result = new ArrayList<TokenBreakdownItem>(list.size());
        for (var elem : list) {
            var m = JsonTree.asObject(elem);
            if (m != null) result.add(TokenBreakdownItem.fromMap(m));
        }
        return result;
    }

    private static long getLong(Object obj) {
        if (obj instanceof JsonNumber jn) return jn.longValue();
        if (obj instanceof Number n) return n.longValue();
        return 0L;
    }
}
