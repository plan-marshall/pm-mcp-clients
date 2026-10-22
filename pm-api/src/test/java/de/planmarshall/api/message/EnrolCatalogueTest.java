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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EnrolCatalogueTest")
class EnrolCatalogueTest {

    private static final String SHA =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    @Test
    @DisplayName("loads the enrolment area in English and German with exact key parity")
    void shouldLoadEnrolmentAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "enrolment");
        var de = MessageCatalogue.getMessages("de", "enrolment");

        assertFalse(en.isEmpty(), "English enrolment messages must not be empty");
        assertFalse(de.isEmpty(), "German enrolment messages must not be empty");

        var enKeys = new TreeSet<>(en.keySet());
        var deKeys = new TreeSet<>(de.keySet());

        assertEquals(enKeys, deKeys, "Keys in English and German enrolment packs must match exactly");
        assertTrue(MessageCatalogue.SUPPORTED_AREAS.contains("enrolment"),
                "the enrolment area must be registered in the catalogue");
    }

    @Test
    @DisplayName("contains all required specification keys")
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "enrolment");

        String[] requiredKeys = {
                "enrolment.enrol.done",
                "enrolment.enrol.mode.pilot",
                "enrolment.enrol.mode.standard",
                "enrolment.enrol.refused.root_conflict",
                "enrolment.enrol.refused.store_root_mismatch",
                "enrolment.enrol.failed",
                "enrolment.unenrol.done",
                "enrolment.unenrol.confirm_required",
                "enrolment.unenrol.not_found",
                "enrolment.unenrol.scope_active",
                "enrolment.unenrol.failed",
                "enrolment.accept.title",
                "enrolment.accept.metric",
                "enrolment.accept.recorded",
                "enrolment.accept.current",
                "enrolment.accept.size",
                "enrolment.accept.sha256",
                "enrolment.accept.bytes",
                "enrolment.accept.mismatch",
                "enrolment.accept.confirm",
                "enrolment.accept.confirm_required",
                "enrolment.accept.declined",
                "enrolment.accept.match",
                "enrolment.accept.done",
                "enrolment.accept.not_found",
                "enrolment.accept.refused",
                "enrolment.accept.refused.stale",
                "enrolment.accept.failed",
                "enrolment.harness.title",
                "enrolment.harness.metric",
                "enrolment.harness.recorded",
                "enrolment.harness.detected",
                "enrolment.harness.path",
                "enrolment.harness.version",
                "enrolment.harness.sha256",
                "enrolment.harness.not_recorded",
                "enrolment.harness.confirm",
                "enrolment.harness.confirm_required",
                "enrolment.harness.declined",
                "enrolment.harness.done",
                "enrolment.harness.updated",
                "enrolment.harness.not_found",
                "enrolment.harness.probe_failed",
                "enrolment.harness.unsupported_client",
                "enrolment.harness.version_unsupported",
                "enrolment.harness.invalid",
                "enrolment.harness.failed"
        };

        for (String key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in enrolment.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("formats parameterized messages in English and German")
    void shouldFormatParameterizedMessages() {
        var enEnrol = MessageCatalogue.format("en", "enrolment.enrol.done",
                Map.of("id", "new-service", "mode", MessageCatalogue.format("en", "enrolment.enrol.mode.pilot", Map.of())));
        assertEquals("✓ Enrolled new-service (pilot mode)", enEnrol);

        var deEnrol = MessageCatalogue.format("de", "enrolment.enrol.done",
                Map.of("id", "new-service", "mode", MessageCatalogue.format("de", "enrolment.enrol.mode.pilot", Map.of())));
        assertEquals("✓ new-service eingetragen (Pilotmodus)", deEnrol);

        var enAccept = MessageCatalogue.format("en", "enrolment.accept.done", Map.of("sha", SHA));
        assertEquals("✓ Edit accepted; the store is re-baselined to " + SHA, enAccept);

        var deBytes = MessageCatalogue.format("de", "enrolment.accept.bytes", Map.of("size", "1.512"));
        assertEquals("1.512 Bytes", deBytes);

        var enHarness = MessageCatalogue.format("en", "enrolment.harness.done",
                Map.of("client", "claude", "path", "/usr/local/bin/claude", "version", "2.4.1"));
        assertEquals("✓ Harness claude enrolled: /usr/local/bin/claude (2.4.1)", enHarness);

        var deHarness = MessageCatalogue.format("de", "enrolment.harness.updated",
                Map.of("client", "claude", "path", "/usr/local/bin/claude", "version", "2.4.1"));
        assertEquals("✓ Harness claude neu aufgezeichnet: /usr/local/bin/claude (2.4.1)", deHarness);
    }
}
