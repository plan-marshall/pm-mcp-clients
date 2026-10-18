/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import java.util.Arrays;
import java.util.List;

/**
 * The closed enumeration of the error codes a refused request of the local API {@code /api/v1} is
 * answered with, each paired with its HTTP status. The catalogue is the closed list of the
 * Runtime Model specification (Local API, Error codes) and is the one the dependency guard
 * {@code PM-WATCH-RUN-12} holds every {@code pm-api} refusal to.
 * <p>
 * The list is closed and fails closed: a code read from the wire that is not a member is refused by
 * {@link #fromWire(String)}, never silently accepted. An unauthenticated request is answered
 * {@code 401} without a code, so no member carries a bare {@code 401}. The wire value is the
 * lower-case form carried in the {@code code} member of the error body.
 */
public enum ApiErrorCode {

    // 400: the request does not fit the endpoint's schema.

    /** The request body or a parameter violates the endpoint's schema. */
    INVALID_ARGUMENTS("invalid_arguments", 400),

    /** An answer whose option, value, or own words the decision's part does not allow. */
    INVALID_ANSWER("invalid_answer", 400),

    /** An answer to a part whose dependency on another part is unanswered. */
    DEPENDENCY_UNANSWERED("dependency_unanswered", 400),

    /** The pairing code is invalid. */
    PAIRING_INVALID("pairing_invalid", 400),

    /** A designed unit's name violates the identifier grammar. */
    UNIT_NAME_INVALID("unit_name_invalid", 400),

    // 403: the listener a request arrived on does not serve the endpoint.

    /** A CLI-only endpoint reached on a web listener. */
    CLI_ONLY("cli_only", 403),

    /** A loopback-only endpoint reached on the LAN listener. */
    LOOPBACK_ONLY("loopback_only", 403),

    // 404: the named scope or target does not exist.

    /** The named project or checkout path resolves to no enrolled project. */
    PROJECT_NOT_ENROLLED("project_not_enrolled", 404),

    /** An unknown plan, epic, decision, report, cleanup job, or entry id. */
    SCOPE_NOT_FOUND("scope_not_found", 404),

    /** An unknown waiver, owed item, retained capture ref, credential entry, consent, or device. */
    TARGET_NOT_FOUND("target_not_found", 404),

    // 409: the act conflicts with the addressed state.

    /** An act the addressed state does not offer. */
    ACT_NOT_AVAILABLE("act_not_available", 409),

    /** An act made against an older revision of a decision or a recorded value. */
    CONFLICT("conflict", 409),

    /** A decision answered in the other client first. */
    ALREADY_ANSWERED("already_answered", 409),

    /** A verb that needs a step of the machine's onboarding not done yet. */
    ONBOARDING_INCOMPLETE("onboarding_incomplete", 409),

    /** A {@code config set} of a key an aspect of the onboarding or the setup owns. */
    ASPECT_OWNED("aspect_owned", 409),

    /** A start in a project where the plan lifecycle is not offered. */
    LIFECYCLE_CLOSED("lifecycle_closed", 409),

    /** An act while the project has running work. */
    SCOPE_ACTIVE("scope_active", 409),

    /** A release while a short landing of the project did not finish within its bound. */
    RELEASE_LANDING_PENDING("release_landing_pending", 409),

    /** A change of a plan behaviour axis of a running plan, bound at its start. */
    AXIS_BOUND("axis_bound", 409),

    /** A change of a value the project's team owns. */
    TEAM_OWNED("team_owned", 409),

    /** An act on an ended plan. */
    PLAN_ENDED("plan_ended", 409),

    /** A waiver whose item was already consumed. */
    ALREADY_CONSUMED("already_consumed", 409),

    /** A waiver request the item does not allow. */
    NOT_WAIVABLE("not_waivable", 409),

    /** A raise above the allowed range. */
    RAISE_OUT_OF_RANGE("raise_out_of_range", 409),

    /** A position outside the addressed document. */
    POSITION_NOT_CONTAINED("position_not_contained", 409),

    /** An enrolment whose root conflicts with another enrolment. */
    ENROLMENT_ROOT_CONFLICT("enrolment_root_conflict", 409),

    /** A project store root that does not match the enrolment. */
    PROJECT_STORE_ROOT_MISMATCH("project_store_root_mismatch", 409),

    /** A request that uses a project's enrolment file failing its verification. */
    ENROLMENT_TAMPERED("enrolment_tampered", 409),

    /** A project's enrolment file that is not private. */
    ENROLMENT_INSECURE("enrolment_insecure", 409),

    /** A project's enrolment file that is invalid. */
    ENROLMENT_INVALID("enrolment_invalid", 409),

    /** A harness version outside the profile's supported versions. */
    HARNESS_VERSION_UNSUPPORTED("harness_version_unsupported", 409),

    /** Opening a web listener whose port is occupied. */
    WEB_LISTENER_CONFLICT("web_listener_conflict", 409),

    /** The withdrawal of a stop or an upgrade that can no longer be withdrawn. */
    NOT_WITHDRAWABLE("not_withdrawable", 409),

    /** An upgrade of an installation the runtime does not upgrade. */
    UPGRADE_NOT_POSSIBLE("upgrade_not_possible", 409),

    /** The recorded credential store cannot be reached. */
    CREDENTIAL_STORE_UNAVAILABLE("credential_store_unavailable", 409),

    /** A unit that cannot be designed at the addressed level. */
    UNIT_NOT_DESIGNABLE("unit_not_designable", 409),

    /** A unit that fails validation, with its findings. */
    UNIT_INVALID("unit_invalid", 409),

    /** A design request with no project to compose for. */
    NO_PROJECT_TO_COMPOSE("no_project_to_compose", 409),

    // 423, 500, 503: the store or the runtime is not in a state to serve the request.

    /** The chosen keyring is locked and unlocking it needs the operator. */
    CREDENTIAL_STORE_LOCKED("credential_store_locked", 423),

    /** An unexpected fault the runtime could not attribute to the request, with an audit id. */
    INTERNAL_FAULT("internal_fault", 500),

    /** The chosen credential store failed a read, a write, a delete, or a read-back. */
    CREDENTIAL_STORE_FAILED("credential_store_failed", 500),

    /** Retryable: the addressed project's recovery has not finished within its wait. */
    RUNTIME_RECOVERING("runtime_recovering", 503);

    /** The HTTP statuses the closed catalogue draws from, derived from its members. */
    private static final List<Integer> STATUSES = Arrays.stream(values())
            .map(ApiErrorCode::status)
            .distinct()
            .sorted()
            .toList();

    private final String wire;
    private final int status;

    ApiErrorCode(String wire, int status) {
        this.wire = wire;
        this.status = status;
    }

    /**
     * @return the wire representation of this code
     */
    public String wire() {
        return wire;
    }

    /**
     * @return the HTTP status this code is answered with
     */
    public int status() {
        return status;
    }

    /**
     * @return the HTTP statuses of the catalogue, in ascending order
     */
    public static List<Integer> statuses() {
        return STATUSES;
    }

    /**
     * Resolves a wire value to its code.
     *
     * @param wire the wire representation
     * @return the matching code
     * @throws IllegalArgumentException if no code matches (fail-closed)
     */
    public static ApiErrorCode fromWire(String wire) {
        var code = fromWireOrNull(wire);
        if (code == null) {
            throw new IllegalArgumentException("Unknown API error code: " + wire);
        }
        return code;
    }

    /**
     * Resolves a wire value to its code.
     *
     * @param wire the wire representation
     * @return the matching code, or {@code null} if none matches
     */
    public static ApiErrorCode fromWireOrNull(String wire) {
        if (wire == null) {
            return null;
        }
        for (var code : values()) {
            if (code.wire.equalsIgnoreCase(wire)) {
                return code;
            }
        }
        return null;
    }

    /**
     * @return the wire representations of every code, in declaration order
     */
    public static List<String> wireValues() {
        return Arrays.stream(values()).map(ApiErrorCode::wire).toList();
    }
}
