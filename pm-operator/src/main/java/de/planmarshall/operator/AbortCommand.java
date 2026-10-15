/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.runtime.RuntimeClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator abort plan|epic <id> --reason <text>}: the shell form of plan cancel or abandon
 * by the plan's state, and of epic cancel (xref:19-actions.adoc).
 */
@Command(name = "abort", description = "Aborts a plan or epic by state.")
@OperatorVerb
public final class AbortCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Parameters(index = "0", description = "Target kind: plan or epic")
    String kind = "";

    @Parameters(index = "1", description = "Target plan or epic ID")
    String targetId = "";

    @Option(names = "--reason", required = true, description = "Reason for abort")
    String reason = "";

    @Option(names = "--close-pr", description = "Close open pull request")
    boolean closePr;

    @Option(names = "--keep-pr", description = "Keep open pull request")
    boolean keepPr;

    @Option(names = "--with-plans", description = "Cancel child plans as well (for epic)")
    boolean withPlans;

    @Option(names = "--without-plans", description = "Leave child plans running (for epic)")
    boolean withoutPlans;

    AbortCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var runtime = context.runtime(true);

        if ("plan".equalsIgnoreCase(kind)) {
            return abortPlan(runtime, out);
        }
        if ("epic".equalsIgnoreCase(kind)) {
            return abortEpic(runtime, out);
        }

        var err = spec.commandLine().getErr();
        err.printf("Unknown abort target: %s. Expected 'plan' or 'epic'.%n", kind);
        err.flush();
        return 2;
    }

    private int abortPlan(RuntimeClient runtime, PrintWriter out) throws IOException {
        boolean isPaused = false;
        var safeId = OperatorUrlUtil.encodePathSegment(targetId);
        var planResponse = runtime.send(HttpRequest.get("/api/v1/plans/" + safeId));
        if (planResponse.status() == 200) {
            var parsed = JsonTree.parse(planResponse.body());
            if (parsed instanceof Map<?, ?> map) {
                var state = String.valueOf(map.get("state"));
                isPaused = "paused".equalsIgnoreCase(state);
            }
        }

        var payload = new LinkedHashMap<String, Object>();
        payload.put("reason", reason);
        payload.put("close_pr", !keepPr);

        var endpoint = isPaused ? "/api/v1/plans/" + safeId + "/abandon" : "/api/v1/plans/" + safeId + "/cancel";
        var response = runtime.send(HttpRequest.json("POST", endpoint, JsonTree.write(payload)));
        if (response.status() == 200 || response.status() == 202) {
            out.printf("✓ %s %s and archived%n", targetId, isPaused ? "abandoned" : "cancelled");
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("abort plan " + targetId, response);
    }

    private int abortEpic(RuntimeClient runtime, PrintWriter out) throws IOException {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("reason", reason);
        payload.put("with_plans", !withoutPlans);

        var safeId = OperatorUrlUtil.encodePathSegment(targetId);
        var response = runtime.send(HttpRequest.json("POST", "/api/v1/epics/" + safeId + "/cancel", JsonTree.write(payload)));
        if (response.status() == 200 || response.status() == 202) {
            out.printf("✓ %s cancelled%n", targetId);
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException("abort epic " + targetId, response);
    }
}
