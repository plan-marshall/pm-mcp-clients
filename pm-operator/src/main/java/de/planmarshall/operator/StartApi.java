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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatStatus;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.decomposition.DecompositionCommitResult;
import de.planmarshall.api.decomposition.DecompositionInstructRequest;
import de.planmarshall.api.decomposition.EpicDecomposition;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.resource.ApiErrorCode;
import de.planmarshall.api.start.EntryOpenRequest;
import de.planmarshall.api.start.RecipeRef;
import de.planmarshall.api.start.StartStateResource;

/**
 * The HTTP surface of the start contract a terminal dialogue drives (design <em>Starting Work</em>):
 * the entry opened under its project, its acts, its chat, the recipes of the project, and the epic's
 * decomposition.
 */
final class StartApi {

    private static final String PROJECTS = "/api/v1/projects/";
    private static final String ENTRIES = "/api/v1/entries/";
    private static final String LINKS = "/links/";
    private static final String CHAT = "chat";
    private static final String RECIPES = "/api/v1/recipes";
    private static final String DECOMPOSITION = "/decomposition";
    private static final String KEY_RECIPES = "recipes";
    private static final Pattern LINK_ID = Pattern.compile("[A-Za-z0-9:_-]+");

    /** How long the terminal waits for the role's reply: a minute, read ten times a second. */
    private static final ReplyWait REPLY_WAIT = new ReplyWait(600, Duration.ofMillis(100));

    private final OperatorContext context;
    private final ReplyWait replyWait;

    /**
     * @param context the operator context
     */
    StartApi(OperatorContext context) {
        this(context, REPLY_WAIT);
    }

    /**
     * @param context   the operator context
     * @param replyWait how often the chat is read while the role's reply is written
     */
    StartApi(OperatorContext context, ReplyWait replyWait) {
        this.context = Objects.requireNonNull(context, "context");
        this.replyWait = Objects.requireNonNull(replyWait, "replyWait");
    }

    /**
     * Opens an entry under its project.
     *
     * @param project the project id
     * @param request the opening request
     * @return the opened entry's state
     * @throws IOException on transport failure
     */
    StartStateResource open(String project, EntryOpenRequest request) throws IOException {
        var response = send(HttpRequest.json("POST",
                            PROJECTS + OperatorUrlUtil.encodePathSegment(project) + "/entries", request.toJson()));
        return state(response, "start " + request.kind().wireValue());
    }

    /**
     * @param entryId the entry
     * @return the entry's current state
     * @throws IOException on transport failure
     */
    StartStateResource fetch(String entryId) throws IOException {
        return state(send(HttpRequest.get(ENTRIES + OperatorUrlUtil.encodePathSegment(entryId))), "start");
    }

    /**
     * Applies one operator act of an entry.
     *
     * @param entryId the entry
     * @param linkId  the link id ({@code add_issue}, {@code remove:src-1}, …)
     * @param body    the act's body
     * @return the entry's new state
     * @throws IOException       on transport failure
     * @throws StartActException if the runtime refuses the act
     */
    StartStateResource act(String entryId, String linkId, Map<String, Object> body)
                        throws IOException {
        if (!LINK_ID.matcher(linkId).matches()) {
            throw new IllegalArgumentException("The link id names no operator act: " + linkId);
        }
        var response = send(HttpRequest.json("POST",
                            ENTRIES + OperatorUrlUtil.encodePathSegment(entryId) + LINKS + linkId,
                            JsonTree.write(body)));
        if (response.status() == 200) {
            return StartStateResource.fromJson(response.bodyText());
        }
        throw new StartActException(linkId, response);
    }

    /**
     * Opens the entry's chat through the entry's chat route; a chat already open is answered as it is.
     *
     * @param entryId the entry
     * @return the entry's chat
     * @throws IOException       on transport failure
     * @throws StartActException if the runtime refuses the chat
     */
    ChatResource openEntryChat(String entryId) throws IOException {
        return chat(send(HttpRequest.json("POST", ChatRoutes.entry(entryId).chat(), "")));
    }

    /**
     * Sends one message of the operator; the runtime takes it at once and the role answers after it,
     * so the chat is read again once the message is taken, and again while the role's reply is still
     * written, at most as often as the reply wait allows.
     *
     * @param entryId the entry
     * @param text    the operator's message
     * @return the entry's chat after the message: the reply written, or still written when the wait ran
     *         out ({@link #isWriting(ChatResource)})
     * @throws IOException       on transport failure
     * @throws StartActException if the runtime refuses the message
     */
    ChatResource entryChatTurn(String entryId, String text) throws IOException {
        var routes = ChatRoutes.entry(entryId);
        var request = new ChatTurnRequest(text, UUID.randomUUID().toString());
        var response = send(HttpRequest.json("POST", routes.turns(), request.toJson()));
        if (response.status() != ChatTurnAccepted.HTTP_STATUS) {
            throw new StartActException(CHAT, response);
        }
        var chat = chat(send(HttpRequest.get(routes.chat())));
        for (int read = 1; read < replyWait.reads() && isWriting(chat); read++) {
            LockSupport.parkNanos(replyWait.pause().toNanos());
            chat = chat(send(HttpRequest.get(routes.chat())));
        }
        return chat;
    }

    /**
     * Reads the page of the entry's chat before a turn ({@code GET …/chat?before=<turn_id>}).
     *
     * @param entryId      the entry
     * @param beforeTurnId the turn the page ends before, as the newer page's {@code before} names it
     * @return the chat with that page of turns
     * @throws IOException       on transport failure
     * @throws StartActException if the runtime refuses the read
     */
    ChatResource entryChatPage(String entryId, String beforeTurnId) throws IOException {
        return chat(send(HttpRequest.get(ChatRoutes.entry(entryId).page(beforeTurnId))));
    }

    /**
     * @param chat the chat as last read
     * @return {@code true} while the role's reply is written: the chat is in {@code chat.reply} or a
     *         turn is still streaming
     */
    static boolean isWriting(ChatResource chat) {
        return chat.state() == ChatStatus.REPLY
                            || chat.turns().stream().anyMatch(turn -> turn.status() == TurnStatus.STREAMING);
    }

    /**
     * Accepts or rejects the chat's open proposal.
     *
     * @param entryId the entry
     * @param turnId  the proposal's turn
     * @param accept  whether the proposal is accepted
     * @return the entry's chat after the answer
     * @throws IOException       on transport failure
     * @throws StartActException if the runtime refuses the answer
     */
    ChatResource answerEntryChatProposal(String entryId, String turnId, boolean accept) throws IOException {
        var routes = ChatRoutes.entry(entryId);
        return chat(send(HttpRequest.json("POST", accept ? routes.accept() : routes.reject(),
                            new ProposalAnswerRequest(turnId).toJson())));
    }

    /**
     * Ends the chat, keeping its transcript with the entry.
     *
     * @param entryId the entry
     * @return the ended chat
     * @throws IOException       on transport failure
     * @throws StartActException if the chat has ended already
     */
    ChatResource endEntryChat(String entryId) throws IOException {
        return chat(send(HttpRequest.json("POST", ChatRoutes.entry(entryId).end(), "")));
    }

    private static ChatResource chat(HttpResponse response) throws IOException {
        if (response.status() == 200 || response.status() == 201) {
            return ChatResource.fromJson(response.bodyText());
        }
        throw new StartActException(CHAT, response);
    }

    /**
     * @return the ids of the enrolled projects
     * @throws IOException on transport failure
     */
    List<String> projects() throws IOException {
        var response = send(HttpRequest.get("/api/v1/projects"));
        if (response.status() != 200) {
            return List.of();
        }
        var parsed = JsonTree.parse(response.bodyText());
        // The explorer names every enrolled project under all_projects; projects is its older name.
        var raw = JsonTree.asArray(parsed instanceof Map<?, ?> map
                            ? (map.get(ProjectExplorer.FIELD_ALL_PROJECTS) != null
                                                ? map.get(ProjectExplorer.FIELD_ALL_PROJECTS) : map.get("projects"))
                            : parsed);
        if (raw == null) {
            return List.of();
        }
        var ids = new ArrayList<String>();
        for (var item : raw) {
            var object = JsonTree.asObject(item);
            if (object != null) {
                var id = JsonTree.string(object, "project_id");
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /**
     * @return the recipes offered on this machine
     * @throws IOException on transport failure
     */
    List<RecipeRef> recipes() throws IOException {
        var response = send(HttpRequest.get(RECIPES));
        if (response.status() != 200) {
            throw new OperatorRefusedException(KEY_RECIPES, response);
        }
        var map = JsonTree.asObject(JsonTree.parse(response.bodyText()));
        var raw = map == null ? null : JsonTree.asArray(map.get(KEY_RECIPES));
        if (raw == null) {
            return List.of();
        }
        var recipes = new ArrayList<RecipeRef>();
        for (var item : raw) {
            var object = JsonTree.asObject(item);
            if (object != null) {
                recipes.add(RecipeRef.fromMap(object));
            }
        }
        return recipes;
    }

    /**
     * @param project the project
     * @param epicId  the epic
     * @return the epic's decomposition, or {@code null} while none is drafted
     * @throws IOException on transport failure
     */
    @Nullable
    EpicDecomposition decomposition(String project, String epicId) throws IOException {
        var response = send(HttpRequest.get(decompositionPath(project, epicId)));
        if (response.status() == 404) {
            return null;
        }
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic decomposition", response);
        }
        return EpicDecomposition.fromJson(response.bodyText());
    }

    /**
     * @param project the project
     * @param epicId  the epic
     * @return the decomposition with its checks run
     * @throws IOException on transport failure
     */
    @Nullable
    EpicDecomposition checkDecomposition(String project, String epicId) throws IOException {
        var response = send(HttpRequest.json("POST", decompositionPath(project, epicId) + "/check", "{}"));
        if (response.status() == 404) {
            return null;
        }
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic decomposition", response);
        }
        return EpicDecomposition.fromJson(response.bodyText());
    }

    /**
     * @param project the project
     * @param epicId  the epic
     * @param request the operator's words
     * @return the rewritten decomposition
     * @throws IOException on transport failure
     */
    EpicDecomposition instruct(String project, String epicId, DecompositionInstructRequest request)
                        throws IOException {
        var response = send(HttpRequest.json("POST", decompositionPath(project, epicId) + "/instruct",
                            request.toJson()));
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic decomposition", response);
        }
        return EpicDecomposition.fromJson(response.bodyText());
    }

    /**
     * @param project the project
     * @param epicId  the epic
     * @return the commit result, or {@code null} where a check still fails
     * @throws IOException on transport failure
     */
    @Nullable
    DecompositionCommitResult commit(String project, String epicId) throws IOException {
        var response = send(HttpRequest.json("POST", decompositionPath(project, epicId) + "/commit", "{}"));
        if (response.status() == 200) {
            return DecompositionCommitResult.fromJson(response.bodyText());
        }
        if (response.status() == 409) {
            return null;
        }
        throw new OperatorRefusedException("epic decomposition", response);
    }

    private static String decompositionPath(String project, String epicId) {
        return PROJECTS + OperatorUrlUtil.encodePathSegment(project) + "/epics/"
                            + OperatorUrlUtil.encodePathSegment(epicId) + DECOMPOSITION;
    }

    private HttpResponse send(HttpRequest request) throws IOException {
        return context.runtime(true).send(request);
    }

    private static StartStateResource state(HttpResponse response, String verb) throws IOException {
        if (response.status() == 200 || response.status() == 201) {
            return StartStateResource.fromJson(response.bodyText());
        }
        throw new OperatorRefusedException(verb, response);
    }

    /**
     * The bound of the wait for the role's reply: the chat is read at most {@code reads} times, with
     * {@code pause} between two reads.
     *
     * @param reads how often the chat is read at most, at least once
     * @param pause the pause between two reads, not negative
     */
    record ReplyWait(int reads, Duration pause) {

        /**
         * @param reads the reads, at least one
         * @param pause the pause, not negative
         */
        ReplyWait {
            Objects.requireNonNull(pause, "pause");
            if (reads < 1 || pause.isNegative()) {
                throw new IllegalArgumentException("a reply wait reads at least once and pauses no negative time");
            }
        }
    }

    /** An act the runtime refused; the dialogue prints it and asks again. */
    static final class StartActException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private static final String KEY_MESSAGE = "message";
        /** The member of an error body that names its code. */
        private static final String KEY_ERROR = "error";

        private final @Nullable ApiErrorCode code;

        /**
         * @param linkId   the refused act's link id
         * @param response the runtime's refusal
         */
        StartActException(String linkId, HttpResponse response) {
            super(linkId + ": " + messageOf(response));
            this.code = codeOf(response);
        }

        /**
         * @return the error code of the refusal's body, or {@code null} when the body carries no known one
         */
        @Nullable
        ApiErrorCode code() {
            return code;
        }

        private static String messageOf(HttpResponse response) {
            var message = bodyMember(response, KEY_MESSAGE);
            return message != null ? OutputSafety.stripControlSequences(message) : "HTTP " + response.status();
        }

        private static @Nullable ApiErrorCode codeOf(HttpResponse response) {
            var wire = bodyMember(response, KEY_ERROR);
            return wire == null ? null : ApiErrorCode.fromWireOrNull(wire);
        }

        /** @return the string member of an error body, or {@code null} when the body has none or is no JSON */
        private static @Nullable String bodyMember(HttpResponse response, String key) {
            try {
                var map = JsonTree.asObject(JsonTree.parse(response.bodyText()));
                return map == null ? null : JsonTree.string(map, key);
            } catch (IOException _) {
                return null;
            }
        }
    }
}
