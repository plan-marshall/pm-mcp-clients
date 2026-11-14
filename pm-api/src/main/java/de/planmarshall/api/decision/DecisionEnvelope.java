/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decision;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Self-contained envelope of a decision delivered to an operator (UC-PLN-2).
 *
 * @param id            unique identifier (e.g. {@code "Q-007"} or {@code "WR-2"})
 * @param revision      revision number (increments on modification)
 * @param title         concise title
 * @param header        optional category or context header
 * @param subject       subject identifier or path (e.g. {@code "plan fix-relay"} or {@code "setup of pm-mcp-clients"})
 * @param scope         scope category ({@code PLAN}, {@code EPIC}, {@code PROJECT_SETUP}, {@code MACHINE})
 * @param askingPhase   phase or setup step that asked the decision
 * @param askingRole    role asking the question
 * @param blocks        what this decision blocks ({@code "plan"}, {@code "epic"}, {@code "setup"}, {@code "entry"}, {@code "task"}, {@code "none"})
 * @param reversibility reversibility rating
 * @param description   full explanation or rationale
 * @param evidence      structured evidence payload (files, lines, diffs, metrics)
 * @param parts         constituent decision parts
 * @param deliveredAt   timestamp when delivered to the operator
 * @param isWaiver      whether this represents a gate waiver request
 * @param waiverGate    the gate under waiver if applicable
 * @param waiverResult  the gate evaluation result if applicable
 * @param waiverReason  the reason submitted by the asking role if applicable
 * @param project       the project the decision belongs to, if the runtime names one
 * @param plan          the plan the decision belongs to, if the runtime names one
 */
public record DecisionEnvelope(
        String id,
        int revision,
        String title,
        @Nullable String header,
        String subject,
        DecisionScope scope,
        @Nullable String askingPhase,
        @Nullable String askingRole,
        String blocks,
        Reversibility reversibility,
        @Nullable String description,
        @Nullable Map<String, Object> evidence,
        List<DecisionPart> parts,
        @Nullable Instant deliveredAt,
        boolean isWaiver,
        @Nullable String waiverGate,
        @Nullable String waiverResult,
        @Nullable String waiverReason,
        @Nullable String project,
        @Nullable String plan
) {
    public DecisionEnvelope {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(reversibility, "reversibility");
        parts = parts != null ? List.copyOf(parts) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("revision", revision);
        map.put("title", title);
        if (header != null) map.put("header", header);
        map.put("subject", subject);
        map.put("scope", scope.value());
        if (askingPhase != null) map.put("asking_phase", askingPhase);
        if (askingRole != null) map.put("asking_role", askingRole);
        map.put("blocks", blocks);
        map.put("reversibility", reversibility.value());
        if (description != null) map.put("description", description);
        if (evidence != null) map.put("evidence", evidence);

        var partsList = new ArrayList<Map<String, Object>>();
        for (var p : parts) {
            partsList.add(p.toMap());
        }
        map.put("parts", partsList);

        if (deliveredAt != null) {
            map.put("delivered_at", deliveredAt.toString());
        }
        map.put("is_waiver", isWaiver);
        if (waiverGate != null) map.put("waiver_gate", waiverGate);
        if (waiverResult != null) map.put("waiver_result", waiverResult);
        if (waiverReason != null) map.put("waiver_reason", waiverReason);
        if (project != null) map.put("project", project);
        if (plan != null) map.put("plan", plan);

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
     * Parses from a JSON string.
     *
     * @param json JSON string
     * @return parsed DecisionEnvelope
     * @throws IOException on error
     */
    public static DecisionEnvelope fromJson(String json) throws IOException {
        var parsed = JsonTree.parse(json);
        var map = JsonTree.asObject(parsed);
        if (map == null) throw new JsonFormatException("Expected JSON object for DecisionEnvelope");
        return fromMap(map);
    }

    /**
     * Parses from a JSON map.
     *
     * @param map tree map
     * @return parsed DecisionEnvelope
     * @throws IOException if the message carries no id: a decision without an id cannot be answered
     */
    public static DecisionEnvelope fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var id = JsonTree.string(map, "id");
        if (id == null || id.isBlank()) {
            throw new JsonFormatException("Missing id in DecisionEnvelope");
        }
        var rev = Objects.requireNonNullElse(JsonTree.integer(map, "revision"), 1);
        var title = Objects.requireNonNullElse(JsonTree.string(map, "title"), "");
        var subject = JsonTree.string(map, "subject");
        if (subject == null) {
            var plan = JsonTree.string(map, "plan");
            subject = plan != null ? "plan " + plan : id;
        }

        var scopeStr = JsonTree.string(map, "scope");
        var scope = scopeStr != null ? DecisionScope.fromValue(scopeStr) : DecisionScope.PLAN;
        var revStr = JsonTree.string(map, "reversibility");
        var reversibility = revStr != null ? Reversibility.fromValue(revStr) : Reversibility.REVERSIBLE;
        var isWaiver = Boolean.TRUE.equals(JsonTree.bool(map, "is_waiver")) || id.startsWith("WR-")
                || "waiver".equalsIgnoreCase(JsonTree.string(map, "kind"));

        return new DecisionEnvelope(
                id, rev, title, JsonTree.string(map, "header"), subject, scope,
                firstString(map, "asking_phase", "phase"), firstString(map, "asking_role", "role"),
                Objects.requireNonNullElse(JsonTree.string(map, "blocks"), "plan"), reversibility,
                JsonTree.string(map, "description"), evidenceOf(map.get("evidence")), partsOf(map.get("parts")),
                deliveredAt(JsonTree.string(map, "delivered_at")),
                isWaiver, firstString(map, "waiver_gate", "gate"), firstString(map, "waiver_result", "result"),
                firstString(map, "waiver_reason", "reason"),
                JsonTree.string(map, "project"), JsonTree.string(map, "plan")
        );
    }

    /** The member under the current name, else the one under the legacy name. */
    private static @Nullable String firstString(Map<String, Object> map, String current, String legacy) {
        var value = JsonTree.string(map, current);
        return value != null ? value : JsonTree.string(map, legacy);
    }

    private static @Nullable Map<String, Object> evidenceOf(@Nullable Object evidence) {
        if (!(evidence instanceof Map<?, ?> source)) {
            return null;
        }
        var copy = new LinkedHashMap<String, Object>();
        for (var entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }

    private static List<DecisionPart> partsOf(@Nullable Object parts) {
        var result = new ArrayList<DecisionPart>();
        if (parts instanceof List<?> list) {
            for (var item : list) {
                var part = JsonTree.asObject(item);
                if (part != null) {
                    result.add(DecisionPart.fromMap(part));
                }
            }
        }
        return result;
    }

    /**
     * An unreadable delivery time is unknown, never the moment of reading: that would invent how long the
     * decision has waited.
     */
    private static @Nullable Instant deliveredAt(@Nullable String text) {
        if (text == null) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException _) {
            return null;
        }
    }
}
