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
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.system.SystemSnapshot;
import de.planmarshall.operator.dialogue.ChoicePrompt;

/**
 * The confirmation of a runtime act ({@code 25-system.adoc#t-stop}, {@code #t-restart},
 * {@code #t-upgrade}; CLI-UC-S-02, CLI-UC-P-05): what runs, then the choice of when, with
 * {@code k} keeping the runtime running.
 *
 * <p>Both entry points show it: the {@code runtime stop|restart|upgrade} verbs and the {@code a}
 * menu of the system view, which runs those verbs. The counts come from the system snapshot the
 * runtime serves, so the confirmation names what the act would end instead of what this process
 * guesses; a runtime that does not answer says that nothing runs and lets the act report itself.
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
     */
    record Answer(@Nullable When when, int exitCode) {
    }

    private static final String DRAIN = "drain";
    private static final String NOW = "now";
    private static final String RUNNING = "running";

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
            return new Answer(explicit, 0);
        }
        try {
            return new Answer(ask(context, out, act, version, noInput), 0);
        } catch (StartPrompt.MissingInputException _) {
            var locale = context.clientEnvironment().locale();
            out.println(MessageCatalogue.format(locale, "operator.actions.stopped",
                                Map.of("state", "runtime " + act.value())));
            out.flush();
            return new Answer(null, StartDialogue.EXIT_MISSING_INPUT);
        }
    }

    private static @Nullable When ask(OperatorContext context, PrintWriter out, Act act,
                        @Nullable String version, boolean noInput) throws IOException {
        if (noInput) {
            throw new StartPrompt.MissingInputException("runtime " + act.value(), "when", null);
        }
        var locale = context.clientEnvironment().locale();
        var prompt = new StartPrompt(context, out, locale, false);
        var snapshot = snapshot(context);
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
        try {
            return NOW.equals(choice.ask().key()) ? When.NOW : When.WHEN_DONE;
        } catch (StartPrompt.StartLeftException _) {
            return null;
        }
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
        var lines = new ArrayList<String>(4);
        lines.add(MessageCatalogue.format(locale, "system.confirm.title",
                            Map.of("act", act.value(), "version", version != null ? version : "")));
        lines.add("  " + running(locale, snapshot));
        var note = noteKey(act);
        if (note != null) {
            lines.add("  " + MessageCatalogue.format(locale, note, Map.of()));
        }
        lines.add("  " + MessageCatalogue.format(locale, "system.confirm.when", Map.of()));
        return String.join("\n", lines);
    }

    private static String running(String locale, @Nullable SystemSnapshot snapshot) {
        var what = snapshot != null ? what(locale, snapshot) : "";
        return what.isBlank()
                            ? MessageCatalogue.format(locale, "system.confirm.nothing_runs", Map.of())
                            : MessageCatalogue.format(locale, "system.confirm.running", Map.of("what", what));
    }

    private static String what(String locale, SystemSnapshot snapshot) {
        return MessageCatalogue.format(locale, "system.confirm.what",
                            Map.of("plans", count(snapshot.work().plansByState()),
                                                "epics", count(snapshot.work().epicsByState()),
                                                "jobs", jobs(snapshot.jobs()),
                                                "workers", snapshot.workers().size()));
    }

    private static @Nullable String newerVersion(@Nullable SystemSnapshot snapshot) {
        return snapshot != null ? snapshot.runtime().newerVersion() : null;
    }

    private static int count(Map<String, Integer> byState) {
        var count = byState.get(RUNNING);
        return count != null ? count : 0;
    }

    private static int jobs(Map<String, Object> jobs) {
        return slotsUsed(jobs, "build") + slotsUsed(jobs, "test");
    }

    private static int slotsUsed(Map<String, Object> jobs, String kind) {
        var slots = JsonTree.asObject(jobs.get(kind));
        var used = slots != null ? JsonTree.integer(slots, "slots_used") : null;
        return used != null ? used : 0;
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
     * @return the snapshot the runtime serves, or {@code null} when it does not answer with one
     */
    private static @Nullable SystemSnapshot snapshot(OperatorContext context) {
        try {
            var response = context.runtime(false).send(HttpRequest.get("/api/v1/system"));
            return response.status() == 200 ? SystemSnapshot.fromJson(response.bodyText()) : null;
        } catch (IOException _) {
            // A runtime that does not answer has nothing to count; the act reports itself.
            return null;
        }
    }
}
