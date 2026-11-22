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
import java.util.stream.Stream;

import de.planmarshall.api.message.MessageCatalogue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The packs carry no text a screen prints that is already known to be wrong: no key of an area a
 * command prints names an HTTP status, the dead cleanup keys that froze invented figures and the
 * cause-less failure keys are gone from both languages, and every kept failure key names its cause.
 */
@DisplayName("the operator catalogue carries no retired key")
class OperatorCatalogueHygieneTest {

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

    /**
     * The cause-less failure keys a refused read used to print instead of its refusal: their
     * printers render the refusal of the response now, and the two that had no printer are gone.
     */
    private static final List<String> RETIRED_FAILURE_KEYS = List.of(
                        "operator.settings.list_failed",
                        "operator.config.update_failed",
                        "operator.client.failed",
                        "operator.client.action_failed",
                        "operator.roles.failed",
                        "operator.devices.list_failed",
                        "operator.devices.rename_failed",
                        "operator.devices.revoke_failed",
                        "operator.devices.listener_off_failed",
                        "operator.devices.actions_failed",
                        "operator.projects.select.load_failed",
                        "operator.projects.detail.load_failed");

    /** The kept failure keys: each names what the refusal does not, and carries its cause. */
    private static final List<String> KEPT_FAILURE_KEYS = List.of(
                        "operator.config.set_failed",
                        "operator.config.unset_failed",
                        "operator.config.documents_unavailable",
                        "operator.roles.show.failed",
                        "operator.roles.qualify.failed",
                        "operator.credentials.failed",
                        "operator.credentials.status_failed",
                        "operator.credentials.remove_failed",
                        "operator.client.install_failed",
                        "operator.client.update_failed",
                        "operator.client.remove_failed");

    /**
     * Every area of the catalogue: a screen of either client may print any of them, so the guard
     * reads the catalogue's own registration instead of a hand-kept list that misses an area.
     *
     * @return the registered areas
     */
    static Stream<String> areas() {
        return MessageCatalogue.SUPPORTED_AREAS.stream();
    }

    @ParameterizedTest(name = "the {0} area")
    @MethodSource("areas")
    @DisplayName("no key of an area a command prints carries an HTTP status a screen would print")
    void noPrintedKeyNamesAnHttpStatus(String area) {
        for (var locale : MessageCatalogue.SUPPORTED_LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, area);
            assertFalse(messages.isEmpty(), "The " + area + " area must carry texts in " + locale);
            for (var entry : messages.entrySet()) {
                assertFalse(entry.getValue().contains("HTTP "),
                                    "the key " + entry.getKey() + " prints an HTTP status in " + locale
                                                        + ": " + entry.getValue());
            }
        }
    }

    @Test
    @DisplayName("the dead cleanup keys and the retired failure keys are gone from both packs")
    void retiredKeysAreGone() {
        for (var locale : MessageCatalogue.SUPPORTED_LOCALES) {
            Map<String, String> messages = MessageCatalogue.getMessages(locale, "operator");
            for (var key : RETIRED_CLEANUP_KEYS) {
                assertFalse(messages.containsKey(key), "the retired key " + key + " is still in " + locale);
            }
            for (var key : RETIRED_FAILURE_KEYS) {
                assertFalse(messages.containsKey(key), "the retired key " + key + " is still in " + locale);
            }
        }
    }

    @Test
    @DisplayName("every kept failure key carries the refusal's cause in both packs")
    void keptFailureKeysCarryTheCause() {
        for (var locale : MessageCatalogue.SUPPORTED_LOCALES) {
            Map<String, String> messages = MessageCatalogue.getMessages(locale, "operator");
            for (var key : KEPT_FAILURE_KEYS) {
                var text = messages.get(key);
                assertTrue(text != null && text.contains("{reason}"),
                                    "the key " + key + " names no cause in " + locale + ": " + text);
            }
        }
    }

    @Test
    @DisplayName("the refusal renderer's texts exist in both languages")
    void refusalTextsExist() {
        for (var locale : MessageCatalogue.SUPPORTED_LOCALES) {
            for (var key : RefusalRenderer.requiredKeys()) {
                assertTrue(MessageCatalogue.hasKey(locale, key), "Missing " + key + " in " + locale);
            }
        }
    }
}
