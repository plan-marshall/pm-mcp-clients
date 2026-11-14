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
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.dialogue.AspectRunSource;
import de.planmarshall.operator.dialogue.ChoicePrompt;
import de.planmarshall.operator.dialogue.StateDialogue;

/**
 * One aspect of the machine's onboarding as a later run: the verb {@code config <aspect>} walks the
 * aspect's own sub-workflow from the machine settings ({@code onboard_run: reconfigure}), answers the
 * choice states its arguments answer, and asks the rest
 * ({@code 01-commands.adoc#direct-verbs}). An argument no state of the run asks for is reported as
 * unused, never silently dropped.
 */
final class ConfigAspectRun {

    /** The exit code of a run stopped by {@code --no-input} at a state that needs the operator. */
    static final int EXIT_MISSING_INPUT = StartDialogue.EXIT_MISSING_INPUT;

    private static final String VALUE_FLAG = "--";
    private static final String ASPECT = "aspect";
    private static final String CONSENT = "consent";
    private static final String CONSENT_MARKER = "consent:";
    /** The store step names its fact {@code current}; the operator types {@code --store}. */
    private static final String STORE_FACT = "current";
    private static final String STORE_OPTION = "store";

    /**
     * The answers a verb carries before the run asks.
     *
     * @param facts    the facts the verb answers a state with, by field name
     * @param consents the consent kinds the verb carries
     * @param noIcons  whether the icons consent is answered with no
     * @param noAdvice whether the model advice consent is answered with no
     * @param yes      whether a displayed change is confirmed
     * @param noInput  whether the run stops at a state it may not be asked about
     */
    record Answers(Map<String, String> facts, List<String> consents, boolean noIcons, boolean noAdvice,
    boolean yes, boolean noInput) {

        Answers {
            facts = Map.copyOf(Objects.requireNonNull(facts, "facts"));
            consents = List.copyOf(Objects.requireNonNull(consents, "consents"));
        }

        /**
         * @param facts the facts the verb answers a state with
         * @return the answers of a verb that carries only facts
         */
        static Answers of(Map<String, String> facts) {
            return new Answers(facts, List.of(), false, false, false, false);
        }
    }

    private final OperatorContext context;
    private final PrintWriter out;
    private final PrintWriter err;
    private final String locale;
    private final Answers answers;

    private ConfigAspectRun(OperatorContext context, PrintWriter out, PrintWriter err, Answers answers) {
        this.context = Objects.requireNonNull(context, "context");
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
        this.locale = context.clientEnvironment().locale();
        this.answers = Objects.requireNonNull(answers, "answers");
    }

    /**
     * The outcome of one later run: its exit code, the facts of the last state it reported, and the
     * id of that last state.
     *
     * @param exitCode    the verb's exit code
     * @param facts       the facts of the last state before the run returned to the settings
     * @param lastStateId the state id of the last state
     */
    record Outcome(int exitCode, Map<String, Object> facts, String lastStateId) {

        Outcome {
            facts = Map.copyOf(Objects.requireNonNull(facts, "facts"));
            Objects.requireNonNull(lastStateId, "lastStateId");
        }

        Outcome(int exitCode, Map<String, Object> facts) {
            this(exitCode, facts, "");
        }
    }

    /**
     * Runs one aspect of the machine's onboarding again.
     *
     * @param context the operator context
     * @param out     the verb's output
     * @param err     the verb's error output
     * @param aspect  the aspect's name as the verb spells it
     * @param answers the answers the verb carries
     * @return the exit code
     * @throws IOException on transport failure
     */
    static int run(OperatorContext context, PrintWriter out, PrintWriter err, String aspect, Answers answers)
                        throws IOException {
        return runFor(context, out, err, aspect, answers).exitCode();
    }

    /**
     * Runs one aspect of the machine's onboarding again and keeps what its last state reported, so a
     * verb can print the outcome of the check the run performed ({@code credentials rotate}).
     *
     * @param context the operator context
     * @param out     the verb's output
     * @param err     the verb's error output
     * @param aspect  the aspect's name as the verb spells it
     * @param answers the answers the verb carries
     * @return the outcome
     * @throws IOException on transport failure
     */
    static Outcome runFor(OperatorContext context, PrintWriter out, PrintWriter err, String aspect,
                        Answers answers) throws IOException {
        return runFor(context, out, err, aspect, answers, null);
    }

    /**
     * Runs one aspect of the machine's onboarding again over a terminal the caller already holds, so
     * a verb that read a secret on its own terminal does not open a second one for the check.
     *
     * @param context  the operator context
     * @param out      the verb's output
     * @param err      the verb's error output
     * @param aspect   the aspect's name as the verb spells it
     * @param answers  the answers the verb carries
     * @param terminal the caller's terminal, or {@code null} to open one
     * @return the outcome
     * @throws IOException on transport failure
     */
    static Outcome runFor(OperatorContext context, PrintWriter out, PrintWriter err, String aspect,
                        Answers answers, @Nullable Terminal terminal) throws IOException {
        return new ConfigAspectRun(context, out, err, answers).execute(aspect, terminal);
    }

    private Outcome execute(String aspect, @Nullable Terminal terminal) throws IOException {
        var source = new AspectRunSource(context.runtime(true), context.gate(),
                            AspectRunSource.reconfigureLink(aspect), answers.facts(), answers.consents(),
                            new AspectRunSource.Flags(answers.noIcons(), answers.noAdvice(), answers.yes(),
                                                answers.noInput()));
        if (terminal != null) {
            return runWith(source, aspect, terminal);
        }
        try (var lease = context.openTerminal()) {
            return runWith(source, aspect, lease.terminal());
        }
    }

    private Outcome runWith(AspectRunSource source, String aspect, Terminal terminal) throws IOException {
        try {
            var reader = LineReaderBuilder.builder().terminal(terminal).build();
            var dialogue = new StateDialogue(source, out, locale,
                                stateId -> new ChoicePrompt(reader, out, locale, stateId));
            dialogue.run();
        } catch (StartPrompt.InterruptException _) {
            return outcome(source, StartPrompt.EXIT_INTERRUPTED);
        } catch (StartPrompt.MissingInputException _) {
            reportUnused(source);
            out.println(MessageCatalogue.format(locale, "operator.config.aspect_stopped", Map.of()));
            out.flush();
            return outcome(source, EXIT_MISSING_INPUT);
        } catch (StartPrompt.StartLeftException _) {
            reportUnused(source);
            out.flush();
            return outcome(source, 0);
        } catch (OperatorRefusedException e) {
            RefusalRenderer.render(err, locale, e);
            err.flush();
            return outcome(source, PmOperator.EXIT_FAILURE);
        } catch (AspectRunSource.RefusedException refused) {
            err.println(MessageCatalogue.format(locale, "operator.config.aspect_refused",
                                Map.of(ASPECT, aspect, "status", refused.status())));
            err.flush();
            return outcome(source, PmOperator.EXIT_FAILURE);
        } catch (IOException _) {
            err.println(MessageCatalogue.format(locale, "operator.config.aspect_failed", Map.of(ASPECT, aspect)));
            err.flush();
            return outcome(source, PmOperator.EXIT_FAILURE);
        }
        reportUnused(source);
        out.println(MessageCatalogue.format(locale, "operator.config.aspect_done", Map.of(ASPECT, aspect)));
        out.flush();
        return outcome(source, 0);
    }

    private static Outcome outcome(AspectRunSource source, int exitCode) {
        return new Outcome(exitCode, source.lastFacts(), source.lastStateId());
    }

    private void reportUnused(AspectRunSource source) {
        for (var unused : source.unusedAnswers()) {
            var argument = optionName(unused);
            out.println(MessageCatalogue.format(locale, "operator.config.argument_unused",
                                Map.of("argument", argument)));
        }
    }

    /**
     * @param unused an answer the run did not ask for: a fact field, {@code consent:<kind>}, or a
     *               flag name
     * @return the option the operator typed for it
     */
    private static String optionName(String unused) {
        if (unused.startsWith(CONSENT_MARKER)) {
            return VALUE_FLAG + CONSENT + " " + unused.substring(CONSENT_MARKER.length());
        }
        if (STORE_FACT.equals(unused)) {
            return VALUE_FLAG + STORE_OPTION;
        }
        return VALUE_FLAG + unused.replace('_', '-');
    }
}
