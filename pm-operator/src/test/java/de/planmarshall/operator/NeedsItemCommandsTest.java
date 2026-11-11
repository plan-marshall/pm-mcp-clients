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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.overview.NeedsOperatorItem;
import de.planmarshall.api.overview.NeedsOperatorKind;
import de.planmarshall.api.overview.WorkLocation;
import de.planmarshall.operator.overview.OverviewFormatting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every command the list of what waits prints is a registered verb
 * ({@code doc/design/shared/needs-and-find.adoc#terminal}): the one derivation serves {@code needs},
 * the overview's rows and {@code !}, so one wrong word reaches all three.
 */
@EnableGeneratorController
@DisplayName("The command the list of what waits prints")
class NeedsItemCommandsTest {

    private static final WorkLocation PROJECT = new WorkLocation("pm-mcp-clients", "pm-mcp-clients");
    private static final WorkLocation EPIC = new WorkLocation("relay-hardening", "Relay Hardening");
    private static final WorkLocation PLAN = new WorkLocation("fix-relay", "Fix Relay Transport");
    private static final Instant SINCE = Instant.parse("2026-10-16T15:05:00Z");

    @Test
    @DisplayName("resolves to a registered verb for every kind")
    void everyKindPrintsARegisteredVerb() {
        for (var kind : NeedsOperatorKind.values()) {
            var command = OverviewFormatting.formatNeedsItemCommand(item(kind, "/api/v1/decisions/Q-007",
                                EPIC, PLAN, "plan"));
            var verb = command.split(" ", 2)[0];
            assertTrue(OperatorVerbs.registered().contains(verb),
                                kind + " prints " + command + ", and " + verb + " is no registered verb");
            assertFalse(command.contains("hold"), kind + " prints a hold verb: " + command);
        }
    }

    @Test
    @DisplayName("a failed landing with a plan opens the epic")
    void landingFailedOpensTheEpic() {
        assertEquals("epic relay-hardening",
                            OverviewFormatting.formatNeedsItemCommand(item(NeedsOperatorKind.LANDING_FAILED,
                                                "/api/v1/plans/fix-relay", EPIC, PLAN, "plan")));
        assertEquals("epic fix-relay",
                            OverviewFormatting.formatNeedsItemCommand(item(NeedsOperatorKind.LANDING_FAILED,
                                                "/api/v1/plans/fix-relay", null, PLAN, "plan")));
    }

    @Test
    @DisplayName("a decision takes its id from the item's own link, never from a store of its own")
    void decisionTakesItsIdFromTheLink() {
        var item = new NeedsOperatorItem(NeedsOperatorKind.DECISION, "/api/v1/decisions/Q-007",
                            PROJECT, EPIC, PLAN, SINCE, "plan");
        assertEquals("answer Q-007", OverviewFormatting.formatNeedsItemCommand(item));
    }

    @Test
    @DisplayName("names the subject by its title, never by its raw id")
    void subjectNamesTheTitle() {
        var item = item(NeedsOperatorKind.DECISION, "/api/v1/decisions/Q-007", EPIC, PLAN, "plan");
        assertEquals("Fix Relay Transport", OverviewFormatting.formatNeedsItemSubject(item, "en"));
        var untitled = new NeedsOperatorItem(NeedsOperatorKind.CONDITION, "/api/v1/plans/fix-relay",
                            PROJECT, EPIC, new WorkLocation("fix-relay", null), SINCE, "plan");
        assertEquals("fix-relay", OverviewFormatting.formatNeedsItemSubject(untitled, "de"));
    }

    @Test
    @DisplayName("renders what an item blocks in the operator's words, never the wire token")
    void blocksAreCatalogueWords() {
        assertEquals(Map.of("en", "blocks: plan", "de", "blockiert: Plan"),
                            Map.of("en", OverviewFormatting.formatNeedsItemBlocks("plan", "en"),
                                                "de", OverviewFormatting.formatNeedsItemBlocks("plan", "de")));
    }

    private static NeedsOperatorItem item(NeedsOperatorKind kind, String link, @Nullable WorkLocation epic,
                        @Nullable WorkLocation plan, @Nullable String blocks) {
        return new NeedsOperatorItem(kind, link, PROJECT, epic, plan, SINCE, blocks);
    }
}
