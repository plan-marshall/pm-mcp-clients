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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.control.ArchiveCheck;
import de.planmarshall.api.control.ChatReadBackResource;
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
 * success the runtime did not confirm. The client is read-only on {@code pm-mock-runtime} and
 * {@code pm-api}; it only sends what those contracts define.
 */
public final class WorkActionsClient {

    private static final String PLANS = "/api/v1/plans/";
    private static final String EPICS = "/api/v1/epics/";
    private static final String ACTIONS = "/actions";
    private static final String HOLDS = "/holds/";

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
     * @return the chat's transcript, its scope, and its open proposal
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatReadBackResource chat(String kind, String id) throws IOException {
        var response = context.runtime(true).send(HttpRequest.get(prefix(kind) + path(id) + "/chat"));
        if (response.status() != 200) {
            throw new OperatorRefusedException(kind + " chat", response);
        }
        return ChatReadBackResource.fromJson(response.bodyText());
    }

    /**
     * @param kind    the subject kind
     * @param id      the subject id
     * @param message the operator's message
     * @return the chat after the turn
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatReadBackResource chatTurn(String kind, String id, String message) throws IOException {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("message", message);
        var response = context.runtime(true).send(HttpRequest.json("POST",
                            prefix(kind) + path(id) + "/chat/turns", JsonTree.write(payload)));
        if (response.status() != 200) {
            throw new OperatorRefusedException(kind + " chat turn", response);
        }
        return ChatReadBackResource.fromJson(response.bodyText());
    }

    /**
     * @param kind   the subject kind
     * @param id     the subject id
     * @param accept whether to accept or reject the open proposal
     * @return the chat after the decision
     * @throws IOException when the runtime refuses or cannot be reached
     */
    public ChatReadBackResource chatProposal(String kind, String id, boolean accept) throws IOException {
        var path = accept ? "/chat/proposal/accept" : "/chat/proposal/reject";
        var response = context.runtime(true).send(HttpRequest.json("POST",
                            prefix(kind) + path(id) + path, "{}"));
        if (response.status() != 200) {
            throw new OperatorRefusedException(kind + " chat proposal", response);
        }
        return ChatReadBackResource.fromJson(response.bodyText());
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
