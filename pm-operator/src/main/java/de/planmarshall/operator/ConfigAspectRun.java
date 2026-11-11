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
    private final String locale;
    private final Answers answers;

    private ConfigAspectRun(OperatorContext context, PrintWriter out, String aspect, Answers answers) {
        this.context = Objects.requireNonNull(context, "context");
        this.out = Objects.requireNonNull(out, "out");
        this.locale = context.clientEnvironment().locale();
        this.answers = Objects.requireNonNull(answers, "answers");
    }

    /**
     * Runs one aspect of the machine's onboarding again.
     *
     * @param context the operator context
     * @param out     the verb's output
     * @param aspect  the aspect's name as the verb spells it
     * @param answers the answers the verb carries
     * @return the exit code
     * @throws IOException on transport failure
     */
    static int run(OperatorContext context, PrintWriter out, String aspect, Answers answers) throws IOException {
        return new ConfigAspectRun(context, out, aspect, answers).execute(aspect);
    }

    private int execute(String aspect) throws IOException {
        var source = new AspectRunSource(context.runtime(true), context.gate(),
                            AspectRunSource.reconfigureLink(aspect), answers.facts(), answers.consents(),
                            answers.noIcons(), answers.noAdvice(), answers.yes(), answers.noInput());
        try (var lease = context.openTerminal()) {
            var reader = LineReaderBuilder.builder().terminal(lease.terminal()).build();
            var dialogue = new StateDialogue(source, out, locale,
                                stateId -> new ChoicePrompt(reader, out, locale, stateId));
            dialogue.run();
        } catch (StartPrompt.InterruptException _) {
            return StartPrompt.EXIT_INTERRUPTED;
        } catch (StartPrompt.MissingInputException missing) {
            out.println(MessageCatalogue.format(locale, "operator.config.aspect_stopped",
                                Map.of("state", missing.state())));
            out.flush();
            return EXIT_MISSING_INPUT;
        } catch (StartPrompt.StartLeftException _) {
            out.flush();
            return 0;
        } catch (OperatorRefusedException e) {
            out.println(e.getMessage());
            out.flush();
            return PmOperator.EXIT_FAILURE;
        } catch (IOException _) {
            out.println(MessageCatalogue.format(locale, "operator.config.aspect_failed", Map.of(ASPECT, aspect)));
            out.flush();
            return PmOperator.EXIT_FAILURE;
        }
        reportUnused(source);
        out.println(MessageCatalogue.format(locale, "operator.config.aspect_done", Map.of(ASPECT, aspect)));
        out.flush();
        return 0;
    }

    private void reportUnused(AspectRunSource source) {
        for (var unused : source.unusedAnswers()) {
            var argument = unused.startsWith(CONSENT_MARKER)
                                ? VALUE_FLAG + CONSENT + " " + unused.substring(CONSENT_MARKER.length())
                                : VALUE_FLAG + unused;
            out.println(MessageCatalogue.format(locale, "operator.config.argument_unused",
                                Map.of("argument", argument)));
        }
    }
}
