/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.config;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The configuration keys an onboarding aspect owns and the plain-key route therefore never writes
 * ({@code 01-commands.adoc:148}): the client refuses them with {@code 409 aspect_owned} and the
 * runtime refuses them the same way, so both read this one list.
 *
 * <p>A prefix owns a family of keys ({@code language.interface}); an exact key owns only itself
 * ({@code roles}, {@code sources.documents_mode}).
 */
public final class AspectOwnedKeys {

    /** The key prefixes an aspect owns. */
    private static final List<String> PREFIXES = List.of(
            "language.", "harness.", "models.", "credentials.", "providers.",
            "plan_defaults.", "roles.");

    /** The keys an aspect owns as a whole. */
    private static final List<String> EXACT = List.of("roles", "sources.documents_mode");

    private AspectOwnedKeys() {
    }

    /**
     * @return the key prefixes an aspect owns, in the order the catalogue lists them
     */
    public static List<String> prefixes() {
        return PREFIXES;
    }

    /**
     * @return the keys an aspect owns as a whole
     */
    public static List<String> exact() {
        return EXACT;
    }

    /**
     * @param key a configuration key, or {@code null}
     * @return {@code true} when an aspect owns the key and only its verb changes it
     */
    public static boolean isAspectOwned(@Nullable String key) {
        if (key == null) {
            return false;
        }
        if (EXACT.contains(key)) {
            return true;
        }
        for (var prefix : PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param key a configuration key
     * @return the key with its whitespace trimmed, for the callers that compare it
     */
    static String normalise(String key) {
        return Objects.requireNonNull(key, "key").trim();
    }
}
