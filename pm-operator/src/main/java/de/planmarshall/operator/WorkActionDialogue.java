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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.ArchiveCheckLine;
import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.operator.actions.WorkActionsClient;

/**
 * One action of a plan or an epic, run as its dialogue ({@code doc/design/terminal-client/19-actions.adoc}).
 *
 * <p>It first asks the runtime what the state offers: an action the state does not allow is refused
 * with the state and the actions it allows, and one that applies but cannot run now is refused with
 * the runtime's reason. Then the dialogue runs: pausing with its point, cancelling or abandoning
 * with the reason and the pull request, a plan's settings, the review, the chat, the archive check,
 * the epic's next plan, automatic mode, and the common files.
 */
final class WorkActionDialogue {

    private static final String STATE_ID = "control.action";
    private static final String PLAN = "plan";
    private static final String EPIC = "epic";
    private static final Set<String> HOLD_ACTS = Set.of("retry", "recycle", "grant", "add_credential",
                        "raise_budget", "accept", "abort", "end_plan", "cancel_epic", "show_pr",
                        "give_up_removal");

    private final OperatorContext context;
    private final PrintWriter out;
    private final StartPrompt prompt;
    private final WorkActionsClient client;
    private final String kind;
    private final String id;

    private WorkActionDialogue(OperatorContext context, PrintWriter out, String kind, String id,
                        boolean noInput) {
        this.context = context;
        this.out = out;
        this.kind = kind;
        this.id = id;
        this.prompt = new StartPrompt(context, out, context.clientEnvironment().locale(), noInput);
        this.client = new WorkActionsClient(context);
    }

    /**
     * Runs one action of a plan or an epic.
     *
     * @param context the operator context
     * @param out     the verb's output
     * @param kind    {@code plan} or {@code epic}
     * @param id      the subject
     * @param action  the action the runtime offered
     * @param noInput whether the verb may ask nothing
     * @param epic    the epic as the view holds it, or {@code null}
     * @param holdId  the hold an act belongs to, or {@code null} for the first
     * @return the exit code
     * @throws IOException on transport failure
     */
    static int run(OperatorContext context, PrintWriter out, String kind, String id, String action,
                        boolean noInput, @Nullable EpicResource epic, @Nullable String holdId)
                        throws IOException {
        if (HOLD_ACTS.contains(action) && PLAN.equals(kind)) {
            return HoldDialogue.run(context, out, id, holdId, action, noInput);
        }
        var dialogue = new WorkActionDialogue(context, out, kind, id, noInput);
        try {
            return dialogue.execute(action, epic);
        } catch (StartPrompt.MissingInputException _) {
            out.println(MessageCatalogue.format(dialogue.prompt.locale(), "operator.actions.stopped", Map.of()));
            out.flush();
            return StartDialogue.EXIT_MISSING_INPUT;
        } catch (StartPrompt.StartLeftException _) {
            return 0;
        }
    }

    private int execute(String action, @Nullable EpicResource epic) throws IOException {
        var actions = client.actions(kind, id);
        if (EPIC.equals(kind) && HOLD_ACTS.contains(action)) {
            return "cancel_epic".equals(action) ? endEpic() : generic(action);
        }
        var availability = actions.action(action);
        if (availability == null) {
            prompt.line("operator.actions.refused",
                                Map.of("name", id, "state", actions.state(),
                                                    "actions", allowed(actions)));
            return 1;
        }
        if (!availability.available()) {
            prompt.line("control.action.disabled",
                                Map.of("action", action, "reason", availability.reason() != null
                                                    ? availability.reason() : ""));
            return 1;
        }
        return switch (action) {
            case "pause" -> pause();
            case "start" -> start();
            case "cancel", "abandon" -> EPIC.equals(kind) ? endEpic() : end(action, actions);
            case "chat" -> chat(epic);
            case "settings" -> PlanSettingsDialogue.run(context, out, id, null, !prompt.asks());
            case "next" -> next(epic);
            case "auto" -> auto(epic);
            case "common_files" -> commonFiles();
            case "decompose" -> decompose(epic);
            case "findings" -> ReviewDialogue.run(context, out, id, "findings", !prompt.asks());
            case "watches" -> ReviewDialogue.run(context, out, id, "watches", !prompt.asks());
            case "archive" -> archive();
            default -> {
                prompt.line("operator.actions.refused",
                                    Map.of("name", id, "state", actions.state(),
                                                        "actions", allowed(actions)));
                yield 1;
            }
        };
    }

    private int generic(String action) throws IOException {
        client.perform(kind, id, action, Map.of());
        prompt.line("operator.actions.result.done", Map.of("action", action, "subject", id));
        return 0;
    }

    private int pause() throws IOException {
        var options = MessageCatalogue.format(prompt.locale(), "operator.actions.pause.ask",
                            Map.of("plan", id));
        var answer = prompt.askRaw(options, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "operator.actions.pause.task", Map.of()), id);
        var point = switch (answer) {
            case "1", "now" -> "now";
            case "3", "phase" -> "phase";
            default -> "task";
        };
        client.perform(kind, id, "pause", Map.of("point", point));
        if ("now".equals(point)) {
            prompt.line("operator.actions.result.paused", Map.of("plan", id));
        } else {
            prompt.line("operator.actions.result.pausing", Map.of("plan", id, "point", point));
        }
        return 0;
    }

    private int start() throws IOException {
        client.perform(kind, id, "start", Map.of());
        prompt.line("operator.actions.result.started", Map.of("plan", id));
        return 0;
    }

    private int end(String action, WorkActions actions) throws IOException {
        var outcome = integrityPaused(actions) ? "abort" : action;
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.end.title",
                            Map.of("action", outcome, "plan", id)));
        if (actions.epicId() != null) {
            prompt.raw("  " + MessageCatalogue.format(prompt.locale(), "operator.actions.end.its_epic", Map.of())
                                + "   " + MessageCatalogue.format(prompt.locale(), "operator.actions.end.told",
                                Map.of("epic", actions.epicId())));
        }
        prompt.raw("  " + MessageCatalogue.format(prompt.locale(), "operator.actions.end.afterwards", Map.of())
                            + "   " + MessageCatalogue.format(prompt.locale(), "operator.actions.end.afterwards_text",
                            Map.of("outcome", "abort".equals(outcome) ? "aborted"
                                                : "cancel".equals(action) ? "cancelled"
                                                : "abandoned")));
        var reason = askReason();
        if (reason == null) {
            return 0;
        }
        var closeAnswer = prompt.askRaw(MessageCatalogue.format(prompt.locale(),
                            "operator.actions.end.pull_request", Map.of()) + "   "
                            + MessageCatalogue.format(prompt.locale(), "operator.actions.end.close_pr", Map.of())
                            + StartPrompt.SUFFIX, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "operator.actions.end.close_pr", Map.of()),
                            id);
        var confirm = prompt.askRaw(MessageCatalogue.format(prompt.locale(), "operator.actions.end.confirm",
                            Map.of("action", outcome)) + StartPrompt.SUFFIX, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "operator.actions.cancel.confirm",
                                                Map.of("action", outcome)), id);
        if (!Answers.isYes(confirm)) {
            return 0;
        }
        var payload = new LinkedHashMap<String, Object>();
        payload.put("reason", reason);
        payload.put("close_pr", !"2".equals(closeAnswer));
        client.perform(kind, id, action, payload);
        prompt.line("operator.actions.result.ended",
                            Map.of("plan", id, "outcome", "cancel".equals(action) ? "cancelled"
                                                : "abandon".equals(action) ? "abandoned" : "aborted"));
        if (actions.epicId() != null) {
            prompt.line("operator.actions.result.epic_told", Map.of("epic", actions.epicId()));
        }
        return 0;
    }

    private int endEpic() throws IOException {
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.epic.cancel.title",
                            Map.of("epic", id)));
        prompt.raw("  " + MessageCatalogue.format(prompt.locale(), "operator.actions.epic.cancel.plans", Map.of())
                            + "   " + MessageCatalogue.format(prompt.locale(),
                            "operator.actions.epic.cancel.with_plans", Map.of()));
        prompt.raw("  " + MessageCatalogue.format(prompt.locale(), "operator.actions.epic.cancel.afterwards",
                            Map.of())
                            + "   " + MessageCatalogue.format(prompt.locale(),
                            "operator.actions.epic.cancel.afterwards_text", Map.of()));
        var plans = prompt.askRaw(MessageCatalogue.format(prompt.locale(),
                            "operator.actions.epic.cancel.with_plans", Map.of()) + StartPrompt.SUFFIX, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "operator.actions.epic.cancel.plans", Map.of()),
                            id);
        var reason = askReason();
        if (reason == null) {
            return 0;
        }
        var confirm = prompt.askRaw(MessageCatalogue.format(prompt.locale(),
                            "operator.actions.epic.cancel.confirm", Map.of()) + StartPrompt.SUFFIX, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "operator.actions.epic.cancel.title",
                                                Map.of("epic", id)), id);
        if (!Answers.isYes(confirm)) {
            return 0;
        }
        var payload = new LinkedHashMap<String, Object>();
        payload.put("reason", reason);
        payload.put("with_plans", !"2".equals(plans));
        client.perform(EPIC, id, "cancel", payload);
        prompt.line("operator.actions.epic.cancel.done", Map.of("epic", id));
        return 0;
    }

    private @Nullable String askReason() {
        var reason = prompt.askRaw(MessageCatalogue.format(prompt.locale(), "operator.actions.end.reason", Map.of())
                            + StartPrompt.SUFFIX, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "operator.actions.end.reason", Map.of()), id);
        if (reason.isBlank()) {
            prompt.line("operator.actions.end.reason_required", Map.of());
            return null;
        }
        return reason;
    }

    private int chat(@Nullable EpicResource epic) throws IOException {
        var description = WorkChatDialogue.run(context, out, kind, id, false, !prompt.asks());
        if (description != null) {
            startFollowUp(description, epic);
        }
        return 0;
    }

    private void startFollowUp(String description, @Nullable EpicResource epic) {
        var project = epic != null ? epic.projectId() : null;
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.chat.follow_up_started",
                            Map.of("description", OutputSafety.stripControlSequences(description))));
        var answers = new StartAnswers().withDescription(description);
        if (project != null) {
            answers.withProject(project);
        }
        try {
            StartDialogue.run(context, out, EntryKind.PLAN, answers);
        } catch (IOException refused) {
            prompt.raw(OutputSafety.stripControlSequences(refused.getMessage()));
        }
    }

    private int next(@Nullable EpicResource epic) throws IOException {
        if (epic != null && epic.running() >= epic.scope() && nextLaunchable(epic) != null) {
            var answer = prompt.askRaw(MessageCatalogue.format(prompt.locale(), "control.epic.scope_full",
                                Map.of("epic", id, "running", epic.running(), "scope", epic.scope(),
                                                    "plan", nextLaunchable(epic)))
                                + "   " + MessageCatalogue.format(prompt.locale(),
                                "operator.actions.key.raise_and_start", Map.of())
                                + ") " + MessageCatalogue.format(prompt.locale(),
                                "control.epic.next_raise", Map.of("count", epic.scope() + 1))
                                + "   " + MessageCatalogue.format(prompt.locale(),
                                "operator.actions.key.leave_scope", Map.of())
                                + ") " + MessageCatalogue.format(prompt.locale(), "control.epic.next_leave",
                                Map.of()) + StartPrompt.SUFFIX,
                                STATE_ID, MessageCatalogue.format(prompt.locale(), "control.epic.next_raise",
                                                    Map.of("count", epic.scope() + 1)), id);
            if (answer.equalsIgnoreCase(MessageCatalogue.format(prompt.locale(),
                                "operator.actions.key.leave_scope", Map.of()))) {
                return 0;
            }
            client.perform(kind, id, "next", Map.of("raise_scope", true));
            return started(epic);
        }
        client.perform(kind, id, "next", Map.of("raise_scope", false));
        return started(epic);
    }

    private int started(@Nullable EpicResource epic) {
        var plan = epic != null ? nextLaunchable(epic) : null;
        if (plan != null && epic != null) {
            prompt.line("operator.actions.result.next_started",
                                Map.of("plan", plan, "running", epic.running() + 1, "scope", epic.scope()));
        } else {
            prompt.line("control.epic.next_none",
                                Map.of("reasons", epic != null ? String.join(", ", waitReasons(epic)) : ""));
        }
        return 0;
    }

    private static List<String> waitReasons(EpicResource epic) {
        var reasons = new ArrayList<String>();
        for (var workstream : epic.workstreams()) {
            for (var row : workstream.rows()) {
                if ("staged".equals(normalize(row.status())) && row.waitReason() != null
                                    && !"none".equals(row.waitReason())) {
                    reasons.add(row.planId() + " " + row.waitReason());
                }
            }
        }
        return reasons;
    }

    private int auto(@Nullable EpicResource epic) throws IOException {
        var resource = epic != null ? epic : client.epic(id);
        boolean enabled = !resource.autoStart();
        client.perform(kind, id, "auto", Map.of("enabled", enabled));
        if (enabled) {
            prompt.line("operator.actions.result.auto_on", Map.of("epic", id));
        } else {
            prompt.line("control.epic.auto_off_done", Map.of("epic", id));
        }
        return 0;
    }

    private int commonFiles() throws IOException {
        var answer = prompt.askRaw(MessageCatalogue.format(prompt.locale(), "operator.actions.common_files.ask",
                            Map.of("epic", id)) + StartPrompt.SUFFIX, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "control.epic.overlap", Map.of("count", 0)),
                            id);
        int count;
        try {
            count = Integer.parseInt(answer.trim());
        } catch (NumberFormatException _) {
            prompt.line("operator.actions.common_files.invalid", Map.of());
            return 1;
        }
        client.perform(kind, id, "common_files", Map.of("count", count));
        prompt.line("control.epic.overlap_done",
                            Map.of("epic", id, "count", count, "started", "none"));
        return 0;
    }

    private int decompose(@Nullable EpicResource epic) throws IOException {
        var resource = epic != null ? epic : client.epic(id);
        return new EpicDecompositionDialogue(new StartApi(context), prompt, resource.projectId(), id, null).run();
    }

    private int archive() throws IOException {
        var check = client.archiveCheck(id);
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.archive.title", Map.of("epic", id)));
        if (check.completionReviewFirst()) {
            prompt.raw("  " + MessageCatalogue.format(prompt.locale(), "control.archive.review_first", Map.of()));
            return 0;
        }
        for (var line : check.lines()) {
            prompt.raw(checkLine(line));
        }
        prompt.raw("  " + MessageCatalogue.format(prompt.locale(), "operator.actions.archive.carried",
                            Map.of("findings", check.carriedFindings(), "watches", check.carriedWatches())));
        if (!check.passed()) {
            prompt.line("operator.actions.result.not_archivable", Map.of("epic", id));
            return 1;
        }
        var answer = prompt.askRaw(MessageCatalogue.format(prompt.locale(), "control.archive.action", Map.of())
                            + "   " + MessageCatalogue.format(prompt.locale(), "operator.actions.yes_no",
                            Map.of()) + StartPrompt.SUFFIX, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "control.archive.action", Map.of()), id);
        if (!Answers.isYes(answer)) {
            return 0;
        }
        client.perform(kind, id, "archive", Map.of());
        prompt.line("operator.actions.result.archived", Map.of("epic", id));
        return 0;
    }

    private String checkLine(ArchiveCheckLine line) {
        var label = label(line.label());
        var note = line.note() != null ? OutputSafety.stripControlSequences(line.note()) : null;
        return MessageCatalogue.format(prompt.locale(), "operator.actions.archive.check",
                            Map.of("mark", line.passed() ? "\u2713" : "\u2717", "label", label,
                                                "note", note != null ? note : ""));
    }

    private String label(String label) {
        var rendered = MessageCatalogue.format(prompt.locale(), label, Map.of());
        return rendered.equals(label) ? OutputSafety.stripControlSequences(label) : rendered;
    }

    private String allowed(WorkActions actions) {
        var labels = new ArrayList<String>();
        for (var availability : actions.actions()) {
            labels.add(availability.action());
        }
        return String.join(", ", labels);
    }

    private static boolean integrityPaused(WorkActions actions) {
        return "integrity_pause".equals(actions.holdKind());
    }

    private static @Nullable String nextLaunchable(EpicResource epic) {
        for (var workstream : epic.workstreams()) {
            for (var row : workstream.rows()) {
                if ("staged".equals(normalize(row.status()))
                                    && (row.waitReason() == null || "none".equals(row.waitReason()))) {
                    return row.planId();
                }
            }
        }
        return null;
    }

    private static String normalize(String status) {
        return status.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
