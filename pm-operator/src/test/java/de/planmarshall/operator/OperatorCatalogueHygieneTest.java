/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import de.planmarshall.api.message.MessageCatalogue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The packs carry no text a screen prints that is already known to be wrong: no key of the operator
 * area names an HTTP status, and the dead cleanup keys that froze invented figures are gone from
 * both languages.
 */
@DisplayName("the operator catalogue carries no retired key")
class OperatorCatalogueHygieneTest {

    private static final List<String> LOCALES = List.of("en", "de");

    /** The keys of {@code operator.cleanup.*} that printed invented figures and were never read. */
    private static final List<String> RETIRED_CLEANUP_KEYS = List.of(
                        "operator.cleanup.footer",
                        "operator.cleanup.footer_sweep",
                        "operator.cleanup.kept_header",
                        "operator.cleanup.none",
                        "operator.cleanup.none_header",
                        "operator.cleanup.released",
                        "operator.cleanup.residue_header",
                        "operator.cleanup.residue.temp",
                        "operator.cleanup.residue.workers",
                        "operator.cleanup.residue.locks",
                        "operator.cleanup.residue.rows",
                        "operator.cleanup.residue.lessons",
                        "operator.config.settings_failed");

    @Test
    @DisplayName("no key of the operator area carries an HTTP status a screen would print")
    void noOperatorKeyNamesAnHttpStatus() {
        for (var locale : LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, "operator");
            assertFalse(messages.isEmpty(), "The operator area must carry texts in " + locale);
            for (var entry : messages.entrySet()) {
                assertFalse(entry.getValue().contains("HTTP "),
                                    "the key " + entry.getKey() + " prints an HTTP status in " + locale
                                                        + ": " + entry.getValue());
            }
        }
    }

    @Test
    @DisplayName("the dead cleanup keys are gone from both packs")
    void retiredCleanupKeysAreGone() {
        for (var locale : LOCALES) {
            Map<String, String> messages = MessageCatalogue.getMessages(locale, "operator");
            for (var key : RETIRED_CLEANUP_KEYS) {
                assertFalse(messages.containsKey(key), "the retired key " + key + " is still in " + locale);
            }
        }
    }

    @Test
    @DisplayName("the refusal renderer's texts exist in both languages")
    void refusalTextsExist() {
        for (var locale : LOCALES) {
            for (var key : List.of("operator.refusal.prefix", "operator.refusal.local.cause",
                                "operator.refusal.local.fix", "operator.refusal.route_missing.title",
                                "operator.refusal.route_missing.cause", "operator.refusal.route_missing.fix",
                                "operator.refusal.unreadable.title", "operator.refusal.unreadable.cause",
                                "operator.refusal.unreadable.fix")) {
                assertTrue(MessageCatalogue.hasKey(locale, key), "Missing " + key + " in " + locale);
            }
        }
    }
}
