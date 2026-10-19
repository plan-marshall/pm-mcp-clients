/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * An epic as a resource (UC-CTL-2, UC-CTL-3, UC-PLN-5; specification <em>The Epic as a
 * Resource</em>): its header, its workstreams and plan rows, its sources, landings, registers,
 * tokens, and history.
 *
 * <p>The header names the project one step away ({@link #projectId()}, {@link #displayName()}) and
 * the values of {@code auto_start} and {@code overlap_max_files} in force; an ended epic names its
 * {@link #terminalReason()} from {@link EpicTerminalReason}.
 *
 * @param epicId           the epic identifier
 * @param projectId        the project's id
 * @param displayName      the project's display name
 * @param link             the epic's link at the provider, or {@code null}
 * @param state            the epic's state (running, waiting for you, held, landing, ended)
 * @param autoStart        whether the epic starts its plans by itself
 * @param overlapMaxFiles  how many files a plan may share with a running one and still start
 * @param scope            how many plans may run at once
 * @param running          how many plans run now
 * @param terminalReason   the epic's end where it has ended, or {@code null}
 * @param workstreams      the workstreams with their plan rows
 * @param sources          the epic's sources, as stored
 * @param landings         how many of the epic's plans have landed
 * @param findings         the findings register's counts
 * @param watches          the watches register's counts
 * @param tokens           the epic's tokens so far
 * @param events           the epic's history, newest first
 */
public record EpicResource(
        String epicId,
        String projectId,
        String displayName,
        @Nullable String link,
        String state,
        boolean autoStart,
        int overlapMaxFiles,
        int scope,
        int running,
        @Nullable String terminalReason,
        List<EpicWorkstream> workstreams,
        List<String> sources,
        int landings,
        RegisterSummary findings,
        RegisterSummary watches,
        EpicTokens tokens,
        List<EpicEvent> events
) {

    public EpicResource {
        Objects.requireNonNull(epicId, "epicId");
        Objects.requireNonNull(projectId, "projectId");
        displayName = displayName != null ? displayName : projectId;
        state = state == null || state.isBlank() ? "running" : state;
        workstreams = workstreams != null ? List.copyOf(workstreams) : List.of();
        sources = sources != null ? List.copyOf(sources) : List.of();
        findings = findings != null ? findings : new RegisterSummary(0, 0, 0);
        watches = watches != null ? watches : new RegisterSummary(0, 0, 0);
        tokens = tokens != null ? tokens : new EpicTokens(TokenFigure.notMeasured(), TokenFigure.notMeasured(), List.of());
        events = events != null ? List.copyOf(events) : List.of();
    }

    /**
     * @return {@code true} if the epic has ended
     */
    public boolean hasEnded() {
        return "ended".equals(state);
    }

    /**
     * @return the terminal reason as a closed enum, or {@code null}
     */
    public @Nullable EpicTerminalReason end() {
        return EpicTerminalReason.fromValue(terminalReason);
    }

    /**
     * @return the count of staged plans across all workstreams
     */
    public int stagedCount() {
        return workstreams.stream()
                .flatMap(workstream -> workstream.rows().stream())
                .mapToInt(row -> "staged".equals(row.status()) ? 1 : 0)
                .sum();
    }

    /**
     * @return the map representation
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("epic_id", epicId);
        map.put("project_id", projectId);
        map.put("display_name", displayName);
        if (link != null) {
            map.put("link", link);
        }
        map.put("state", state);
        map.put("auto_start", autoStart);
        map.put("overlap_max_files", overlapMaxFiles);
        map.put("scope", scope);
        map.put("running", running);
        if (terminalReason != null) {
            map.put("terminal_reason", terminalReason);
        }
        map.put("workstreams", workstreams.stream().map(EpicWorkstream::toMap).toList());
        map.put("sources", sources);
        map.put("landings", landings);
        map.put("findings", findings.toMap());
        map.put("watches", watches.toMap());
        map.put("tokens", tokens.toMap());
        map.put("events", events.stream().map(EpicEvent::toMap).toList());
        return map;
    }

    /**
     * @return the compact JSON representation
     */
    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * @param map the input map
     * @return the parsed epic
     * @throws IOException if a required field is missing
     */
    public static EpicResource fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var epicId = JsonTree.string(map, "epic_id");
        var projectId = JsonTree.string(map, "project_id");
        if (epicId == null || projectId == null) {
            throw new JsonFormatException("Missing required fields for EpicResource: epic_id, project_id");
        }
        return new EpicResource(
                epicId,
                projectId,
                JsonTree.string(map, "display_name"),
                JsonTree.string(map, "link"),
                JsonTree.string(map, "state"),
                ControlJson.bool(map, "auto_start"),
                integer(map, "overlap_max_files"),
                integer(map, "scope"),
                integer(map, "running"),
                JsonTree.string(map, "terminal_reason"),
                readWorkstreams(map),
                ControlJson.readStrings(map, "sources"),
                integer(map, "landings"),
                summary(map.get("findings")),
                summary(map.get("watches")),
                tokens(map.get("tokens")),
                readEvents(map));
    }

    /**
     * @param json the JSON object string
     * @return the parsed epic
     * @throws IOException if the value is not an object
     */
    public static EpicResource fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for EpicResource");
        }
        return fromMap(obj);
    }

    private static int integer(Map<String, Object> map, String key) {
        var value = ControlJson.optInteger(map, key);
        return value != null ? value : 0;
    }

    private static RegisterSummary summary(@Nullable Object raw) throws IOException {
        var object = JsonTree.asObject(raw);
        return object != null ? RegisterSummary.fromMap(object) : new RegisterSummary(0, 0, 0);
    }

    private static EpicTokens tokens(@Nullable Object raw) throws IOException {
        var object = JsonTree.asObject(raw);
        return object != null ? EpicTokens.fromMap(object)
                : new EpicTokens(TokenFigure.notMeasured(), TokenFigure.notMeasured(), List.of());
    }

    private static List<EpicWorkstream> readWorkstreams(Map<String, Object> map) throws IOException {
        var workstreams = new ArrayList<EpicWorkstream>();
        for (var object : ControlJson.readObjects(map, "workstreams")) {
            workstreams.add(EpicWorkstream.fromMap(object));
        }
        return workstreams;
    }

    private static List<EpicEvent> readEvents(Map<String, Object> map) throws IOException {
        var events = new ArrayList<EpicEvent>();
        for (var object : ControlJson.readObjects(map, "events")) {
            events.add(EpicEvent.fromMap(object));
        }
        return events;
    }
}
