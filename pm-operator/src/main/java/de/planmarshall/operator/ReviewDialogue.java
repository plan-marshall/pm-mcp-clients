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
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.DraftedPlan;
import de.planmarshall.api.control.ReviewDisposition;
import de.planmarshall.api.control.ReviewItemResource;
import de.planmarshall.api.control.ReviewResource;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.actions.WorkActionsClient;

/**
 * The findings or watches review of an epic ({@code review.open}, {@code review.item},
 * {@code review.keep}, {@code review.drop}, {@code review.commit}, UC-CTL-3).
 *
 * <p>It goes through the register's items one at a time, open ones first, and shows each item's
 * statement, rules, origin, and proposal. An item from outside the machine is printed as text
 * between quotes and never rendered. Keep, drop, the remaining proposals, and the finish write the
 * decisions to the epic's ledger.
 */
final class ReviewDialogue {

    private static final String STATE_ID = "control.review";
    private static final String REGISTER_FINDINGS = "findings";
    private static final String REGISTER_WATCHES = "watches";

    private final OperatorContext context;
    private final PrintWriter out;
    private final StartPrompt prompt;
    private final WorkActionsClient client;
    private final String epicId;
    private final String register;

    private ReviewDialogue(OperatorContext context, PrintWriter out, String epicId, String register,
                        boolean noInput) {
        this.context = context;
        this.out = out;
        this.epicId = epicId;
        this.register = register;
        this.prompt = new StartPrompt(context, out, context.clientEnvironment().locale(), noInput);
        this.client = new WorkActionsClient(context);
    }

    /**
     * Runs one register's review.
     *
     * @param context  the operator context
     * @param out      the verb's output
     * @param epicId   the epic
     * @param register {@code findings} or {@code watches}
     * @param noInput  whether the verb may ask nothing
     * @return the exit code
     * @throws IOException on transport failure
     */
    static int run(OperatorContext context, PrintWriter out, String epicId, String register,
                        boolean noInput) throws IOException {
        var dialogue = new ReviewDialogue(context, out, epicId,
                            REGISTER_WATCHES.equals(register) ? REGISTER_WATCHES : REGISTER_FINDINGS, noInput);
        try {
            return dialogue.execute();
        } catch (StartPrompt.MissingInputException _) {
            out.println(MessageCatalogue.format(dialogue.prompt.locale(), "operator.actions.review.stopped", Map.of()));
            out.flush();
            return StartDialogue.EXIT_MISSING_INPUT;
        } catch (StartPrompt.StartLeftException _) {
            return 0;
        }
    }

    private int execute() throws IOException {
        var review = client.review(epicId, register);
        while (true) {
            var open = review.openItems();
            if (open.isEmpty()) {
                return finish();
            }
            var item = open.getFirst();
            show(review, item);
            var decision = askDecision(item, review);
            if (decision == null) {
                return 0;
            }
            review = switch (decision) {
                case KEEP -> keep(item);
                case DROP -> drop(item);
                case PROMOTE, EDIT -> {
                    prompt.line("operator.actions.review.no_draft", Map.of());
                    yield review;
                }
                case CHAT -> {
                    WorkChatDialogue.run(context, out, "epic", epicId, false, !prompt.asks());
                    yield client.review(epicId, register);
                }
                case REMAINING -> acceptRemaining(review);
                case FINISH -> review;
            };
            if (decision == Decision.FINISH) {
                return finish();
            }
        }
    }

    private enum Decision {
        KEEP, DROP, PROMOTE, EDIT, CHAT, REMAINING, FINISH
    }

    private void show(ReviewResource review, ReviewItemResource item) {
        prompt.raw(MessageCatalogue.format(prompt.locale(), "control.review.title",
                            Map.of("epic", OutputSafety.stripControlSequences(epicId),
                                                "register", register)));
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.review.progress",
                            Map.of("done", review.decided(), "total", review.total())));
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.review.item",
                            Map.of("item", OutputSafety.stripControlSequences(item.itemId()),
                                                "title", firstLine(item.statement()),
                                                "state", item.disposition().value())));
        field("control.review.statement", text(item));
        if (!item.rules().isEmpty()) {
            field("control.review.rules", String.join("; ", cleanAll(item.rules())));
        }
        field("control.review.origin", OutputSafety.stripControlSequences(item.origin()));
        if (item.proposal() != null && !item.proposal().isBlank()) {
            field("control.review.proposal", OutputSafety.stripControlSequences(item.proposal()));
        }
        var draft = review.draftedPlan();
        if (draft != null) {
            showDraft(draft);
        }
    }

    private void showDraft(DraftedPlan draft) {
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.review.draft",
                            Map.of("plan", OutputSafety.stripControlSequences(draft.planId()))));
        if (draft.objective() != null) {
            field("decomposition.objective", OutputSafety.stripControlSequences(draft.objective()));
        }
        if (!draft.deliverables().isEmpty()) {
            field("decomposition.deliverables", String.join("; ", cleanAll(draft.deliverables())));
        }
        if (!draft.surface().isEmpty()) {
            field("decomposition.surface", String.join(", ", cleanAll(draft.surface())));
        }
    }

    private void field(String labelKey, String value) {
        prompt.line("operator.actions.review.field",
                            Map.of("label", MessageCatalogue.format(prompt.locale(), labelKey, Map.of()),
                                                "text", value));
    }

    private @Nullable Decision askDecision(ReviewItemResource item, ReviewResource review) {
        var options = new StringBuilder();
        options.append(key("keep")).append(") ").append(text("control.review.keep_finding")).append("   ");
        options.append(key("drop")).append(") ").append(text("control.review.drop")).append("   ");
        if (item.proposal() != null && !item.proposal().isBlank() && review.draftedPlan() != null) {
            options.append(key("promote")).append(") ").append(text("control.review.promote")).append("   ");
            options.append("e) ").append(text("control.review.edit")).append("   ");
        }
        options.append(key("chat")).append(") ").append(text("control.review.chat")).append("   ");
        if (review.openItems().size() > 1) {
            options.append(key("accept_remaining")).append(") ")
                                .append(MessageCatalogue.format(prompt.locale(), "operator.actions.review.remaining",
                                                    Map.of("count", review.openItems().size()))).append("   ");
        }
        options.append(key("finish")).append(") ").append(text("operator.actions.review.finish"))
                            .append(StartPrompt.SUFFIX);
        var answer = prompt.askInline(options.toString(), STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "control.review.title",
                                                Map.of("epic", epicId, "register", register)), epicId);
        if (answer.isEmpty()) {
            return null;
        }
        if (answer.equalsIgnoreCase(key("keep"))) {
            return Decision.KEEP;
        }
        if (answer.equalsIgnoreCase(key("drop"))) {
            return Decision.DROP;
        }
        if (answer.equalsIgnoreCase(key("promote"))) {
            return Decision.PROMOTE;
        }
        if ("e".equalsIgnoreCase(answer)) {
            return Decision.EDIT;
        }
        if (answer.equalsIgnoreCase(key("chat"))) {
            return Decision.CHAT;
        }
        if (answer.equalsIgnoreCase(key("accept_remaining"))) {
            return Decision.REMAINING;
        }
        if (answer.equalsIgnoreCase(key("finish"))) {
            return Decision.FINISH;
        }
        return null;
    }

    private ReviewResource keep(ReviewItemResource item) throws IOException {
        var other = REGISTER_FINDINGS.equals(register) ? REGISTER_WATCHES : REGISTER_FINDINGS;
        var answer = prompt.askRaw("1) " + registerLabel(register) + "  2) " + registerLabel(other)
                            + StartPrompt.SUFFIX, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "control.review.keep_finding", Map.of()),
                            epicId);
        boolean stay = !"2".equals(answer);
        var disposition = stay ? keptValue() : movedValue();
        var updated = client.disposition(epicId, item.itemId(), disposition, null);
        if (stay) {
            prompt.line("operator.actions.review.kept",
                                Map.of("item", OutputSafety.stripControlSequences(item.itemId()),
                                                    "register", register));
        } else {
            prompt.line("operator.actions.review.moved",
                                Map.of("item", OutputSafety.stripControlSequences(item.itemId()),
                                                    "register", other));
        }
        return updated;
    }

    private ReviewResource drop(ReviewItemResource item) throws IOException {
        var reason = prompt.askRaw(text("control.review.drop_reason") + StartPrompt.SUFFIX, STATE_ID,
                            text("control.review.drop"), epicId);
        var updated = client.disposition(epicId, item.itemId(), ReviewDisposition.DROPPED.value(), reason);
        prompt.line("operator.actions.review.dropped",
                            Map.of("item", OutputSafety.stripControlSequences(item.itemId())));
        return updated;
    }

    private ReviewResource acceptRemaining(ReviewResource review) throws IOException {
        var open = review.openItems();
        for (var item : open) {
            prompt.raw("  " + OutputSafety.stripControlSequences(item.itemId()) + "  "
                                + firstLine(item.statement()));
        }
        var answer = prompt.askRaw(MessageCatalogue.format(prompt.locale(),
                            "control.review.accept_remaining_confirm",
                            Map.of("count", open.size())) + "   " + text("operator.actions.yes_no")
                            + StartPrompt.SUFFIX, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "control.review.accept_remaining",
                                                Map.of("count", open.size())), epicId);
        if (!Answers.isYes(answer)) {
            return review;
        }
        var updated = review;
        for (var item : open) {
            updated = client.disposition(epicId, item.itemId(), keptValue(), null);
            prompt.line("operator.actions.review.kept",
                                Map.of("item", OutputSafety.stripControlSequences(item.itemId()),
                                                    "register", register));
        }
        return updated;
    }

    private int finish() throws IOException {
        if (!prompt.asks()) {
            StartPrompt.missing(STATE_ID, text("operator.actions.review.finish"), epicId);
        }
        var answer = prompt.askRaw(MessageCatalogue.format(prompt.locale(), "control.review.finish", Map.of())
                            + "   " + text("operator.actions.yes_no") + StartPrompt.SUFFIX, STATE_ID,
                            MessageCatalogue.format(prompt.locale(), "control.review.finish", Map.of()), epicId);
        if (!Answers.isYes(answer)) {
            return 0;
        }
        var finished = client.commitReview(epicId);
        prompt.line("operator.actions.review.finished",
                            Map.of("epic", OutputSafety.stripControlSequences(epicId),
                                                "done", finished.decided(), "total", finished.total()));
        return 0;
    }

    private String keptValue() {
        return REGISTER_FINDINGS.equals(register) ? ReviewDisposition.KEPT.value()
                            : ReviewDisposition.WATCH.value();
    }

    private String movedValue() {
        return REGISTER_FINDINGS.equals(register) ? ReviewDisposition.WATCH.value()
                            : ReviewDisposition.KEPT.value();
    }

    private String registerLabel(String register) {
        return REGISTER_FINDINGS.equals(register)
                            ? MessageCatalogue.format(prompt.locale(), "operator.actions.review.keep_finding",
                            Map.of())
                            : MessageCatalogue.format(prompt.locale(), "operator.actions.review.keep_watch",
                            Map.of());
    }

    private String text(String key) {
        return MessageCatalogue.format(prompt.locale(), key, Map.of());
    }

    private String key(String name) {
        return MessageCatalogue.format(prompt.locale(), "operator.actions.key." + name, Map.of());
    }

    private String text(ReviewItemResource item) {
        var statement = OutputSafety.stripControlSequences(item.statement()).replace('\n', ' ');
        if (item.isUntrusted()) {
            return "\"" + statement + "\"   "
                                + MessageCatalogue.format(prompt.locale(), "operator.actions.review.untrusted",
                                Map.of());
        }
        return statement;
    }

    private static String firstLine(String text) {
        var clean = OutputSafety.stripControlSequences(text).replace('\n', ' ').trim();
        var cut = clean.length() > 60 ? clean.substring(0, 60) + "\u2026" : clean;
        return cut;
    }

    private static List<String> cleanAll(List<String> values) {
        var clean = new ArrayList<String>(values.size());
        for (var value : values) {
            clean.add(OutputSafety.stripControlSequences(value).replace('\n', ' '));
        }
        return clean;
    }
}
