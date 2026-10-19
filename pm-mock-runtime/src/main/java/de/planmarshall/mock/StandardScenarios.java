/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock;

import java.util.List;


import lombok.experimental.UtilityClass;

/**
 * Standard scenario names delivered out of the box in {@code pm-mock-runtime}.
 * <p>
 * These scenarios are located under {@code src/main/resources/scenarios/} and are automatically
 * resolved by {@link de.planmarshall.mock.scenario.ScenarioRegistry}.
 */
@UtilityClass
public final class StandardScenarios {

    /**
     * Represents an idle runtime serving machine status ({@code /api/v1/status}).
     */
    public static final String STATUS_ONLY = "status-only";

    /**
     * Complete happy path machine onboarding flow through mode selection, language selection,
     * harness verification, and confirmation.
     */
    public static final String ONBOARDING_HAPPY = "onboarding-happy";

    /**
     * Machine onboarding where harness probing fails with a missing login diagnosis and offers a
     * fix/retry link.
     */
    public static final String ONBOARDING_HARNESS_NOT_LOGGED_IN = "onboarding-harness-not-logged-in";

    /**
     * The full machine onboarding and the machine's settings: every aspect with its consents,
     * checks, the automatic run, and the revision-guarded settings (UC-ONB-1..10).
     */
    public static final String ONBOARDING = "onboarding";

    /**
     * Browser pairing flow demonstrating pairing code exchange and subsequent authenticated web
     * client requests.
     */
    public static final String WEB_PAIRING = "web-pairing";

    /**
     * The entry: a machine not onboarded yet, the pairing on loopback, the network listener with
     * its certificate fingerprint, and the fresh code after a redemption (UC-ENT-1, UC-ONB-8).
     */
    public static final String ENTRY = "entry";

    /**
     * Paired devices management flow demonstrating listing, renaming, revoking devices,
     * and network listener toggling.
     */
    public static final String PAIRED_DEVICES = "paired-devices";

    /**
     * The entry on a machine that has not been onboarded: the onboarding has not started
     * (UC-ENT-1, {@code entry.check-onboarded}).
     */
    public static final String ENTRY_UNONBOARDED = "entry-unonboarded";

    /**
     * The entry with the onboarding running, offered in the terminal and in the web app
     * (UC-ENT-1, {@code entry.where-to-onboard}).
     */
    public static final String ENTRY_ONBOARDING = "entry-onboarding";

    /**
     * The entry on an onboarded machine whose browser is paired already: no code, the loopback
     * URL printed (UC-ENT-1, {@code entry.browser}).
     */
    public static final String ENTRY_PAIRED_BROWSER = "entry-paired-browser";

    /**
     * The entry in an SSH session: the ways in to copy, a code even when a browser of the machine
     * is paired (UC-ENT-1, {@code entry.show}).
     */
    public static final String ENTRY_SSH_SESSION = "entry-ssh-session";

    /**
     * The entry after a redemption: the redeemed code is single use and the next one is fresh
     * (UC-ENT-1, {@code entry.wait}, 03-web-server.adoc#fresh-code).
     */
    public static final String ENTRY_FRESH_CODE = "entry-fresh-code";

    /**
     * The network listener could not open because its port is occupied; the open is refused
     * {@code web_listener_conflict} and the loopback listener stays open
     * (UC-ENT-1, 03-web-server.adoc#web-listener-bind).
     */
    public static final String ENTRY_LAN_CONFLICT = "entry-lan-conflict";

    /**
     * A credential entered on the network listener is refused {@code loopback_only}
     * (UC-ONB-8, 03-web-server.adoc#web-credentials).
     */
    public static final String ENTRY_LAN_CREDENTIALS = "entry-lan-credentials";

    /**
     * The paired devices renamed, revoked, and revoked as all others; the list is served with the
     * {@code paired_by} of every device (UC-ENT-2, 03-web-server.adoc#devices).
     */
    public static final String ENTRY_DEVICE_LIFECYCLE = "entry-device-lifecycle";

    /**
     * A revoked device's stream ends with {@code stream.closed} and the reason {@code device_revoked},
     * and the LAN listener's close ends its streams with {@code listener_closed}
     * (UC-ENT-2, 06-event-catalogue.adoc#stream.closed).
     */
    public static final String ENTRY_STREAM_CLOSE = "entry-stream-close";

    /**
     * The cases of the entry, one scenario per case of the use cases UC-ENT-1, UC-ENT-2, and
     * UC-ONB-8, in the order of the flow.
     */
    public static final List<String> ENTRY_CASES = List.of(
            ENTRY_UNONBOARDED,
            ENTRY_ONBOARDING,
            ENTRY_PAIRED_BROWSER,
            ENTRY_SSH_SESSION,
            ENTRY_FRESH_CODE,
            ENTRY_LAN_CONFLICT,
            ENTRY_LAN_CREDENTIALS,
            ENTRY_DEVICE_LIFECYCLE,
            ENTRY_STREAM_CLOSE);

    /**
     * Overview of running work across projects, plans, and epics with ended work archive (UC-PLN-4).
     */
    public static final String RUNNING_WORK = "running-work";

    /**
     * Operator reports scenario demonstrating unread reports list, mark-read actions,
     * arrival notices, and plan completion reports.
     */
    public static final String REPORTS = "reports";

    /**
     * Decisions scenario demonstrating open questions, multi-part decisions,
     * quorum voting, own-words interpretations, and waiver requests (UC-PLN-2).
     */
    public static final String DECISIONS = "decisions";

    /**
     * System view, runtime lifecycle controls, and cleanup operations (UC-SYS-1..3).
     */
    public static final String SYSTEM = "system";

    /**
     * Project Explorer, Onboarding, and Release operations (UC-PRJ-1..3).
     */
    public static final String PROJECTS = "projects";

    /**
     * Process tree, sub-workflow traversal, leaf states, and live worker activity (UC-PLN-1, UC-PLN-5).
     */
    public static final String PLAN_VIEW = "plan-view";

    /**
     * Plan and epic actions, holds, and a plan's settings (UC-CTL-1..5).
     */
    public static final String CONTROL = "control";

    /**
     * Starting work: a plan from sources, an epic's start, a recipe with its parameters, and the
     * draft intake (UC-STA-1..5).
     */
    public static final String START = "start";

    /**
     * Starting a plan from a lesson: the unclaimed lessons of a project, a holding and a broken
     * premise, the three-way failure recovery, and the retirement of a lesson (UC-STA-8).
     */
    public static final String LESSON_SOURCE = "lesson_source";

    /**
     * Git plan sources: adopting a pull request in the adopt lane and capturing the checkout's
     * local changes in the land lane (UC-STA-7, UC-STA-9).
     */
    public static final String GIT_SOURCES = "git_sources";

    /**
     * Epic decomposition: the workstreams and staged plans, the surface and dependency checks, the
     * operator's instruction with read-back, and the commit (UC-STA-6).
     */
    public static final String DECOMPOSITION = "decomposition";

    /**
     * The process designer: installed units at every level, the catalogue of building blocks,
     * validation findings, placing, and removing (UC-PLN-3).
     */
    public static final String DESIGNER = "designer";

    /**
     * Operator verbs of enrolment and store integrity: the project enrol and unenrol, and the digest
     * verification of a store the operator accepts an edit for (PM-EXEC-2, PM-SEC-3).
     */
    public static final String ENROLMENT = "enrolment";

    /**
     * The shared operator interaction of both clients: an operator state with links keyed by their id,
     * {@code check.result} events from the closed catalogue, and a moved state whose stale link is
     * refused {@code act_not_available} (PM-ARCH-11).
     */
    public static final String OPERATOR_INTERACTION = "operator-interaction";

    /**
     * Unmodifiable list of all delivered standard scenario names.
     */
    public static final List<String> ALL = List.of(
            STATUS_ONLY,
            ONBOARDING_HAPPY,
            ONBOARDING_HARNESS_NOT_LOGGED_IN,
            ONBOARDING,
            WEB_PAIRING,
            ENTRY,
            ENTRY_UNONBOARDED,
            ENTRY_ONBOARDING,
            ENTRY_PAIRED_BROWSER,
            ENTRY_SSH_SESSION,
            ENTRY_FRESH_CODE,
            ENTRY_LAN_CONFLICT,
            ENTRY_LAN_CREDENTIALS,
            ENTRY_DEVICE_LIFECYCLE,
            ENTRY_STREAM_CLOSE,
            PAIRED_DEVICES,
            RUNNING_WORK,
            REPORTS,
            DECISIONS,
            SYSTEM,
            PROJECTS,
            PLAN_VIEW,
            CONTROL,
            START,
            LESSON_SOURCE,
            GIT_SOURCES,
            DECOMPOSITION,
            DESIGNER,
            ENROLMENT,
            OPERATOR_INTERACTION
    );
}
