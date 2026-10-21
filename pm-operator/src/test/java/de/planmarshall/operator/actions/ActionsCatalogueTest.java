/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.control.ActionAvailability;
import de.planmarshall.api.control.HoldRecord;
import de.planmarshall.api.control.WorkActions;
import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.operator.tui.TuiMenuEntry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("actions catalogue in both languages")
class ActionsCatalogueTest {

    private static final List<String> PREFIXES = List.of("operator.actions.", "operator.epic.",
                        "overview.epic.");

    @Test
    @DisplayName("every action and epic-view text exists in English and German")
    void textsExist() {
        var en = MessageCatalogue.getMessages("en", "all");
        var de = MessageCatalogue.getMessages("de", "all");
        for (var key : en.keySet()) {
            if (PREFIXES.stream().noneMatch(key::startsWith)) {
                continue;
            }
            assertTrue(de.containsKey(key), "missing in German: " + key);
            assertNotEquals(key, MessageCatalogue.format("en", key, params()), key);
            assertNotEquals(key, MessageCatalogue.format("de", key, params()), key);
        }
    }

    @Test
    @DisplayName("every mnemonic is one character and no mnemonic is a")
    void mnemonics() {
        for (var locale : List.of("en", "de")) {
            var messages = MessageCatalogue.getMessages(locale, "all");
            for (var key : messages.keySet()) {
                if (!key.startsWith("operator.actions.key.")) {
                    continue;
                }
                var value = messages.get(key);
                assertEquals(1, value.length(), locale + " " + key + " = " + value);
                assertNotEquals("a", value, locale + " " + key);
            }
        }
    }

    @Test
    @DisplayName("within every menu and prompt no mnemonic repeats, in either language")
    void uniqueMnemonics() {
        var holds = List.of(new HoldRecord("H-1", "p", "plan", "condition", "cause", 1,
                                                Instant.parse("2026-10-16T10:00:00Z"), null, null, null, null, null,
                                                List.of("retry", "grant", "raise_budget", "end_plan"), null),
                            new HoldRecord("H-2", "p", "plan", "integrity_pause", "cause", 0, null,
                                                "f", 1L, "a", 2L, "b", List.of("retry", "accept", "abort"),
                                                null),
                            new HoldRecord("H-3", "p", "plan", "stalled", "cause", 0, null,
                                                null, null, null, null, null,
                                                List.of("retry", "recycle", "end_plan"), null));
        for (var locale : List.of("en", "de")) {
            assertUnique(ActionMenus.plan(locale, actions("running", null, "pause", "cancel", "chat",
                                "settings"), null, null), locale);
            assertUnique(ActionMenus.plan(locale, actions("pausing", null, "start", "cancel", "chat",
                                "settings"), null, null), locale);
            assertUnique(ActionMenus.plan(locale, actions("paused", null, "start", "abandon", "chat",
                                "settings"), null, null), locale);
            for (var hold : holds) {
                assertUnique(ActionMenus.plan(locale, actions("held", hold.kind(), "pause", "cancel", "chat",
                                    "settings"), hold, null), locale);
            }
            assertUnique(ActionMenus.epic(locale, actions("running", null, "next", "auto", "common_files",
                                "decompose", "findings", "watches", "chat", "archive", "cancel"),
                                "next", List.of(), true, 0, 1, 2, null), locale);
            assertUnique(ActionMenus.ended(locale, "p", "p", null), locale);
            assertUnique(ActionMenus.landing(locale, "l", "landing_failed", null), locale);
        }
    }


    private static Map<String, Object> params() {
        var params = new LinkedHashMap<String, Object>();
        params.put("count", 1);
        params.put("plan", "p");
        params.put("other", "o");
        params.put("name", "n");
        params.put("state", "running");
        params.put("actions", "a");
        params.put("epic", "e");
        params.put("action", "a");
        params.put("register", "findings");
        params.put("item", "i");
        params.put("value", "v");
        params.put("reason", "r");
        params.put("subject", "s");
        params.put("role", "r");
        params.put("scope", "s");
        params.put("label", "l");
        params.put("text", "t");
        params.put("when", "w");
        params.put("version", "1");
        params.put("file", "f");
        params.put("size", 1);
        params.put("digest", "d");
        params.put("kind", "k");
        params.put("time", "t");
        params.put("mark", "\u2713");
        params.put("project", "pr");
        params.put("mode", "automatic");
        params.put("running", 1);
        params.put("id", "WS-1");
        params.put("status", "staged");
        params.put("files", 1);
        params.put("paths", "p");
        params.put("plans", 2);
        params.put("split", "s");
        params.put("figure", "f");
        params.put("outcome", "completed");
        params.put("wait", "w");
        params.put("since", "s");
        params.put("counts", "c");
        params.put("number", 1);
        return params;
    }

    @Test
    @DisplayName("every action the runtime offers is on a mnemonic, or reported as dropped")
    void everyOfferedActionAppears() {
        var holds = List.of(
                            new HoldRecord("H-1", "p", "plan", "stalled", "cause", 1, null, null, null,
                                                null, null, null, List.of("retry", "recycle", "end_plan"), null),
                            new HoldRecord("H-2", "p", "plan", "condition", "cause", 1, null, null, null,
                                                null, null, null,
                                                List.of("retry", "grant", "add_credential", "raise_budget",
                                                                    "end_plan"), null),
                            new HoldRecord("H-3", "p", "plan", "integrity_pause", "cause", 0, null, "f", 1L,
                                                "a", 2L, "b", List.of("retry", "accept", "abort"), null));
        var planActions = List.of("pause", "cancel", "chat", "settings");
        for (var locale : List.of("en", "de")) {
            for (var hold : holds) {
                assertOffered(ActionMenus.plan(locale, actions("held", hold.kind(),
                                    planActions.toArray(String[]::new)), hold, null), planActions, locale);
            }
            for (var holdKind : List.of("stalled", "condition", "integrity_pause", "held",
                                "landing_failed")) {
                var epicActions = List.of("next", "auto", "common_files", "decompose", "findings",
                                    "watches", "chat", "archive", "cancel");
                assertOffered(ActionMenus.epic(locale, actions("held", holdKind,
                                    epicActions.toArray(String[]::new)), "next", List.of(), true, 0, 1, 2,
                                    null), epicActions, locale);
            }
        }
    }

    private static void assertOffered(ActionsMenu menu, List<String> offered, String locale) {
        var present = new HashSet<String>();
        for (var entry : menu.entries()) {
            present.add(entry.action());
        }
        for (var action : offered) {
            if ("cancel".equals(action) || "abandon".equals(action)) {
                // The cancel of a held subject is its hold's end/abort, which the menu shows instead.
                continue;
            }
            assertTrue(present.contains(action) || menu.droppedActions().contains(action),
                                locale + " lost " + action + " in " + menu.entries() + " dropped="
                                                    + menu.droppedActions());
        }
    }

    @Test
    @DisplayName("a plan's landing hold never takes the pause mnemonic")
    void planLandingHoldDoesNotTakePause() {
        var hold = new HoldRecord("H-4", "p", "plan", "landing_failed", "cause", 0, null, null, null,
                            null, null, null, List.of("retry", "show_pr", "give_up_removal", "end_plan"),
                            "https://x/pull/1");
        var menu = ActionMenus.plan("en", actions("held", "landing_failed", "pause", "cancel", "chat",
                            "settings"), hold, null);
        assertTrue(menu.entries().stream().anyMatch(entry -> "pause".equals(entry.action())));
        assertTrue(menu.entries().stream().noneMatch(entry -> "show_pr".equals(entry.action())));
        assertTrue(menu.entries().stream().noneMatch(entry -> "give_up_removal".equals(entry.action())));
    }

    private static WorkActions actions(String state, String holdKind, String... offered) {
        var list = new ArrayList<ActionAvailability>();
        for (var action : offered) {
            list.add(ActionAvailability.enabled(action));
        }
        return new WorkActions("p", "plan", state, null, holdKind, null, list);
    }

    private static void assertUnique(ActionsMenu menu, String locale) {
        var triggers = new HashSet<String>();
        for (TuiMenuEntry entry : menu.entries()) {
            assertTrue(triggers.add(entry.trigger()), locale + " repeats " + entry.trigger()
                                + " in " + menu.entries());
            assertNotEquals("a", entry.trigger(), locale + " uses a");
        }
    }
}
