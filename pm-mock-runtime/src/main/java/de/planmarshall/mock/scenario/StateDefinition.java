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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Definition of a single hypermedia state in a scenario.
 *
 * @param representation the hypermedia state payload as returned by the runtime
 * @param links          offered link relations mapped to their target transitions
 * @param events         timed events to emit over SSE while in this state
 * @param auto           optional automatic transition without a request
 */
public record StateDefinition(
        Object representation,
        Map<String, LinkDefinition> links,
        List<EventDefinition> events,
        AutoTransitionDefinition auto) {

    /**
     * Compact constructor copying collections and ensuring non-null defaults.
     */
    public StateDefinition {
        Objects.requireNonNull(representation, "representation");
        links = links == null ? Map.of() : Map.copyOf(links);
        events = events == null ? List.of() : List.copyOf(events);
    }
}
