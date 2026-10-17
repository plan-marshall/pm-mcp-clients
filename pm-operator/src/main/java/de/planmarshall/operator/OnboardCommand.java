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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jline.reader.LineReaderBuilder;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.runtime.RuntimeClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * {@code pm-operator onboard}: the machine onboarding (UC-ONB-1..9), guided by following the
 * states the runtime reports, or automatic with {@code --auto}.
 *
 * <p>The verb reads the state machine from the runtime and never keeps a step order or rule of its
 * own. Every consent is a state of the runtime; {@code --auto} asks once for the automatic run and
 * needs a confirmation, which {@code --yes} answers (PM-SEC-3).
 */
@Command(name = "onboard", description = "Runs the machine onboarding, guided or automatic.")
@OperatorVerb
@SuppressWarnings("java:S1075")
public final class OnboardCommand implements Callable<Integer> {

    static final String STATE_PATH = "/api/v1/onboarding/state";
    static final String STEP_PATH = "/api/v1/onboarding/step";
    static final String AUTO_PATH = "/api/v1/onboarding/auto";

    private static final int EXIT_NEEDS_INPUT = 3;

    private final OperatorContext context;

    @Spec
    CommandSpec spec = CommandSpec.create();

    @Option(names = "--auto", description = "Start the automatic run")
    boolean auto;

    @Option(names = "--step", description = "Follow the named step of the current state")
    @Nullable
    String step;

    @Option(names = "--yes", description = "Answer the automatic run's confirmation with yes")
    boolean yes;

    @Option(names = "--no-input", description = "Do not prompt; fail if a choice is needed")
    boolean noInput;

    OnboardCommand(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public Integer call() throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var runtime = context.runtime(true);

        if (auto) {
            return runAutomatic(out, runtime);
        }

        if (step != null && !step.isBlank()) {
            var body = JsonTree.write(Map.of("link", step));
            return render(out, runtime.send(HttpRequest.json("POST", STEP_PATH, body)));
        }

        return render(out, runtime.send(HttpRequest.get(STATE_PATH)));
    }

    private int runAutomatic(PrintWriter out, RuntimeClient runtime)
                        throws IOException {
        if (!yes && noInput) {
            out.println(MessageCatalogue.get("operator.onboard.auto.confirm"));
            out.flush();
            return EXIT_NEEDS_INPUT;
        }
        if (!yes && !confirm(MessageCatalogue.get("operator.onboard.auto.confirm"))) {
            out.println(MessageCatalogue.get("operator.onboard.auto.declined"));
            out.flush();
            return 0;
        }
        return render(out, runtime.send(HttpRequest.json("POST", AUTO_PATH, "{}")));
    }

    private int render(PrintWriter out, HttpResponse response) throws IOException {
        if (response.status() != 200) {
            out.println(MessageCatalogue.format("operator.onboard.refused",
                                Map.of("reason", "HTTP " + response.status())));
            out.flush();
            return 1;
        }
        var map = JsonTree.asObject(JsonTree.parse(response.body()));
        if (map == null) {
            out.println(response.bodyText());
            out.flush();
            return 0;
        }
        var state = JsonTree.string(map, "state");
        var summary = JsonTree.string(map, "summary");
        out.println(MessageCatalogue.get("operator.onboard.title"));
        out.printf("  %s%n", summary != null ? summary : Objects.toString(state, ""));
        var steps = JsonTree.integer(map, "completed_steps");
        int shown = Math.min((steps != null ? steps : 0) + 1, 7);
        out.println("  " + MessageCatalogue.format("onboard.frame.progress", Map.of("step", shown)));
        if ("onboard.completed".equals(state)) {
            out.println("  " + MessageCatalogue.get("operator.onboard.done"));
        }
        var links = JsonTree.object(map, "links");
        if (links != null && !links.isEmpty()) {
            out.println("  " + MessageCatalogue.get("operator.onboard.links"));
            for (var entry : links.entrySet()) {
                out.printf("    %s%n", entry.getKey());
            }
        }
        out.flush();
        return 0;
    }

    private boolean confirm(String prompt) throws IOException {
        try (var terminal = context.createTerminal()) {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var line = reader.readLine(prompt + " \u203a ");
            return line != null && (line.isBlank() || line.trim().toLowerCase().startsWith("y")
                                || line.trim().toLowerCase().startsWith("j"));
        }
    }
}
