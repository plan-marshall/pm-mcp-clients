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
import java.util.ArrayList;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.runtime.RuntimeRecord;
import de.planmarshall.api.runtime.RuntimeUnavailableException;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.operator.dialogue.ChoicePrompt;
import de.planmarshall.operator.system.SystemView;

/**
 * The confirmation of a runtime act ({@code 25-system.adoc#t-stop}, {@code #t-restart},
 * {@code #t-upgrade}; CLI-UC-S-02, CLI-UC-P-05): what runs, then the choice of when, with
 * {@code k} keeping the runtime running.
 *
 * <p>Both entry points show it: the {@code runtime stop|restart|upgrade} verbs and the {@code a}
 * menu of the system view, which runs those verbs. The counts come from the system snapshot the
 * runtime serves, so the confirmation names what the act would end instead of what this process
 * guesses.
 *
 * <p>A stopped runtime is not asked: {@code runtime stop} says that there is nothing to stop, and a
 * restart or an upgrade starts it as every verb that needs the runtime does
 * ({@code 25-system.adoc#t-not-running}).
 *
 * <p>The confirmation comes after the operator checks, so a refused gate asks nothing.
 */
final class RuntimeActDialogue {

    /** The act the dialogue confirms. */
    enum Act {
        /** {@code runtime stop}. */
        STOP("stop"),
        /** {@code runtime restart}. */
        RESTART("restart"),
        /** {@code runtime upgrade}. */
        UPGRADE("upgrade");

        private final String value;

        Act(String value) {
            this.value = value;
        }

        /** @return the value the catalogue texts select on */
        String value() {
            return value;
        }
    }

    /** The operator's answer to the choice of when. */
    enum When {
        /** When what runs is done: the drain. */
        WHEN_DONE,
        /** Now: running tasks are withdrawn and jobs stopped. */
        NOW
    }

    /**
     * The outcome of the confirmation.
     *
     * @param when     the choice of when, or {@code null} when nothing is sent
     * @param exitCode the exit code of the verb that asked
     * @param target   the version an upgrade goes to as the dialogue showed it, or {@code null}
     */
    record Answer(@Nullable When when, int exitCode, @Nullable String target) {
    }

    /**
     * What a stop that could not reach the runtime says, and the exit code of its verb.
     *
     * @param key      the catalogue key of the line
     * @param exitCode the exit code: {@code 0} when no runtime runs, {@code 1} when one does not answer
     */
    record Refusal(String key, int exitCode) {
    }

    /** A stop the dialogue could not even ask, carrying the line and the exit code it owes. */
    private static final class StopRefusedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final transient Refusal refusal;

        StopRefusedException(Refusal refusal) {
            super(refusal.key());
            this.refusal = refusal;
        }
    }

    private static final String DRAIN = "drain";
    private static final String NOW = "now";
    private static final String DOT = " \u00b7 ";

    private RuntimeActDialogue() {
    }

    /**
     * Resolves the choice of when of an act: an option that answered it, or the dialogue.
     *
     * @param context  the operator context
     * @param out      the verb's output
     * @param act      the act
     * @param explicit the when an option named, or {@code null} for none
     * @param noInput  whether the verb may ask nothing
     * @param version  the version an upgrade goes to, or {@code null}
     * @return the when to send, or the exit code of a verb that stopped at the choice
     * @throws IOException when the confirmation cannot be drawn
     */
    static Answer confirm(OperatorContext context, PrintWriter out, Act act, @Nullable When explicit,
                        boolean noInput, @Nullable String version) throws IOException {
        if (explicit != null) {
            return new Answer(explicit, 0, version);
        }
        try {
            return ask(context, out, act, version, noInput);
        } catch (StopRefusedException refused) {
            out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                                refused.refusal.key(), Map.of()));
            out.flush();
            return new Answer(null, refused.refusal.exitCode(), null);
        } catch (ChoicePrompt.KeepException _) {
            return new Answer(null, 0, null);
        } catch (ChoicePrompt.InterruptException _) {
            return new Answer(null, StartPrompt.EXIT_INTERRUPTED, null);
        } catch (StartPrompt.StartLeftException _) {
            return new Answer(null, StartDialogue.EXIT_MISSING_INPUT, null);
        } catch (StartPrompt.MissingInputException _) {
            var locale = context.clientEnvironment().locale();
            out.println(MessageCatalogue.format(locale, "operator.system.stopped_no_input",
                                Map.of("act", act.value())));
            out.flush();
            return new Answer(null, StartDialogue.EXIT_MISSING_INPUT, null);
        }
    }

    private static Answer ask(OperatorContext context, PrintWriter out, Act act,
                        @Nullable String version, boolean noInput) throws IOException {
        var locale = context.clientEnvironment().locale();
        if (noInput) {
            // A script stops at the choice at once; the snapshot is only read for the dialogue.
            if (act == Act.STOP && provenStopped(context)) {
                throw new StopRefusedException(new Refusal("operator.system.nothing_to_stop", 0));
            }
            throw new StartPrompt.MissingInputException("runtime " + act.value(), "when", null);
        }
        SystemSnapshot snapshot;
        try {
            snapshot = snapshot(context);
        } catch (IOException _) {
            // No counts, but a restart or an upgrade still asks; a stop says why it cannot.
            if (act == Act.STOP) {
                throw new StopRefusedException(notStopped(context));
            }
            snapshot = null;
        }
        var prompt = new StartPrompt(context, out, locale, false);
        var target = version != null ? version : newerVersion(snapshot);
        var choice = new ChoicePrompt(prompt, "runtime " + act.value())
                            .title(title(locale, act, target, snapshot))
                            .leaveLabel(MessageCatalogue.format(locale,
                                                "operator.dialogue.label." + act.value(), Map.of()))
                            .keepKey(MessageCatalogue.format(locale, "operator.system.keep", Map.of()));
        choice.addChoice(DRAIN, when(locale, act, DRAIN),
                            MessageCatalogue.format(locale, "system.confirm.drain_explain", Map.of()), null, true);
        choice.addChoice(NOW, when(locale, act, NOW),
                            MessageCatalogue.format(locale, "system.confirm.now_explain", Map.of()), null, false);
        var when = NOW.equals(choice.ask().key()) ? When.NOW : When.WHEN_DONE;
        return new Answer(when, 0, target);
    }

    /**
     * A runtime process that is alive but refuses its socket is not answering, not absent: the two
     * cases read differently to the operator, and the design names the first
     * ({@code operator.system.not_answering}).
     *
     * @param context the operator context
     * @return the line a stop that could not reach the runtime prints, and its exit code
     */
    static Refusal notStopped(OperatorContext context) {
        return provenStopped(context)
                            ? new Refusal("operator.system.nothing_to_stop", 0)
                            : new Refusal("operator.system.not_answering", 1);
    }

    /**
     * @param context the operator context
     * @return {@code true} only when the runtime record proves that no runtime process runs
     */
    private static boolean provenStopped(OperatorContext context) {
        try {
            return RuntimeRecord.read(context.paths().runtimeRecord())
                                .filter(RuntimeRecord::processAlive).isEmpty();
        } catch (IOException _) {
            // A record this client cannot read does not establish that the process is absent.
            return false;
        }
    }

    /**
     * @param locale the interface locale
     * @param act    the act
     * @param target the version an upgrade goes to, empty when none is known
     * @return the act's question
     */
    private static String heading(String locale, Act act, String target) {
        return act == Act.UPGRADE && target.isBlank()
                            ? MessageCatalogue.format(locale, "system.confirm.title_upgrade", Map.of())
                            : MessageCatalogue.format(locale, "system.confirm.title",
                            Map.of("act", act.value(), "version", target));
    }

    /**
     * @param locale   the interface locale
     * @param act      the act
     * @param version  the version an upgrade goes to, or {@code null}
     * @param snapshot the snapshot the runtime serves, or {@code null} when it serves none
     * @return the heading of the dialogue: the act's question, what runs, its note, and the choice
     */
    private static String title(String locale, Act act, @Nullable String version,
                        @Nullable SystemSnapshot snapshot) {
        var target = version != null ? OutputSafety.stripControlSequences(version) : "";
        var lines = new ArrayList<String>(4);
        lines.add(heading(locale, act, target));
        lines.add("  " + running(locale, snapshot));
        var note = noteKey(act);
        if (note != null) {
            lines.add("  " + MessageCatalogue.format(locale, note, Map.of()));
        }
        lines.add("  " + MessageCatalogue.format(locale, "system.confirm.when", Map.of()));
        return String.join("\n", lines);
    }

    private static String running(String locale, @Nullable SystemSnapshot snapshot) {
        if (snapshot == null) {
            return MessageCatalogue.format(locale, "system.confirm.running",
                                Map.of("what", MessageCatalogue.format(locale, "system.figure.unavailable", Map.of())));
        }
        var what = SystemView.runningNow(locale, snapshot);
        return what.isBlank()
                            ? MessageCatalogue.format(locale, "system.confirm.nothing_runs", Map.of())
                            : MessageCatalogue.format(locale, "system.confirm.running", Map.of("what", what));
    }

    private static @Nullable String newerVersion(@Nullable SystemSnapshot snapshot) {
        return snapshot != null ? snapshot.runtime().newerVersion() : null;
    }

    private static @Nullable String noteKey(Act act) {
        return switch (act) {
            case STOP -> "system.confirm.stop_note";
            case RESTART -> "system.confirm.restart_note";
            case UPGRADE -> null;
        };
    }

    private static String when(String locale, Act act, String when) {
        return MessageCatalogue.format(locale, "system.confirm.do", Map.of("act", act.value(), "when", when));
    }

    /**
     * @param context the operator context
     * @return the snapshot the runtime serves, or {@code null} when it serves none
     * @throws RuntimeUnavailableException when the runtime does not answer at all
     */
    private static @Nullable SystemSnapshot snapshot(OperatorContext context) throws IOException {
        var response = context.runtime(false).send(HttpRequest.get("/api/v1/system"));
        if (response.status() != 200) {
            return null;
        }
        try {
            return SystemSnapshot.fromJson(response.bodyText());
        } catch (IOException _) {
            // A body this client cannot read counts nothing; the confirmation still asks.
            return null;
        }
    }
}
