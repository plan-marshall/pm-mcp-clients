/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * The HTTP paths of a chat's routes below its subject ({@code 01-processes-and-transport.adoc#chat-routes}),
 * in the manner of {@code MachineApiPaths}: one builder per subject, and from it the chat ({@code GET}
 * reads it, {@code POST} opens it), the turns ({@code POST}, a message), the stop of a reply, the accept
 * and the reject of the open proposal, the end, and the page of turns before a turn.
 * <p>
 * The subjects are a plan, an epic, an entry, a decision below each of its scopes (a plan, an epic, an
 * entry, a project's workspace, the machine), and ended work, which is an ended plan or epic addressed
 * like the running one. Every id is encoded as one path segment, and the {@code before} of a page read
 * as a query value; a blank id is refused.
 * <p>
 * The chats of an epic's decomposition draft and of its review (one item or the whole register) are
 * left out until the runtime specification and the core state catalogue name their addresses.
 */
public final class ChatRoutes {

    /** The prefix of every path of the local API. */
    public static final String API_PREFIX = "/api/v1";

    private static final String CHAT = "/chat";
    private static final String DECISIONS = "/decisions/";
    private static final String QUESTION_ID = "questionId";

    private final String subjectPath;

    private ChatRoutes(String subjectPath) {
        this.subjectPath = subjectPath;
    }

    /**
     * @param planId the plan's id, running or ended
     * @return the routes of a plan's chat
     */
    public static ChatRoutes plan(String planId) {
        return new ChatRoutes(planPath(planId));
    }

    /**
     * @param epicId the epic's id, running or ended
     * @return the routes of an epic's chat
     */
    public static ChatRoutes epic(String epicId) {
        return new ChatRoutes(epicPath(epicId));
    }

    /**
     * @param entryId the entry's id
     * @return the routes of a start entry's chat
     */
    public static ChatRoutes entry(String entryId) {
        return new ChatRoutes(entryPath(entryId));
    }

    /**
     * Ended work is addressed like the running plan.
     *
     * @param planId the ended plan's id
     * @return the routes of the chat about the ended plan
     */
    public static ChatRoutes endedPlan(String planId) {
        return plan(planId);
    }

    /**
     * Ended work is addressed like the running epic.
     *
     * @param epicId the ended epic's id
     * @return the routes of the chat about the ended epic
     */
    public static ChatRoutes endedEpic(String epicId) {
        return epic(epicId);
    }

    /**
     * @param planId     the plan the decision belongs to
     * @param questionId the decision's question id
     * @return the routes of the chat about a plan's decision
     */
    public static ChatRoutes planDecision(String planId, String questionId) {
        return new ChatRoutes(planPath(planId) + DECISIONS + segment(questionId, QUESTION_ID));
    }

    /**
     * @param epicId     the epic the decision belongs to
     * @param questionId the decision's question id
     * @return the routes of the chat about an epic's decision
     */
    public static ChatRoutes epicDecision(String epicId, String questionId) {
        return new ChatRoutes(epicPath(epicId) + DECISIONS + segment(questionId, QUESTION_ID));
    }

    /**
     * @param entryId    the entry the decision belongs to
     * @param questionId the decision's question id
     * @return the routes of the chat about an entry's decision
     */
    public static ChatRoutes entryDecision(String entryId, String questionId) {
        return new ChatRoutes(entryPath(entryId) + DECISIONS + segment(questionId, QUESTION_ID));
    }

    /**
     * @param projectId  the project whose workspace scope holds the decision
     * @param questionId the decision's question id
     * @return the routes of the chat about a decision of a project's workspace
     */
    public static ChatRoutes workspaceDecision(String projectId, String questionId) {
        return new ChatRoutes(API_PREFIX + "/projects/" + segment(projectId, "projectId") + "/workspace"
                + DECISIONS + segment(questionId, QUESTION_ID));
    }

    /**
     * @param questionId the decision's question id
     * @return the routes of the chat about a decision of the machine scope
     */
    public static ChatRoutes machineDecision(String questionId) {
        return new ChatRoutes(API_PREFIX + "/machine" + DECISIONS + segment(questionId, QUESTION_ID));
    }

    /**
     * @return the path of the subject the chat is addressed below
     */
    public String subject() {
        return subjectPath;
    }

    /**
     * @return the chat's path: {@code GET} reads it, {@code POST} opens it
     */
    public String chat() {
        return subjectPath + CHAT;
    }

    /**
     * @param beforeTurnId the turn the page ends before
     * @return the path of the page of turns before that turn ({@code GET …/chat?before=<turn_id>})
     */
    public String page(String beforeTurnId) {
        return chat() + "?before=" + segment(beforeTurnId, "beforeTurnId");
    }

    /**
     * @return the path the operator's message is posted to
     */
    public String turns() {
        return chat() + "/turns";
    }

    /**
     * @param turnId the turn of the reply being written
     * @return the path that stops the reply
     */
    public String stop(String turnId) {
        return turns() + "/" + segment(turnId, "turnId") + "/stop";
    }

    /**
     * @return the path that accepts the open proposal
     */
    public String accept() {
        return chat() + "/proposal/accept";
    }

    /**
     * @return the path that rejects the open proposal
     */
    public String reject() {
        return chat() + "/proposal/reject";
    }

    /**
     * @return the path that ends the chat
     */
    public String end() {
        return chat() + "/end";
    }

    private static String planPath(String planId) {
        return API_PREFIX + "/plans/" + segment(planId, "planId");
    }

    private static String epicPath(String epicId) {
        return API_PREFIX + "/epics/" + segment(epicId, "epicId");
    }

    private static String entryPath(String entryId) {
        return API_PREFIX + "/entries/" + segment(entryId, "entryId");
    }

    private static String segment(String id, String name) {
        return URLEncoder.encode(requireId(id, name), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String requireId(String id, String name) {
        Objects.requireNonNull(id, name);
        if (id.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (".".equals(id) || "..".equals(id)) {
            throw new IllegalArgumentException(name + " must not be a dot segment");
        }
        return id;
    }
}
