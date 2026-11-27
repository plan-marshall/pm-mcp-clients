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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.actions.WorkActionsClient;
import de.planmarshall.operator.dialogue.ChoicePrompt;

/**
 * The chat with a plan's or an epic's planning role ({@code chat.open}, {@code chat.turn},
 * {@code chat.read-back}, {@code chat.apply}, {@code chat.end}).
 *
 * <p>One dialogue wherever it is opened; here it is the one of a plan's or an epic's actions and of
 * ended work. A message is never interpreted or read back: the role answers it, and only a change
 * the role proposes is read back, old against new, and applied only when the operator accepts it.
 * The chat about ended work never changes the archive; its one proposal is a follow-up plan's
 * description, and answering it accepts it on the runtime, which opens the start and names no way
 * to the entry — the terminal says so and starts nothing a second time.
 */
final class WorkChatDialogue {

    private static final String STATE_ID = "control.chat";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_CHAT_LEFT = "operator.actions.chat.left";
    private static final String KEY_CHAT_NEW_COMMAND = "operator.actions.chat.new_command";
    private static final String ROLE_OPERATOR = ChatTurn.AUTHOR_OPERATOR;

    private enum ProposalDecision {
        ACCEPT,
        REJECT,
        NEW
    }

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
     * @return the dialogue's exit code ({@code 0}, {@link StartDialogue#EXIT_MISSING_INPUT})
     * @throws IOException on transport failure
     */
    static int run(OperatorContext context, PrintWriter out, String kind, String subjectId,
                        boolean ended, boolean noInput) throws IOException {
        var dialogue = new WorkChatDialogue(context, out, kind, subjectId, ended, noInput);
        try {
            return dialogue.execute();
        } catch (StartPrompt.MissingInputException _) {
            out.println(MessageCatalogue.format(dialogue.prompt.locale(), "operator.actions.chat.no_input", Map.of()));
            out.flush();
            return StartDialogue.EXIT_MISSING_INPUT;
        } catch (StartPrompt.InterruptException _) {
            // The chat is one of the view's other dialogues (04-console.adoc#console-dialogue-handoff):
            // q and Ctrl+C both leave it back to the view, and nothing is sent.
            return 0;
        } catch (StartPrompt.StartLeftException _) {
            out.println(MessageCatalogue.format(dialogue.prompt.locale(), KEY_CHAT_LEFT,
                                Map.of(KEY_SUBJECT, OutputSafety.stripControlSequences(subjectId))));
            out.flush();
            return 0;
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
        dialogue.showTranscript(chat, endedChat);
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

    private int execute() throws IOException {
        if (!prompt.asks()) {
            throw new StartPrompt.MissingInputException(STATE_ID, "turn", subjectId);
        }
        var chat = client.openChat(kind, subjectId);
        boolean endedChat = ended || chat.scope() == ChatScopeKind.ARCHIVE;
        showTranscript(chat, endedChat);
        var seen = lastTurnId(chat);
        boolean saidNoProposal = false;
        while (true) {
            if (chat.hasProposal()) {
                var proposalOutcome = handleProposal(chat, endedChat, seen);
                if (proposalOutcome == null) {
                    return 0;
                }
                chat = proposalOutcome;
                seen = lastTurnId(chat);
                continue;
            }
            var message = askMessageOrNew();
            if (message == null) {
                var reset = confirmAndStartNew(null);
                if (reset != null) {
                    chat = reset;
                    seen = lastTurnId(chat);
                    saidNoProposal = false;
                }
                continue;
            }
            var updated = sendTurn(message);
            if (updated == null) {
                continue;
            }
            chat = updated;
            showNewTurns(chat, seen);
            seen = lastTurnId(chat);
            saidNoProposal = reportNoProposalOnce(chat, saidNoProposal);
        }
    }

    private @Nullable ChatResource sendTurn(String message) throws IOException {
        var clientTurnId = UUID.randomUUID().toString();
        while (true) {
            try {
                return client.chatTurn(kind, subjectId, message, clientTurnId);
            } catch (IOException failure) {
                prompt.raw(OperatorRefusedException.screenText(failure, prompt.locale()));
                var retryTitle = MessageCatalogue.format(prompt.locale(), "common.action.retry", Map.of()) + "?";
                if (!new ChoicePrompt(prompt, STATE_ID).title(retryTitle).askConsent()) {
                    return null;
                }
            }
        }
    }

    private void showTranscript(ChatResource chat, boolean endedChat) {
        showHead(chat, endedChat);
        for (var turn : chat.turns()) {
            showTurn(turn);
        }
    }

    /**
     * Reads one message (one or more non-empty lines, sent on an empty line), or returns
     * {@code null} when the operator typed {@code new}/{@code neu} on the first line. An empty
     * first line, the localized leave key, or {@code Ctrl+D} throws
     * {@link StartPrompt.StartLeftException} so the dialogue leaves without closing the chat.
     */
    private @Nullable String askMessageOrNew() {
        var leaveHint = MessageCatalogue.format(prompt.locale(), "operator.actions.chat_leave", Map.of());
        var first = prompt.askInline(MessageCatalogue.format(prompt.locale(), "operator.actions.chat.you",
                            Map.of()) + StartPrompt.SUFFIX, STATE_ID, leaveHint, subjectId);
        var trimmed = first.trim();
        if (first.isBlank() || ChoicePrompt.isLeave(prompt.locale(), trimmed)) {
            throw new StartPrompt.StartLeftException();
        }
        if (isNewCommand(trimmed)) {
            return null;
        }
        var lines = new ArrayList<String>();
        lines.add(first);
        while (true) {
            var next = prompt.askRaw("", STATE_ID, leaveHint, subjectId);
            if (next.isBlank()) {
                return String.join("\n", lines);
            }
            lines.add(next);
        }
    }

    private boolean isNewCommand(String trimmedInput) {
        var command = MessageCatalogue.format(prompt.locale(), KEY_CHAT_NEW_COMMAND, Map.of());
        return command.equalsIgnoreCase(trimmedInput);
    }

    private @Nullable ChatResource confirmAndStartNew(@Nullable ChatProposal openProposal) throws IOException {
        if (openProposal != null && openProposal.isOpen()) {
            var kindText = MessageCatalogue.format(prompt.locale(), "control.chat.proposal.kind",
                                Map.of("kind", openProposal.kind().wireValue()));
            prompt.line("control.chat.new_confirm.body", Map.of(
                                "kind", kindText,
                                KEY_SUBJECT, OutputSafety.stripControlSequences(subjectId)));
        }
        var title = MessageCatalogue.format(prompt.locale(), "control.chat.new_confirm.title", Map.of());
        var question = title + "   "
                            + MessageCatalogue.format(prompt.locale(), "operator.actions.yes_no", Map.of())
                            + " [Enter]";
        while (true) {
            var answer = prompt.askRaw(question + StartPrompt.SUFFIX, STATE_ID, title, subjectId);
            if (Answers.isYes(answer)) {
                client.endChat(kind, subjectId);
                var reopened = client.openChat(kind, subjectId);
                prompt.line("control.chat.ended",
                                    Map.of(KEY_SUBJECT, OutputSafety.stripControlSequences(subjectId)));
                return reopened;
            }
            if (answer.isEmpty() || Answers.isNo(answer)) {
                return null;
            }
            if (ChoicePrompt.isLeave(prompt.locale(), answer)) {
                throw new StartPrompt.StartLeftException();
            }
            prompt.line("operator.dialogue.not_offered",
                                Map.of("input", TerminalSanitizer.sanitizeLine(answer),
                                                    "offered", "y, n"));
        }
    }

    private boolean reportNoProposalOnce(ChatResource chat, boolean saidNoProposal) {
        if (!chat.hasProposal() && !saidNoProposal) {
            prompt.line("operator.actions.chat.no_proposal", Map.of(KEY_SUBJECT, subjectId));
            return true;
        }
        return saidNoProposal;
    }

    /**
     * Answers the proposal the chat holds, as the operator answered it.
     *
     * @param chat      the chat with an open proposal
     * @param endedChat {@code true} for the read-only chat about ended work
     * @param seen      the id of the last turn shown, or {@code null}
     * @return the chat as the runtime answered it, or {@code null} to leave the chat
     * @throws IOException on transport failure
     */
    private @Nullable ChatResource handleProposal(ChatResource chat, boolean endedChat, @Nullable String seen)
                        throws IOException {
        var proposal = chat.proposal();
        var decision = askProposal(chat, endedChat);
        if (decision == ProposalDecision.NEW) {
            var reset = confirmAndStartNew(proposal);
            return reset != null ? reset : chat;
        }
        if (decision == ProposalDecision.ACCEPT) {
            if (endedChat) {
                // The accept opens the start on the runtime; no B1 surface names the entry it
                // opened, so the operator is told the start is being prepared and the chat is
                // left with the proposal answered (PLAN-161, D2).
                var accepted = proposal;
                var description = accepted == null ? null : followUp(accepted);
                client.chatProposal(kind, subjectId, accepted.turnId(), true);
                if (description != null) {
                    prompt.line("operator.actions.chat.follow_up_started",
                                        Map.of("description", description));
                }
                return null;
            }
            var acceptedChat = client.chatProposal(kind, subjectId, proposal.turnId(), true);
            showNewTurns(acceptedChat, seen);
            var boundary = proposal != null && proposal.applies() != null
                                && proposal.applies().key().endsWith("after_task");
            prompt.line("operator.actions.chat.accepted",
                                Map.of("when", boundary ? "boundary" : "other"));
            return acceptedChat;
        }
        var rejectedChat = client.chatProposal(kind, subjectId, proposal.turnId(), false);
        showNewTurns(rejectedChat, seen);
        return rejectedChat;
    }

    /**
     * @return the operator's choice at the open proposal
     */
    private ProposalDecision askProposal(ChatResource chat, boolean endedChat) {
        var proposal = chat.proposal();
        if (proposal == null) {
            throw new StartPrompt.StartLeftException();
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
        var question = MessageCatalogue.format(prompt.locale(), endedChat
                            ? "operator.actions.chat.follow_up"
                            : "operator.actions.chat.accept", Map.of());
        var rejectKey = MessageCatalogue.format(prompt.locale(), "operator.actions.key.reject", Map.of());
        while (true) {
            var answer = prompt.askRaw(question + "   " + MessageCatalogue.format(prompt.locale(),
                                "operator.actions.accept_reject", Map.of("key", rejectKey)),
                                STATE_ID, question, subjectId);
            if (answer.isEmpty()) {
                return ProposalDecision.ACCEPT;
            }
            if (answer.equalsIgnoreCase(rejectKey)) {
                return ProposalDecision.REJECT;
            }
            if (isNewCommand(answer.trim())) {
                return ProposalDecision.NEW;
            }
            if (ChoicePrompt.isLeave(prompt.locale(), answer)) {
                throw new StartPrompt.StartLeftException();
            }
            // Any other input is answered with one line and the proposal is asked again; only the leave
            // key leaves the chat (design Dialogues, links as keys).
            prompt.line("?".equals(answer) ? "operator.dialogue.no_explanation" : "operator.dialogue.not_offered",
                                Map.of("input", TerminalSanitizer.sanitizeLine(answer), "offered",
                                                    rejectKey + ", " + leaveKey() + ", ?"));
        }
    }

    private String leaveKey() {
        return prompt.locale().toLowerCase(Locale.ROOT).startsWith("de") ? "v" : "q";
    }

    private void showHead(ChatResource chat, boolean endedChat) {
        var scope = switch (chat.scope()) {
            case EPIC -> "operator.actions.chat.scope.epic";
            case ARCHIVE -> "operator.actions.chat.scope.archive";
            default -> "operator.actions.chat.scope.plan";
        };
        prompt.raw(MessageCatalogue.format(prompt.locale(), "operator.actions.chat.head",
                            Map.of(KEY_SUBJECT, OutputSafety.stripControlSequences(chat.subject().id()),
                                                "role", chat.role().name(),
                                                "scope", MessageCatalogue.format(prompt.locale(), scope, Map.of())))
                            + "   " + MessageCatalogue.format(prompt.locale(), "operator.actions.chat_leave",
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
     * @return the id of its last turn that is no longer being written, or {@code null} where it has
     *         none: a streaming turn stays unseen so its finished text is printed once, whole
     */
    private static @Nullable String lastTurnId(ChatResource chat) {
        var turns = chat.turns();
        for (int index = turns.size() - 1; index >= 0; index--) {
            if (turns.get(index).status() != TurnStatus.STREAMING) {
                return turns.get(index).turnId();
            }
        }
        return null;
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
