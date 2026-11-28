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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatMessageKeys;
import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatScopeKind;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.MessageText;
import de.planmarshall.api.chat.ProposalState;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.resource.ErrorTexts;
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
    private static final String KEY_PLAN = "plan";
    private static final String KEY_VERSION = "version";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_CHAT_LEFT = "operator.actions.chat.left";
    private static final String KEY_CHAT_NEW_COMMAND = "operator.actions.chat.new_command";
    private static final String KEY_NOT_OFFERED = "operator.dialogue.not_offered";
    private static final String KEY_INPUT = "input";
    private static final String KEY_OFFERED = "offered";
    private static final String FINDING_CHANGED_SINCE = "chat.finding.changed_since";
    private static final String FINDING_ALREADY_QUEUED = "chat.finding.already_queued";
    private static final String ROLE_OPERATOR = ChatTurn.AUTHOR_OPERATOR;

    private enum ProposalDecision {
        ACCEPT,
        REJECT,
        NEW
    }

    private static final class ChatLoopState {

        private final boolean endedChat;
        private final Set<String> printedTurnIds = new HashSet<>();
        private final Set<String> readBackTurnIds = new HashSet<>();
        private final Set<String> acceptedProposalTurnIds = new HashSet<>();
        private final Set<String> reportedAppliedTurnIds = new HashSet<>();
        private final Set<String> reportedLapsedTurnIds = new HashSet<>();
        private boolean saidNoProposal;
        private @Nullable String heldFirstLine;

        private ChatLoopState(boolean endedChat) {
            this.endedChat = endedChat;
        }

        private void reset(ChatResource chat) {
            printedTurnIds.clear();
            readBackTurnIds.clear();
            acceptedProposalTurnIds.clear();
            reportedAppliedTurnIds.clear();
            reportedLapsedTurnIds.clear();
            seedPrintedTurns(chat, printedTurnIds);
            seedAcceptedProposal(chat, acceptedProposalTurnIds);
            saidNoProposal = false;
        }

        private static void seedPrintedTurns(ChatResource chat, Set<String> printedTurnIds) {
            for (var turn : chat.turns()) {
                printedTurnIds.add(turn.turnId());
            }
        }
    }

    private final StartPrompt prompt;
    private final WorkActionsClient client;
    private final String kind;
    private final String subjectId;
    private final boolean ended;
    private boolean chatReset;

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
        dialogue.showTranscript(chat, endedChat, new HashSet<>());
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

    private record TurnSubmission(@Nullable ChatResource updatedChat, @Nullable String heldFirstLine,
    boolean endedExternally) {
    }

    private int execute() throws IOException {
        if (!prompt.asks()) {
            throw new StartPrompt.MissingInputException(STATE_ID, "turn", subjectId);
        }
        try (var follower = ChatStreamFollower.open(client, kind, subjectId, prompt.locale())) {
            var chat = client.openChat(kind, subjectId);
            var loop = new ChatLoopState(ended || chat.scope() == ChatScopeKind.ARCHIVE);
            showTranscript(chat, loop.endedChat, loop.printedTurnIds);
            seedAcceptedProposal(chat, loop.acceptedProposalTurnIds);
            while (true) {
                if (chatReset) {
                    chatReset = false;
                    loop.reset(chat);
                }
                reportProposalStateUpdates(chat, loop);
                var next = runSingleStep(chat, follower, loop);
                if (next == null) {
                    return 0;
                }
                chat = next;
            }
        }
    }

    private @Nullable ChatResource runSingleStep(ChatResource chat, ChatStreamFollower follower,
                        ChatLoopState loop) throws IOException {
        var proposal = chat.proposal();
        if (proposal != null && proposal.isOpen(false)) {
            var proposalOutcome = handleProposal(chat, proposal, loop);
            if (proposalOutcome != null && proposalOutcome.turns().isEmpty()) {
                loop.printedTurnIds.clear();
            }
            return proposalOutcome;
        }
        var message = askMessageOrNew(loop.heldFirstLine);
        loop.heldFirstLine = null;
        if (message == null) {
            var reset = confirmAndStartNew(null);
            return reset != null ? reset : chat;
        }
        var submission = sendTurn(message, follower, chat, loop.printedTurnIds);
        if (submission == null) {
            return chat;
        }
        loop.heldFirstLine = submission.heldFirstLine();
        ChatResource updated = chat;
        var submissionChat = submission.updatedChat();
        if (submissionChat != null) {
            updated = submissionChat;
            showUnprintedTurns(submissionChat, loop.printedTurnIds);
        }
        if (submission.endedExternally()) {
            return null;
        }
        loop.saidNoProposal = reportNoProposalOnce(updated, loop.saidNoProposal);
        return updated;
    }

    private @Nullable TurnSubmission sendTurn(String message, ChatStreamFollower follower, ChatResource currentChat,
                        Set<String> printedTurnIds) throws IOException {
        var clientTurnId = UUID.randomUUID().toString();
        while (true) {
            try {
                if (follower.isActive()) {
                    follower.drainQueuedEvents();
                    var accepted = client.postTurn(kind, subjectId, message, clientTurnId);
                    printedTurnIds.add(accepted.turnId());
                    var outcome = follower.followReply(prompt.reader(), currentChat.id(), accepted.turnId(),
                                        currentChat.role().name(), printedTurnIds);
                    if (outcome.leftDialogue()) {
                        throw new StartPrompt.StartLeftException();
                    }
                    var refreshed = client.readChatQuietly(kind, subjectId, currentChat);
                    return new TurnSubmission(refreshed, outcome.heldFirstLine(), outcome.endedEvent() != null);
                }
                var updated = client.chatTurn(kind, subjectId, message, clientTurnId, currentChat);
                return new TurnSubmission(updated, null, false);
            } catch (IOException failure) {
                prompt.raw(OperatorRefusedException.screenText(failure, prompt.locale()));
                var retryTitle = MessageCatalogue.format(prompt.locale(), "common.action.retry", Map.of()) + "?";
                if (!new ChoicePrompt(prompt, STATE_ID).title(retryTitle).askConsent()) {
                    return null;
                }
            }
        }
    }

    private void showTranscript(ChatResource chat, boolean endedChat, Set<String> printedTurnIds) {
        showHead(chat, endedChat);
        for (var turn : chat.turns()) {
            printedTurnIds.add(turn.turnId());
            showTurn(turn);
        }
    }

    /**
     * Reads one message (one or more non-empty lines, sent on an empty line), or returns
     * {@code null} when the operator typed {@code new}/{@code neu} on the first line. An empty
     * first line, the localized leave key, or {@code Ctrl+D} throws
     * {@link StartPrompt.StartLeftException} so the dialogue leaves without closing the chat.
     */
    private @Nullable String askMessageOrNew(@Nullable String prefill) {
        var leaveHint = MessageCatalogue.format(prompt.locale(), "operator.actions.chat_leave", Map.of());
        var first = prompt.askInline(MessageCatalogue.format(prompt.locale(), "operator.actions.chat.you",
                            Map.of()) + StartPrompt.SUFFIX, STATE_ID, leaveHint, subjectId, prefill);
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

    private String newConfirmPrompt(String title) {
        return title + "   "
                            + MessageCatalogue.format(prompt.locale(), "operator.actions.yes_no", Map.of())
                            + " [Enter]";
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
        var question = newConfirmPrompt(title);
        while (true) {
            var answer = prompt.askRaw(question + StartPrompt.SUFFIX, STATE_ID, title, subjectId);
            if (Answers.isYes(answer)) {
                client.endChat(kind, subjectId);
                var reopened = client.openChat(kind, subjectId);
                prompt.line("control.chat.ended",
                                    Map.of(KEY_SUBJECT, OutputSafety.stripControlSequences(subjectId)));
                chatReset = true;
                return reopened;
            }
            if (answer.isEmpty() || Answers.isNo(answer)) {
                return null;
            }
            if (ChoicePrompt.isLeave(prompt.locale(), answer)) {
                throw new StartPrompt.StartLeftException();
            }
            prompt.line(KEY_NOT_OFFERED,
                                Map.of(KEY_INPUT, TerminalSanitizer.sanitizeLine(answer),
                                                    KEY_OFFERED, yesNoHint()));
        }
    }

    private String yesNoHint() {
        return prompt.locale().toLowerCase(Locale.ROOT).startsWith("de") ? "j, n" : "y, n";
    }

    private static void seedAcceptedProposal(ChatResource chat, Set<String> acceptedProposalTurnIds) {
        var proposal = chat.proposal();
        if (proposal != null && proposal.state() == ProposalState.ACCEPTED) {
            acceptedProposalTurnIds.add(proposal.turnId());
        }
    }

    private void reportProposalStateUpdates(ChatResource chat, ChatLoopState loop) {
        var proposal = chat.proposal();
        if (proposal == null) {
            return;
        }
        var cleanSubject = OutputSafety.stripControlSequences(subjectId);
        if (proposal.state() == ProposalState.LAPSED && loop.reportedLapsedTurnIds.add(proposal.turnId())) {
            printProposalReadBack(proposal, loop.endedChat, loop.readBackTurnIds);
            prompt.line("control.chat.lapsed", Map.of(KEY_PLAN, cleanSubject));
        } else if (!loop.endedChat && proposal.state() == ProposalState.APPLIED
                            && loop.acceptedProposalTurnIds.contains(proposal.turnId())
                            && loop.reportedAppliedTurnIds.add(proposal.turnId())) {
            prompt.line("operator.actions.chat.applied",
                                Map.of(KEY_SUBJECT, cleanSubject, KEY_VERSION, resolveVersion(chat)));
        }
    }

    private boolean reportNoProposalOnce(ChatResource chat, boolean saidNoProposal) {
        if (!chat.hasProposal() && !saidNoProposal) {
            prompt.line("operator.actions.chat.no_proposal",
                                Map.of(KEY_SUBJECT, OutputSafety.stripControlSequences(subjectId)));
            return true;
        }
        return saidNoProposal;
    }

    /**
     * Answers the proposal the chat holds, as the operator answered it.
     *
     * @param chat     the chat with an open or refused proposal
     * @param proposal the open or refused proposal
     * @param loop     the active chat loop state
     * @return the chat as the runtime answered it, or {@code null} to leave the chat
     * @throws IOException on transport failure
     */
    private @Nullable ChatResource handleProposal(ChatResource chat, ChatProposal proposal,
                        ChatLoopState loop) throws IOException {
        printProposalReadBack(proposal, loop.endedChat, loop.readBackTurnIds);
        if (proposal.state() == ProposalState.REFUSED) {
            printFinding(chat, proposal.finding());
            return handleRejectOnlyProposal(chat, proposal, loop.endedChat, loop.printedTurnIds);
        }
        var decision = askProposal(loop.endedChat);
        if (decision == ProposalDecision.NEW) {
            var reset = confirmAndStartNew(proposal);
            return reset != null ? reset : chat;
        }
        if (decision == ProposalDecision.ACCEPT) {
            return acceptProposal(chat, proposal, loop);
        }
        var rejectedChat = client.chatProposal(kind, subjectId, proposal.turnId(), false);
        showUnprintedTurns(rejectedChat, loop.printedTurnIds);
        return rejectedChat;
    }

    private @Nullable ChatResource acceptProposal(ChatResource chat, ChatProposal proposal,
                        ChatLoopState loop) throws IOException {
        var cleanSubject = OutputSafety.stripControlSequences(subjectId);
        if (loop.endedChat) {
            client.chatProposal(kind, subjectId, proposal.turnId(), true);
            var resolvedProject = client.endedProject(kind, subjectId);
            var project = OutputSafety.stripControlSequences(
                                resolvedProject != null ? resolvedProject : subjectId);
            prompt.line("control.chat.follow_up.opened", Map.of(KEY_PROJECT, project));
            prompt.line(KEY_CHAT_LEFT, Map.of(KEY_SUBJECT, cleanSubject));
            return null;
        }
        ChatResource answered;
        try {
            answered = client.chatProposal(kind, subjectId, proposal.turnId(), true);
        } catch (OperatorRefusedException refusal) {
            var refused = handleAcceptConflict(chat, proposal, refusal, loop.printedTurnIds, cleanSubject);
            if (refused != null) {
                return refused;
            }
            throw refusal;
        }
        showUnprintedTurns(answered, loop.printedTurnIds);
        var updated = answered.proposal();
        if (updated != null) {
            reportAcceptedOutcome(answered, updated, loop, cleanSubject);
        }
        return answered;
    }

    private @Nullable ChatResource handleAcceptConflict(ChatResource chat, ChatProposal proposal,
                        OperatorRefusedException refusal, Set<String> printedTurnIds, String cleanSubject)
                        throws IOException {
        if (refusal.code() != ApiErrorCode.CONFLICT) {
            return null;
        }
        var serverMessage = refusal.serverMessage();
        if (FINDING_CHANGED_SINCE.equals(serverMessage)) {
            prompt.line("control.chat.changed_since",
                                Map.of(KEY_PLAN, cleanSubject, KEY_VERSION, resolveVersion(chat)));
            return handleRejectOnlyProposal(chat, proposal, false, printedTurnIds);
        }
        if (FINDING_ALREADY_QUEUED.equals(serverMessage)) {
            prompt.line("control.chat.already_queued", Map.of(KEY_PLAN, cleanSubject));
            return chat;
        }
        return null;
    }

    private void reportAcceptedOutcome(ChatResource answered, ChatProposal updated, ChatLoopState loop,
                        String cleanSubject) {
        switch (updated.state()) {
            case ACCEPTED -> printAcceptedNotice(updated, loop);
            case APPLIED -> reportAppliedOutcome(answered, updated, loop, cleanSubject);
            case LAPSED -> reportLapsedOutcome(updated, loop, cleanSubject);
            case REFUSED -> {
                // Remains ProposalState.REFUSED: the next loop iteration prints the finding and
                // offers r) reject only without repeating the read-back diff.
            }
            default -> {
                // Other states need no accept confirmation line.
            }
        }
    }

    private void reportAppliedOutcome(ChatResource answered, ChatProposal updated, ChatLoopState loop,
                        String cleanSubject) {
        if (loop.reportedAppliedTurnIds.add(updated.turnId())) {
            prompt.line("operator.actions.chat.applied",
                                Map.of(KEY_SUBJECT, cleanSubject, KEY_VERSION, resolveVersion(answered)));
        }
    }

    private void reportLapsedOutcome(ChatProposal updated, ChatLoopState loop, String cleanSubject) {
        if (loop.reportedLapsedTurnIds.add(updated.turnId())) {
            loop.readBackTurnIds.add(updated.turnId());
            prompt.line("control.chat.lapsed", Map.of(KEY_PLAN, cleanSubject));
        }
    }

    private void printAcceptedNotice(ChatProposal updated, ChatLoopState loop) {
        loop.acceptedProposalTurnIds.add(updated.turnId());
        var applies = updated.applies();
        if (applies != null && applies.key().endsWith("on_start")) {
            var onStart = appliesText(updated);
            if (onStart != null) {
                prompt.raw("  " + OutputSafety.stripControlSequences(onStart));
            }
            return;
        }
        var boundary = applies != null && applies.key().endsWith("after_task");
        prompt.line("operator.actions.chat.accepted",
                            Map.of("when", boundary ? "boundary" : "other"));
    }

    private @Nullable ChatResource handleRejectOnlyProposal(ChatResource chat, ChatProposal proposal,
                        boolean endedChat, Set<String> printedTurnIds) throws IOException {
        var decision = askRejectOnly(endedChat);
        if (decision == ProposalDecision.NEW) {
            var reset = confirmAndStartNew(proposal);
            return reset != null ? reset : chat;
        }
        var rejectedChat = client.chatProposal(kind, subjectId, proposal.turnId(), false);
        showUnprintedTurns(rejectedChat, printedTurnIds);
        return rejectedChat;
    }

    private void printProposalReadBack(ChatProposal proposal, boolean endedChat, Set<String> readBackTurnIds) {
        if (!readBackTurnIds.add(proposal.turnId())) {
            return;
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
    }

    private void printFinding(ChatResource chat, @Nullable MessageText finding) {
        if (finding == null) {
            return;
        }
        var rendered = ChatMessageKeys.render(finding);
        var args = new LinkedHashMap<String, Object>(rendered.args());
        args.putIfAbsent(KEY_PLAN, OutputSafety.stripControlSequences(subjectId));
        if ("control.chat.changed_since".equals(rendered.key())) {
            args.putIfAbsent(KEY_VERSION, resolveVersion(chat));
        }
        if ("control.chat.check_failed".equals(rendered.key()) && !args.containsKey("reason")) {
            args.put("reason", ErrorTexts.title(prompt.locale(), ApiErrorCode.CONFLICT));
        }
        prompt.line(rendered.key(), args);
    }

    private String resolveVersion(ChatResource chat) {
        if (!"epic".equals(kind)) {
            var version = client.planVersion(subjectId);
            if (version != null) {
                return String.valueOf(version);
            }
        }
        for (var knows : chat.context().knows()) {
            if ("chat.knows.version".equals(knows.key())) {
                var value = knows.args().get(KEY_VERSION);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        }
        return "?";
    }

    /**
     * @return the operator's choice at the open proposal
     */
    private ProposalDecision askProposal(boolean endedChat) {
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
            prompt.line("?".equals(answer) ? "operator.dialogue.no_explanation" : KEY_NOT_OFFERED,
                                Map.of(KEY_INPUT, TerminalSanitizer.sanitizeLine(answer), KEY_OFFERED,
                                                    rejectKey + ", " + leaveKey() + ", ?"));
        }
    }

    private ProposalDecision askRejectOnly(boolean endedChat) {
        var title = MessageCatalogue.format(prompt.locale(), endedChat
                            ? "operator.actions.chat.follow_up"
                            : "operator.actions.chat.accept", Map.of());
        var rejectKey = MessageCatalogue.format(prompt.locale(), "operator.actions.key.reject", Map.of());
        var rejectLabel = MessageCatalogue.format(prompt.locale(), "operator.actions.chat.reject", Map.of());
        var question = rejectOnlyQuestion(rejectKey, rejectLabel);
        while (true) {
            var answer = prompt.askRaw(question, STATE_ID, title, subjectId);
            if (answer.equalsIgnoreCase(rejectKey)) {
                return ProposalDecision.REJECT;
            }
            if (isNewCommand(answer.trim())) {
                return ProposalDecision.NEW;
            }
            if (ChoicePrompt.isLeave(prompt.locale(), answer)) {
                throw new StartPrompt.StartLeftException();
            }
            prompt.line("?".equals(answer) ? "operator.dialogue.no_explanation" : KEY_NOT_OFFERED,
                                Map.of(KEY_INPUT, TerminalSanitizer.sanitizeLine(answer), KEY_OFFERED,
                                                    rejectKey + ", " + leaveKey() + ", ?"));
        }
    }

    private String rejectOnlyQuestion(String rejectKey, String rejectLabel) {
        return rejectKey + ") " + rejectLabel;
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

    private void showUnprintedTurns(ChatResource chat, Set<String> printedTurnIds) {
        for (var turn : chat.turns()) {
            if (printedTurnIds.add(turn.turnId())) {
                showTurn(turn);
            }
        }
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
}
