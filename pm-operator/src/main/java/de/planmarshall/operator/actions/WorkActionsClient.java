/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.actions;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.chat.ChatResource;
import de.planmarshall.api.chat.ChatRoutes;
import de.planmarshall.api.chat.ChatTurn;
import de.planmarshall.api.chat.ChatTurnAccepted;
import de.planmarshall.api.chat.ChatTurnRequest;
import de.planmarshall.api.chat.ProposalAnswerRequest;
import de.planmarshall.api.chat.TurnStatus;
import de.planmarshall.api.control.ArchiveCheck;
import de.planmarshall.api.control.EpicResource;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.HoldResolutionResult;
import de.planmarshall.api.control.PlanSettingsRecord;
import de.planmarshall.api.control.PlanSettingsUpdateResult;
import de.planmarshall.api.control.ReviewResource;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.operator.OperatorContext;
import de.planmarshall.operator.OperatorRefusedException;
import de.planmarshall.operator.OperatorUrlUtil;

/**
 * The control contract as the full-screen views and the action dialogues read and write it
 * ({@code doc/design/shared/work-actions.adoc}).
 *
 * <p>Every method is fail-closed: a status other than the one the contract names is raised as an
 * {@link OperatorRefusedException}, so the view never invents an action result and never shows a
 * success the runtime did not confirm. The one exception is the plan's decisions read
 * ({@link #openDecision(String)}), which falls back to the scoped decisions list where the
 * plan-scoped collection is absent. The client is read-only on {@code pm-mock-runtime} and
 * {@code pm-api}; it only sends what those contracts define.
 */
public final class WorkActionsClient {

    private static final String PLANS = "/api/v1/plans/";
    private static final String EPICS = "/api/v1/epics/";
    private static final String DECISIONS = "/api/v1/decisions";
    private static final String ACTIONS = "/actions";
    private static final String HOLDS = "/holds/";

    /** How long a chat turn waits for the role's reply before the chat is read as it stands. */
    private static final Duration CHAT_REPLY_BOUND = Duration.ofSeconds(20);
    /** The pause between two reads of a chat whose reply is still being written. */
    private static final long CHAT_READ_PAUSE = 250L;

    private final OperatorContext context;

    /**
     * @param context the operator context
     */
    public WorkActionsClient(OperatorContext context) {
        this.context = context;
    }

    /**
     * @param kind the subject kind: {@code plan} or {@code epic}
     * @param id   the subject id
     * @return the actions the runtime offers in the subject's state
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public WorkActions actions(String kind, String id) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(prefix(kind) + path(id) + ACTIONS));
        if (response.status() != 200) {
            throw new OperatorRefusedException(kind + " actions", response);
        }
        return WorkActions.fromJson(response.bodyText());
    }

    /**
     * @param planId the plan
     * @return the id of the plan's first open decision, or {@code null} when it waits for none
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public @Nullable String openDecision(String planId) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(PLANS + path(planId) + "/decisions"));
        if (response.status() == 404) {
            // The scope query of the decisions list is the read the console's `questions` makes.
            response = context.runtime(true).send(HttpRequest.get(DECISIONS + "?plan=" + path(planId)));
        }
        if (response.status() != 200) {
            throw new OperatorRefusedException("plan decisions", response);
        }
        var parsed = JsonTree.parse(response.bodyText());
        if (parsed instanceof List<?> list) {
            for (var item : list) {
                var map = JsonTree.asObject(item);
                var id = map != null ? JsonTree.string(map, "id") : null;
                if (id != null && !id.isBlank()) {
                    return id;
                }
            }
        }
        return null;
    }

    /**
     * @param planId the plan
     * @return every hold that holds the plan, in the runtime's order
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public List<HoldRecord> holds(String planId) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(PLANS + path(planId) + "/holds"));
        if (response.status() != 200) {
            throw new OperatorRefusedException("plan holds", response);
        }
        var parsed = JsonTree.parse(response.bodyText());
        var holds = new ArrayList<HoldRecord>();
        if (parsed instanceof List<?> list) {
            for (var item : list) {
                var map = JsonTree.asObject(item);
                if (map != null) {
                    holds.add(HoldRecord.fromMap(map));
                }
            }
        }
        return List.copyOf(holds);
    }

    /**
     * @param planId the plan
     * @return the plan's first hold, or {@code null} when it has none
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public @Nullable HoldRecord hold(String planId) throws IOException {
        var holds = holds(planId);
        return holds.isEmpty() ? null : holds.getFirst();
    }

    /**
     * Sends one action of the subject's state.
     *
     * @param kind    the subject kind
     * @param id      the subject id
     * @param action  the action the runtime offered
     * @param payload the action's arguments
     * @return the subject's actions after the action
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public WorkActions perform(String kind, String id, String action, Map<String, Object> payload)
                        throws IOException {
        context.gate().verifyAct();
        var response = context.runtime(true).send(HttpRequest.json("POST",
                            prefix(kind) + path(id) + ACTIONS + "/" + path(action), JsonTree.write(payload)));
        if (response.status() != 200) {
            throw new OperatorRefusedException(kind + " " + action, response);
        }
        return WorkActions.fromJson(response.bodyText());
    }

    /**
     * @param planId the plan
     * @param holdId the hold
     * @param act    the act the hold offers
     * @param reason the reason of an act that cannot be undone, or {@code null}
     * @return the runtime's answer: released, or still held with the cause
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public HoldResolutionResult resolveHold(String planId, String holdId, String act, @Nullable String reason)
                        throws IOException {
        context.gate().verifyAct();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("act", act);
        if (reason != null && !reason.isBlank()) {
            payload.put("reason", reason);
        }
        var response = context.runtime(true).send(HttpRequest.json("POST",
                            PLANS + path(planId) + HOLDS + path(holdId) + "/resolve", JsonTree.write(payload)));
        if (response.status() != 200) {
            throw new OperatorRefusedException("plan hold " + act, response);
        }
        return HoldResolutionResult.fromJson(response.bodyText());
    }

    /**
     * @param planId the plan
     * @return the plan's own settings values with their revision
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public PlanSettingsRecord settings(String planId) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(PLANS + path(planId) + "/settings"));
        if (response.status() != 200) {
            throw new OperatorRefusedException("plan settings", response);
        }
        return PlanSettingsRecord.fromJson(response.bodyText());
    }

    /**
     * The runtime's answer to a settings change: the result the contract names, and the reason when
     * a value was refused.
     *
     * @param result        the result
     * @param refusalReason why the value was refused, or {@code null}
     */
    public record SettingsAnswer(PlanSettingsUpdateResult result, @Nullable String refusalReason) {
    }

    /**
     * @param planId   the plan
     * @param revision the revision the dialogue was opened with
     * @param aspect   the aspect
     * @param value    the value, or {@code null} to remove the plan's own value
     * @return the runtime's answer: applied, refused, too late, or a conflict
     * @throws IOException when the runtime cannot be reached or answers unexpectedly
     */
    public SettingsAnswer updateSettings(String planId, long revision, String aspect,
                        @Nullable String value) throws IOException {
        context.gate().verifyAct();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("revision", revision);
        payload.put("aspect", aspect);
        if (value == null) {
            payload.put("remove", true);
        } else {
            payload.put("value", value);
        }
        var response = context.runtime(true).send(HttpRequest.json("PUT",
                            PLANS + path(planId) + "/settings", JsonTree.write(payload)));
        if (response.status() == 200) {
            return new SettingsAnswer(PlanSettingsUpdateResult.fromJson(response.bodyText()), null);
        }
        if (response.status() == 409) {
            return new SettingsAnswer(PlanSettingsUpdateResult.fromJson(response.bodyText()), null);
        }
        if (response.status() == 400) {
            var body = JsonTree.asObject(JsonTree.parse(response.bodyText()));
            var reason = body != null ? JsonTree.string(body, "reason") : null;
            return new SettingsAnswer(new PlanSettingsUpdateResult(false, false, (int) revision, null, null),
                                reason != null ? reason : "");
        }
        throw new OperatorRefusedException("plan settings", response);
    }

    /**
     * @param epicId   the epic
     * @param register {@code findings} or {@code watches}
     * @return the review of that register
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ReviewResource review(String epicId, String register) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(EPICS + path(epicId)
                            + "/review?register=" + OperatorUrlUtil.encodePathSegment(register)));
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic " + register + " review", response);
        }
        return ReviewResource.fromJson(response.bodyText());
    }

    /**
     * Records one item's disposition.
     *
     * @param epicId      the epic
     * @param itemId      the item
     * @param disposition the disposition: {@code kept}, {@code watch}, {@code dropped}
     * @param reason      the reason of a drop, or {@code null}
     * @return the review after the disposition
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ReviewResource disposition(String epicId, String itemId, String disposition,
                        @Nullable String reason) throws IOException {
        context.gate().verifyAct();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("disposition", disposition);
        if (reason != null && !reason.isBlank()) {
            payload.put("reason", reason);
        }
        var response = context.runtime(true).send(HttpRequest.json("POST",
                            EPICS + path(epicId) + "/review/" + path(itemId) + "/disposition",
                            JsonTree.write(payload)));
        if (response.status() != 200) {
            throw new OperatorRefusedException("review disposition", response);
        }
        return ReviewResource.fromJson(response.bodyText());
    }

    /**
     * Writes the review's decisions to the epic's ledger.
     *
     * @param epicId the epic
     * @return the finished review
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ReviewResource commitReview(String epicId) throws IOException {
        context.gate().verifyAct();
        var response = context.runtime(true).send(HttpRequest.json("POST",
                            EPICS + path(epicId) + "/review/commit", "{}"));
        if (response.status() != 200) {
            throw new OperatorRefusedException("review commit", response);
        }
        return ReviewResource.fromJson(response.bodyText());
    }

    /**
     * @param kind the subject kind
     * @param id   the subject id
     * @return the chat, with its transcript, its scope and its open proposal
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatResource chat(String kind, String id) throws IOException {
        return readChat(chatRoutes(kind, id), kind + " chat");
    }

    /**
     * Sends the operator's message on the chat's turns and follows the reply until its turn ends.
     *
     * <p>The message carries the client's own {@code client_turn_id}, drawn once per message: the
     * runtime records the turn and answers {@code 202 turn_id} before the role writes, and a repeat
     * with the same id records nothing again. The reply is followed by its turn id and status, never
     * by a count of turns, because the transcript is paged and the reply is written into it.
     *
     * @param kind    the subject kind
     * @param id      the subject id
     * @param message the operator's message
     * @return the chat after the reply, or as it stands when the wait runs out
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatResource chatTurn(String kind, String id, String message) throws IOException {
        context.gate().verifyAct();
        var routes = chatRoutes(kind, id);
        var request = new ChatTurnRequest(message, UUID.randomUUID().toString());
        var response = context.runtime(true).send(
                            HttpRequest.json("POST", routes.turns(), JsonTree.write(request.toMap())));
        if (response.status() != ChatTurnAccepted.HTTP_STATUS) {
            throw new OperatorRefusedException(kind + " chat turn", response);
        }
        var accepted = ChatTurnAccepted.fromJson(response.bodyText());
        return awaitReply(routes, accepted.turnId(), kind);
    }

    /**
     * @param kind   the subject kind
     * @param id     the subject id
     * @param turnId the proposal's own turn, which accept and reject name
     * @param accept whether to accept or reject the open proposal
     * @return the chat after the decision
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatResource chatProposal(String kind, String id, String turnId, boolean accept)
                        throws IOException {
        context.gate().verifyAct();
        var routes = chatRoutes(kind, id);
        var answer = JsonTree.write(new ProposalAnswerRequest(turnId).toMap());
        var response = context.runtime(true).send(HttpRequest.json("POST",
                            accept ? routes.accept() : routes.reject(), answer));
        if (response.status() != 200) {
            throw new OperatorRefusedException(kind + " chat proposal", response);
        }
        return ChatResource.fromJson(response.bodyText());
    }

    /**
     * @param routes the chat's routes
     * @param action what the read is for, in a refusal
     * @return the chat as the runtime serves it
     * @throws IOException when the runtime refuses or cannot be reached
     */
    private ChatResource readChat(ChatRoutes routes, String action) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(routes.chat()));
        if (response.status() != 200) {
            throw new OperatorRefusedException(action, response);
        }
        return ChatResource.fromJson(response.bodyText());
    }

    /**
     * Follows the reply to the operator's turn until the role's turn is no longer being written.
     *
     * @param routes        the chat's routes
     * @param operatorTurnId the operator's turn the reply answers
     * @param kind          the subject kind, for a refusal
     * @return the chat once the reply settled, or as it stands at the bound or on an interrupt
     * @throws IOException when the runtime refuses or cannot be reached
     */
    private ChatResource awaitReply(ChatRoutes routes, String operatorTurnId, String kind)
                        throws IOException {
        var bound = System.nanoTime() + CHAT_REPLY_BOUND.toNanos();
        var chat = readChat(routes, kind + " chat");
        while (!replySettled(chat, operatorTurnId) && System.nanoTime() < bound) {
            if (Thread.currentThread().isInterrupted()) {
                return chat;
            }
            try {
                Thread.sleep(CHAT_READ_PAUSE);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                return chat;
            }
            chat = readChat(routes, kind + " chat");
        }
        return chat;
    }

    /**
     * @param chat           the chat as read
     * @param operatorTurnId the operator's turn the reply answers
     * @return {@code true} once a turn after it is no longer being written
     */
    private static boolean replySettled(ChatResource chat, String operatorTurnId) {
        var turns = chat.turns();
        var answered = -1;
        for (int index = 0; index < turns.size(); index++) {
            if (turns.get(index).turnId().equals(operatorTurnId)) {
                answered = index;
                break;
            }
        }
        if (answered < 0) {
            return false;
        }
        for (int index = answered + 1; index < turns.size(); index++) {
            var turn = turns.get(index);
            if (!ChatTurn.AUTHOR_OPERATOR.equals(turn.author()) && turn.status() != TurnStatus.STREAMING) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param kind the subject kind: {@code plan} or {@code epic}
     * @param id   the subject id
     * @return the chat's routes
     */
    private static ChatRoutes chatRoutes(String kind, String id) {
        return "epic".equals(kind) ? ChatRoutes.epic(id) : ChatRoutes.plan(id);
    }

    /**
     * @param epicId the epic
     * @return the epic as a resource
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public EpicResource epic(String epicId) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(EPICS + path(epicId)));
        if (response.status() != 200 || !response.bodyText().contains("\"workstreams\"")) {
            throw new OperatorRefusedException("epic", response);
        }
        return EpicResource.fromJson(response.bodyText());
    }

    /**
     * @param epicId the epic
     * @return the archive check
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ArchiveCheck archiveCheck(String epicId) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(EPICS + path(epicId) + "/archive-check"));
        if (response.status() != 200) {
            throw new OperatorRefusedException("epic archive check", response);
        }
        return ArchiveCheck.fromJson(response.bodyText());
    }

    /**
     * Sends one action of an alongside landing.
     *
     * @param projectId the project
     * @param unit      the landing's unit
     * @param action    the act the menu offers
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public void performLanding(String projectId, String unit, String action) throws IOException {
        context.gate().verifyAct();
        var response = context.runtime(true).send(HttpRequest.json("POST",
                            "/api/v1/projects/" + path(projectId) + "/landings/" + path(unit)
                                                + "/actions/" + path(action), "{}"));
        if (response.status() != 200) {
            throw new OperatorRefusedException("landing " + action, response);
        }
    }

    private static String prefix(String kind) {
        return "epic".equals(kind) ? EPICS : PLANS;
    }

    private static String path(String segment) {
        return OperatorUrlUtil.encodePathSegment(segment);
    }
}
