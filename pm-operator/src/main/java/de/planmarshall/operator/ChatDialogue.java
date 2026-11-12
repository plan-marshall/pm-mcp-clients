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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatProposal;
import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.TurnKind;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.start.EntryKind;

/**
 * The one chat dialogue of the terminal client (design <em>Starting Work</em>, chatting about the
 * request): its scope is the entry, its role is routed by the kind, and its one change is the
 * description, read back against the old one and stored only on <em>Accept</em>. The chat is the
 * entry's chat resource, opened and read through the entry's chat routes. An empty line ends the
 * chat; the entry then goes on as before.
 */
final class ChatDialogue {

    private static final String KEY_TEXT = "text";
    private static final String KEY_STORED = "start.chat.stored";
    private static final String KEY_ENDED = "start.chat.ended";
    private static final String STATE_SOURCES = "start.sources";
    private static final String KEY_OPEN = "start.chat.open";
    /** The line that says the terminal stopped waiting while the role still writes its reply. */
    private static final String KEY_WAIT_ENDED = "control.chat.wait_ended";
    private static final String KEY_ROLE = "role";

    private final StartApi api;
    private final StartPrompt prompt;
    private final EntryKind kind;
    private final String project;
    /** The turn shown last (a proposal counts, it is read back on its own), or {@code null} before any. */
    private @Nullable String lastShown;

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
     * @param entryId the entry the chat is about
     * @param opened  the entry's chat as the open answered it
     * @throws IOException on transport failure
     */
    void run(String entryId, ChatResource opened) throws IOException {
        var chat = opened;
        prompt.raw(heading(chat));
        renderTurns(entryId, chat);
        while (true) {
            var proposal = chat.proposal();
            if (proposal != null && proposal.isOpen()) {
                chat = readBack(entryId, chat, proposal);
            }
            var answer = prompt.askInline(text("start.chat.prompt", Map.of()), STATE_SOURCES,
                                text(KEY_OPEN, Map.of()), entryId);
            if (answer.isEmpty()) {
                endChat(entryId);
                prompt.raw(text(KEY_ENDED, Map.of()));
                return;
            }
            try {
                chat = api.entryChatTurn(entryId, answer);
                renderTurns(entryId, chat);
                if (StartApi.isWriting(chat)) {
                    prompt.raw(text(KEY_WAIT_ENDED, Map.of(KEY_ROLE,
                                        TerminalSanitizer.sanitizeLine(chat.role().name()))));
                }
            } catch (StartApi.StartActException e) {
                prompt.raw(OutputSafety.stripControlSequences(e.getMessage()));
            }
        }
    }

    /** Ends the chat; a chat the runtime has ended meanwhile is ended already. */
    private void endChat(String entryId) throws IOException {
        try {
            api.endEntryChat(entryId);
        } catch (StartApi.StartActException _) {
            // The chat ended meanwhile, with the entry's create or another device's end.
        }
    }

    private ChatResource readBack(String entryId, ChatResource chat, ChatProposal proposal) throws IOException {
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
        var answer = prompt.askRaw(text("start.chat.accept_prompt", Map.of()), STATE_SOURCES,
                            text(KEY_OPEN, Map.of()), entryId);
        var accept = answer.isEmpty();
        if (!accept && !"r".equalsIgnoreCase(answer)) {
            return chat;
        }
        try {
            var answered = api.answerEntryChatProposal(entryId, proposal.turnId(), accept);
            if (accept) {
                prompt.raw(text(KEY_STORED, Map.of()));
            }
            renderTurns(entryId, answered);
            return answered;
        } catch (StartApi.StartActException e) {
            prompt.raw(OutputSafety.stripControlSequences(e.getMessage()));
            return chat;
        }
    }

    private String heading(ChatResource chat) {
        var subject = text("start.chat.subject_new", Map.of("kind", kind.wireValue(), "project", project));
        return text("start.chat.heading", Map.of("subject", subject,
                            KEY_ROLE, TerminalSanitizer.sanitizeLine(chat.role().name())));
    }

    /**
     * Renders the messages of the transcript after the turn shown last, up to a reply still written:
     * that one and what follows it are shown once it is written. A proposal is read back on its own.
     */
    private void renderTurns(String entryId, ChatResource chat) throws IOException {
        for (var turn : unshown(entryId, chat)) {
            if (turn.status() == TurnStatus.STREAMING) {
                return;
            }
            lastShown = turn.turnId();
            if (turn.kind() != TurnKind.PROPOSAL) {
                renderTurn(turn);
            }
        }
    }

    /**
     * The turns after the turn shown last. The chat holds only the newest page of its transcript; when
     * that turn has slid off it, the pages before are read back until it is found or the transcript's
     * start is reached, so no turn is skipped and none is shown twice.
     */
    private List<ChatTurn> unshown(String entryId, ChatResource chat) throws IOException {
        var turns = new ArrayList<>(chat.turns());
        var before = chat.before();
        var at = indexOfLastShown(turns);
        while (at < 0 && lastShown != null && before != null) {
            var page = api.entryChatPage(entryId, before);
            turns.addAll(0, page.turns());
            before = page.before();
            at = indexOfLastShown(turns);
        }
        return turns.subList(at + 1, turns.size());
    }

    private int indexOfLastShown(List<ChatTurn> turns) {
        for (var i = turns.size() - 1; i >= 0; i--) {
            if (turns.get(i).turnId().equals(lastShown)) {
                return i;
            }
        }
        return -1;
    }

    private void renderTurn(ChatTurn turn) {
        var text = TerminalSanitizer.sanitizeLine(turn.text());
        if (turn.isOperator()) {
            prompt.raw(text("start.chat.you_line", Map.of(KEY_TEXT, text)));
        } else {
            prompt.raw(text("start.chat.role_line", Map.of(KEY_ROLE,
                                TerminalSanitizer.sanitizeLine(turn.author()), KEY_TEXT, text)));
        }
    }

    private String text(String key, Map<String, ?> args) {
        return MessageCatalogue.format(prompt.locale(), key, args);
    }
}
