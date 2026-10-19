/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.start;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * The derivation of a recipe's plan id (Domain Extensions, Recipe Plan ID; State Catalogue,
 * {@code start.ready}): {@code recipe-{recipe_key}-{yyyy-mm-dd-hh-mm-ss}}.
 * <p>
 * The id is derived from the recipe's key and the time of the creation and is <strong>not</strong>
 * changeable: the operator changes the title of a recipe's plan, never its id. A recipe key is
 * {@code [a-z][a-z0-9]*(-[a-z0-9]+)*}, so the derived id satisfies the plan-id grammar and the
 * {@link #isRecipePlanId(String)} check is a total, closed test.
 */
public final class RecipePlanId {

    /** The prefix of every recipe's plan id. */
    public static final String PREFIX = "recipe-";

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private RecipePlanId() {
        // Utility class
    }

    /**
     * Derives the plan id of a recipe at an instant.
     *
     * @param recipeKey the recipe's key
     * @param at        the instant of the creation, in UTC
     * @return {@code recipe-{recipe_key}-{yyyy-mm-dd-hh-mm-ss}}
     */
    public static String derive(String recipeKey, LocalDateTime at) {
        if (recipeKey == null || recipeKey.isBlank()) {
            throw new IllegalArgumentException("A recipe key is required to derive its plan id");
        }
        var utc = at == null ? LocalDateTime.now(ZoneOffset.UTC) : at;
        return PREFIX + recipeKey + "-" + TIMESTAMP.format(utc);
    }

    /**
     * @param planId a candidate plan id
     * @return {@code true} if the id is one this derivation produces
     */
    public static boolean isRecipePlanId(String planId) {
        if (planId == null || !planId.startsWith(PREFIX)) {
            return false;
        }
        var rest = planId.substring(PREFIX.length());
        // the timestamp is the last 19 characters (yyyy-mm-dd-hh-mm-ss)
        if (rest.length() < PREFIX.length() + 19) {
            return false;
        }
        var split = rest.length() - 19;
        if (split < 1 || rest.charAt(split - 1) != '-') {
            return false;
        }
        var key = rest.substring(0, split - 1);
        var timestamp = rest.substring(split);
        if (!matchesKey(key)) {
            return false;
        }
        try {
            LocalDateTime.parse(timestamp, TIMESTAMP);
            return true;
        }
        /*TODO: Catch specific not RuntimeException. Suppress: // cui-rewrite:disable InvalidExceptionUsageRecipe*/
        catch (RuntimeException _) {
            return false;
        }
    }

    /**
     * @param key a candidate recipe key
     * @return {@code true} if the key satisfies {@code [a-z][a-z0-9]*(-[a-z0-9]+)*}
     */
    public static boolean matchesKey(String key) {
        if (key == null || key.isEmpty() || key.length() > 37) {
            return false;
        }
        if (key.charAt(0) < 'a' || key.charAt(0) > 'z') {
            return false;
        }
        var previousDash = false;
        for (var i = 1; i < key.length(); i++) {
            var c = key.charAt(i);
            if (c == '-') {
                if (previousDash || i == key.length() - 1) {
                    return false;
                }
                previousDash = true;
            } else if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                previousDash = false;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the catalogue of the id's parts, for a client that shows it
     */
    public static List<String> parts() {
        return Arrays.asList(PREFIX, "recipe_key", "yyyy-mm-dd-hh-mm-ss");
    }
}
