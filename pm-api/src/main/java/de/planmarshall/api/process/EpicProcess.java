/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.process;

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
 * Representation of an epic's process (UC-PLN-5).
 *
 * @param epicId            unique identifier of the epic
 * @param project           display name or ID of parent project
 * @param title             display title
 * @param state             overall epic state
 * @param runningPlans      plans actively executing
 * @param stagedPlans       plans queued/staged for execution
 * @param alongsideLandings active alongside landing descriptions
 * @param tokensSoFar       cumulative token metrics across epic
 * @param reviewReady       whether the completion review is ready
 */
public record EpicProcess(
        String epicId,
        String project,
        String title,
        String state,
        List<EpicProcessPlan> runningPlans,
        List<EpicProcessPlan> stagedPlans,
        List<String> alongsideLandings,
        @Nullable ProcessTokens tokensSoFar,
        boolean reviewReady
) {
    /** Wire key of the project member. */
    private static final String PROJECT_KEY = "project";

    /** The project name a document that carries no project member is rendered with. */
    private static final String DEFAULT_PROJECT = "project";

    public EpicProcess {
        Objects.requireNonNull(epicId, "epicId");
        project = project != null ? project : DEFAULT_PROJECT;
        title = title != null ? title : epicId;
        state = state != null ? state : "running";
        runningPlans = runningPlans != null ? List.copyOf(runningPlans) : List.of();
        stagedPlans = stagedPlans != null ? List.copyOf(stagedPlans) : List.of();
        alongsideLandings = alongsideLandings != null ? List.copyOf(alongsideLandings) : List.of();
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("epic_id", epicId);
        map.put(PROJECT_KEY, project);
        map.put("title", title);
        map.put("state", state);

        var runList = new ArrayList<Map<String, Object>>();
        for (var p : runningPlans) {
            runList.add(p.toMap());
        }
        map.put("running_plans", runList);

        var stagedList = new ArrayList<Map<String, Object>>();
        for (var p : stagedPlans) {
            stagedList.add(p.toMap());
        }
        map.put("staged_plans", stagedList);

        map.put("alongside_landings", alongsideLandings);
        if (tokensSoFar != null) {
            map.put("tokens_so_far", tokensSoFar.toMap());
        }
        if (reviewReady) {
            map.put("review_ready", true);
        }
        return map;
    }

    public String toJson() {
        return JsonTree.write(toMap());
    }

    /**
     * Reads an epic's process from a JSON object.
     * <p>
     * The reader is lenient by design: an entry of {@code running_plans} or {@code staged_plans} that is
     * not an object, and a {@code null} entry of {@code alongside_landings}, are skipped, not refused.
     * This is deliberate tolerance of shapes the reader does not know, so a newer runtime that adds a
     * kind of entry still shows the rest of the epic; it is not an oversight to be closed. A missing
     * {@code epic_id} is still refused.
     *
     * @param map the JSON object
     * @return the epic's process
     * @throws IOException if the object carries no epic id or a nested record is malformed
     */
    public static EpicProcess fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var epicId = JsonTree.string(map, "epic_id");
        if (epicId == null) {
            epicId = JsonTree.string(map, "id");
        }
        if (epicId == null) {
            throw new JsonFormatException("Missing epic_id in EpicProcess");
        }

        var project = JsonTree.string(map, PROJECT_KEY);
        var title = JsonTree.string(map, "title");
        var state = JsonTree.string(map, "state");

        var runningPlans = readPlans(map.get("running_plans"));
        var stagedPlans = readPlans(map.get("staged_plans"));
        var alongsideLandings = readStrings(map.get("alongside_landings"));

        ProcessTokens tokensSoFar = null;
        var tokensObj = JsonTree.asObject(map.get("tokens_so_far"));
        if (tokensObj != null) {
            tokensSoFar = ProcessTokens.fromMap(tokensObj);
        }

        var reviewReady = Boolean.TRUE.equals(map.get("review_ready"));

        // absent members stay null here: the canonical constructor supplies the defaults
        return new EpicProcess(
                epicId,
                project,
                title,
                state,
                runningPlans,
                stagedPlans,
                alongsideLandings,
                tokensSoFar,
                reviewReady
        );
    }

    /** Reads the object entries of a plan list; an entry that is not an object is skipped. */
    private static List<EpicProcessPlan> readPlans(@Nullable Object raw) throws IOException {
        var plans = new ArrayList<EpicProcessPlan>();
        if (raw instanceof List<?> list) {
            for (var item : list) {
                var pMap = JsonTree.asObject(item);
                if (pMap != null) {
                    plans.add(EpicProcessPlan.fromMap(pMap));
                }
            }
        }
        return plans;
    }

    /** Reads the entries of a list as text; a {@code null} entry is skipped. */
    static List<String> readStrings(@Nullable Object raw) {
        var values = new ArrayList<String>();
        if (raw instanceof List<?> list) {
            for (var item : list) {
                if (item != null) {
                    values.add(item.toString());
                }
            }
        }
        return values;
    }

    public static EpicProcess fromJson(String json) throws IOException {
        var obj = JsonTree.asObject(JsonTree.parse(json));
        if (obj == null) {
            throw new JsonFormatException("Expected JSON object for EpicProcess");
        }
        return fromMap(obj);
    }
}
