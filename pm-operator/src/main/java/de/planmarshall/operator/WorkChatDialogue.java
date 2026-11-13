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

import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.TurnKind;
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
    private static final String ROLE_OPERATOR = ChatTurn.AUTHOR_OPERATOR;

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
            // The chat is one of the view's other dialogues (04-console.adoc#console-dialogue-handoff):
            // q and Ctrl+C both leave it back to the view, and nothing is sent.
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
    static void render(OperatorContext context, PrintWriter out, ChatResource chat) {
        var dialogue = new WorkChatDialogue(context, out, chat.subject().kind().wireValue(), chat.subject().id(),
                            false, true);
        boolean endedChat = chat.scope() == ChatScopeKind.ARCHIVE;
        dialogue.showHead(chat, endedChat);
        for (var turn : chat.turns()) {
            dialogue.showTurn(turn);
        }
        var proposal = chat.proposal();
        if (proposal != null && chat.hasProposal()) {
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
            var applies = dialogue.appliesText(proposal);
            if (applies != null) {
                out.println("  " + OutputSafety.stripControlSequences(applies));
            }
        }
        out.flush();
    }

    private @Nullable String execute() throws IOException {
        var chat = client.chat(kind, subjectId);
        boolean endedChat = ended || chat.scope() == ChatScopeKind.ARCHIVE;
        showHead(chat, endedChat);
        for (var turn : chat.turns()) {
            showTurn(turn);
        }
        var seen = lastTurnId(chat);
        boolean saidNoProposal = false;
        while (true) {
            if (chat.hasProposal()) {
                var proposal = chat.proposal();
                var answer = askProposal(chat, endedChat);
                if (answer == null) {
                    return null;
                }
                if (answer) {
                    if (endedChat) {
                        prompt.line("operator.actions.chat.kept", Map.of("subject", subjectId));
                        return proposal == null ? null : followUp(proposal);
                    }
                    chat = client.chatProposal(kind, subjectId, proposal.turnId(), true);
                    showNewTurns(chat, seen);
                    seen = lastTurnId(chat);
                    prompt.line("operator.actions.chat.kept", Map.of("subject", subjectId));
                    continue;
                }
                chat = client.chatProposal(kind, subjectId, proposal.turnId(), false);
                showNewTurns(chat, seen);
                seen = lastTurnId(chat);
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
            seen = lastTurnId(chat);
            if (!chat.hasProposal() && !saidNoProposal) {
                prompt.line("operator.actions.chat.no_proposal", Map.of("subject", subjectId));
                saidNoProposal = true;
            }
        }
    }

    /**
     * @return {@code true} to accept, {@code false} to reject, or {@code null} to leave the chat
     */
    private @Nullable Boolean askProposal(ChatResource chat, boolean endedChat) {
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
        var applies = appliesText(proposal);
        if (applies != null) {
            prompt.raw("  " + OutputSafety.stripControlSequences(applies));
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

    private void showHead(ChatResource chat, boolean endedChat) {
        var scope = switch (chat.scope()) {
            case EPIC -> "operator.actions.chat.scope.epic";
            case ARCHIVE -> "operator.actions.chat.scope.archive";
            default -> "operator.actions.chat.scope.plan";
        };
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.chat.head",
                            Map.of("subject", OutputSafety.stripControlSequences(chat.subject().id()),
                                                "role", chat.role().name(),
                                                "scope", MessageCatalogue.format(prompt.locale(), scope, Map.of())))
                            + "   " + MessageCatalogue.format(prompt.locale(), "operator.actions.chat_end",
                            Map.of()));
        if (endedChat) {
            prompt.raw("  " + MessageCatalogue.format(prompt.locale(), "operator.actions.chat.read_only",
                                Map.of()));
        }
    }

    private void showNewTurns(ChatResource chat, @Nullable String seenTurnId) {
        var turns = chat.turns();
        var seen = -1;
        if (seenTurnId != null) {
            for (int index = 0; index < turns.size(); index++) {
                if (turns.get(index).turnId().equals(seenTurnId)) {
                    seen = index;
                    break;
                }
            }
        }
        // A page that no longer holds the last turn shown is printed whole: a re-print is recoverable,
        // a turn silently left out is not.
        for (int index = seen + 1; index < turns.size(); index++) {
            showTurn(turns.get(index));
        }
    }

    /**
     * @param chat the chat as read
     * @return the id of its last turn, or {@code null} where it has none
     */
    private static @Nullable String lastTurnId(ChatResource chat) {
        var turns = chat.turns();
        return turns.isEmpty() ? null : turns.getLast().turnId();
    }

    private void showTurn(ChatTurn turn) {
        if (turn.kind() == TurnKind.PROPOSAL) {
            // The proposal is the read-back block's; a transcript line for it would say it twice.
            return;
        }
        var role = ROLE_OPERATOR.equals(turn.author())
                            ? MessageCatalogue.format(prompt.locale(), "operator.actions.chat.you", Map.of())
                            : OutputSafety.stripControlSequences(turn.author());
        var text = OutputSafety.stripControlSequences(turn.text()).replace('\n', ' ');
        if (turn.untrusted()) {
            text = "\"" + text + "\"   "
                                + MessageCatalogue.format(prompt.locale(), "operator.actions.review.untrusted",
                                Map.of());
        }
        prompt.raw("  " + role + "  " + text);
    }

    /**
     * @param proposal the proposal the role read back
     * @return when it applies, read from the runtime's message key, or {@code null} where it names none
     */
    private @Nullable String appliesText(ChatProposal proposal) {
        var applies = proposal.applies();
        if (applies == null) {
            return null;
        }
        var key = applies.key();
        var point = key.substring(key.lastIndexOf('.') + 1);
        return MessageCatalogue.format(prompt.locale(), "operator.actions.chat.applies." + point, applies.args());
    }

    private static @Nullable String followUp(ChatProposal proposal) {
        if (proposal.after() != null && !proposal.after().isBlank()) {
            return proposal.after();
        }
        return proposal.summary();
    }
}
