/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.message.MessageCatalogue;

@DisplayName("Error texts of the local API")
class ErrorTextsTest {

    private static final List<String> LOCALES = List.of("en", "de");
    private static final List<String> PARTS = List.of("title", "cause", "fix");

    @Test
    @DisplayName("every code carries a title, a cause and a fix in both languages")
    void everyCodeCarriesItsThreeTexts() {
        for (var locale : LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, "error");
            for (var code : ApiErrorCode.values()) {
                for (var part : PARTS) {
                    var key = key(code.wire(), part);
                    assertTrue(messages.containsKey(key), "Missing " + key + " in " + locale);
                    assertFalse(messages.get(key).isBlank(), "Blank " + key + " in " + locale);
                    assertNotEquals(key, messages.get(key), "The key stands in for the text of " + key);
                }
            }
        }
    }

    @Test
    @DisplayName("the unknown fallback exists in both languages")
    void unknownFallbackExists() {
        for (var locale : LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, "error");
            for (var part : PARTS) {
                var key = key("unknown", part);
                assertTrue(messages.containsKey(key), "Missing " + key + " in " + locale);
                assertFalse(messages.get(key).isBlank(), "Blank " + key + " in " + locale);
            }
        }
    }

    @Test
    @DisplayName("no key of the area names a code outside the closed catalogue")
    void noKeyNamesACodeOutsideTheCatalogue() {
        var known = new TreeSet<>(ApiErrorCode.wireValues());
        known.add("unknown");

        for (var locale : LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, "error");
            assertFalse(messages.isEmpty(), "The error area must carry texts in " + locale);

            for (var key : messages.keySet()) {
                assertTrue(key.startsWith("error."), "Key outside the error area: " + key);

                var segments = key.split("\\.");
                assertEquals(3, segments.length, "Key must read error.<code>.<part>: " + key);
                assertTrue(PARTS.contains(segments[2]), "Key names no part title, cause or fix: " + key);
                assertTrue(known.contains(segments[1]), "Key names no code of the catalogue: " + key);
            }
        }
    }

    @Test
    @DisplayName("the area is served to both clients")
    void areaIsServed() {
        assertTrue(MessageCatalogue.SUPPORTED_AREAS.contains("error"), "The error area must be served");
    }

    @Test
    @DisplayName("the lookup reads the three texts of every member, by code and by wire value")
    void lookupReadsEveryMember() {
        for (var locale : LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, "error");
            for (var code : ApiErrorCode.values()) {
                assertEquals(messages.get(key(code.wire(), "title")), ErrorTexts.title(locale, code), code.wire());
                assertEquals(messages.get(key(code.wire(), "cause")), ErrorTexts.cause(locale, code), code.wire());
                assertEquals(messages.get(key(code.wire(), "fix")), ErrorTexts.fix(locale, code), code.wire());

                assertEquals(ErrorTexts.title(locale, code), ErrorTexts.title(locale, code.wire()), code.wire());
                assertEquals(ErrorTexts.cause(locale, code), ErrorTexts.cause(locale, code.wire()), code.wire());
                assertEquals(ErrorTexts.fix(locale, code), ErrorTexts.fix(locale, code.wire()), code.wire());
            }
        }
    }

    @Test
    @DisplayName("the default language is English")
    void defaultLanguageIsEnglish() {
        for (var code : ApiErrorCode.values()) {
            assertEquals(ErrorTexts.title("en", code), ErrorTexts.title(code), code.wire());
            assertEquals(ErrorTexts.cause("en", code), ErrorTexts.cause(code), code.wire());
            assertEquals(ErrorTexts.fix("en", code), ErrorTexts.fix(code), code.wire());
        }
    }

    @Test
    @DisplayName("a wire value that is no member falls back to the unknown texts")
    void unknownWireValueFallsBack() {
        for (var locale : LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, "error");
            assertEquals(messages.get(key("unknown", "title")), ErrorTexts.title(locale, "no_such_code"));
            assertEquals(messages.get(key("unknown", "cause")), ErrorTexts.cause(locale, "no_such_code"));
            assertEquals(messages.get(key("unknown", "fix")), ErrorTexts.fix(locale, "no_such_code"));
        }
    }

    @Test
    @DisplayName("a null code falls back to the unknown texts")
    void nullCodeFallsBack() {
        for (var locale : LOCALES) {
            var messages = MessageCatalogue.getMessages(locale, "error");
            assertEquals(messages.get(key("unknown", "title")), ErrorTexts.title(locale, (ApiErrorCode) null));
            assertEquals(messages.get(key("unknown", "cause")), ErrorTexts.cause(locale, (ApiErrorCode) null));
            assertEquals(messages.get(key("unknown", "fix")), ErrorTexts.fix(locale, (ApiErrorCode) null));
            assertEquals(messages.get(key("unknown", "title")), ErrorTexts.title(locale, (String) null));
        }
    }

    private static String key(String code, String part) {
        return "error." + code + "." + part;
    }
}
