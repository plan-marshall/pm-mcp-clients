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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;

/**
 * Checks the chat's texts of the {@code control} and {@code decisions} packs. The catalogue is the
 * source of the texts (ADR 002), so the test keeps the families' en/de parity and the texts it states
 * literally; it reads no document.
 */
@DisplayName("Chat texts: the packs keep their control.chat.* and decision.chat.* keys in both languages")
@EnableGeneratorController
class ChatTextsCatalogueTest {

    @Test
    @DisplayName("both chat families have equal, non-empty key sets in English and German")
    void chatFamiliesKeepTheirKeySets() {
        assertAll(
                () -> assertKeyParity("control", "control.chat."),
                () -> assertKeyParity("decisions", "decision.chat."));
    }

    private static void assertKeyParity(String area, String prefix) {
        var en = keysWithPrefix(MessageCatalogue.getMessages("en", area), prefix);
        var de = keysWithPrefix(MessageCatalogue.getMessages("de", area), prefix);

        assertAll(prefix,
                () -> assertFalse(en.isEmpty(), "the " + area + " pack must carry " + prefix + " keys"),
                () -> assertEquals(en, de, "the " + prefix + " key sets of en and de must match"));
    }

    private static Set<String> keysWithPrefix(Map<String, String> pack, String prefix) {
        var keys = new TreeSet<String>();
        pack.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(keys::add);
        return keys;
    }

    @Test
    @DisplayName("a decision answered names no channel when it carries none, the terminal for cli, and no device "
            + "in the variant without one")
    void decisionAnsweredWithAbsentChannelOrDevice() {
        var answered = "control.chat.line.decision_answered";
        var noDevice = "control.chat.line.decision_answered_no_device";
        var id = Generators.letterStrings(3, 12).next();
        var device = Generators.letterStrings(3, 12).next();

        assertAll(
                () -> assertEquals(id + " answered", MessageCatalogue.format("en", answered, Map.of("id", id))),
                () -> assertEquals(id + " answered in the terminal",
                        MessageCatalogue.format("en", answered, Map.of("id", id, "channel", "cli"))),
                () -> assertEquals(id + " answered in the web app (" + device + ")",
                        MessageCatalogue.format("en", answered, Map.of("id", id, "channel", "web", "device", device))),
                () -> assertEquals(id + " answered in the web app",
                        MessageCatalogue.format("en", noDevice, Map.of("id", id, "channel", "web"))),
                () -> assertEquals(id + " answered", MessageCatalogue.format("en", noDevice, Map.of("id", id))),
                () -> assertEquals(id + " beantwortet",
                        MessageCatalogue.format("de", answered, Map.of("id", id))),
                () -> assertEquals(id + " beantwortet im Terminal",
                        MessageCatalogue.format("de", answered, Map.of("id", id, "channel", "cli"))),
                () -> assertEquals(id + " beantwortet in der Web-App",
                        MessageCatalogue.format("de", noDevice, Map.of("id", id, "channel", "web"))));
    }

    @Test
    @DisplayName("a decision that blocks the plan names the plan in the catalogue's decision_opened line")
    void decisionOpenedNamesTheBlocker() {
        var opened = "control.chat.line.decision_opened";
        var id = Generators.letterStrings(3, 12).next();

        assertAll(
                () -> assertEquals(id + " opened: blocks the plan",
                        MessageCatalogue.format("en", opened, Map.of("id", id, "blocks", "plan"))),
                () -> assertEquals(id + " gestellt: blockiert den Plan",
                        MessageCatalogue.format("de", opened, Map.of("id", id, "blocks", "plan"))),
                () -> assertEquals(id + " opened",
                        MessageCatalogue.format("en", opened, Map.of("id", id, "blocks", "nothing")),
                        "a decision that blocks nothing shows no label"));
    }
}
