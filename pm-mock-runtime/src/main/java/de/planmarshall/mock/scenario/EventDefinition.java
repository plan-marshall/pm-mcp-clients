/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.mock.scenario;

import java.util.Objects;

/**
 * Definition of a timed event emitted over Server-Sent Events relative to entering a state.
 *
 * @param afterMs delay in milliseconds relative to state entry
 * @param event   the event name (e.g. {@code check})
 * @param data    the event payload
 */
public record EventDefinition(long afterMs, String event, Object data) {

    /**
     * Compact constructor validating required fields.
     */
    public EventDefinition {
        if (afterMs < 0) {
            throw new IllegalArgumentException("afterMs cannot be negative: " + afterMs);
        }
        Objects.requireNonNull(event, "event");
    }
}
