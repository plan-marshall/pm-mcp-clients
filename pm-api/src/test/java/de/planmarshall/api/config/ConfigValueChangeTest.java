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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.json.JsonFormatException;

/**
 * The change a {@code PUT} or a {@code DELETE} of a configuration value carries: the value and the
 * revision it was made against ({@code ConfigValueChange}).
 */
@EnableGeneratorController
@DisplayName("a change of a configuration value")
class ConfigValueChangeTest {

    @Test
    @DisplayName("a change round-trips its value and revision")
    void roundTrips() throws IOException {
        var value = Generators.letterStrings(2, 8).next();
        var revision = Generators.integers(0, 10_000).next().longValue();

        var parsed = ConfigValueChange.fromJson(new ConfigValueChange(value, revision).toJson());

        assertEquals(new ConfigValueChange(value, revision), parsed);
    }

    @Test
    @DisplayName("a deletion carries its revision and no value")
    void aDeletionCarriesNoValue() throws IOException {
        var revision = Generators.integers(1, 10_000).next().longValue();

        var deletion = ConfigValueChange.deletion(revision);
        var parsed = ConfigValueChange.fromJson(deletion.toJson());

        assertNull(deletion.toMap().get(ConfigValueChange.FIELD_VALUE));
        assertTrue(parsed.deletion());
        assertNull(parsed.value());
        assertEquals(revision, parsed.revision());
    }

    @Test
    @DisplayName("a body without a revision is refused: a change carries it")
    void anAbsentRevisionIsRefused() {
        var map = new LinkedHashMap<String, Object>();
        map.put(ConfigValueChange.FIELD_VALUE, Generators.letterStrings(2, 8).next());

        assertThrows(JsonFormatException.class, () -> ConfigValueChange.fromMap(map));
    }

    @Test
    @DisplayName("a negative revision is refused")
    void aNegativeRevisionIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigValueChange(null, -1));

        var map = new LinkedHashMap<String, Object>();
        map.put(ConfigValueChange.FIELD_REVISION, -1);
        assertThrows(JsonFormatException.class, () -> ConfigValueChange.fromMap(map));
    }

    @Test
    @DisplayName("a body that is no object is refused")
    void aBodyThatIsNoObjectIsRefused() {
        assertThrows(JsonFormatException.class, () -> ConfigValueChange.fromJson("[]"));
    }

    @Test
    @DisplayName("the map carries the value only when the change names one")
    void theMapCarriesTheValueOnlyWhenNamed() {
        Map<String, Object> deletion = ConfigValueChange.deletion(3).toMap();
        Map<String, Object> change = new ConfigValueChange(Generators.letterStrings(2, 8).next(), 3).toMap();

        assertFalse(deletion.containsKey(ConfigValueChange.FIELD_VALUE));
        assertTrue(change.containsKey(ConfigValueChange.FIELD_VALUE));
    }
}
