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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.LockSupport;
import org.jline.reader.LineReaderBuilder;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.runtime.RuntimeClient;
import de.planmarshall.operator.dialogue.ChoicePrompt;
import de.planmarshall.operator.dialogue.OnboardingSource;
import de.planmarshall.operator.dialogue.StateDialogue;

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
 * needs a confirmation, which {@code --yes} answers (PM-SEC-3). Without options the verb runs the
 * one state dialogue ({@code CLI-UC-S-06}); {@code --step} and {@code --auto --yes} stay for
 * scripts and print one line per state.
 */
@Command(name = "onboard", description = "Runs the machine onboarding, guided or automatic.")
@OperatorVerb
@SuppressWarnings("java:S1075")
public final class OnboardCommand implements Callable<Integer> {

    static final String STATE_PATH = OnboardingSource.STATE_PATH;
    static final String STEP_PATH = OnboardingSource.STEP_PATH;
    static final String AUTO_PATH = "/api/v1/onboarding/auto";

    private static final int EXIT_NEEDS_INPUT = 3;
    private static final int WRAP_WIDTH = 76;
    private static final long POLL_PAUSE_NANOS = 100_000_000L;
    private static final String ASPECT = "aspect";
    private static final String VALUE = "value";
    private static final String REASON = "reason";
    private static final String UNKNOWN = "onboard.state.unknown";
    private static final long AUTO_TIMEOUT_NANOS = 60_000_000_000L;
    private static final String AUTO_RESULT = "auto.result";

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
        var locale = context.clientEnvironment().locale();
        var source = new OnboardingSource(runtime);
        try {
            if (auto) {
                return runAutomatic(out, runtime, source, locale);
            }
            if (step != null && !step.isBlank()) {
                return runStep(out, source, locale);
            }
            return runDialogue(out, source, locale);
        } catch (IOException _) {
            out.println(MessageCatalogue.format(locale, "operator.onboard.unreadable", Map.of()));
            out.flush();
            return 1;
        }
    }

    private int runDialogue(PrintWriter out, OnboardingSource source, String locale) throws IOException {
        var startPrompt = new StartPrompt(context, out, locale, noInput);
        var dialogue = new StateDialogue(source, out, locale,
                            stateId -> new ChoicePrompt(startPrompt, stateId));
        try {
            dialogue.run();
            return 0;
        } catch (StartPrompt.StartLeftException _) {
            return 0;
        } catch (StartPrompt.MissingInputException _) {
            out.println(MessageCatalogue.format(locale, "operator.onboard.needs_input", Map.of()));
            out.flush();
            return EXIT_NEEDS_INPUT;
        }
    }

    private int runStep(PrintWriter out, OnboardingSource source, String locale) throws IOException {
        var frame = source.choose(step);
        printStateLine(out, locale, frame);
        return 0;
    }

    private int runAutomatic(PrintWriter out, RuntimeClient runtime, OnboardingSource source,
                        String locale) throws IOException {
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
        var response = runtime.send(HttpRequest.json("POST", AUTO_PATH, "{}"));
        if (response.status() != 200) {
            out.println(MessageCatalogue.format("operator.onboard.refused",
                                Map.of(REASON, "HTTP " + response.status())));
            out.flush();
            return 1;
        }
        var frame = StateDialogue.fromJson(response.bodyText(), 7, false);
        printStateLine(out, locale, frame);
        frame = advance(out, locale, source, frame);
        if (AUTO_RESULT.equals(frame.stateId())) {
            printResults(out, locale, frame);
        }
        out.flush();
        return 0;
    }

    /**
     * Walks the automatic run: each state without a link is re-read until the runtime moved on,
     * each state with links takes its preselection, and one line is printed per state.
     */
    private StateDialogue.Frame advance(PrintWriter out, String locale, OnboardingSource source,
                        StateDialogue.Frame frame) throws IOException {
        var deadline = System.nanoTime() + AUTO_TIMEOUT_NANOS;
        var last = frame.stateId();
        while (!frame.isFinal() && !AUTO_RESULT.equals(frame.stateId())) {
            if (System.nanoTime() > deadline) {
                out.println(MessageCatalogue.format(locale, "onboard.state.waits", Map.of()));
                return frame;
            }
            if (frame.links().isEmpty()) {
                LockSupport.parkNanos(POLL_PAUSE_NANOS);
                frame = source.fetch();
            } else {
                LockSupport.parkNanos(POLL_PAUSE_NANOS);
                var link = frame.primary() != null ? frame.primary() : frame.links().getFirst();
                frame = source.choose(link.id());
            }
            if (!frame.stateId().equals(last)) {
                printStateLine(out, locale, frame);
                last = frame.stateId();
                deadline = System.nanoTime() + AUTO_TIMEOUT_NANOS;
            }
        }
        return frame;
    }

    private void printStateLine(PrintWriter out, String locale, StateDialogue.Frame frame) {
        var title = StateDialogue.titleOf(locale, frame);
        var summary = TerminalSanitizer.sanitizeLine(frame.summary());
        if (!summary.isBlank() && !summary.equals(title)) {
            for (var line : StateDialogue.wrap(MessageCatalogue.format(locale, "onboard.state.line",
                                Map.of("title", title, "summary", summary)),
                                WRAP_WIDTH)) {
                out.println(line);
            }
        } else {
            out.println(MessageCatalogue.format(locale, "onboard.state.line_no_summary",
                                Map.of("title", title)));
        }
        out.flush();
    }

    private void printResults(PrintWriter out, String locale, StateDialogue.Frame frame) {
        out.println(MessageCatalogue.format(locale, "onboard.state.result_header", Map.of(
                            ASPECT, MessageCatalogue.format(locale, "onboard.auto.result.aspect", Map.of()),
                            VALUE, MessageCatalogue.format(locale, "onboard.auto.result.value", Map.of()),
                            REASON, MessageCatalogue.format(locale, "onboard.auto.result.reason", Map.of()))));
        if (!(frame.facts().get("results") instanceof List<?> results)) {
            return;
        }
        for (var item : results) {
            if (item instanceof Map<?, ?> row) {
                printResultRow(out, locale, row);
            }
        }
    }

    private void printResultRow(PrintWriter out, String locale, Map<?, ?> row) {
        var aspect = Objects.toString(row.get(ASPECT), "");
        var value = Objects.toString(row.get(VALUE), "");
        var reason = Objects.toString(row.get(REASON), "");
        for (var line : StateDialogue.wrap(MessageCatalogue.format(locale,
                            "onboard.state.result_row", Map.of(
                                                ASPECT, autoText("onboard.auto.aspect.", locale, aspect),
                                                VALUE, TerminalSanitizer.sanitizeLine(value),
                                                REASON, autoText("onboard.auto.reason.", locale, reason))),
                            WRAP_WIDTH)) {
            out.println(line);
        }
    }

    private static String autoText(String prefix, String locale, String token) {
        var key = prefix + token;
        if (MessageCatalogue.hasKey(locale, key)) {
            return MessageCatalogue.format(locale, key, Map.of());
        }
        return MessageCatalogue.format(locale, UNKNOWN, Map.of());
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
