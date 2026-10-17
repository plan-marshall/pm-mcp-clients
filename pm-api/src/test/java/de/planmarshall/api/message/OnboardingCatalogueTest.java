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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.planmarshall.api.onboarding.ConsentKind;

@DisplayName("Onboarding and machine settings catalogue")
class OnboardingCatalogueTest {

    @ParameterizedTest
    @ValueSource(strings = {"onboarding", "settings"})
    @DisplayName("loads the area in English and German with matching keys")
    void shouldHaveMatchingKeys(String area) {
        var en = MessageCatalogue.getMessages("en", area);
        var de = MessageCatalogue.getMessages("de", area);

        assertFalse(en.isEmpty(), "English " + area + " messages must not be empty");
        assertFalse(de.isEmpty(), "German " + area + " messages must not be empty");
        assertEquals(new TreeSet<>(en.keySet()), new TreeSet<>(de.keySet()),
                "Keys in English and German " + area + " packs must match exactly");
    }

    @Test
    @DisplayName("carries every onboarding key the design names")
    void shouldCarryOnboardingKeys() {
        var en = MessageCatalogue.getMessages("en", "onboarding");
        var required = new String[]{
                "onboard.step.language", "onboard.step.done",
                "onboard.entry.title", "onboard.mode.auto", "onboard.mode.guided",
                "onboard.frame.progress",
                "onboard.language.choose.title", "onboard.language.choose.why",
                "onboard.harness.consent.title", "onboard.harness.none.title",
                "onboard.harness.choose.title", "onboard.harness.probe.title",
                "onboard.harness.blocked.title", "onboard.harness.confirm.title",
                "onboard.harness.tokens.complete", "onboard.harness.tokens.partial",
                "onboard.harness.tokens.not_reported", "onboard.harness.fetch_icons.failed",
                "onboard.catalogue.consent.title", "onboard.catalogue.select.title",
                "onboard.roles.review.title", "onboard.roles.consent.title",
                "onboard.store.choose.title", "onboard.store.check.passed",
                "onboard.store.move.key_moved", "onboard.store.move.failed",
                "onboard.providers.consent.title", "onboard.providers.verify.title",
                "onboard.defaults.review.title", "onboard.defaults.validate.title",
                "onboard.auto.consent.title", "onboard.auto.run.title",
                "onboard.auto.result.title", "onboard.auto.reason.all_supported",
                "onboard.completed.title", "onboard.completed.next"
        };
        for (var key : required) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in onboarding.en.properties");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("carries every machine settings key the design names")
    void shouldCarrySettingsKeys() {
        var en = MessageCatalogue.getMessages("en", "settings");
        var required = new String[]{
                "web.settings.page.title", "web.settings.aspects.title", "web.settings.aspects.change",
                "web.settings.providers.title", "web.settings.providers.check_all",
                "web.settings.credentials.rotate", "web.settings.credentials.replace",
                "web.settings.consents.title", "web.settings.consents.withdraw",
                "web.settings.consents.none", "web.settings.back",
                "web.settings.harnesses.reassign", "web.settings.harnesses.default_locked",
                "web.settings.devices.title", "web.settings.devices.summary",
                "web.settings.cleanup.title", "web.settings.cleanup.save",
                "web.settings.documents.title"
        };
        for (var key : required) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in settings.en.properties");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("carries a title for every consent kind")
    void shouldCarryEveryConsentKindTitle() {
        for (var kind : ConsentKind.values()) {
            assertTrue(MessageCatalogue.hasKey(kind.titleKey()),
                    "Missing title for consent kind " + kind.wireValue() + ": " + kind.titleKey());
        }
    }

    @Test
    @DisplayName("formats the consent fields and the conflict line")
    void shouldFormatConsentAndConflictLines() {
        assertEquals("about 50 k tokens",
                MessageCatalogue.format("en", "common.consent.tokens.estimate", Map.of("tokens", "50 k")));
        assertEquals("not known yet: first run",
                MessageCatalogue.get("common.consent.tokens.unknown"));
        assertEquals("Consent remembered: look for harnesses",
                MessageCatalogue.format("en", "common.consent.remembered", Map.of("action", "look for harnesses")));
        assertTrue(MessageCatalogue.format("en", "web.settings.providers.expires_soon", Map.of("days", 9))
                .contains("9 days"));
        assertTrue(MessageCatalogue.format("en", "web.settings.devices.summary",
                Map.of("count", 3, "lan", "off")).contains("3 devices"));
    }

    @Test
    @DisplayName("formats the onboarding progress and step titles")
    void shouldFormatProgress() {
        assertEquals("Step 3 of 7", MessageCatalogue.format("en", "onboard.frame.progress", Map.of("step", 3)));
        assertEquals("Modelle", MessageCatalogue.format("de", "onboard.step.models", Map.of()));
    }
}
