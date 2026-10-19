/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.onboarding;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * What one harness and version report of their token usage (Client Profiles, Tokens, Not Money;
 * UC-ONB-4): the {@link TokenUsageCoverage} summary and, per {@link UsageFigure}, whether the figure
 * is {@link UsageFigureStatus#REPORTED reported}, {@link UsageFigureStatus#NOT_APPLICABLE not
 * applicable}, or {@link UsageFigureStatus#NOT_REPORTED not reported}. It is the "why?" behind the
 * coverage line of a harness, and the result of the verification stage {@code usage}.
 *
 * <p>Only the figures the report names are stated; a figure the report does not name carries no
 * status. The summary and the named figures are held consistent and fail closed: a {@code complete}
 * report names no missing figure, a {@code partial} one names at least one, and a
 * {@code not_reported} one reports no figure.
 *
 * @param coverage what the harness and version report of their token usage in summary
 * @param figures  the status of each named figure; a figure that is not named carries none
 */
public record HarnessUsageReport(
        TokenUsageCoverage coverage,
        Map<UsageFigure, UsageFigureStatus> figures
) {
    public HarnessUsageReport {
        coverage = coverage != null ? coverage : TokenUsageCoverage.NOT_REPORTED;
        var copy = new EnumMap<UsageFigure, UsageFigureStatus>(UsageFigure.class);
        if (figures != null) {
            copy.putAll(figures);
        }
        figures = Collections.unmodifiableMap(copy);
        if (coverage == TokenUsageCoverage.COMPLETE && hasStatus(figures, UsageFigureStatus.NOT_REPORTED)) {
            throw new IllegalArgumentException("A complete usage report names no missing figure");
        }
        if (coverage == TokenUsageCoverage.PARTIAL && !hasStatus(figures, UsageFigureStatus.NOT_REPORTED)) {
            throw new IllegalArgumentException("A partial usage report names at least one missing figure");
        }
        if (coverage == TokenUsageCoverage.NOT_REPORTED && hasStatus(figures, UsageFigureStatus.REPORTED)) {
            throw new IllegalArgumentException("A not-reported usage report reports no figure");
        }
    }

    private static boolean hasStatus(Map<UsageFigure, UsageFigureStatus> figures, UsageFigureStatus status) {
        return figures.containsValue(status);
    }

    /**
     * @param figure the figure to look up
     * @return the status of the figure, or {@code null} when the report does not name it
     */
    public @Nullable UsageFigureStatus statusOf(UsageFigure figure) {
        Objects.requireNonNull(figure, "figure");
        return figures.get(figure);
    }

    /**
     * @return the figures the report names as missing, in catalogue order, as their wire values
     */
    public List<String> missingFigures() {
        return Arrays.stream(UsageFigure.values())
                .filter(figure -> figures.get(figure) == UsageFigureStatus.NOT_REPORTED)
                .map(UsageFigure::wireValue)
                .toList();
    }

    /**
     * Converts to a JSON tree map.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("coverage", coverage.wireValue());
        if (!figures.isEmpty()) {
            var figureMap = new LinkedHashMap<String, Object>();
            for (var entry : figures.entrySet()) {
                figureMap.put(entry.getKey().wireValue(), entry.getValue().wireValue());
            }
            map.put("figures", figureMap);
        }
        return map;
    }

    /**
     * Serializes to a JSON string.
     *
     * @return JSON string
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses from a JSON tree map.
     *
     * @param map map representation
     * @return parsed report
     * @throws IOException on format failure
     */
    public static HarnessUsageReport fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var coverage = TokenUsageCoverage.fromWire(JsonTree.string(map, "coverage"));
        var figures = new EnumMap<UsageFigure, UsageFigureStatus>(UsageFigure.class);
        var figureMap = JsonTree.asObject(map.get("figures"));
        if (figureMap != null) {
            for (var entry : figureMap.entrySet()) {
                var figure = UsageFigure.fromWire(entry.getKey());
                if (figure == null) {
                    throw new JsonFormatException("Unknown usage figure in HarnessUsageReport: " + entry.getKey());
                }
                var status = UsageFigureStatus.fromWire(
                        entry.getValue() instanceof String text ? text : null);
                if (status == null) {
                    throw new JsonFormatException(
                            "Unknown figure status in HarnessUsageReport: " + entry.getValue());
                }
                figures.put(figure, status);
            }
        }
        try {
            return new HarnessUsageReport(coverage, figures);
        } catch (IllegalArgumentException e) {
            throw new JsonFormatException("Invalid HarnessUsageReport: " + e.getMessage(), e);
        }
    }

    /**
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed report
     * @throws IOException on format failure
     */
    public static HarnessUsageReport fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for HarnessUsageReport");
        }
        return fromMap(obj);
    }
}
