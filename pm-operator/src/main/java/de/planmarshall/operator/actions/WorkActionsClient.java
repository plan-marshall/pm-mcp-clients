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
import de.planmarshall.api.http.HttpExchange;
import de.planmarshall.api.http.HttpRequest;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.overview.EndedWorkPage;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.project.ProjectExplorer;
import de.planmarshall.api.resource.ApiErrorCode;
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
    private static final String REASON = "reason";

    /** How long a chat turn waits for the role's reply before the chat is read as it stands. */
    private static final Duration CHAT_REPLY_BOUND = Duration.ofSeconds(20);
    /** The pause between two reads of a chat whose reply is still being written. */
    private static final long CHAT_READ_PAUSE = 250L;
    /** The suffix that names a chat's refusal after the subject kind, one spelling for every act. */
    private static final String CHAT = " chat";

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
            payload.put(REASON, reason);
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
     * The runtime's answer to a settings change: the result the contract names, and why the value
     * was refused when the body names a reason.
     *
     * @param result        the result
     * @param refusalCode   the code the runtime refused the value with, or {@code null}
     * @param refusalReason the reason the body carries, or {@code null}
     */
    public record SettingsAnswer(PlanSettingsUpdateResult result, @Nullable ApiErrorCode refusalCode,
    @Nullable String refusalReason) {
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
            return new SettingsAnswer(PlanSettingsUpdateResult.fromJson(response.bodyText()), null, null);
        }
        if (response.status() == 409) {
            var result = PlanSettingsUpdateResult.resultOrNull(response.bodyText());
            if (result != null) {
                return new SettingsAnswer(result, null, null);
            }
        }
        if (response.status() == 400) {
            var refusal = new OperatorRefusedException("plan settings", response);
            var body = JsonTree.asObject(JsonTree.parse(response.bodyText()));
            var reason = body != null ? JsonTree.string(body, REASON) : null;
            return new SettingsAnswer(new PlanSettingsUpdateResult(false, false, (int) revision, null, null),
                                refusal.code(), reason != null ? reason : "");
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
            payload.put(REASON, reason);
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
        return readChat(chatRoutes(kind, id), kind + CHAT);
    }

    /**
     * Reads the chat after a turn or stream ended, falling back to {@code fallback} when a read
     * fails after the turn was already recorded.
     *
     * @param kind     the subject kind
     * @param id       the subject id
     * @param fallback snapshot to keep when reading fails, or {@code null}
     * @return the updated chat, or {@code fallback} if the read failed
     */
    public @Nullable ChatResource readChatQuietly(String kind, String id, @Nullable ChatResource fallback) {
        try {
            return chat(kind, id);
        } catch (IOException _) {
            return fallback;
        }
    }

    /**
     * Opens the subject's SSE event stream ({@code /api/v1/{plans|epics}/{id}/events}).
     *
     * @param kind the subject kind
     * @param id   the subject id
     * @return the open HTTP exchange
     * @throws IOException on connection failure
     */
    public HttpExchange openEvents(String kind, String id) throws IOException {
        return context.runtime(false).open(HttpRequest.get(prefix(kind) + path(id) + "/events"));
    }

    /**
     * Opens or returns the chat for a plan or an epic ({@code POST …/chat}).
     *
     * @param kind the subject kind ({@code plan} or {@code epic})
     * @param id   the subject id
     * @return the chat, opened if it did not exist yet
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatResource openChat(String kind, String id) throws IOException {
        context.gate().verifyAct();
        var routes = chatRoutes(kind, id);
        var response = context.runtime(true).send(HttpRequest.json("POST", routes.chat(), "{}"));
        if (response.status() != 200 && response.status() != 201) {
            throw new OperatorRefusedException(kind + CHAT, response);
        }
        return ChatResource.fromJson(response.bodyText());
    }

    /**
     * Ends the open chat for a plan or an epic ({@code POST …/chat/end}).
     *
     * @param kind the subject kind ({@code plan} or {@code epic})
     * @param id   the subject id
     * @return the ended chat
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatResource endChat(String kind, String id) throws IOException {
        context.gate().verifyAct();
        var routes = chatRoutes(kind, id);
        var response = context.runtime(true).send(HttpRequest.json("POST", routes.end(), "{}"));
        if (response.status() != 200) {
            throw new OperatorRefusedException(kind + CHAT + " end", response);
        }
        return ChatResource.fromJson(response.bodyText());
    }

    /**
     * Sends the operator's message on the chat's turns and follows the reply until its turn ends.
     *
     * @param kind    the subject kind
     * @param id      the subject id
     * @param message the operator's message
     * @return the chat after the reply, or as it stands when the wait runs out
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatResource chatTurn(String kind, String id, String message) throws IOException {
        return chatTurn(kind, id, message, UUID.randomUUID().toString());
    }

    /**
     * Sends the operator's message with a caller-supplied {@code client_turn_id} so retries reuse
     * the same id and the runtime deduplicates repeats.
     *
     * @param kind         the subject kind
     * @param id           the subject id
     * @param message      the operator's message
     * @param clientTurnId the client's own id of this message
     * @return the chat after the reply, or as it stands when the wait runs out
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatResource chatTurn(String kind, String id, String message, String clientTurnId)
                        throws IOException {
        return chatTurn(kind, id, message, clientTurnId, null);
    }

    /**
     * Sends the operator's message with a caller-supplied {@code client_turn_id} and waits via
     * polling, keeping {@code fallback} if a read fails after the turn was recorded.
     *
     * @param kind         the subject kind
     * @param id           the subject id
     * @param message      the operator's message
     * @param clientTurnId the client's own id of this message
     * @param fallback     last known chat snapshot before this turn, or {@code null}
     * @return the chat after the reply, or the last snapshot when a read fails or wait runs out
     * @throws IOException when posting the turn is refused or fails
     */
    public ChatResource chatTurn(String kind, String id, String message, String clientTurnId,
                        @Nullable ChatResource fallback) throws IOException {
        var accepted = postTurn(kind, id, message, clientTurnId);
        return awaitReply(chatRoutes(kind, id), accepted.turnId(), kind, fallback);
    }

    /**
     * Posts an operator chat turn ({@code POST …/chat/turns}) and returns the {@code 202} ack.
     *
     * @param kind         the subject kind
     * @param id           the subject id
     * @param message      the operator's message
     * @param clientTurnId the client's own id of this message
     * @return the recorded turn ack
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatTurnAccepted postTurn(String kind, String id, String message, String clientTurnId)
                        throws IOException {
        context.gate().verifyAct();
        var routes = chatRoutes(kind, id);
        var request = new ChatTurnRequest(message, clientTurnId);
        var response = context.runtime(true).send(
                            HttpRequest.json("POST", routes.turns(), JsonTree.write(request.toMap())));
        if (response.status() != ChatTurnAccepted.HTTP_STATUS) {
            throw new OperatorRefusedException(kind + CHAT + " turn", response);
        }
        return ChatTurnAccepted.fromJson(response.bodyText());
    }

    /**
     * Requests stopping an in-progress role reply ({@code POST …/chat/turns/{turnId}/stop}).
     *
     * @param kind   the subject kind
     * @param id     the subject id
     * @param turnId the role's reply turn id
     * @return the chat as the runtime left it
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatResource stopReply(String kind, String id, String turnId) throws IOException {
        context.gate().verifyAct();
        var routes = chatRoutes(kind, id);
        var response = context.runtime(true).send(HttpRequest.json("POST", routes.stop(turnId), "{}"));
        if (response.status() != 200) {
            throw new OperatorRefusedException(kind + CHAT + " stop", response);
        }
        return ChatResource.fromJson(response.bodyText());
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
            throw new OperatorRefusedException(kind + CHAT + " proposal", response);
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
    private ChatResource awaitReply(ChatRoutes routes, String operatorTurnId, String kind,
                        @Nullable ChatResource fallback) throws IOException {
        var bound = System.nanoTime() + CHAT_REPLY_BOUND.toNanos();
        ChatResource chat;
        try {
            chat = readChat(routes, kind + CHAT);
        } catch (IOException failure) {
            if (fallback != null) {
                return fallback;
            }
            throw failure;
        }
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
            try {
                chat = readChat(routes, kind + CHAT);
            } catch (IOException _) {
                return chat;
            }
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
     * Looks up a plan's current composition instance version, or {@code null} when unavailable.
     *
     * @param planId the plan id
     * @return the instance version, or {@code null} when the plan read does not answer with one
     */
    public @Nullable Integer planVersion(String planId) {
        try {
            var response = context.runtime(true).send(HttpRequest.get(PLANS + path(planId)));
            if (response.status() == 200) {
                return ProcessTree.fromJson(response.bodyText()).instanceVersion();
            }
        } catch (IOException _) {
            return null;
        }
        return null;
    }

    /**
     * Resolves the project of an ended subject from the explorer and each project's ended-work list,
     * returning {@code null} when unavailable.
     *
     * @param kind      {@code plan} or {@code epic}
     * @param subjectId the ended subject id
     * @return the owning project id, or {@code null} when not found
     */
    public @Nullable String endedProject(String kind, String subjectId) {
        if ("epic".equals(kind)) {
            try {
                return epic(subjectId).projectId();
            } catch (IOException _) {
                // Fall back to ended-work scan below when the live epic resource is gone.
            }
        }
        try {
            var projectsResp = context.runtime(true).send(HttpRequest.get("/api/v1/projects"));
            if (projectsResp.status() != 200) {
                return null;
            }
            var projects = ProjectExplorer.fromJson(projectsResp.bodyText()).allProjects();
            for (var project : projects) {
                var endedResp = context.runtime(true).send(HttpRequest.get(
                                    "/api/v1/projects/" + path(project.projectId()) + "/ended?page=1"));
                if (endedResp.status() != 200) {
                    continue;
                }
                var page = EndedWorkPage.fromJson(endedResp.bodyText());
                if (page.items().stream().anyMatch(item -> subjectId.equals(item.id()))) {
                    return project.projectId();
                }
            }
        } catch (IOException _) {
            return null;
        }
        return null;
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
