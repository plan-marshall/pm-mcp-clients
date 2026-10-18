/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.host;

/**
 * Negative control for {@link HarnessIsolationArchTest}: a test class that names a harness, the
 * violation the isolation guard must find. It is written for the test and never shipped; the guard
 * scans the production classes of the package only, so the fixture must be flagged by the scan itself.
 */
final class HarnessNamingControlFixture {

    private HarnessNamingControlFixture() {
    }

    /**
     * @return a harness id, the injected name the guard must find in the compiled class file
     */
    static String injectedHarnessId() {
        return "claude";
    }
}
