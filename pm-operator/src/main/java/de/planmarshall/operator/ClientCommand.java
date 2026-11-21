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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.runtime.RuntimeUnavailableException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator client}: manages client bundle installations into coding agent harnesses
 * (01-commands.adoc#commands, 02-credentials-and-operator.adoc).
 */
@Command(name = "client", description = "Manages client bundle installations for coding agent harnesses.",
                    subcommands = {
                                        ClientCommand.StatusCommand.class,
                                        ClientCommand.InstallCommand.class,
                                        ClientCommand.UpdateCommand.class,
                                        ClientCommand.RemoveCommand.class
                    })
public final class ClientCommand implements Callable<Integer> {

    private static final String HARNESSES = "harnesses";

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    ClientCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    @Command(name = "status", description = "Shows installation status of client bundles. Never starts runtime.")
    static final class StatusCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        StatusCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            var out = spec.commandLine().getOut();
            var locale = context.clientEnvironment().locale();
            try {
                var response = context.runtime(false).send(HttpRequest.get("/api/v1/client/bundles"));
                if (response.status() != 200) {
                    // The refusal is rendered with its cause on stderr by the one handler of every verb.
                    throw new OperatorRefusedException("client", response);
                }
                var parsed = JsonTree.parse(response.body());
                if (parsed instanceof List<?> list && !list.isEmpty()) {
                    printBundles(out, locale, list);
                } else {
                    out.println(MessageCatalogue.format(locale, "operator.client.none", Map.of()));
                }
            } catch (RuntimeUnavailableException _) {
                out.println(MessageCatalogue.format(locale, "operator.runtime.status_not_running", Map.of()));
            }
            out.flush();
            return 0;
        }

        private void printBundles(PrintWriter out, String locale, List<?> list) {
            out.println(MessageCatalogue.format(locale, "operator.client.title", Map.of()));
            for (var item : list) {
                if (item instanceof Map<?, ?> bundle) {
                    var version = bundle.get("version");
                    out.printf("  %-16s %-10s %s%n",
                                        bundle.get("name"),
                                        bundle.get("status"),
                                        version != null ? version : "");
                } else {
                    out.println("  " + item);
                }
            }
        }
    }

    /**
     * Sends one harness action and prints its outcome.
     *
     * @param context    the process context
     * @param spec       the command's spec, whose writers the outcome goes to
     * @param action     the wire name of the action
     * @param harnesses  the harnesses the action names, or {@code null} for all
     * @param successMsg the line printed on success
     * @param failedKey  the catalogue key of the action's own failure line, which names the action and
     *                   takes the refusal's text as {@code reason}
     * @return the exit code
     * @throws IOException when the runtime cannot be reached
     */
    static int executeHarnessAction(OperatorContext context, CommandSpec spec, String action,
                        @Nullable List<String> harnesses, String successMsg, String failedKey) throws IOException {
        context.gate().verify();
        var body = new LinkedHashMap<String, Object>();
        body.put(HARNESSES, harnesses != null ? harnesses : List.of());
        var response = context.runtime(true).send(
                            HttpRequest.json("POST", "/api/v1/client/" + action, JsonTree.write(body)));
        var out = spec.commandLine().getOut();
        if (response.status() == 200 || response.status() == 201 || response.status() == 204) {
            out.println(successMsg);
            out.flush();
            return 0;
        }
        var err = spec.commandLine().getErr();
        var locale = context.clientEnvironment().locale();
        var refusal = new OperatorRefusedException("client", response);
        err.println(MessageCatalogue.format(locale, failedKey, Map.of("reason", refusal.message(locale))));
        err.flush();
        return refusal.exitCode();
    }

    @Command(name = "install", description = "Installs client bundles for the specified harnesses.")
    @OperatorVerb
    static final class InstallCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0..*", description = "Harness IDs to install")
        @Nullable
        List<String> harnesses;

        InstallCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            return executeHarnessAction(context, spec, "install", harnesses,
                                "✓ Client bundle installation completed", "operator.client.install_failed");
        }
    }

    @Command(name = "update", description = "Updates client bundles for the specified harnesses.")
    @OperatorVerb
    static final class UpdateCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0..*", description = "Harness IDs to update")
        @Nullable
        List<String> harnesses;

        UpdateCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            return executeHarnessAction(context, spec, "update", harnesses,
                                "✓ Client bundle update completed", "operator.client.update_failed");
        }
    }

    @Command(name = "remove", description = "Removes client bundles for the specified harnesses.")
    @OperatorVerb
    static final class RemoveCommand implements Callable<Integer> {
        @Spec
        CommandSpec spec = CommandSpec.create();
        private final OperatorContext context;

        @Parameters(index = "0..*", description = "Harness IDs to remove")
        @Nullable
        List<String> harnesses;

        RemoveCommand(OperatorContext context) {
            this.context = context;
        }

        @Override
        public Integer call() throws IOException {
            return executeHarnessAction(context, spec, "remove", harnesses,
                                "✓ Client bundle removal completed", "operator.client.remove_failed");
        }
    }
}
