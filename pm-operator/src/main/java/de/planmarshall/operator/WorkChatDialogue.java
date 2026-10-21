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
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.ChatProposalResource;
import de.planmarshall.api.control.ChatReadBackResource;
import de.planmarshall.api.control.ChatScope;
import de.planmarshall.api.control.ChatTurnResource;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.actions.WorkActionsClient;

/**
 * The chat with a plan's or an epic's planning role ({@code chat.open}, {@code chat.turn},
 * {@code chat.read-back}, {@code chat.apply}, {@code chat.end}).
 *
 * <p>One dialogue wherever it is opened; here it is the one of a plan's or an epic's actions and of
 * ended work. A message is never interpreted or read back: the role answers it, and only a change
 * the role proposes is read back, old against new, and applied only when the operator accepts it.
 * The chat about ended work never changes the archive; its one proposal is a follow-up plan's
 * description, which {@code Enter} returns so that the caller opens {@code plan add} with it.
 */
final class WorkChatDialogue {

    private static final String STATE_ID = "control.chat";
    private static final String ROLE_OPERATOR = ChatTurnResource.ROLE_OPERATOR;

    private final StartPrompt prompt;
    private final WorkActionsClient client;
    private final String kind;
    private final String subjectId;
    private final boolean ended;

    private WorkChatDialogue(OperatorContext context, PrintWriter out, String kind, String subjectId,
                        boolean ended, boolean noInput) {
        this.kind = kind;
        this.subjectId = subjectId;
        this.ended = ended;
        this.prompt = new StartPrompt(context, out, context.clientEnvironment().locale(), noInput);
        this.client = new WorkActionsClient(context);
    }

    /**
     * Runs one chat.
     *
     * @param context   the operator context
     * @param out       the verb's output
     * @param kind      the subject kind: {@code plan} or {@code epic}
     * @param subjectId the subject
     * @param ended     {@code true} for the read-only chat about ended work
     * @param noInput   whether the verb may ask nothing
     * @return the follow-up description of an ended-work chat, or {@code null}
     * @throws IOException on transport failure
     */
    static @Nullable String run(OperatorContext context, PrintWriter out, String kind, String subjectId,
                        boolean ended, boolean noInput) throws IOException {
        var dialogue = new WorkChatDialogue(context, out, kind, subjectId, ended, noInput);
        try {
            return dialogue.execute();
        } catch (StartPrompt.MissingInputException _) {
            out.println(MessageCatalogue.format(dialogue.prompt.locale(), "operator.actions.chat.no_input", Map.of()));
            out.flush();
            return null;
        } catch (StartPrompt.StartLeftException _) {
            return null;
        }
    }

    /**
     * Prints a chat's head, its transcript, and its open proposal without asking anything: the
     * accessible form of the chat for a terminal that cannot run the dialogue.
     *
     * @param context the operator context
     * @param out     the verb's output
     * @param chat    the chat read-back
     */
    static void render(OperatorContext context, PrintWriter out, ChatReadBackResource chat) {
        var dialogue = new WorkChatDialogue(context, out, chat.subjectKind(), chat.subjectId(), false, true);
        boolean endedChat = chat.chatScope() == ChatScope.ARCHIVE;
        dialogue.showHead(chat, endedChat);
        for (var turn : chat.turns()) {
            dialogue.showTurn(turn);
        }
        var proposal = chat.proposal();
        if (proposal != null) {
            out.println(MessageCatalogue.format(dialogue.prompt.locale(), endedChat
                                ? "operator.actions.chat.proposed_follow_up"
                                : "operator.actions.chat.proposed", Map.of()));
            out.println("  + " + OutputSafety.stripControlSequences(proposal.summary()));
            if (proposal.before() != null) {
                out.println("  - " + OutputSafety.stripControlSequences(proposal.before()));
            }
            if (proposal.after() != null) {
                out.println("  + " + OutputSafety.stripControlSequences(proposal.after()));
            }
        }
        out.flush();
    }

    private @Nullable String execute() throws IOException {
        var chat = client.chat(kind, subjectId);
        boolean endedChat = ended || chat.chatScope() == ChatScope.ARCHIVE;
        showHead(chat, endedChat);
        for (var turn : chat.turns()) {
            showTurn(turn);
        }
        int seen = chat.turns().size();
        boolean saidNoProposal = false;
        while (true) {
            if (chat.proposal() != null) {
                var answer = askProposal(chat, endedChat);
                if (answer == null) {
                    return null;
                }
                if (answer) {
                    if (endedChat) {
                        prompt.line("operator.actions.chat.kept", Map.of("subject", subjectId));
                        return followUp(chat.proposal());
                    }
                    chat = client.chatProposal(kind, subjectId, true);
                    showNewTurns(chat, seen);
                    seen = chat.turns().size();
                    prompt.line("operator.actions.chat.kept", Map.of("subject", subjectId));
                    continue;
                }
                chat = client.chatProposal(kind, subjectId, false);
                continue;
            }
            var message = prompt.askInline(MessageCatalogue.format(prompt.locale(), "operator.actions.chat.you",
                                Map.of()) + StartPrompt.SUFFIX, STATE_ID,
                                MessageCatalogue.format(prompt.locale(), "operator.actions.chat_end", Map.of()),
                                subjectId);
            if (message.isBlank()) {
                prompt.line("operator.actions.chat.kept", Map.of("subject", subjectId));
                return null;
            }
            chat = client.chatTurn(kind, subjectId, message);
            showNewTurns(chat, seen);
            seen = chat.turns().size();
            if (chat.proposal() == null && !saidNoProposal) {
                prompt.line("operator.actions.chat.no_proposal", Map.of("subject", subjectId));
                saidNoProposal = true;
            }
        }
    }

    /**
     * @return {@code true} to accept, {@code false} to reject, or {@code null} to leave the chat
     */
    private @Nullable Boolean askProposal(ChatReadBackResource chat, boolean endedChat) {
        var proposal = chat.proposal();
        if (proposal == null) {
            return null;
        }
        prompt.raw(MessageCatalogue.format(prompt.locale(), endedChat
                            ? "operator.actions.chat.proposed_follow_up"
                            : "operator.actions.chat.proposed", Map.of()));
        prompt.raw("  + " + OutputSafety.stripControlSequences(proposal.summary()));
        if (proposal.before() != null) {
            prompt.raw("  - " + OutputSafety.stripControlSequences(proposal.before()));
        }
        if (proposal.after() != null) {
            prompt.raw("  + " + OutputSafety.stripControlSequences(proposal.after()));
        }
        if (proposal.applies() != null) {
            prompt.raw("  " + OutputSafety.stripControlSequences(proposal.applies()));
        }
        var question = endedChat
                            ? MessageCatalogue.format(prompt.locale(), "operator.actions.chat.follow_up", Map.of())
                            : MessageCatalogue.format(prompt.locale(), "operator.actions.chat.accept", Map.of());
        var answer = prompt.askRaw(question + "   " + MessageCatalogue.format(prompt.locale(),
                            "operator.actions.accept_reject",
                            Map.of("key", MessageCatalogue.format(prompt.locale(),
                                                "operator.actions.key.reject", Map.of()))),
                            STATE_ID, question, subjectId);
        if (answer.isEmpty()) {
            return Boolean.TRUE;
        }
        if (answer.equalsIgnoreCase(MessageCatalogue.format(prompt.locale(),
                            "operator.actions.key.reject", Map.of()))) {
            return Boolean.FALSE;
        }
        return null;
    }

    private void showHead(ChatReadBackResource chat, boolean endedChat) {
        var scope = switch (chat.chatScope()) {
            case EPIC -> "operator.actions.chat.scope.epic";
            case ARCHIVE -> "operator.actions.chat.scope.archive";
            default -> "operator.actions.chat.scope.plan";
        };
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.chat.head",
                            Map.of("subject", OutputSafety.stripControlSequences(chat.subjectId()),
                                                "role", chat.role(),
                                                "scope", MessageCatalogue.format(prompt.locale(), scope, Map.of())))
                            + "   " + MessageCatalogue.format(prompt.locale(), "operator.actions.chat_end",
                            Map.of()));
        if (endedChat) {
            prompt.raw("  " + MessageCatalogue.format(prompt.locale(), "operator.actions.chat.read_only",
                                Map.of()));
        }
    }

    private void showNewTurns(ChatReadBackResource chat, int seen) {
        var turns = chat.turns();
        for (int index = seen; index < turns.size(); index++) {
            showTurn(turns.get(index));
        }
    }

    private void showTurn(ChatTurnResource turn) {
        var role = ROLE_OPERATOR.equals(turn.role())
                            ? MessageCatalogue.format(prompt.locale(), "operator.actions.chat.you", Map.of())
                            : OutputSafety.stripControlSequences(turn.role());
        var text = OutputSafety.stripControlSequences(turn.text()).replace('\n', ' ');
        if (turn.untrusted()) {
            text = "\"" + text + "\"   "
                                + MessageCatalogue.format(prompt.locale(), "operator.actions.review.untrusted",
                                Map.of());
        }
        prompt.raw("  " + role + "  " + text);
    }

    private static @Nullable String followUp(ChatProposalResource proposal) {
        if (proposal.after() != null && !proposal.after().isBlank()) {
            return proposal.after();
        }
        return proposal.summary();
    }
}
