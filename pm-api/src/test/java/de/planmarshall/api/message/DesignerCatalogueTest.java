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

class DesignerCatalogueTest {

    @Test
    void shouldLoadDesignerAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "designer");
        var de = MessageCatalogue.getMessages("de", "designer");

        assertFalse(en.isEmpty(), "English designer messages must not be empty");
        assertFalse(de.isEmpty(), "German designer messages must not be empty");

        assertEquals(new TreeSet<>(en.keySet()), new TreeSet<>(de.keySet()),
                "Keys in English and German designer message packs must match exactly");
    }

    @Test
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "designer");

        var requiredKeys = new String[]{
                "designer.view.title",
                "designer.open.title",
                "designer.open.question",
                "designer.input.upload",
                "designer.input.upload_hint",
                "designer.input.not_text",
                "designer.input.no_header",
                "designer.input.paste",
                "designer.input.installed",
                "designer.input.open",
                "designer.open.new_recipe",
                "designer.open.new_recipe_hint",
                "designer.level.project",
                "designer.level.machine",
                "designer.level.default",
                "designer.level.in_force",
                "designer.installed.copy",
                "designer.installed.compiled_only",
                "designer.draft.restored",
                "designer.leave.question",
                "designer.frontmatter.title",
                "designer.frontmatter.add",
                "designer.frontmatter.author",
                "designer.frontmatter.signature_dropped",
                "designer.frontmatter.interface_kept",
                "designer.leave.stay",
                "designer.leave.discard",
                "designer.checked_against",
                "designer.checked_against_machine",
                "designer.pane.text",
                "designer.pane.structure",
                "designer.structure.not_parsed",
                "designer.blocks.title",
                "designer.blocks.search",
                "designer.blocks.scope",
                "designer.blocks.group",
                "designer.blocks.no_match",
                "designer.blocks.compiled_only",
                "designer.blocks.detail.parameters",
                "designer.blocks.detail.outcomes",
                "designer.blocks.detail.needs",
                "designer.blocks.detail.used_in",
                "designer.blocks.detail.forms",
                "designer.blocks.detail.bounds",
                "designer.blocks.detail.inserts",
                "designer.blocks.version",
                "designer.blocks.reloaded",
                "designer.blocks.insert",
                "designer.blocks.not_here",
                "designer.validate.action",
                "designer.validate.stage",
                "designer.validate.not_run",
                "designer.validate.valid",
                "designer.validate.findings",
                "designer.validate.finding",
                "designer.validate.no_project",
                "designer.validate.outdated",
                "designer.preview.open",
                "designer.preview.title",
                "designer.place.action",
                "designer.place.question",
                "designer.place.machine",
                "designer.place.machine_hint",
                "designer.place.project",
                "designer.place.project_hint",
                "designer.place.order",
                "designer.place.compare",
                "designer.confirm.title",
                "designer.confirm.against",
                "designer.confirm.lines",
                "designer.confirm.projects",
                "designer.confirm.uncommitted",
                "designer.place.name_invalid",
                "designer.place.recipe_key_taken",
                "designer.place.no_project",
                "designer.place.unchanged",
                "designer.place.project_findings",
                "designer.confirm.adopt",
                "designer.confirm.place",
                "designer.back",
                "designer.place.step",
                "designer.place.machine_done",
                "designer.place.failed",
                "designer.place.retry",
                "designer.place.conflict",
                "designer.place.result",
                "designer.remove.action",
                "designer.remove.title",
                "designer.remove.then",
                "designer.remove.confirm",
                "designer.phone.note"
        };

        for (var key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in designer.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    void shouldExposeTheTerminalHelpLineInTheOperatorArea() {
        assertTrue(MessageCatalogue.SUPPORTED_AREAS.contains("operator"),
                "The operator area must be registered for the terminal's help line");

        var en = MessageCatalogue.getMessages("en", "operator");
        var de = MessageCatalogue.getMessages("de", "operator");

        assertEquals(new TreeSet<>(en.keySet()), new TreeSet<>(de.keySet()));
        assertTrue(en.containsKey("operator.help.designer"));
        assertTrue(en.get("operator.help.designer").contains("web app"));
        assertTrue(de.containsKey("operator.help.designer"));
    }

    @Test
    void shouldFormatSelectMessagesOfTheBlocksAndStages() {
        assertEquals("sub-workflows",
                MessageCatalogue.format("en", "designer.blocks.group", Map.of("group", "sub_workflow")));
        assertEquals("parameters",
                MessageCatalogue.format("en", "designer.blocks.group", Map.of("group", "unknown")));

        assertEquals("Building blocks of this machine",
                MessageCatalogue.format("en", "designer.blocks.scope", Map.of("scope", "machine")));
        assertEquals("Building blocks of pm-mcp-clients",
                MessageCatalogue.format("en", "designer.blocks.scope",
                        Map.of("scope", "project", "project", "pm-mcp-clients")));

        assertEquals("composed against pm-mcp-clients",
                MessageCatalogue.format("en", "designer.validate.stage",
                        Map.of("stage", "composition", "project", "pm-mcp-clients")));
        assertEquals("parses",
                MessageCatalogue.format("en", "designer.validate.stage", Map.of("stage", "parse")));
    }

    @Test
    void shouldFormatPluralAndParameterizedDesignerMessages() {
        assertEquals("1 finding: it cannot be placed.",
                MessageCatalogue.format("en", "designer.validate.findings", Map.of("count", 1)));
        assertEquals("3 findings: it cannot be placed.",
                MessageCatalogue.format("en", "designer.validate.findings", Map.of("count", 3)));

        assertEquals("INV-FIN-1 · line 42, column 3: a mandatory finalize step is missing",
                MessageCatalogue.format("en", "designer.validate.finding",
                        Map.of("rule", "INV-FIN-1", "line", 42, "column", 3,
                                "message", "a mandatory finalize step is missing")));

        assertEquals("written to .marshall/config/phase-6-finalize.pmwf",
                MessageCatalogue.format("en", "designer.place.step",
                        Map.of("step", "written", "path", ".marshall/config/phase-6-finalize.pmwf")));
        assertEquals("landed: in force from now on",
                MessageCatalogue.format("en", "designer.place.step", Map.of("step", "landed")));

        assertEquals("Place on this machine",
                MessageCatalogue.format("en", "designer.confirm.place", Map.of("target", "machine")));
        assertEquals("Place in pm-mcp-clients",
                MessageCatalogue.format("en", "designer.confirm.place",
                        Map.of("target", "project", "project", "pm-mcp-clients")));
    }
}
