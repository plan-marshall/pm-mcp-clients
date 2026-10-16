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
     * Browser pairing flow demonstrating pairing code exchange and subsequent authenticated web
     * client requests.
     */
    public static final String WEB_PAIRING = "web-pairing";

    /**
     * Paired devices management flow demonstrating listing, renaming, revoking devices,
     * and network listener toggling.
     */
    public static final String PAIRED_DEVICES = "paired-devices";

    /**
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
     * Unmodifiable list of all delivered standard scenario names.
     */
    public static final List<String> ALL = List.of(
            STATUS_ONLY,
            ONBOARDING_HAPPY,
            ONBOARDING_HARNESS_NOT_LOGGED_IN,
            WEB_PAIRING,
            PAIRED_DEVICES,
            RUNNING_WORK,
            REPORTS
    );
}
