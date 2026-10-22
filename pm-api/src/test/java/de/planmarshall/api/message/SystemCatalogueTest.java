/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class SystemCatalogueTest {

    @Test
    void shouldLoadSystemAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "system");
        var de = MessageCatalogue.getMessages("de", "system");

        assertFalse(en.isEmpty(), "English system messages must not be empty");
        assertFalse(de.isEmpty(), "German system messages must not be empty");

        var enKeys = new TreeSet<>(en.keySet());
        var deKeys = new TreeSet<>(de.keySet());

        assertEquals(enKeys, deKeys, "Keys in English and German system message packs must match exactly");
    }

    @Test
    void shouldContainKeySpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "system");

        String[] requiredKeys = {
                "system.view.title",
                "system.runtime.title",
                "system.runtime.version",
                "system.runtime.memory",
                "system.runtime.cpu",
                "system.runtime.store",
                "system.runtime.listeners",
                "system.work.title",
                "system.work.plans",
                "system.work.tasks",
                "system.workers.title",
                "system.workers.count",
                "system.jobs.title",
                "system.jobs.slots",
                "system.models.title",
                "system.models.window",
                "system.action.stop",
                "system.action.restart",
                "system.action.upgrade",
                "system.cleanup.title",
                "system.cleanup.line_due",
                "system.cleanup.summary.title",
                "system.cleanup.details.title",
                "system.cleanup.action.delete",
                "system.cleanup.action.remind",
                "system.cleanup.action.ignore",
                "system.cleanup.kept.title",
                "system.cleanup.durations.title",
                "system.cleanup.residue.title"
        };

        for (String key : requiredKeys) {
            assertTrue(en.containsKey(key), "Missing required key in system message catalogue: " + key);
            assertNotNull(en.get(key));
            assertFalse(en.get(key).isBlank());
        }
    }

    @Test
    void shouldFormatInterpolatedValuesCorrectly() {
        var formattedEn = MessageCatalogue.format("en", "system.runtime.cpu", Map.of("percent", 4));
        assertEquals("CPU 4 %", formattedEn);

        var formattedDe = MessageCatalogue.format("de", "system.runtime.cpu", Map.of("percent", 4));
        assertEquals("CPU 4 %", formattedDe);
    }
}
