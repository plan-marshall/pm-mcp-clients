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
import java.util.Map;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.runtime.RuntimeRecord;
import de.planmarshall.api.runtime.RuntimeUnavailableException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator system}: prints the system view (runtime, work, workers, jobs, models, cleanup);
 * never starts the runtime.
 */
@Command(name = "system", description = "Shows the system view; never starts the runtime.")
public final class SystemCommand implements Callable<Integer> {

    private final OperatorContext context;

    @Spec
    CommandSpec spec;

    @Option(names = "--project", description = "Target project ID")
    @Nullable
    String project;

    @Option(names = "--window", description = "Time window for metrics: hour, today, 7d", defaultValue = "hour")
    String window = "hour";

    @Option(names = "--by", description = "Group metrics by model or role", defaultValue = "model")
    String by = "model";

    @Option(names = "--watch", description = "Watch updates")
    boolean watch;

    SystemCommand(OperatorContext context) {
        this.context = context;
    }

    @Override
    public Integer call() throws IOException {
        var out = spec.commandLine().getOut();
        var record = RuntimeRecord.read(context.paths().runtimeRecord());
        if (record.isEmpty() || !record.get().processAlive()) {
            out.println("runtime   not running");
            out.flush();
            return 0;
        }

        try {
            var runtime = context.runtime(false);
            var response = runtime.send(HttpRequest.get("/api/v1/system"));
            if (response.status() == 200) {
                var parsed = JsonTree.parse(response.body());
                if (parsed instanceof Map<?, ?> map) {
                    var version = map.get("version") != null ? map.get("version") : "pm-mcpd";
                    var build = map.get("build") != null ? map.get("build") : "";
                    out.println("System");
                    out.printf("Runtime   %s · build %s%n", version, build);
                    if (map.containsKey("uptime")) {
                        out.printf("          running since %s%n", map.get("uptime"));
                    }
                    if (map.containsKey("memory")) {
                        out.printf("          memory %s%n", map.get("memory"));
                    }
                    if (map.containsKey("work")) {
                        out.printf("Work      %s%n", map.get("work"));
                    }
                    if (map.containsKey("workers")) {
                        out.printf("Workers   %s%n", map.get("workers"));
                    }
                    if (map.containsKey("jobs")) {
                        out.printf("Jobs      %s%n", map.get("jobs"));
                    }
                    if (map.containsKey("cleanup")) {
                        out.printf("Cleanup   %s%n", map.get("cleanup"));
                    }
                } else {
                    out.println(response.bodyText());
                }
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("system", response);
        } catch (RuntimeUnavailableException e) {
            out.println("runtime   not running");
            out.flush();
            return 0;
        }
    }
}
