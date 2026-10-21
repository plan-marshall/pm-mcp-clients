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

    private static final List<String> ACTION_KEYS = List.of(
                        "operator.actions.keys.actions", "operator.actions.refused", "operator.actions.ended",
                        "operator.actions.menu.footer", "operator.actions.disabled_suffix",
                        "operator.actions.chat_end", "operator.actions.chat.you",
                        "operator.actions.chat.this_item", "operator.actions.chat.accept",
                        "operator.actions.chat.reject", "operator.actions.chat.accepted",
                        "operator.actions.chat.follow_up", "operator.actions.chat.no_input",
                        "operator.actions.chat.head", "operator.actions.chat.scope.plan",
                        "operator.actions.chat.scope.epic", "operator.actions.chat.scope.archive",
                        "operator.actions.chat.answers", "operator.actions.chat.kept",
                        "operator.actions.chat.applied", "operator.actions.chat.no_proposal",
                        "operator.actions.chat.read_only", "operator.actions.chat.proposed",
                        "operator.actions.chat.proposed_follow_up",
                        "operator.actions.settings.which", "operator.actions.settings.aspect.plan-defaults",
                        "operator.actions.settings.aspect.roles", "operator.actions.settings.aspect.integrations",
                        "operator.actions.settings.aspect.providers", "operator.actions.settings.header",
                        "operator.actions.settings.value", "operator.actions.settings.applied",
                        "operator.actions.settings.removed", "operator.actions.settings.use_inherited",
                        "operator.actions.settings.no_input", "operator.actions.settings.stopped",
                        "operator.actions.pr_open", "operator.actions.cancel.confirm",
                        "operator.actions.keep", "operator.actions.result.ended",
                        "operator.actions.result.pr_closed", "operator.actions.result.epic_told",
                        "operator.actions.result.pausing", "operator.actions.result.paused",
                        "operator.actions.result.started", "operator.actions.result.next_started",
                        "operator.actions.result.not_archivable", "operator.actions.result.archived",
                        "operator.actions.archive_landing", "operator.actions.epic.needs_you",
                        "operator.actions.epic.waits_for", "operator.actions.epic.waits_for_lock",
                        "operator.actions.epic.waits_for_scope", "operator.actions.epic.analysis",
                        "operator.actions.epic.events", "operator.actions.epic.more_events",
                        "operator.actions.hold.confirm_accept", "operator.actions.hold.goes_on",
                        "operator.actions.review.keep_as", "operator.actions.review.kept",
                        "operator.actions.review.moved", "operator.actions.review.dropped",
                        "operator.actions.review.promoted", "operator.actions.review.remaining",
                        "operator.actions.review.finish", "operator.actions.review.finished",
                        "operator.actions.review.untrusted", "operator.actions.review.field",
                        "operator.actions.review.draft", "operator.actions.review.no_draft",
                        "operator.actions.review.stopped", "operator.actions.review.progress",
                        "operator.actions.review.none", "operator.actions.review.item",
                        "operator.actions.review.keep_finding", "operator.actions.review.keep_watch",
                        "operator.actions.review.reason",
                        "operator.actions.pause.title", "operator.actions.pause.now",
                        "operator.actions.pause.task", "operator.actions.pause.phase",
                        "operator.actions.pause.ask", "operator.actions.end.title",
                        "operator.actions.end.what_ends", "operator.actions.end.pull_request",
                        "operator.actions.end.its_epic", "operator.actions.end.told",
                        "operator.actions.end.afterwards", "operator.actions.end.afterwards_text",
                        "operator.actions.end.reason", "operator.actions.end.confirm",
                        "operator.actions.end.close_pr", "operator.actions.end.reason_required",
                        "operator.actions.epic.cancel.title", "operator.actions.epic.cancel.plans",
                        "operator.actions.epic.cancel.with_plans", "operator.actions.epic.cancel.afterwards",
                        "operator.actions.epic.cancel.afterwards_text", "operator.actions.epic.cancel.confirm",
                        "operator.actions.epic.cancel.done", "operator.actions.archive.title",
                        "operator.actions.archive.check", "operator.actions.archive.carried",
                        "operator.actions.hold.title", "operator.actions.hold.cause",
                        "operator.actions.hold.tried", "operator.actions.hold.recorded",
                        "operator.actions.hold.current", "operator.actions.hold.prompt",
                        "operator.actions.hold.none", "operator.actions.hold.confirm",
                        "operator.actions.hold.accept_prompt", "operator.actions.hold.act.grant",
                        "operator.actions.hold.act.add_credential", "operator.actions.hold.act.raise_budget",
                        "operator.actions.hold.act.show_pr", "operator.hold.refused",
                        "operator.actions.stopped", "operator.actions.result.auto_on",
                        "operator.actions.common_files.ask", "operator.actions.common_files.invalid",
                        "operator.actions.chat.follow_up_started",
                        "operator.actions.pause.stopped", "operator.actions.result.done",
                        "operator.actions.yes_no", "operator.actions.yes_no_q",
                        "operator.actions.accept_reject", "operator.actions.plan_option_exclusive",
                        "operator.actions.dropped");

    private static final List<String> MNEMONIC_KEYS = List.of(
                        "pause", "start", "cancel", "abandon", "settings", "keep_current", "next", "auto",
                        "common_files", "decompose_again", "findings_review", "watches_review", "archive",
                        "raise_and_start", "leave_scope", "retry", "recycle", "grant", "add_credential",
                        "raise_budget", "accept_change", "show_pr", "give_up_removal", "end_plan",
                        "abort_plan", "cancel_epic", "keep", "drop", "promote", "chat", "reject",
                        "accept_remaining", "finish");

    private static final List<String> EPIC_VIEW_KEYS = List.of(
                        "operator.epic.header.path", "operator.epic.header.line", "operator.epic.header.needs",
                        "operator.epic.keys.open", "operator.epic.keys.close", "operator.epic.keys.actions",
                        "operator.epic.keys.project", "operator.epic.workstream", "operator.epic.plan.wait",
                        "operator.epic.sources", "operator.epic.landings", "operator.epic.landing",
                        "operator.epic.landing.analysis", "operator.epic.findings", "operator.epic.watches",
                        "operator.epic.tokens", "operator.epic.tokens_line", "operator.epic.tokens_plan",
                        "operator.epic.tokens_own", "operator.epic.events", "operator.epic.event",
                        "operator.epic.event_more", "operator.epic.needs", "operator.epic.ended",
                        "operator.epic.ended_plain", "operator.epic.empty", "operator.epic.not_measured",
                        "overview.epic.state", "overview.epic.at_once", "overview.epic.status",
                        "overview.epic.mode", "overview.epic.waits.dependency", "overview.epic.waits.lock",
                        "overview.epic.waits.scope", "overview.epic.waits.overlap",
                        "overview.epic.waits.overlap_files", "overview.epic.waits.none",
                        "overview.epic.counts.staged", "overview.epic.counts.running",
                        "overview.epic.counts.pausing", "overview.epic.counts.paused",
                        "overview.epic.counts.waiting", "overview.epic.counts.shipped",
                        "overview.epic.counts.cancelled", "overview.epic.counts.abandoned",
                        "overview.epic.counts.held");

    @Test
    @DisplayName("every text of the actions exists in English and German")
    void textsExist() {
        for (var key : ACTION_KEYS) {
            assertNotEquals(key, MessageCatalogue.format("en", key, params()), key);
            assertNotEquals(key, MessageCatalogue.format("de", key, params()), key);
        }
    }

    @Test
    @DisplayName("every text of the epic view exists in English and German")
    void epicViewTextsExist() {
        for (var key : EPIC_VIEW_KEYS) {
            assertNotEquals(key, MessageCatalogue.format("en", key, params()), key);
            assertNotEquals(key, MessageCatalogue.format("de", key, params()), key);
        }
    }

    @Test
    @DisplayName("every mnemonic is one character and no mnemonic is a")
    void mnemonics() {
        for (var locale : List.of("en", "de")) {
            for (var name : MNEMONIC_KEYS) {
                var key = MessageCatalogue.format(locale, "operator.actions.key." + name, Map.of());
                assertEquals(1, key.length(), locale + " " + name + " = " + key);
                assertNotEquals("a", key, locale + " " + name);
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
