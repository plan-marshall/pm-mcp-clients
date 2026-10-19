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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.planmarshall.api.control.ChatProposalResource;
import de.planmarshall.api.control.ChatTurnResource;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.start.EntryKind;
import de.planmarshall.api.start.StartChatResource;
import de.planmarshall.api.start.StartStateResource;

/**
 * The one chat dialogue of the terminal client (design <em>Starting Work</em>, chatting about the
 * request): its scope is the entry, its role is routed by the kind, and its one change is the
 * description, read back against the old one and stored only on <em>Accept</em>. An empty line ends
 * the chat; the entry then goes on as before.
 */
final class ChatDialogue {

    private static final String KEY_TEXT = "text";
    private static final String KEY_STORED = "start.chat.stored";
    private static final String KEY_ENDED = "start.chat.ended";

    private final StartApi api;
    private final StartPrompt prompt;
    private final EntryKind kind;
    private final String project;
    private int seenTurns;

    /**
     * @param api     the start contract
     * @param prompt  the dialogue's input and output surface
     * @param kind    the kind of work the entry starts
     * @param project the project
     */
    ChatDialogue(StartApi api, StartPrompt prompt, EntryKind kind, String project) {
        this.api = Objects.requireNonNull(api, "api");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.project = Objects.requireNonNull(project, "project");
    }

    /**
     * Runs the chat until the operator ends it.
     *
     * @param state the entry's state after the chat was opened
     * @return the entry's state after the chat ended
     * @throws IOException on transport failure
     */
    StartStateResource run(StartStateResource state) throws IOException {
        var entry = state.entry();
        if (entry == null || state.chat() == null) {
            return state;
        }
        var chat = state.chat();
        prompt.raw(heading(chat));
        renderTurns(chat.turns());
        StartStateResource current;
        while (true) {
            var answer = prompt.askInline(text("start.chat.prompt", Map.of()), "start.sources",
                                text("start.chat.open", Map.of()), entry.entryId());
            if (answer.isEmpty()) {
                current = api.endChat(entry.entryId());
                prompt.raw(text(KEY_ENDED, Map.of()));
                return current;
            }
            try {
                current = api.chatTurn(entry.entryId(), answer);
            } catch (StartApi.StartActException e) {
                prompt.raw(OutputSafety.stripControlSequences(e.getMessage()));
                continue;
            }
            var updated = current.chat();
            if (updated != null) {
                renderTurns(updated.turns());
                if (updated.proposal() != null) {
                    readBack(current, Objects.requireNonNull(updated.proposal()));
                }
            }
        }
    }

    private void readBack(StartStateResource state, ChatProposalResource proposal)
                        throws IOException {
        prompt.raw(text("start.chat.proposal", Map.of("summary",
                            TerminalSanitizer.sanitizeLine(proposal.summary()))));
        if (proposal.before() != null) {
            prompt.raw(text("start.chat.before", Map.of("before",
                                TerminalSanitizer.sanitizeLine(proposal.before()))));
        }
        if (proposal.after() != null) {
            prompt.raw(text("start.chat.after", Map.of("after",
                                TerminalSanitizer.sanitizeLine(proposal.after()))));
        }
        var answer = prompt.askRaw(text("start.chat.accept_prompt", Map.of()), "start.sources",
                            text("start.chat.open", Map.of()), state.entryId());
        var accept = answer.isEmpty() || "y".equalsIgnoreCase(answer) || "a".equalsIgnoreCase(answer);
        if (!accept && !"r".equalsIgnoreCase(answer)) {
            return;
        }
        try {
            var updated = api.chatProposal(state.entryId(), accept);
            if (accept) {
                prompt.raw(text(KEY_STORED, Map.of()));
            }
            var chat = updated.chat();
            if (chat != null) {
                renderTurns(chat.turns());
            }
        } catch (StartApi.StartActException e) {
            prompt.raw(OutputSafety.stripControlSequences(e.getMessage()));
        }
    }

    private String heading(StartChatResource chat) {
        var subject = text("start.chat.subject_new", Map.of("kind", kind.wireValue(), "project", project));
        return text("start.chat.heading", Map.of("subject", subject,
                            "role", TerminalSanitizer.sanitizeLine(chat.role().value())));
    }

    private void renderTurns(List<ChatTurnResource> turns) {
        for (int index = seenTurns; index < turns.size(); index++) {
            var turn = turns.get(index);
            var text = TerminalSanitizer.sanitizeLine(turn.text());
            if (turn.isOperator()) {
                prompt.raw(text("start.chat.you_line", Map.of(KEY_TEXT, text)));
            } else {
                prompt.raw(text("start.chat.role_line", Map.of("role",
                                    TerminalSanitizer.sanitizeLine(turn.role()), KEY_TEXT, text)));
            }
        }
        seenTurns = turns.size();
    }

    private String text(String key, Map<String, ?> args) {
        return MessageCatalogue.format(prompt.locale(), key, args);
    }
}
