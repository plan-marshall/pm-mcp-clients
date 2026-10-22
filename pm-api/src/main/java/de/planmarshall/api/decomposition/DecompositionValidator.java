/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decomposition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.experimental.UtilityClass;

/**
 * The server-side checks of a decomposition ({@code decomposition.check}, UC-STA-6): the surface
 * disjointness of concurrent plans, the acyclicity of the dependency graph, and the integrity of
 * the workstreams.
 * <p>
 * The check is pure: it reads the cut and answers the findings; the empty list means every check
 * passes and the cut can be agreed.
 */
@UtilityClass
public class DecompositionValidator {

    /**
     * Runs every check over the workstreams.
     *
     * @param workstreams the workstreams of the cut
     * @return the findings, in a stable order; empty when every check passes
     */
    public static List<DecompositionCheckFinding> validate(List<WorkstreamRecord> workstreams) {
        var safe = workstreams == null ? List.<WorkstreamRecord>of() : workstreams;
        var findings = new ArrayList<DecompositionCheckFinding>();
        findings.addAll(checkWorkstreamIntegrity(safe));
        findings.addAll(checkSurfaceDisjointness(safe));
        var cycle = dependencyCycle(safe);
        if (cycle != null) {
            findings.add(new DecompositionCheckFinding(CheckSeverity.ERROR,
                    DecompositionCheckFinding.RULE_DEPENDENCY_CYCLE, cycle,
                    "the dependencies form a cycle: " + String.join(" -> ", cycle)));
        }
        return List.copyOf(findings);
    }

    /**
     * @param workstreams the cut
     * @return {@code true} if every check passes
     */
    public static boolean passes(List<WorkstreamRecord> workstreams) {
        return validate(workstreams).stream().noneMatch(finding -> finding.severity() == CheckSeverity.ERROR);
    }

    private static List<DecompositionCheckFinding> checkWorkstreamIntegrity(List<WorkstreamRecord> workstreams) {
        var findings = new ArrayList<DecompositionCheckFinding>();
        var planIds = new LinkedHashMap<String, String>();
        var orders = new LinkedHashMap<Integer, String>();
        for (var workstream : workstreams) {
            var previous = orders.put(workstream.order(), workstream.id());
            if (previous != null) {
                findings.add(new DecompositionCheckFinding(CheckSeverity.WARNING,
                        DecompositionCheckFinding.RULE_WORKSTREAM_INTEGRITY, List.of(previous, workstream.id()),
                        "workstreams " + previous + " and " + workstream.id() + " share the order "
                                + workstream.order()));
            }
            for (var plan : workstream.stagedPlans()) {
                var owner = planIds.put(plan.planId(), workstream.id());
                if (owner != null) {
                    findings.add(new DecompositionCheckFinding(CheckSeverity.ERROR,
                            DecompositionCheckFinding.RULE_WORKSTREAM_INTEGRITY, List.of(plan.planId()),
                            "plan " + plan.planId() + " is staged in " + owner + " and " + workstream.id()));
                }
                if (plan.title().isBlank()) {
                    findings.add(new DecompositionCheckFinding(CheckSeverity.WARNING,
                            DecompositionCheckFinding.RULE_WORKSTREAM_INTEGRITY, List.of(plan.planId()),
                            "the plan " + plan.planId() + " has no title"));
                }
            }
        }
        return findings;
    }

    private static List<DecompositionCheckFinding> checkSurfaceDisjointness(List<WorkstreamRecord> workstreams) {
        var findings = new ArrayList<DecompositionCheckFinding>();
        var plans = startablePlans(workstreams);
        for (int i = 0; i < plans.size(); i++) {
            for (int j = i + 1; j < plans.size(); j++) {
                var first = plans.get(i);
                var second = plans.get(j);
                var shared = new TreeSet<>(first.expectedSurface());
                shared.retainAll(new LinkedHashSet<>(second.expectedSurface()));
                if (!shared.isEmpty()) {
                    findings.add(new DecompositionCheckFinding(CheckSeverity.ERROR,
                            DecompositionCheckFinding.RULE_SURFACE_OVERLAP,
                            List.of(first.planId(), second.planId()),
                            "surface overlaps " + second.planId() + " (" + String.join(", ", shared) + ")"));
                }
            }
        }
        return findings;
    }

    private static List<StagedPlanRecord> startablePlans(List<WorkstreamRecord> workstreams) {
        var plans = new ArrayList<StagedPlanRecord>();
        for (var workstream : workstreams) {
            for (var plan : workstream.stagedPlans()) {
                if (!plan.isDropped() && !plan.isLocked()) {
                    plans.add(plan);
                }
            }
        }
        return plans;
    }

    private static List<String> dependencyCycle(List<WorkstreamRecord> workstreams) {
        var nodes = new LinkedHashSet<String>();
        var edges = new LinkedHashMap<String, List<String>>();
        for (var workstream : workstreams) {
            for (var plan : workstream.stagedPlans()) {
                if (plan.isDropped()) {
                    continue;
                }
                nodes.add(plan.planId());
                edges.put(plan.planId(), plan.dependencies());
            }
        }
        var state = new LinkedHashMap<String, Integer>();
        for (var node : nodes) {
            var cycle = visit(node, edges, nodes, state, new ArrayList<>());
            if (cycle != null) {
                return cycle;
            }
        }
        return null;
    }

    private static List<String> visit(String node, Map<String, List<String>> edges, Set<String> nodes,
                                      Map<String, Integer> state, List<String> path) {
        var current = state.getOrDefault(node, 0);
        if (current == 1) {
            // A back edge: the cycle is the path from the node's first appearance.
            var cycle = new ArrayList<>(path.subList(path.indexOf(node), path.size()));
            cycle.add(node);
            return cycle;
        }
        if (current == 2) {
            return null;
        }
        state.put(node, 1);
        path.add(node);
        for (var dependency : edges.getOrDefault(node, List.of())) {
            if (!nodes.contains(dependency)) {
                continue;
            }
            var cycle = visit(dependency, edges, nodes, state, path);
            if (cycle != null) {
                return cycle;
            }
        }
        path.remove(path.size() - 1);
        state.put(node, 2);
        return null;
    }
}
