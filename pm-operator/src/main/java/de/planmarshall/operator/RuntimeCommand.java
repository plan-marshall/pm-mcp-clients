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
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.runtime.RuntimeUnavailableException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator runtime}: starts, stops, restarts, or upgrades the runtime (01-commands.adoc#commands,
 * 25-system.adoc).
 */
@Command(name = "runtime", description = "Starts, stops, restarts, or upgrades the runtime.",
              subcommands = {
                            RuntimeCommand.StartCommand.class,
                            RuntimeCommand.StopCommand.class,
                            RuntimeCommand.RestartCommand.class,
                            RuntimeCommand.UpgradeCommand.class
              })
public final class RuntimeCommand implements Callable<Integer> {

    private static final String DRAIN = "drain";

    @Spec
    CommandSpec spec = CommandSpec.create();

    RuntimeCommand(OperatorContext context) {
        Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    @Command(name = "start", description = "Starts the runtime unless one runs. Runs without operator checks.")
    static final class StartCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        StartCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            var status = StatusCommand.fetch(context.runtime(true));
            var out = spec.commandLine().getOut();
            out.println("runtime   running (pid " + status.get("pid") + ")");
            out.flush();
            return 0;
        }
    }

    @Command(name = "stop", description = "Asks the runtime to stop.")
    @OperatorVerb
    static final class StopCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--drain", description = "Stop gracefully after active work completes")
        boolean drain;

        @Option(names = "--now", description = "Stop immediately")
        boolean now;

        @Option(names = "--withdraw", description = "Withdraw a requested drain")
        boolean withdraw;

        StopCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            try {
                if (withdraw) {
                    var response = context.runtime(false).send(
                                  new HttpRequest("DELETE", "/api/v1/runtime/stop", Map.of(), null));
                    if (response.status() == 200 || response.status() == 204) {
                        out.println("runtime   drain withdrawn");
                        out.flush();
                        return 0;
                    }
                    var err = spec.commandLine().getErr();
                    err.printf("Failed to withdraw drain: HTTP %d%n", response.status());
                    err.flush();
                    return 1;
                }

                var body = new LinkedHashMap<String, Object>();
                if (drain) body.put(DRAIN, true);
                if (now) body.put("now", true);

                var response = context.runtime(false).send(
                              HttpRequest.json("POST", "/api/v1/runtime/stop", JsonTree.write(body)));
                if (response.status() != 202 && response.status() != 200) {
                    throw new OperatorRefusedException("runtime stop", response);
                }
                out.println(drain ? "runtime   draining" : "runtime   stopping");
            } catch (RuntimeUnavailableException e) {
                out.println("runtime   not running");
            }
            out.flush();
            return 0;
        }
    }

    @Command(name = "restart", description = "Restarts the runtime.")
    @OperatorVerb
    static final class RestartCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Option(names = "--drain", description = "Restart after active work completes")
        boolean drain;

        @Option(names = "--now", description = "Restart immediately")
        boolean now;

        @Option(names = "--yes", description = "Accept confirmation")
        boolean yes;

        RestartCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            var body = new LinkedHashMap<String, Object>();
            body.put("then", "restart");
            if (drain) body.put(DRAIN, true);
            if (now) body.put("now", true);

            var response = context.runtime(true).send(
                          HttpRequest.json("POST", "/api/v1/operator/runtime/stop", JsonTree.write(body)));
            if (response.status() == 200 || response.status() == 202) {
                out.println("runtime   restarting");
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("runtime restart", response);
        }
    }

    @Command(name = "upgrade", description = "Checks, checks out notes, or upgrades runtime version.")
    @OperatorVerb
    static final class UpgradeCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0", arity = "0..1", description = "Version to upgrade to")
        @Nullable
        String version;

        @Option(names = "--check", description = "Check for newer version")
        boolean check;

        @Option(names = "--notes", description = "Display release notes for newer version")
        boolean notes;

        @Option(names = "--withdraw", description = "Withdraw upgrade request")
        boolean withdraw;

        @Option(names = "--drain", description = "Upgrade after active work completes")
        boolean drain;

        @Option(names = "--now", description = "Upgrade immediately")
        boolean now;

        @Option(names = "--yes", description = "Accept confirmation")
        boolean yes;

        UpgradeCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            context.gate().verify();
            var out = spec.commandLine().getOut();
            if (notes) {
                return showNotes(out);
            }
            if (check) {
                return runCheck(out);
            }
            if (withdraw) {
                return withdrawUpgrade(out);
            }
            return executeUpgrade(out);
        }

        private int showNotes(PrintWriter out) throws IOException {
            var response = context.runtime(false).send(HttpRequest.get("/api/v1/system/release-notes"));
            if (response.status() == 200) {
                out.println(response.bodyText());
            } else {
                out.println("No release notes found.");
            }
            out.flush();
            return 0;
        }

        private int runCheck(PrintWriter out) throws IOException {
            var response = context.runtime(true).send(HttpRequest.json("POST", "/api/v1/operator/runtime/upgrade/check", "{}"));
            if (response.status() == 200) {
                out.println("✓ Version check completed");
            } else {
                out.printf("Version check failed: HTTP %d%n", response.status());
            }
            out.flush();
            return 0;
        }

        private int withdrawUpgrade(PrintWriter out) throws IOException {
            var response = context.runtime(false).send(new HttpRequest("DELETE", "/api/v1/operator/runtime/upgrade", Map.of(), null));
            if (response.status() == 200 || response.status() == 204) {
                out.println("runtime   upgrade withdrawn");
            } else {
                out.printf("Failed to withdraw upgrade: HTTP %d%n", response.status());
            }
            out.flush();
            return 0;
        }

        private int executeUpgrade(PrintWriter out) throws IOException {
            var body = new LinkedHashMap<String, Object>();
            if (version != null) body.put("version", version);
            if (drain) body.put(DRAIN, true);
            if (now) body.put("now", true);

            var response = context.runtime(true).send(
                          HttpRequest.json("POST", "/api/v1/operator/runtime/upgrade", JsonTree.write(body)));
            if (response.status() == 200 || response.status() == 202) {
                out.println("runtime   upgrading");
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException("runtime upgrade", response);
        }
    }
}
