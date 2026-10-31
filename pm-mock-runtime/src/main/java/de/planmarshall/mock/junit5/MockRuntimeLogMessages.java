/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.junit5;

import lombok.experimental.UtilityClass;

import de.cuioss.tools.logging.LogRecord;
import de.cuioss.tools.logging.LogRecordModel;

/**
 * The production log messages of {@code pm-mock-runtime}.
 * <p>
 * One holder serves this module, the module that logs; the messages are documented in the
 * module's {@code README.adoc}. Identifiers follow the range allocation of the CUI logging
 * standard: the {@code ERROR} messages use 200 to 299.
 */
@UtilityClass
public final class MockRuntimeLogMessages {

    /**
     * The component prefix of every identifier of this module.
     */
    public static final String PREFIX = "MOCK";

    /**
     * The {@code ERROR} messages of the mock runtime.
     */
    @UtilityClass
    public static final class ERROR {

        /**
         * Logged when the JUnit 5 extension cannot start the mock runtime.
         */
        public static final LogRecord SETUP_FAILED = LogRecordModel.builder()
                .template("Error during MockRuntime setup: %s")
                .prefix(PREFIX)
                .identifier(200)
                .build();

        /**
         * Logged when the JUnit 5 extension cannot close a started mock runtime.
         */
        public static final LogRecord CLOSE_FAILED = LogRecordModel.builder()
                .template("Failed to close MockRuntime: %s")
                .prefix(PREFIX)
                .identifier(201)
                .build();
    }
}
