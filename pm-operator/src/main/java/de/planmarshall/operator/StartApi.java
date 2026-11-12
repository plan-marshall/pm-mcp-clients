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
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.decomposition.DecompositionCommitResult;
import de.planmarshall.api.decomposition.DecompositionInstructRequest;
import de.planmarshall.api.decomposition.EpicDecomposition;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.http.HttpResponse;
import de.planmarshall.api.json.JsonTree;
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
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_RECIPES = "recipes";
    private static final Pattern LINK_ID = Pattern.compile("[A-Za-z0-9:_-]+");

    private final OperatorContext context;

    /**
     * @param context the operator context
     */
    StartApi(OperatorContext context) {
        this.context = Objects.requireNonNull(context, "context");
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
        throw new StartActException(linkId, messageOf(response));
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
     * so the chat is read again once the message is taken.
     *
     * @param entryId the entry
     * @param text    the operator's message
     * @return the entry's chat after the message
     * @throws IOException       on transport failure
     * @throws StartActException if the runtime refuses the message
     */
    ChatResource entryChatTurn(String entryId, String text) throws IOException {
        var routes = ChatRoutes.entry(entryId);
        var request = new ChatTurnRequest(text, UUID.randomUUID().toString());
        var response = send(HttpRequest.json("POST", routes.turns(), request.toJson()));
        if (response.status() != ChatTurnAccepted.HTTP_STATUS) {
            throw new StartActException(CHAT, messageOf(response));
        }
        return chat(send(HttpRequest.get(routes.chat())));
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
        throw new StartActException(CHAT, messageOf(response));
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
        var raw = JsonTree.asArray(parsed instanceof Map<?, ?> map ? map.get("projects") : parsed);
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

    private static String messageOf(HttpResponse response) {
        try {
            var map = JsonTree.asObject(JsonTree.parse(response.bodyText()));
            var message = map == null ? null : JsonTree.string(map, KEY_MESSAGE);
            return message != null ? OutputSafety.stripControlSequences(message)
                                : "HTTP " + response.status();
        } catch (IOException _) {
            return "HTTP " + response.status();
        }
    }

    /** An act the runtime refused; the dialogue prints it and asks again. */
    static final class StartActException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        StartActException(String linkId, String message) {
            super(linkId + ": " + message);
        }
    }
}
