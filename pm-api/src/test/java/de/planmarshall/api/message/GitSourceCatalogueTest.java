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

@DisplayName("Git sources message catalogue")
class GitSourceCatalogueTest {

    @Test
    @DisplayName("loads the git_sources area in English and German with identical keys")
    void shouldLoadGitSourcesAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "git_sources");
        var de = MessageCatalogue.getMessages("de", "git_sources");

        assertFalse(en.isEmpty(), "English git_sources messages must not be empty");
        assertFalse(de.isEmpty(), "German git_sources messages must not be empty");

        var enKeys = new TreeSet<>(en.keySet());
        var deKeys = new TreeSet<>(de.keySet());

        assertEquals(enKeys, deKeys, "Keys in English and German git_sources message packs must match exactly");
    }

    @Test
    @DisplayName("contains the specification keys of the observation, screening and local changes")
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "git_sources");

        var requiredKeys = new String[]{
                "git_sources.pr.title",
                "git_sources.pr.field",
                "git_sources.pr.observe",
                "git_sources.pr.observation",
                "git_sources.pr.writable_head",
                "git_sources.pr.head_not_writable",
                "git_sources.pr.not_open",
                "git_sources.pr.files",
                "git_sources.pr.files_untrusted",
                "git_sources.pr.rate_limited",
                "git_sources.pr.flagged.title",
                "git_sources.pr.flagged.accept",
                "git_sources.pr.unscreened.accept",
                "git_sources.pr.adopt.action",
                "git_sources.pr.adopt.title",
                "git_sources.pr.adopted",
                "git_sources.local.title",
                "git_sources.local.preview",
                "git_sources.local.eligible",
                "git_sources.local.ineligible",
                "git_sources.local.pm_wf_25",
                "git_sources.local.capture",
                "git_sources.local.captured",
                "git_sources.local.snapshot",
                "git_sources.local.confirm",
                "git_sources.local.none",
                "git_sources.confirm.required",
                "git_sources.screening.clean",
                "git_sources.screening.flagged",
                "git_sources.screening.unscreened",
                "git_sources.lane.adopt",
                "git_sources.lane.land",
                "git_sources.mode.adopt_pr",
                "git_sources.mode.local_changes",
                "git_sources.tab.pr",
                "git_sources.tab.local",
                "git_sources.error"
        };

        for (var key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in git_sources.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    @DisplayName("is part of the supported areas so both clients can load it")
    void shouldBePartOfTheSupportedAreas() {
        assertTrue(MessageCatalogue.SUPPORTED_AREAS.contains("git_sources"),
                "The git_sources area must be registered so both clients can load it");
    }

    @Test
    @DisplayName("formats the parameterized and pluralized git source messages")
    void shouldFormatParameterizedGitSourceMessages() {
        assertEquals("Pull request #7001",
                MessageCatalogue.format("en", "git_sources.pr.title", Map.of("number", 7001)));
        assertEquals("1 changed file",
                MessageCatalogue.format("en", "git_sources.pr.files", Map.of("count", 1)));
        assertEquals("3 changed files",
                MessageCatalogue.format("en", "git_sources.pr.files", Map.of("count", 3)));
        assertEquals("1 file changed against main",
                MessageCatalogue.format("en", "git_sources.local.preview",
                        Map.of("count", 1, "branch", "main")));
        assertEquals("codeberg-provider-issues adopted the pull request",
                MessageCatalogue.format("en", "git_sources.pr.adopted",
                        Map.of("name", "codeberg-provider-issues")));
        assertEquals("adopt lane",
                MessageCatalogue.format("en", "git_sources.lane", Map.of("lane", "adopt")));
        assertEquals("Pull Request #7001",
                MessageCatalogue.format("de", "git_sources.pr.title", Map.of("number", 7001)));
    }
}
