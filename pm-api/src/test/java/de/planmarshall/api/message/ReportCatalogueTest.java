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

import de.planmarshall.api.report.ReportKind;

class ReportCatalogueTest {

    @Test
    void shouldLoadReportsAreaInEnglishAndGerman() {
        var en = MessageCatalogue.getMessages("en", "reports");
        var de = MessageCatalogue.getMessages("de", "reports");

        assertFalse(en.isEmpty(), "English reports messages must not be empty");
        assertFalse(de.isEmpty(), "German reports messages must not be empty");

        var enKeys = new TreeSet<>(en.keySet());
        var deKeys = new TreeSet<>(de.keySet());

        assertEquals(enKeys, deKeys, "Keys in English and German reports message packs must match exactly");
    }

    @Test
    void shouldContainSubjectKeyForEveryReportKind() {
        var en = MessageCatalogue.getMessages("en", "reports");
        var de = MessageCatalogue.getMessages("de", "reports");

        for (ReportKind kind : ReportKind.values()) {
            String key = "report.subject." + kind.value().replace('.', '_');
            assertTrue(en.containsKey(key), "English pack must contain subject key for ReportKind " + kind + " ('" + key + "')");
            assertTrue(de.containsKey(key), "German pack must contain subject key for ReportKind " + kind + " ('" + key + "')");
            assertNotNull(en.get(key));
            assertNotNull(de.get(key));
            assertFalse(en.get(key).isBlank());
            assertFalse(de.get(key).isBlank());
        }
    }

    @Test
    void shouldContainAllSpecificationKeys() {
        var en = MessageCatalogue.getMessages("en", "reports");

        String[] requiredKeys = {
                "report.list.title",
                "report.list.unread",
                "report.list.empty",
                "report.list.from",
                "report.list.subject",
                "report.list.created",
                "report.list.sent",
                "report.list.sent_at",
                "report.list.project",
                "report.list.show_older",
                "report.action.details",
                "report.action.mark_read",
                "report.action.mark_all_read",
                "report.read.done",
                "report.arrival.line",
                "report.arrival.show",
                "report.notice.one",
                "report.notice.many",
                "report.detail.deleted",
                "report.list.machine",
                "report.completion.title",
                "report.completion.open",
                "report.completion.outcome",
                "report.completion.reason",
                "report.completion.ran",
                "report.completion.ran_value",
                "report.completion.deliverables",
                "report.completion.deliverables_done",
                "report.completion.deliverables_none",
                "report.completion.replanning",
                "report.completion.replanning_entry",
                "report.completion.decisions",
                "report.completion.decision_by",
                "report.completion.own_calls",
                "report.completion.own_call_kind",
                "report.completion.own_call_limit",
                "report.completion.own_call_ends",
                "report.completion.own_call_consistent",
                "report.completion.own_call_conflicts",
                "report.completion.change_map",
                "report.completion.change_map_state",
                "report.completion.change_map_cause",
                "report.completion.tokens",
                "report.completion.tokens_value",
                "report.completion.tokens_scope",
                "report.completion.tokens_open",
                "report.tokens.title",
                "report.tokens.total",
                "report.tokens.total_value",
                "report.tokens.coverage",
                "report.tokens.all_measured",
                "report.tokens.by_phase",
                "report.tokens.by_role",
                "report.tokens.by_model",
                "report.tokens.role_pin",
                "report.tokens.unmeasured",
                "report.tokens.waste",
                "report.tokens.waste_value",
                "report.tokens.waste.no_product",
                "report.tokens.waste.retries",
                "report.tokens.waste.remediation",
                "report.tokens.waste_tasks",
                "report.tokens.waste_cause",
                "report.tokens.waste_none",
                "report.tokens.components",
                "report.tokens.component",
                "report.tokens.component_not_reported",
                "report.tokens.component_not_applicable",
                "report.completion.plan_view",
                "report.completion.none",
                "operator.reports.arrival_hint",
                "operator.reports.open_hint",
                "operator.reports.read_hint",
                "operator.reports.older",
                "operator.reports.key_count",
                "operator.reports.keys.details",
                "operator.reports.keys.read",
                "operator.reports.keys.read_all",
                "operator.reports.reconnected",
                "operator.reports.not_found",
                "operator.reports.already_read",
                "operator.reports.not_ended",
                "operator.reports.tokens_hint",
                "operator.reports.components_hint"
        };

        for (String key : requiredKeys) {
            assertTrue(en.containsKey(key), "Key '" + key + "' must exist in reports.en.properties");
            assertNotNull(en.get(key), "Value for '" + key + "' must not be null");
            assertFalse(en.get(key).isBlank(), "Value for '" + key + "' must not be blank");
        }
    }

    @Test
    void shouldFormatParameterizedReportMessages() {
        var arrivalEn = MessageCatalogue.format("en", "report.arrival.show", Map.of());
        assertEquals("Show them", arrivalEn);

        var arrivalDe = MessageCatalogue.format("de", "report.arrival.show", Map.of());
        assertEquals("Zeige sie", arrivalDe);

        var openHint = MessageCatalogue.format("en", "operator.reports.open_hint", Map.of("id", "rep-0142"));
        assertEquals("reports open rep-0142", openHint);

        assertEquals("design call",
                MessageCatalogue.format("en", "report.completion.own_call_kind", Map.of("kind", "design")));
        assertEquals("ersetzte Abhängigkeit",
                MessageCatalogue.format("de", "report.completion.own_call_kind", Map.of("kind", "dependency")));
        assertEquals("conflicts with the transport test",
                MessageCatalogue.format("en", "report.completion.own_call_conflicts", Map.of("item", "the transport test")));
        assertEquals("stimmig",
                MessageCatalogue.format("de", "report.completion.own_call_consistent", Map.of()));
        assertEquals("partial",
                MessageCatalogue.format("en", "report.completion.change_map_state", Map.of("state", "partial")));
        assertEquals("unbestimmt",
                MessageCatalogue.format("de", "report.completion.change_map_state", Map.of("state", "indeterminate")));
    }
}
