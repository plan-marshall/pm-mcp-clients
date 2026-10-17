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

@DisplayName("Start message catalogue")
class StartCatalogueTest {

    @Test
    @DisplayName("loads the start area in English and German with identical keys")
    void shouldLoadStartAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "start");
        var de = MessageCatalogue.getMessages("de", "start");

        assertFalse(en.isEmpty(), "English start messages must not be empty");
        assertFalse(de.isEmpty(), "German start messages must not be empty");

        var enKeys = new TreeSet<>(en.keySet());
        var deKeys = new TreeSet<>(de.keySet());

        assertEquals(enKeys, deKeys, "Keys in English and German start message packs must match exactly");
    }

    @Test
    @DisplayName("contains the specification keys of the steps, sources, recipes and states")
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "start");

        var requiredKeys = new String[]{
                "start.plan.title",
                "start.epic.title",
                "start.gate.machine",
                "start.gate.project",
                "start.gate.continue_onboarding",
                "start.gate.continue_project",
                "start.step.sources",
                "start.step.ready",
                "start.step.started",
                "start.step.decomposition",
                "start.choose.title",
                "start.choose.sources",
                "start.choose.recipe",
                "start.choose.no_recipe",
                "start.epic.adopt.offer",
                "start.epic.adopt.action",
                "start.epic.adopt.new",
                "start.epic.adopt.failed",
                "start.sources.title",
                "start.source.description",
                "start.source.documents",
                "start.source.issue",
                "start.source.lesson",
                "start.source.local_changes",
                "start.source.pull_request",
                "start.source.add",
                "start.source.remove",
                "start.source.remove_it",
                "start.source.retry",
                "start.sources.waits",
                "start.sources.alone",
                "start.sources.continue",
                "start.sources.chat",
                "start.sources.local_changes_landing",
                "start.sources.project_settings",
                "start.ready.starts_from",
                "start.ready.title",
                "start.ready.id",
                "start.ready.id_from_title",
                "start.ready.start_plan",
                "start.ready.start_epic",
                "start.ready.epic_auto",
                "start.create.failed",
                "start.started.title",
                "start.started.follow",
                "start.started.back",
                "start.recipe.choose.title",
                "start.recipe.parameters.title",
                "start.recipe.proposed",
                "start.recipe.no_option",
                "start.recipe.no_module",
                "start.recipe.run",
                "start.entry.continue",
                "start.entry.abandon",
                "start.state.checking",
                "start.state.fetching",
                "start.state.screening",
                "start.state.stored",
                "start.state.copied",
                "start.state.moved",
                "start.state.flagged",
                "start.state.unscreened",
                "start.state.untrusted",
                "start.cause.not_found",
                "start.cause.not_granted",
                "start.cause.no_credential",
                "start.cause.rate_limited",
                "start.cause.screening_failed",
                "start.cause.no_screener",
                "start.cause.not_open",
                "start.cause.head_not_writable",
                "start.cause.no_local_changes",
                "start.documents.directory",
                "start.documents.mode.title",
                "start.documents.mode.copy",
                "start.documents.mode.move",
                "start.issue.field",
                "start.issue.grant.title",
                "start.issue.public_only",
                "start.issue.flagged.title",
                "start.unscreened.accept",
                "start.pr.field",
                "start.pr.files_untrusted",
                "start.lesson.premise",
                "start.local_changes.add",
                "start.local_changes.not_now"
        };

        for (var key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in start.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("is part of the supported areas so both clients can load it")
    void shouldBePartOfTheSupportedAreas() {
        assertTrue(MessageCatalogue.SUPPORTED_AREAS.contains("start"),
                "The start area must be registered so both clients can load it");
    }

    @Test
    @DisplayName("formats the parameterized and pluralized start messages")
    void shouldFormatParameterizedStartMessages() {
        assertEquals("waits for 1 source",
                MessageCatalogue.format("en", "start.sources.waits", Map.of("count", 1)));
        assertEquals("waits for 3 sources",
                MessageCatalogue.format("en", "start.sources.waits", Map.of("count", 3)));

        assertEquals("codeberg-provider-issues has started",
                MessageCatalogue.format("en", "start.started.title",
                        Map.of("name", "codeberg-provider-issues")));

        assertEquals("What does the plan start from?",
                MessageCatalogue.format("en", "start.sources.title", Map.of("kind", "plan")));
        assertEquals("What does the epic start from?",
                MessageCatalogue.format("en", "start.sources.title", Map.of("kind", "epic")));

        assertEquals("larger than 10 MB",
                MessageCatalogue.format("en", "start.cause.too_large", Map.of("size", "10 MB")));

        assertEquals("Ein Epic in pm-mcp-clients starten",
                MessageCatalogue.format("de", "start.epic.title", Map.of("project", "pm-mcp-clients")));
    }
}
