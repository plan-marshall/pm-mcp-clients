/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.designer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * One finding of a workflow-unit validation (UC-PLN-3).
 * <p>
 * A finding carries the stage it arose in, its rule (the frontmatter rule, {@code Rule 6}, or a
 * compiled invariant such as {@code INV-GEN-4}), its line and column, its severity, and the
 * runtime's message. The designer marks the line in the text and the node in the structure.
 *
 * @param stage    the validation stage the finding arose in ({@link #STAGE_PARSE}, {@link #STAGE_RULES}, {@link #STAGE_INVARIANTS}, or {@link #STAGE_COMPOSITION})
 * @param rule     the rule or invariant the finding cites
 * @param line     the line the finding is at (1-based), or 0 when unknown
 * @param column   the column the finding is at (1-based), or 0 when unknown
 * @param severity the severity ({@link #SEVERITY_ERROR} or {@link #SEVERITY_WARNING})
 * @param message  the runtime's message
 * @param project  the project the finding names when the check ran against the machine target, else {@code null}
 */
public record WorkflowFinding(
        String stage,
        String rule,
        int line,
        int column,
        String severity,
        String message,
        String project
) {
    /** Stage: the text is parsed. */
    public static final String STAGE_PARSE = "parse";
    /** Stage: the static AST verification rules. */
    public static final String STAGE_RULES = "rules";
    /** Stage: the compiled invariants of a replacing unit. */
    public static final String STAGE_INVARIANTS = "invariants";
    /** Stage: the composition against a project. */
    public static final String STAGE_COMPOSITION = "composition";

    /** Severity: the unit cannot be placed. */
    public static final String SEVERITY_ERROR = "error";
    /** Severity: an advisory finding. */
    public static final String SEVERITY_WARNING = "warning";

    private static final String FIELD_STAGE = "stage";
    private static final String FIELD_RULE = "rule";
    private static final String FIELD_LINE = "line";
    private static final String FIELD_COLUMN = "column";
    private static final String FIELD_SEVERITY = "severity";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_PROJECT = "project";

    /**
     * Compact constructor validating required properties.
     */
    public WorkflowFinding {
        stage = stage != null ? stage : STAGE_RULES;
        rule = rule != null ? rule : "";
        severity = severity != null ? severity : SEVERITY_ERROR;
        Objects.requireNonNull(message, FIELD_MESSAGE);
    }

    /**
     * @return this finding as a JSON tree
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put(FIELD_STAGE, stage);
        map.put(FIELD_RULE, rule);
        map.put(FIELD_LINE, line);
        map.put(FIELD_COLUMN, column);
        map.put(FIELD_SEVERITY, severity);
        map.put(FIELD_MESSAGE, message);
        if (project != null) {
            map.put(FIELD_PROJECT, project);
        }
        return map;
    }

    /**
     * @return this finding as compact JSON
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Parses a finding from a JSON tree.
     *
     * @param map the input tree
     * @return the finding
     * @throws IOException on a malformed tree
     */
    public static WorkflowFinding fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var message = JsonTree.string(map, FIELD_MESSAGE);
        if (message == null) {
            throw new JsonFormatException("WorkflowFinding requires a message");
        }
        var line = JsonTree.integer(map, FIELD_LINE);
        var column = JsonTree.integer(map, FIELD_COLUMN);
        return new WorkflowFinding(
                JsonTree.string(map, FIELD_STAGE),
                JsonTree.string(map, FIELD_RULE),
                line != null ? line : 0,
                column != null ? column : 0,
                JsonTree.string(map, FIELD_SEVERITY),
                message,
                JsonTree.string(map, FIELD_PROJECT)
        );
    }

    /**
     * Parses a finding from JSON.
     *
     * @param json the JSON text
     * @return the finding
     * @throws IOException on a malformed document
     */
    public static WorkflowFinding fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected a JSON object for WorkflowFinding");
        }
        return fromMap(obj);
    }

    /**
     * Parses a list of findings from a JSON array string.
     *
     * @param json the JSON text
     * @return the findings
     * @throws IOException on a malformed document
     */
    public static List<WorkflowFinding> parseList(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        if (!(parsed instanceof List<?> list)) {
            throw new JsonFormatException("Expected a JSON array for a WorkflowFinding list");
        }
        var result = new ArrayList<WorkflowFinding>(list.size());
        for (var item : list) {
            var map = JsonTree.asObject(item);
            if (map != null) {
                result.add(fromMap(map));
            }
        }
        return result;
    }

    /**
     * Serializes a list of findings to a JSON array string.
     *
     * @param findings the findings
     * @return the JSON text
     */
    public static String writeList(List<WorkflowFinding> findings) {
        return JsonTree.write(findings.stream().map(WorkflowFinding::toMap).toList());
    }
}
