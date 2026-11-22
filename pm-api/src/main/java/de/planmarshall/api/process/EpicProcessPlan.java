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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;

/**
 * Summary of a running or staged plan within an epic process (UC-PLN-5).
 *
 * @param planId       plan identifier
 * @param title        plan title
 * @param currentPhase phase name (or planned start phase)
 * @param phaseState   activity state ("running", "staged", "planned", "done")
 * @param tasksDone    number of completed tasks
 * @param tasksTotal   total planned tasks
 * @param running      {@code true} if actively executing, {@code false} if staged
 */
public record EpicProcessPlan(
        String planId,
        String title,
        String currentPhase,
        String phaseState,
        int tasksDone,
        int tasksTotal,
        boolean running
) {
    private static final String RUNNING_KEY = "running";
    private static final String STATE_RUNNING = "running";
    private static final String STATE_STAGED = "staged";

    public EpicProcessPlan {
        Objects.requireNonNull(planId, "planId");
        title = title != null ? title : planId;
        currentPhase = currentPhase != null ? currentPhase : "init";
        phaseState = phaseState != null ? phaseState : defaultPhaseState(running);
    }

    /** A plan that names no phase state is running when it runs, else staged. */
    private static String defaultPhaseState(boolean running) {
        return running ? STATE_RUNNING : STATE_STAGED;
    }

    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("plan_id", planId);
        map.put("title", title);
        map.put("current_phase", currentPhase);
        map.put("phase_state", phaseState);
        map.put("tasks_done", tasksDone);
        map.put("tasks_total", tasksTotal);
        map.put(RUNNING_KEY, running);
        return map;
    }

    public static EpicProcessPlan fromMap(Map<String, Object> map) throws IOException {
        Objects.requireNonNull(map, "map");
        var planId = JsonTree.string(map, "plan_id");
        if (planId == null) {
            planId = JsonTree.string(map, "id");
        }
        if (planId == null) {
            throw new JsonFormatException("Missing plan_id in EpicProcessPlan");
        }
        var title = JsonTree.string(map, "title");
        var currentPhase = JsonTree.string(map, "current_phase");
        var phaseState = JsonTree.string(map, "phase_state");
        var doneNum = JsonTree.number(map, "tasks_done");
        int tasksDone = doneNum != null ? doneNum.intValue() : 0;
        var totNum = JsonTree.number(map, "tasks_total");
        int tasksTotal = totNum != null ? totNum.intValue() : 0;
        var running = Boolean.TRUE.equals(map.get(RUNNING_KEY));

        // absent members stay null here: the canonical constructor supplies the defaults
        return new EpicProcessPlan(
                planId,
                title,
                currentPhase,
                phaseState,
                tasksDone,
                tasksTotal,
                running
        );
    }
}
