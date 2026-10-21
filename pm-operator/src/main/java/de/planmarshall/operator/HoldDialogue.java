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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.HoldResolutionResult;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.actions.WorkActionsClient;

/**
 * What holds a plan, and its acts ({@code hold.show}, {@code hold.resolve}, {@code hold.apply},
 * UC-CTL-4).
 *
 * <p>It shows the hold's kind, its cause, how often it was tried, and — for an integrity pause —
 * the tampered file with its recorded and current size and digest. An act that cannot be undone
 * confirms first; after the act the plan either goes on or the remaining hold is shown again with
 * what still holds it.
 */
final class HoldDialogue {

    private static final String STATE_ID = "control.hold";
    private static final String ACT_ACCEPT = "accept";
    private static final String ACT_SHOW_PR = "show_pr";

    private final StartPrompt prompt;
    private final WorkActionsClient client;
    private final String planId;
    private final @Nullable String preselectedAct;

    private HoldDialogue(OperatorContext context, PrintWriter out, String planId, boolean noInput,
                        @Nullable String preselectedAct) {
        this.planId = planId;
        this.preselectedAct = preselectedAct;
        this.prompt = new StartPrompt(context, out, context.clientEnvironment().locale(), noInput);
        this.client = new WorkActionsClient(context);
    }

    /**
     * Runs the dialogue of what holds a plan.
     *
     * @param context the operator context
     * @param out     the verb's output
     * @param planId  the plan
     * @param holdId  the hold to resolve, or {@code null} for the first
     * @param noInput whether the verb may ask nothing
     * @return the exit code
     * @throws IOException on transport failure
     */
    static int run(OperatorContext context, PrintWriter out, String planId, @Nullable String holdId,
                        boolean noInput) throws IOException {
        return run(context, out, planId, holdId, null, noInput);
    }

    /**
     * Runs the dialogue of what holds a plan, with the act an actions menu already chose.
     *
     * @param context the operator context
     * @param out     the verb's output
     * @param planId  the plan
     * @param holdId  the hold to resolve, or {@code null} for the first
     * @param act     the act the menu chose, or {@code null} to ask it
     * @param noInput whether the verb may ask nothing
     * @return the exit code
     * @throws IOException on transport failure
     */
    static int run(OperatorContext context, PrintWriter out, String planId, @Nullable String holdId,
                        @Nullable String act, boolean noInput) throws IOException {
        var dialogue = new HoldDialogue(context, out, planId, noInput, act);
        try {
            return dialogue.execute(holdId);
        } catch (StartPrompt.MissingInputException _) {
            return StartDialogue.EXIT_MISSING_INPUT;
        } catch (StartPrompt.StartLeftException _) {
            return 0;
        }
    }

    private int execute(@Nullable String holdId) throws IOException {
        var holds = client.holds(planId);
        if (holds.isEmpty()) {
            prompt.line("operator.actions.hold.none", Map.of("plan", planId));
            return 0;
        }
        var hold = select(holds, holdId);
        String chosen = preselectedAct;
        while (true) {
            show(hold);
            var act = chosen != null ? chosen : askAct(hold);
            chosen = null;
            if (act == null) {
                return 0;
            }
            if (ACT_SHOW_PR.equals(act)) {
                showPullRequest(hold);
                continue;
            }
            var reason = confirm(hold, act);
            if (reason == null) {
                return 0;
            }
            HoldResolutionResult result;
            try {
                result = client.resolveHold(planId, hold.holdId(), act, reason);
            } catch (IOException refused) {
                prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.hold.refused",
                                    Map.of("reason", OutputSafety.stripControlSequences(refused.getMessage()))));
                return 1;
            }
            if (result.released()) {
                prompt.line("control.hold.released", Map.of("subject", OutputSafety.stripControlSequences(
                                    result.subject())));
                if (ACT_ACCEPT.equals(act)) {
                    prompt.line("operator.actions.hold.goes_on", Map.of("plan", planId));
                }
                return 0;
            }
            prompt.line("control.hold.still_held",
                                Map.of("cause", OutputSafety.stripControlSequences(
                                                    result.cause() != null ? result.cause() : "")));
            holds = client.holds(planId);
            if (holds.isEmpty()) {
                prompt.line("control.hold.released", Map.of("subject", planId));
                return 0;
            }
            hold = select(holds, hold.holdId());
        }
    }

    private void show(HoldRecord hold) {
        var kind = MessageCatalogue.format(prompt.locale(), "control.hold.kind." + hold.kind(), Map.of());
        prompt.raw(MessageCatalogue.format(prompt.locale(), "control.hold.title",
                            Map.of("name", OutputSafety.stripControlSequences(hold.subjectId()),
                                                "kind", kind)));
        prompt.line("operator.actions.hold.cause",
                            Map.of("cause", OutputSafety.stripControlSequences(hold.cause())));
        if (hold.attempts() > 0) {
            prompt.line("control.hold.tried",
                                Map.of("count", hold.attempts(),
                                                    "time", hold.lastTriedAt() != null
                                                                        ? time(hold.lastTriedAt()) : ""));
        }
        if ("integrity_pause".equals(hold.kind())) {
            prompt.line("control.hold.integrity.file",
                                Map.of("file", OutputSafety.stripControlSequences(
                                                    hold.filePath() != null ? hold.filePath() : "")));
            prompt.line("operator.actions.hold.recorded",
                                Map.of("size", hold.recordedSize() != null ? hold.recordedSize() : 0,
                                                    "digest", OutputSafety.stripControlSequences(
                                                                        hold.recordedDigest() != null
                                                                                            ? hold.recordedDigest() : "")));
            prompt.line("operator.actions.hold.current",
                                Map.of("size", hold.currentSize() != null ? hold.currentSize() : 0,
                                                    "digest", OutputSafety.stripControlSequences(
                                                                        hold.currentDigest() != null
                                                                                            ? hold.currentDigest() : "")));
            prompt.line("control.hold.integrity.no_restore", Map.of());
        }
    }

    private @Nullable String askAct(HoldRecord hold) {
        var options = new StringBuilder();
        for (var act : hold.acts()) {
            options.append(MessageCatalogue.format(prompt.locale(), "operator.actions.key."
                                + keyName(act), Map.of()))
                                .append(") ").append(actLabel(act)).append("   ");
        }
        options.append(MessageCatalogue.format(prompt.locale(), "operator.actions.hold.prompt", Map.of()))
                            .append(StartPrompt.SUFFIX);
        var answer = prompt.askInline(options.toString(), STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "control.hold.resolve", Map.of()), planId);
        if (answer.isEmpty() || "q".equalsIgnoreCase(answer)) {
            return null;
        }
        for (var act : hold.acts()) {
            var key = MessageCatalogue.format(prompt.locale(), "operator.actions.key." + keyName(act), Map.of());
            if (key.equalsIgnoreCase(answer)) {
                return act;
            }
        }
        for (var act : hold.acts()) {
            if (act.equalsIgnoreCase(answer)) {
                return act;
            }
        }
        return null;
    }

    /**
     * @return the reason to send with the act, an empty string when the act needs none, or
     *         {@code null} when the operator declined
     */
    private @Nullable String confirm(HoldRecord hold, String act) {
        String reason = "";
        if (ACT_ACCEPT.equals(act)) {
            var answer = prompt.askRaw(MessageCatalogue.format(prompt.locale(),
                                                    "operator.actions.hold.accept_prompt",
                                                    Map.of("file", OutputSafety.stripControlSequences(
                                                                        hold.filePath() != null ? hold.filePath() : ""))),
                                STATE_ID, MessageCatalogue.format(prompt.locale(),
                                                    "operator.actions.hold.confirm_accept", Map.of()), planId);
            if (!"y".equalsIgnoreCase(answer)) {
                return null;
            }
        } else if (irreversible(act)) {
            var question = MessageCatalogue.format(prompt.locale(), "operator.actions.hold.confirm",
                                Map.of("action", actLabel(act)));
            var answer = prompt.askRaw(question, STATE_ID, act, planId);
            if (!"y".equalsIgnoreCase(answer)) {
                return null;
            }
        }
        if ("end_plan".equals(act) || "abort".equals(act) || "cancel_epic".equals(act)) {
            reason = prompt.askRaw(MessageCatalogue.format(prompt.locale(),
                                "operator.actions.end.reason", Map.of()) + StartPrompt.SUFFIX,
                                STATE_ID, MessageCatalogue.format(prompt.locale(),
                                                    "operator.actions.end.reason", Map.of()), planId);
        }
        return reason;
    }

    private void showPullRequest(HoldRecord hold) {
        var url = hold.pullRequestUrl();
        if (url == null || url.isBlank()) {
            prompt.line("operator.actions.hold.act.show_pr", Map.of());
            return;
        }
        var clean = OutputSafety.stripControlSequences(url);
        prompt.line("operator.actions.pr_open", Map.of("number", number(clean), "url", clean));
    }

    private String actLabel(String act) {
        return switch (act) {
            case "grant" -> MessageCatalogue.format(prompt.locale(), "operator.actions.hold.act.grant", Map.of());
            case "add_credential" -> MessageCatalogue.format(prompt.locale(),
                                "operator.actions.hold.act.add_credential", Map.of());
            case "raise_budget" -> MessageCatalogue.format(prompt.locale(),
                                "operator.actions.hold.act.raise_budget", Map.of());
            case "show_pr" -> MessageCatalogue.format(prompt.locale(),
                                "operator.actions.hold.act.show_pr", Map.of());
            case ACT_ACCEPT -> MessageCatalogue.format(prompt.locale(), "control.hold.act.accept_change", Map.of());
            case "abort" -> MessageCatalogue.format(prompt.locale(), "control.hold.act.abort_plan", Map.of());
            case "end_plan" -> MessageCatalogue.format(prompt.locale(), "control.hold.act.end_plan", Map.of());
            case "cancel_epic" -> MessageCatalogue.format(prompt.locale(), "control.hold.act.cancel_epic",
                                Map.of());
            case "give_up_removal" -> MessageCatalogue.format(prompt.locale(),
                                "control.hold.act.give_up_removal", Map.of());
            case "recycle" -> MessageCatalogue.format(prompt.locale(), "control.hold.act.recycle", Map.of());
            default -> MessageCatalogue.format(prompt.locale(), "control.hold.act.retry", Map.of());
        };
    }

    private static String keyName(String act) {
        return switch (act) {
            case "accept" -> "accept_change";
            case "abort" -> "abort_plan";
            default -> act;
        };
    }

    private static boolean irreversible(String act) {
        return ACT_ACCEPT.equals(act) || "end_plan".equals(act) || "abort".equals(act)
                            || "cancel_epic".equals(act) || "give_up_removal".equals(act);
    }

    private static HoldRecord select(List<HoldRecord> holds, @Nullable String holdId) {
        if (holdId == null || holdId.isBlank()) {
            return holds.getFirst();
        }
        for (var hold : holds) {
            if (hold.holdId().equals(holdId)) {
                return hold;
            }
        }
        return holds.getFirst();
    }

    private static String number(String url) {
        var slash = url.lastIndexOf('/');
        return slash >= 0 && slash + 1 < url.length() ? url.substring(slash + 1) : "";
    }

    private static String time(Instant instant) {
        return DateTimeFormatter.ofPattern("HH:mm")
                            .format(instant.atZone(ZoneId.systemDefault()));
    }
}
