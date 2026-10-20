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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;


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
     * The five canonical, user-facing scenarios of the mock runtime. They are the only entries the
     * interactive launcher offers; the fine-grained case scenarios below remain resolvable through
     * the legacy alias table of {@link de.planmarshall.mock.scenario.ScenarioRegistry} while the
     * consolidation waves migrate them into these five files.
     */
    public static final String PROJECTS_AND_START = "projects-and-start";

    /**
     * The system and reports scenario: the reports inbox, plan completion reports, the runtime
     * lifecycle (drain, stop, restart), and cleanup (UC-PLN-6, UC-SYS-1..3).
     */
    public static final String REPORTS_AND_SYSTEM = "reports-and-system";

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
     * The harness scan where every enrolled harness reports its token usage completely (UC-ONB-4).
     */
    public static final String ONBOARDING_HARNESS_TOKENS_COMPLETE = "onboarding-harness-tokens-complete";

    /**
     * The harness scan where a harness reports its token usage only in part, naming the missing
     * figures (UC-ONB-4).
     */
    public static final String ONBOARDING_HARNESS_TOKENS_PARTIAL = "onboarding-harness-tokens-partial";

    /**
     * The harness scan where every chosen harness reports nothing of its token usage (UC-ONB-4).
     */
    public static final String ONBOARDING_HARNESS_TOKENS_NOT_REPORTED = "onboarding-harness-tokens-not-reported";

    /**
     * The harness scan where every chosen harness fails its check, so the step comes to the same
     * place as none found (UC-ONB-4).
     */
    public static final String ONBOARDING_HARNESS_ALL_FAILED = "onboarding-harness-all-failed";

    /**
     * Each consent of the guided run answered no: a required consent stays, an optional one goes on
     * without it (UC-ONB-1..9).
     */
    public static final String ONBOARDING_REFUSALS = "onboarding-refusals";

    /**
     * A harness the binary does not know, added under Advanced, run to verify its draft profile, and
     * whose usage stage fails on a figure the draft names (UC-ONB-4).
     */
    public static final String ONBOARDING_UNKNOWN_HARNESS = "onboarding-unknown-harness";

    /**
     * The machine settings where the documents mode is changed in two clients at once: the second
     * change is refused with {@code conflict} and applied again (UC-ONB-10).
     */
    public static final String ONBOARDING_SETTINGS_CONFLICT = "onboarding-settings-conflict";

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
     * Pausing and resuming a plan at its point (UC-CTL-1).
     */
    public static final String CONTROL_PAUSE = "control-pause";

    /**
     * The five closed hold kinds and the acts that resolve them (UC-CTL-4).
     */
    public static final String CONTROL_HOLDS = "control-holds";

    /**
     * A plan's settings and their revision guard (UC-CTL-5).
     */
    public static final String CONTROL_SETTINGS = "control-settings";

    /**
     * The epic as a resource with its rows, waits, automatic mode, archive check, and tokens
     * (UC-CTL-2, UC-CTL-3).
     */
    public static final String CONTROL_EPIC = "control-epic";

    /**
     * The decomposition draft as the control contract shows it (UC-STA-6).
     */
    public static final String CONTROL_DECOMPOSITION = "control-decomposition";

    /**
     * A chat with the planning role about a plan, an epic, and ended work.
     */
    public static final String CONTROL_CHAT = "control-chat";

    /**
     * The findings and watches review with every disposition (UC-CTL-5).
     */
    public static final String CONTROL_REVIEW = "control-review";

    /**
     * An epic's tokens: a floor with its coverage, and not measured at all.
     */
    public static final String CONTROL_TOKENS = "control-tokens";

    /**
     * Starting work: a plan from sources, an epic's start, a recipe with its parameters, and the
     * draft intake (UC-STA-1..5).
     */
    public static final String START = "start";

    /**
     * A plan from one description, and a plan from a description, documents, and an issue of another
     * repository on Codeberg with its grant (UC-STA-1, UC-STA-4).
     */
    public static final String START_SOURCES = "start-sources";

    /**
     * Documents copied and moved, a directory, a file tracked by git, and a file refused (UC-STA-3).
     */
    public static final String START_DOCUMENTS = "start-documents";

    /**
     * An issue not found and tried again, a flagged issue accepted and one removed, and an issue
     * whose grant is refused (UC-STA-4).
     */
    public static final String START_ISSUE_FAILED = "start-issue-failed";

    /**
     * An issue and a pull request left unscreened (the screener failing, no qualified configuration)
     * and accepted as untrusted (UC-STA-4, UC-STA-7).
     */
    public static final String START_UNSCREENED = "start-unscreened";

    /**
     * A lesson whose premise no longer holds, used anyway, retired, and removed; a lesson claimed
     * meanwhile returning to the choice with its cause (UC-STA-8).
     */
    public static final String START_LESSON = "start-lesson";

    /**
     * An issue fetched without a credential and one with a credential refused as {@code rate_limited},
     * tried again after the reset (UC-STA-4).
     */
    public static final String START_RATE_LIMITED = "start-rate-limited";

    /**
     * Local changes, and local changes not offered while the project's landing is
     * {@code landing.files}, with the reason (UC-STA-9).
     */
    public static final String START_LOCAL_CHANGES = "start-local-changes";

    /**
     * A pull request from a fork with its untrusted changed files (UC-STA-7).
     */
    public static final String START_PULL_REQUEST = "start-pull-request";

    /**
     * An epic from documents, its ready step showing {@code epic.auto_start} in force with its origin
     * and never asking the scope (UC-STA-2).
     */
    public static final String START_EPIC = "start-epic";

    /**
     * An existing epic adopted at {@code start-epic.adopt}, and an adoption failed at
     * {@code start-epic.adopt-failed} (UC-STA-2).
     */
    public static final String START_EPIC_ADOPT = "start-epic-adopt";

    /**
     * A start refused at {@code start.refused} because its project is not ready, with the need and
     * the action that fixes it (UC-STA-1).
     */
    public static final String START_REFUSED = "start-refused";

    /**
     * A recipe with parameters through {@code start.ready}, and one with a parameter without an
     * option (UC-STA-5).
     */
    public static final String START_RECIPE = "start-recipe";

    /**
     * An entry left and continued, with no time limit: an entry never expires and no sweep removes
     * it (UC-STA-1).
     */
    public static final String START_ENTRY_CONTINUE = "start-entry-continue";

    /**
     * The start chat of a plan's entry: a description-only entry with Chat available at once, a
     * question only, and an addition to the description accepted (UC-STA-1, UC-CTL-1).
     */
    public static final String START_CHAT = "start-chat";

    /**
     * The follow-up plan of a chat about ended work, from an ended plan and from an ended epic
     * (UC-CTL-1).
     */
    public static final String START_FOLLOW_UP = "start-follow-up";

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
     * A plan with an intent record, its self-review with a {@code not_verified} criterion, and a
     * builder check (UC-PLN-1, UC-PLN-2; PM-WF-10, PM-WF-13).
     */
    public static final String INTENT_PLAN = "intent-plan";

    /**
     * A plan's outline record with deliverables, affected files, criteria, justified dependencies,
     * and a missing key link (UC-PLN-1; PM-WF-10).
     */
    public static final String INTENT_OUTLINE = "intent-outline";

    /**
     * An epic's intent with its done-when checks, evidence, non-goals, unspecified aspects, and its
     * plans' criteria (UC-CTL-2; PM-WF-15).
     */
    public static final String INTENT_EPIC = "intent-epic";

    /**
     * The {@code over_size} token distribution of a role's past plan tasks against their predicted
     * size, with the share a changed multiple would flag (PM-WORK-7).
     */
    public static final String INTENT_OVERSIZE = "intent-oversize";

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
     * Unmodifiable list of the five canonical scenarios, in the order the launcher presents them.
     * The list is loaded from {@code scenarios/primary.txt}, the one file {@code start.sh} reads too,
     * so the Java catalog and the interactive menu cannot drift apart.
     */
    public static final List<String> PRIMARY_SCENARIOS = loadPrimaryScenarios();

    /**
     * @return the primary scenario names in launcher order
     */
    private static List<String> loadPrimaryScenarios() {
        var resource = "scenarios/primary.txt";
        var expectedCount = 5;
        try (var in = StandardScenarios.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + resource);
            }
            try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                var names = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList();
                if (names.size() != expectedCount) {
                    throw new IllegalStateException("Expected " + expectedCount
                            + " primary scenarios in " + resource + ", found " + names.size());
                }
                return names;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + resource, e);
        }
    }

    /**
     * Unmodifiable list of all delivered standard scenario names. The entry cases are derived from
     * {@link #ENTRY_CASES}, so a new case is listed here by construction.
     */
    public static final List<String> ALL = Stream.of(
            List.of(STATUS_ONLY, ONBOARDING_HAPPY, ONBOARDING_HARNESS_NOT_LOGGED_IN, ONBOARDING,
                    ONBOARDING_HARNESS_TOKENS_COMPLETE, ONBOARDING_HARNESS_TOKENS_PARTIAL,
                    ONBOARDING_HARNESS_TOKENS_NOT_REPORTED, ONBOARDING_HARNESS_ALL_FAILED,
                    ONBOARDING_REFUSALS, ONBOARDING_UNKNOWN_HARNESS, ONBOARDING_SETTINGS_CONFLICT,
                    WEB_PAIRING, ENTRY),
            ENTRY_CASES,
            List.of(PAIRED_DEVICES, RUNNING_WORK, REPORTS, DECISIONS, SYSTEM, PROJECTS, PLAN_VIEW, CONTROL,
                    CONTROL_PAUSE, CONTROL_HOLDS, CONTROL_SETTINGS, CONTROL_EPIC, CONTROL_DECOMPOSITION,
                    CONTROL_CHAT, CONTROL_REVIEW, CONTROL_TOKENS,
                    START, START_SOURCES, START_DOCUMENTS, START_ISSUE_FAILED, START_UNSCREENED,
                    START_LESSON, START_RATE_LIMITED, START_LOCAL_CHANGES, START_PULL_REQUEST,
                    START_EPIC, START_EPIC_ADOPT, START_REFUSED, START_RECIPE, START_ENTRY_CONTINUE,
                    START_CHAT, START_FOLLOW_UP,
                    LESSON_SOURCE, GIT_SOURCES, DECOMPOSITION, DESIGNER, ENROLMENT,
                    INTENT_PLAN, INTENT_OUTLINE, INTENT_EPIC, INTENT_OVERSIZE,
                    OPERATOR_INTERACTION, PROJECTS_AND_START, REPORTS_AND_SYSTEM))
            .flatMap(List::stream)
            .toList();
}
