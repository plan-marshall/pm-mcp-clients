/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.overview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.planmarshall.api.MachineApiPaths;
import de.planmarshall.api.json.JsonTree;
import de.planmarshall.api.message.MessageCatalogue;

/**
 * The contract of what waits and of finding work by name: the closed list of kinds, the page of the
 * list, the page of the matches, the machine's summary, the paths, and the catalogue keys.
 */
@DisplayName("NeedsAndFindContractTest")
class NeedsAndFindContractTest {

    private static final WorkLocation CLIENTS = new WorkLocation("pm-mcp-clients", "pm-mcp-clients");
    private static final WorkLocation HARDENING = new WorkLocation("relay-hardening", "Relay Hardening");
    private static final WorkLocation FIX_RELAY = new WorkLocation("fix-relay", "Fix Relay Transport");

    @Nested
    @DisplayName("The closed list")
    class TheClosedList {

        @Test
        @DisplayName("joins credential and cleanup_due after setup, in the specification's order")
        void order() {
            assertEquals(List.of(
                    NeedsOperatorKind.DECISION,
                    NeedsOperatorKind.WAIVER,
                    NeedsOperatorKind.WAITS_FOR_YOU,
                    NeedsOperatorKind.STALLED,
                    NeedsOperatorKind.CONDITION,
                    NeedsOperatorKind.INTEGRITY_PAUSE,
                    NeedsOperatorKind.HELD,
                    NeedsOperatorKind.LANDING_FAILED,
                    NeedsOperatorKind.FINDINGS,
                    NeedsOperatorKind.SETUP,
                    NeedsOperatorKind.CREDENTIAL,
                    NeedsOperatorKind.CLEANUP_DUE), List.of(NeedsOperatorKind.values()));
        }

        @Test
        @DisplayName("parses both new kinds from their wire strings")
        void wireValues() {
            assertEquals(NeedsOperatorKind.CREDENTIAL, NeedsOperatorKind.parse("credential"));
            assertEquals(NeedsOperatorKind.CLEANUP_DUE, NeedsOperatorKind.parse("cleanup_due"));
        }

        @Test
        @DisplayName("accepts the specification's spellings of the two waits after a decision")
        void specificationSpellings() {
            assertEquals(NeedsOperatorKind.WAIVER, NeedsOperatorKind.parse("waiver_request"));
            assertEquals(NeedsOperatorKind.WAITS_FOR_YOU, NeedsOperatorKind.parse("operator_wait"));
        }

        @Test
        @DisplayName("has a word in both languages for every kind")
        void catalogueWords() {
            for (var kind : NeedsOperatorKind.values()) {
                var key = "overview.needs." + kind.value();
                assertTrue(MessageCatalogue.hasKey("en", key), "Missing English key " + key);
                assertTrue(MessageCatalogue.hasKey("de", key), "Missing German key " + key);
            }
            assertEquals("credential", MessageCatalogue.format("en", "overview.needs.credential", Map.of()));
            assertEquals("Zugangsdaten", MessageCatalogue.format("de", "overview.needs.credential", Map.of()));
            assertEquals("cleanup due", MessageCatalogue.format("en", "overview.needs.cleanup_due", Map.of()));
            assertEquals("Aufräumen fällig", MessageCatalogue.format("de", "overview.needs.cleanup_due", Map.of()));
        }
    }

    @Nested
    @DisplayName("The list of what waits")
    class TheList {

        @Test
        @DisplayName("round-trips an item with its kind, link, place, since, and what it blocks")
        void itemRoundTrip() throws Exception {
            var item = new NeedsOperatorItem(NeedsOperatorKind.DECISION, "/api/v1/plans/fix-relay",
                    CLIENTS, HARDENING, FIX_RELAY, Instant.parse("2026-10-16T15:05:00Z"), "plan");

            var again = NeedsOperatorItem.fromJson(item.toJson());

            assertEquals(item, again);
            var map = item.toMap();
            assertEquals("decision", map.get("kind"));
            assertEquals("plan", map.get("blocks"));
            assertEquals("relay-hardening", JsonTree.string(JsonTree.asObject(map.get("epic")), "id"));
        }

        @Test
        @DisplayName("leaves the place and the blocked work out where the item has none")
        void itemWithoutPlace() throws Exception {
            var item = new NeedsOperatorItem(NeedsOperatorKind.CREDENTIAL, "/api/v1/credentials/github",
                    null, null, null, null, null);

            var map = item.toMap();
            assertFalse(map.containsKey("project"));
            assertFalse(map.containsKey("epic"));
            assertFalse(map.containsKey("plan"));
            assertFalse(map.containsKey("since"));
            assertFalse(map.containsKey("blocks"));
            assertEquals(item, NeedsOperatorItem.fromJson(item.toJson()));
        }

        @Test
        @DisplayName("round-trips a page of items")
        void pageRoundTrip() throws Exception {
            var item = new NeedsOperatorItem(NeedsOperatorKind.CONDITION, "/api/v1/plans/relay-metrics",
                    CLIENTS, HARDENING, new WorkLocation("relay-metrics", "Relay metrics"), null, "plan");
            var page = new NeedsOperatorPage(1, 2, false, true, 3, List.of(item));

            var again = NeedsOperatorPage.fromJson(page.toJson());

            assertEquals(page, again);
            assertEquals(1, JsonTree.integer(JsonTree.asObject(JsonTree.parse(page.toJson())), "page"));
            assertEquals(3, again.total(), "the total counts every page, not the rows served");
        }

        @Test
        @DisplayName("serves an empty page when nothing waits")
        void emptyPage() throws Exception {
            var page = new NeedsOperatorPage(1, 1, false, false, 0, List.of());

            assertEquals(page, NeedsOperatorPage.fromJson(page.toJson()));
            assertTrue(page.items().isEmpty());
        }
    }

    @Nested
    @DisplayName("Finding work by name")
    class Finding {

        @Test
        @DisplayName("round-trips a match of running work")
        void runningMatch() throws Exception {
            var match = new FindMatch(FindKind.PLAN, "fix-relay", "Fix Relay Transport",
                    CLIENTS, HARDENING, FIX_RELAY, false, null);

            var again = FindMatch.fromJson(match.toJson());

            assertEquals(match, again);
            assertFalse(again.ended());
            assertNull(again.outcome());
        }

        @Test
        @DisplayName("marks ended work with its outcome")
        void endedMatch() throws Exception {
            var match = new FindMatch(FindKind.PLAN, "fix-relay-v1", "fix-relay-v1",
                    CLIENTS, HARDENING, null, true, "merged");

            var map = match.toMap();
            assertEquals(Boolean.TRUE, map.get("ended"));
            assertEquals("merged", map.get("outcome"));
            assertTrue(FindMatch.fromJson(match.toJson()).ended());
        }

        @Test
        @DisplayName("round-trips a page of matches with the text that was searched")
        void pageRoundTrip() throws Exception {
            var page = new FindPage(1, 1, false, false, "relay",
                    List.of(new FindMatch(FindKind.EPIC, "relay-hardening", "Relay Hardening",
                            CLIENTS, HARDENING, null, false, null)));

            var again = FindPage.fromJson(page.toJson());

            assertEquals(page, again);
            assertEquals("relay", again.query());
        }

        @Test
        @DisplayName("names the five kinds of a match")
        void kinds() {
            assertEquals(List.of(FindKind.PROJECT, FindKind.EPIC, FindKind.PLAN, FindKind.DECISION, FindKind.REPORT),
                    List.of(FindKind.values()));
            for (var kind : FindKind.values()) {
                assertEquals(kind, FindKind.fromValue(kind.value()));
            }
        }
    }

    @Nested
    @DisplayName("The machine")
    class TheMachine {

        @Test
        @DisplayName("carries a summary with its count and its items")
        void summary() throws Exception {
            var summary = new NeedsOperatorSummary(1, List.of(
                    new NeedsOperatorItemRef(NeedsOperatorKind.CREDENTIAL, "/api/v1/credentials/github")));

            var again = NeedsOperatorSummary.fromJson(summary.toJson());

            assertEquals(summary, again);
            assertEquals(1, again.count());
            assertEquals(NeedsOperatorKind.CREDENTIAL, again.items().getFirst().kind());
        }

        @Test
        @DisplayName("announces a change of the machine's summary with the event")
        void event() throws Exception {
            var summary = new NeedsOperatorSummary(1, List.of(
                    new NeedsOperatorItemRef(NeedsOperatorKind.CLEANUP_DUE, "/api/v1/system/cleanup")));
            var event = new NeedsOperatorChangedEvent(null, null, null, "cleanup_due", 1, summary);

            var again = NeedsOperatorChangedEvent.fromJson(event.toJson());

            assertEquals(event, again);
            assertEquals(NeedsOperatorKind.CLEANUP_DUE, again.summary().items().getFirst().kind());
        }

        @Test
        @DisplayName("names the paths of its summary, its list, and find")
        void paths() {
            assertEquals("/api/v1/machine", MachineApiPaths.MACHINE);
            assertEquals("/api/v1/machine/needs-operator", MachineApiPaths.NEEDS_OPERATOR);
            assertEquals("/api/v1/find", MachineApiPaths.FIND);
            assertEquals("q", MachineApiPaths.QUERY_PARAM);
        }
    }
}
