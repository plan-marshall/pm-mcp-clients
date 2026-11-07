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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.ChatReadBackResource;
import de.planmarshall.api.control.PausePoint;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.operator.actions.WorkActionsClient;
import de.planmarshall.operator.dialogue.ChoicePrompt;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Shared action execution and formatting support for the plan and epic verbs.
 */
final class ActionSupport {

    private ActionSupport() {
    }

    static class TerminationOptions {
        @Parameters(index = "0", arity = "0..1", description = "Work item ID")
        @Nullable
        String id;

        @Option(names = "--reason", description = "Reason for termination")
        @Nullable
        String reason;

        @Option(names = "--close-pr", description = "Close open pull request")
        boolean closePr;

        @Option(names = "--keep-pr", description = "Keep open pull request")
        boolean keepPr;

        @Option(names = "--no-input", description = "Non-interactive mode")
        boolean noInput;
    }

    static class CommonAddOptions {
        @Option(names = "--project", description = "Target project ID")
        @Nullable
        String project;

        @Option(names = "--title", description = "Title")
        @Nullable
        String title;

        @Option(names = "--id", description = "Explicit ID")
        @Nullable
        String id;

        @Option(names = "--description", description = "Description")
        @Nullable
        String description;
    }

    static void populateCommonAdd(Map<String, Object> payload, CommonAddOptions opts) {
        if (opts.project != null && !".".equals(opts.project)) {
            payload.put("project", opts.project);
        }
        if (opts.title != null) payload.put("title", opts.title);
        if (opts.id != null) payload.put("id", opts.id);
        if (opts.description != null) payload.put("description", opts.description);
    }

    static final String CANCEL = "cancel";

    /** Exit code of two flags that contradict each other. */
    static final int EXIT_USAGE = 2;

    /**
     * Two flags of one verb that contradict each other when given together.
     *
     * @param first  the first flag, as the operator types it
     * @param second the second flag, as the operator types it
     */
    record FlagPair(String first, String second) {

        /** What happens to the open pull request of an ended plan. */
        static final FlagPair PULL_REQUEST = new FlagPair("--close-pr", "--keep-pr");

        /** What happens to the running plans of a cancelled epic. */
        static final FlagPair PLANS = new FlagPair("--with-plans", "--without-plans");
    }

    /**
     * Rejects two flags given together that contradict each other, such as {@code --close-pr} with
     * {@code --keep-pr}: the usage error is written to the error stream in the context's locale and the
     * caller returns {@link #EXIT_USAGE} before any request is sent.
     *
     * @param context the operator context, whose locale renders the text
     * @param spec    the picocli command specification
     * @param first   whether the first flag was given
     * @param second  whether the second flag was given
     * @param pair    the names of the two flags, as the operator types them
     * @return {@code true} when both flags were given and the usage error was written
     */
    static boolean contradicts(OperatorContext context, CommandSpec spec, boolean first, boolean second,
                        FlagPair pair) {
        if (!first || !second) {
            return false;
        }
        var err = spec.commandLine().getErr();
        err.println(MessageCatalogue.format(context.clientEnvironment().locale(), "operator.actions.flags.exclusive",
                            Map.of("first", pair.first(), "second", pair.second())));
        err.flush();
        return true;
    }

    /**
     * Builds the pause choice of both entry points: the console's {@code pause} act and the direct
     * {@code plan pause} verb show the same three points, the same consequences, and the same
     * explanation, so the two can never drift apart.
     *
     * @param prompt the dialogue's prompt
     * @param planId the plan
     * @param epicId the plan's epic, or {@code null}
     * @return the configured pause choice
     */
    static ChoicePrompt pausePrompt(StartPrompt prompt, String planId, @Nullable String epicId) {
        var locale = prompt.locale();
        var pause = new ChoicePrompt(prompt, "control.pause").leaveLabel(
                            MessageCatalogue.format(locale, "operator.dialogue.label.pause", Map.of()));
        pause.title(MessageCatalogue.format(locale, "operator.actions.pause.where", Map.of("plan", planId)));
        pause.explanation(MessageCatalogue.format(locale, "operator.actions.pause.explain", Map.of()));
        for (var point : PausePoint.values()) {
            var key = point.value();
            pause.addChoice(key,
                                MessageCatalogue.format(locale, "operator.actions.pause." + key, Map.of()),
                                MessageCatalogue.format(locale, "operator.actions.pause.consequence." + key,
                                                    Map.of()),
                                MessageCatalogue.format(locale, "operator.actions.pause.explain." + key, Map.of()),
                                point == PausePoint.AFTER_TASK);
        }
        if (epicId != null) {
            pause.epicTold(MessageCatalogue.format(locale, "operator.actions.end.told", Map.of("epic", epicId)));
        }
        return pause;
    }

    /**
     * Asks the reason of an act that cannot be undone, echoes it, and lets {@code q}/{@code v} leave
     * the dialogue with nothing sent. A blank answer is reported and answered with {@code null}, so
     * the caller stops without sending; the reading goes through the prompt, which stops a
     * {@code --no-input} run instead of blocking on a terminal nobody watches.
     *
     * @param prompt     the dialogue's prompt
     * @param state      the state id named when {@code --no-input} stops the run
     * @param leaveLabel the human label of the dialogue
     * @param entryId    the entry named when {@code --no-input} stops the run
     * @return the trimmed reason, or {@code null} when it was blank
     * @throws StartPrompt.StartLeftException when the operator left the dialogue
     */
    static @Nullable String askEndReason(StartPrompt prompt, String state, String leaveLabel,
                        @Nullable String entryId) {
        var question = MessageCatalogue.format(prompt.locale(), "operator.actions.end.reason", Map.of());
        var reason = prompt.askRaw(question, state, question, entryId);
        if (ChoicePrompt.isLeave(prompt.locale(), reason)) {
            prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.dialogue.left",
                                Map.of("state", leaveLabel)));
            throw new StartPrompt.StartLeftException();
        }
        if (reason.isBlank()) {
            prompt.line("operator.actions.end.reason_required", Map.of());
            return null;
        }
        return reason;
    }

    /**
     * Names the act the runtime performs in the subject's state: a plan held by an integrity pause is
     * aborted, not cancelled. The act resource is read first, and the verb the operator typed stands
     * in only where the runtime does not answer it.
     *
     * @param context    the operator context
     * @param kind       {@code plan} or {@code epic}
     * @param id         the subject
     * @param actionVerb the verb the operator typed
     * @return {@code abort}, or the typed verb
     */
    static String endOutcome(OperatorContext context, String kind, String id, String actionVerb) {
        try {
            var actions = new WorkActionsClient(context).actions(kind, id);
            return "integrity_pause".equals(actions.holdKind()) ? "abort" : actionVerb;
        } catch (IOException _) {
            return actionVerb;
        }
    }

    /**
     * Executes work cancellation or abandonment (plan, epic).
     *
     * @param context    the operator context
     * @param spec       the picocli command specification
     * @param kind       "plan" or "epic"
     * @param options    the termination options
     * @param actionVerb "cancel" or "abandon"
     * @return exit code (0 on success, 2 for {@code --close-pr} with {@code --keep-pr}, 3 on missing
     *         input, 130 when the operator interrupted)
     * @throws IOException on network or runtime error
     */
    static int endWork(OperatorContext context, CommandSpec spec, String kind,
                        TerminationOptions options, String actionVerb) throws IOException {
        context.gate().verify();
        if (contradicts(context, spec, options.closePr, options.keepPr, FlagPair.PULL_REQUEST)) {
            return EXIT_USAGE;
        }
        var out = spec.commandLine().getOut();
        var id = options.id != null ? options.id : "";
        String effectiveReason = options.reason;
        @Nullable String act = null;
        if (effectiveReason == null || effectiveReason.isBlank()) {
            if (options.noInput) {
                var label = CANCEL.equalsIgnoreCase(actionVerb) ? "Cancellation" : "Abandon";
                out.printf("%s reason required (--reason)%n", label);
                out.flush();
                return 3;
            }
            if (context.isInteractiveTerminal()) {
                var locale = context.clientEnvironment().locale();
                var prompt = new StartPrompt(context, out, locale, false);
                var leaveLabel = MessageCatalogue.format(locale,
                                    "operator.dialogue.label." + actionVerb.toLowerCase(Locale.ROOT), Map.of());
                try {
                    var reason = askEndReason(prompt, "control.action", leaveLabel, id);
                    if (reason == null) {
                        return 1;
                    }
                    effectiveReason = reason;
                    act = endOutcome(context, kind, id, actionVerb);
                    if (!confirmEnd(prompt, act, leaveLabel)) {
                        return 0;
                    }
                } catch (StartPrompt.InterruptException _) {
                    return StartPrompt.EXIT_INTERRUPTED;
                } catch (StartPrompt.StartLeftException _) {
                    return 0;
                }
            } else {
                effectiveReason = (CANCEL.equalsIgnoreCase(actionVerb) ? "Cancelled" : "Abandoned")
                                    + " by operator";
            }
        }

        if (act == null) {
            // Without the dialogue the act is still the one the runtime performs in the plan's state,
            // read before the plan ends and its hold is gone: only a plan's cancel can be an abort.
            act = "plan".equals(kind) && CANCEL.equalsIgnoreCase(actionVerb)
                    ? endOutcome(context, kind, id, actionVerb)
                    : actionVerb;
        }

        var payload = new LinkedHashMap<String, Object>();
        payload.put("reason", effectiveReason);
        payload.put("close_pr", !options.keepPr);

        var endpoint = "/api/v1/" + kind + "s/" + OperatorUrlUtil.encodePathSegment(id) + "/" + actionVerb;
        var response = context.runtime(true).send(HttpRequest.json("POST", endpoint, JsonTree.write(payload)));
        if (response.status() == 200 || response.status() == 202) {
            out.println("✓ " + MessageCatalogue.format(context.clientEnvironment().locale(),
                                "operator.actions.result.ended",
                                Map.of("plan", id, "outcome", endedOutcome(response.bodyText(), act))));
            out.flush();
            return 0;
        }
        throw new OperatorRefusedException(kind + " " + actionVerb, response);
    }

    /**
     * The outcome the ended work has: the runtime's answer names it where it carries an
     * {@code outcome}; otherwise it is the outcome of the act the runtime performed in the subject's
     * state ({@link #endOutcome}), never only the verb the operator typed.
     *
     * @param answer the runtime's answer to the act
     * @param act    the act the runtime performs in the subject's state
     * @return {@code cancelled}, {@code abandoned} or {@code aborted}
     */
    static String endedOutcome(String answer, String act) {
        if (!answer.isBlank()) {
            try {
                var body = JsonTree.asObject(JsonTree.parse(answer));
                var outcome = body != null ? JsonTree.string(body, "outcome") : null;
                if (outcome != null && ENDED_OUTCOMES.contains(outcome)) {
                    return outcome;
                }
            } catch (IOException | IllegalArgumentException _) {
                // an answer that is no JSON object names no outcome; the act names it
            }
        }
        return switch (act.toLowerCase(Locale.ROOT)) {
            case CANCEL -> "cancelled";
            case "abandon" -> "abandoned";
            default -> "aborted";
        };
    }

    private static final List<String> ENDED_OUTCOMES = List.of("cancelled", "abandoned", "aborted");

    private static boolean confirmEnd(StartPrompt prompt, String act, String leaveLabel) {
        var consent = new ChoicePrompt(prompt, "control.action").leaveLabel(leaveLabel);
        consent.title(MessageCatalogue.format(prompt.locale(), "operator.actions.cancel.confirm",
                            Map.of("action", act)) + "?");
        consent.explanation(MessageCatalogue.format(prompt.locale(), "operator.actions.end.consent_explain",
                            Map.of()));
        try {
            return consent.askConsent();
        } catch (StartPrompt.InterruptException interrupt) {
            throw interrupt;
        } catch (StartPrompt.StartLeftException _) {
            return false;
        }
    }

    /**
     * Executes plan or epic chat communication.
     *
     * @param context the operator context
     * @param spec    the picocli command specification
     * @param kind    "plan" or "epic"
     * @param id      the work item ID
     * @param message the chat message, or {@code null} to fetch history
     * @return exit code
     * @throws IOException on network or runtime error
     */
    static int chat(OperatorContext context, CommandSpec spec, String kind, String id,
                        @Nullable String message) throws IOException {
        context.gate().verify();
        var out = spec.commandLine().getOut();
        var endpoint = "/api/v1/" + kind + "s/" + OperatorUrlUtil.encodePathSegment(id) + "/chat";
        if (message != null && !message.isBlank()) {
            var response = context.runtime(true).send(
                                HttpRequest.json("POST", endpoint, JsonTree.write(Map.of("message", message))));
            if (response.status() == 200 || response.status() == 201) {
                out.println(response.bodyText());
                out.flush();
                return 0;
            }
            throw new OperatorRefusedException(kind + " chat", response);
        }

        var response = context.runtime(true).send(HttpRequest.get(endpoint));
        if (response.status() == 200) {
            if (context.isConsoleActive()) {
                var description = WorkChatDialogue.run(context, out, kind, id, false, false);
                if (description != null) {
                    out.println(MessageCatalogue.format(context.clientEnvironment().locale(),
                                        "operator.actions.chat.follow_up_started",
                                        Map.of("description", OutputSafety.stripControlSequences(description))));
                    out.flush();
                    var code = StartDialogue.run(context, out, EntryKind.PLAN,
                                        new StartAnswers().withDescription(description));
                    // The chat is one of the view's other dialogues: a start it opens leaves back
                    // with it, so its interrupt never ends the console or the view with 130.
                    return leaveBack(code);
                }
                return 0;
            }
            WorkChatDialogue.render(context, out, ChatReadBackResource.fromJson(response.bodyText()));
            return 0;
        }
        throw new OperatorRefusedException(kind + " chat", response);
    }

    /**
     * Maps the exit code of a dialogue the view opened onto the view's own: an interrupt inside it
     * is the view's other dialogues' leave-back ({@code q}), never the process's 130
     * ({@code 04-console.adoc#console-dialogue-handoff}).
     *
     * @param code the exit code of the dialogue the view opened
     * @return {@code 0} for an interrupt, the code itself otherwise
     */
    static int leaveBack(int code) {
        return code == StartPrompt.EXIT_INTERRUPTED ? 0 : code;
    }

    /**
     * Renders a list of running work items (plans, epics).
     *
     * @param out      the output writer
     * @param heading  the list header
     * @param stateKey the property name for state/phase
     * @param list     the list of work item maps
     */
    static void printWorkList(PrintWriter out, String heading, String stateKey, List<?> list) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(heading, "heading");
        Objects.requireNonNull(stateKey, "stateKey");
        if (list.isEmpty()) {
            out.println("No " + heading.toLowerCase() + " running.");
            return;
        }
        out.println(heading);
        for (var item : list) {
            if (item instanceof Map<?, ?> work) {
                var title = work.get("title");
                out.printf("  %-20s %-12s %s%n",
                                    work.get("id"),
                                    work.get(stateKey),
                                    title != null ? title : "");
            } else {
                out.println("  " + item);
            }
        }
    }
}
