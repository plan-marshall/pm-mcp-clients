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
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonFormatException;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.PlanActivity;

/**
 * Summary of a running or staged plan within an epic process (UC-PLN-5).
 *
 * @param planId       plan identifier
 * @param title        plan title
 * @param currentPhase phase name (or planned start phase)
 * @param phaseState   the plan's activity state, one wire value of the closed {@link PlanActivity} set:
 *                     {@code running}, {@code pausing}, {@code paused}, {@code waiting_for_you},
 *                     {@code held}, {@code staged}, or {@code ended}
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

    /**
     * Reads the plan's activity state as the closed set of {@link PlanActivity} declares it.
     *
     * @param wire the member as the wire carries it, or {@code null}
     * @return the member's canonical wire value, or {@code null} when it is absent or blank
     * @throws JsonFormatException if the value is no member of the closed set
     */
    private static @Nullable String readPhaseState(@Nullable String wire) throws JsonFormatException {
        if (wire == null || wire.isBlank()) {
            return null;
        }
        var activity = PlanActivity.fromValue(wire);
        if (activity == null) {
            throw new JsonFormatException("Unknown phase_state in EpicProcessPlan: " + wire);
        }
        return activity.value();
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
        var phaseStateWire = JsonTree.string(map, "phase_state");
        var phaseState = readPhaseState(phaseStateWire);
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
