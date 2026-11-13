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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

/**
 * The one table of the aspect-owned keys ({@code 01-commands.adoc:148},
 * {@code 01-provider-credentials.adoc:140}): each owned key, the aspect that owns it, a key of a
 * family the aspect does not own, and a prefix without its dot.
 */
@EnableGeneratorController
@DisplayName("the aspect-owned keys")
class AspectOwnedKeysTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "language.interface", "harness.default", "models.allowed", "credentials.store",
            "providers.enabled", "plan_defaults.landing", "roles", "sources.documents_mode"
    })
    @DisplayName("an owned key is known to be owned")
    void anOwnedKeyIsOwned(String key) {
        assertTrue(AspectOwnedKeys.isAspectOwned(key), key);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "language", "harness", "models", "credentials.github", "providers", "plan_defaults",
            "roleset", "sources.documents"
    })
    @DisplayName("a key no aspect owns is not owned")
    void anUnownedKeyIsNotOwned(String key) {
        assertFalse(AspectOwnedKeys.isAspectOwned(key), key);
    }

    @Test
    @DisplayName("the credentials aspect owns its store alone")
    void theCredentialsAspectOwnsOnlyTheStore() {
        var other = "credentials." + Generators.letterStrings(6, 10).next();

        assertFalse(AspectOwnedKeys.isAspectOwned(other), other);
    }

    @Test
    @DisplayName("a prefix without its dot belongs to no aspect")
    void aPrefixWithoutItsDotBelongsToNoAspect() {
        var key = "roles" + Generators.letterStrings(2, 6).next();

        assertFalse(AspectOwnedKeys.isAspectOwned(key), key);
    }

    @Test
    @DisplayName("the owning aspect is named from the same table, and nothing else")
    void theOwningAspectIsNamedFromTheSameTable() {
        assertEquals("language", AspectOwnedKeys.owningAspect("language.interface").orElseThrow());
        assertEquals("credentials", AspectOwnedKeys.owningAspect("credentials.store").orElseThrow());
        assertEquals("roles", AspectOwnedKeys.owningAspect("roles").orElseThrow());
        assertEquals("documents", AspectOwnedKeys.owningAspect("sources.documents_mode").orElseThrow());
        assertFalse(AspectOwnedKeys.owningAspect("roleset").isPresent());
        assertFalse(AspectOwnedKeys.owningAspect(null).isPresent());
    }
}
