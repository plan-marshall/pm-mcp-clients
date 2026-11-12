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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The configuration keys an onboarding aspect owns and the plain-key route therefore never writes
 * ({@code 01-commands.adoc:148}): the client refuses them with {@code 409 aspect_owned} and the
 * runtime refuses them the same way, so both read this one table.
 *
 * <p>A key ending in {@code .} owns a family of keys ({@code language.interface}); any other key
 * owns only itself ({@code roles}, {@code credentials.store}, {@code sources.documents_mode}), so a
 * family an aspect does not own is never swept in by a prefix that merely starts the same way.
 */
public final class AspectOwnedKeys {

    /** The one table: an owned key or key prefix, and the aspect verb that owns it. */
    private static final Map<String, String> OWNED = owned();

    private AspectOwnedKeys() {
    }

    private static Map<String, String> owned() {
        var map = new LinkedHashMap<String, String>();
        map.put("language.", "language");
        map.put("harness.", "harnesses");
        map.put("models.", "models");
        map.put("credentials.store", "credentials");
        map.put("providers.", "providers");
        map.put("plan_defaults.", "plan-defaults");
        map.put("roles", "roles");
        map.put("roles.", "roles");
        map.put("sources.documents_mode", "documents");
        return Collections.unmodifiableMap(map);
    }

    /**
     * @param key a configuration key, or {@code null}
     * @return {@code true} when an aspect owns the key and only its verb changes it
     */
    public static boolean isAspectOwned(@Nullable String key) {
        return owningAspect(key).isPresent();
    }

    /**
     * @param key a configuration key, or {@code null}
     * @return the aspect verb that owns the key, or an empty optional when no aspect owns it
     */
    public static Optional<String> owningAspect(@Nullable String key) {
        if (key == null) {
            return Optional.empty();
        }
        var trimmed = normalise(key);
        for (var entry : OWNED.entrySet()) {
            var owned = entry.getKey();
            var matches = owned.endsWith(".") ? trimmed.startsWith(owned) : trimmed.equals(owned);
            if (matches) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * @param key a configuration key
     * @return the key with its whitespace trimmed, for the callers that compare it
     */
    static String normalise(String key) {
        return Objects.requireNonNull(key, "key").trim();
    }
}
