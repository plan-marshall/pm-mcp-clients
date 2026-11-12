/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.chat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import de.planmarshall.api.message.MessageCatalogue;

@DisplayName("Chat message keys: each runtime key maps to a rendering key of the control packs")
@EnableGeneratorController
class ChatMessageKeysTest {

    private static final String LINE_PREFIX = "control.chat.line.";
    private static final String NO_DEVICE = "control.chat.line.decision_answered_no_device";
    private static final String RENDERING_PREFIX = "control.chat.";
    private static final String STEP_PREFIX = "control.chat.step.";
    private static final String KNOWS_ITEM = "control.chat.knows.item";
    private static final String REVISION_QUEUED = "control.chat.revision_queued";
    /** The branch of a select that takes every value it does not name. */
    private static final String OTHER = "other";

    /** The runtime keys the goldens, the test values of the chat package and the mock runtime send on the wire. */
    static Stream<String> wireKeys() {
        return Stream.of("chat.knows.decision", "chat.knows.phase", "chat.knows.description", "chat.knows.other",
                "chat.step.read", "chat.step.check",
                "chat.reason.lost", "chat.reason.too_long", "chat.reason.infeasible", "chat.line.task_ended",
                "chat.line.phase_changed", "chat.line.decision_answered", "chat.finding.check_failed",
                "chat.finding.changed_since", "chat.finding.lapsed", "chat.finding.already_queued",
                "chat.applies.after_task", "chat.applies.on_start", "chat.step.search");
    }

    /** Every narrative line key of the control pack, but the variant without a device. */
    static Stream<String> packLines() {
        return MessageCatalogue.getMessages("en", "control").keySet().stream()
                .filter(key -> key.startsWith(LINE_PREFIX) && !NO_DEVICE.equals(key)).sorted();
    }

    /** Every item the select of {@code control.chat.knows.item} names, but its other branch, the archive. */
    static Stream<String> knowsItems() {
        return selectBranches(controlPack().get(KNOWS_ITEM), ChatMessageKeys.ARG_ITEM).stream()
                .filter(item -> !OTHER.equals(item));
    }

    private static Map<String, String> controlPack() {
        return MessageCatalogue.getMessages("en", "control");
    }

    private static String name() {
        return Generators.letterStrings(3, 12).next().toLowerCase(Locale.ROOT);
    }

    private static boolean inBothPacks(String key) {
        return MessageCatalogue.getMessages("en", "control").containsKey(key)
                && MessageCatalogue.getMessages("de", "control").containsKey(key);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("wireKeys")
    @DisplayName("a runtime key the goldens use renders through a key of the en and de control packs")
    void wireKeyHasRenderingKey(String key) {
        var rendering = ChatMessageKeys.render(MessageText.of(key));

        assertAll(key,
                () -> assertTrue(ChatMessageKeys.maps(key)),
                () -> assertTrue(rendering.key().startsWith("control.chat."), rendering.key()),
                () -> assertTrue(inBothPacks(rendering.key()), rendering.key()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("packLines")
    @DisplayName("every narrative line of the pack has its runtime key chat.line.<name>")
    void everyPackLineIsMapped(String renderingKey) {
        var runtimeKey = "chat.line." + renderingKey.substring(LINE_PREFIX.length());
        var args = Map.<String, Object>of(ChatMessageKeys.ARG_DEVICE, name());

        assertAll(renderingKey,
                () -> assertTrue(ChatMessageKeys.maps(runtimeKey), runtimeKey),
                () -> assertEquals(renderingKey, ChatMessageKeys.render(new MessageText(runtimeKey, args)).key()));
    }

    @Test
    @DisplayName("decision_answered without a device renders as the variant without one, with one as itself")
    void decisionAnsweredDevice() {
        var id = name();
        var device = name();
        var withDevice = new MessageText("chat.line.decision_answered",
                Map.of("id", id, "channel", "web", ChatMessageKeys.ARG_DEVICE, device));
        var withoutDevice = new MessageText("chat.line.decision_answered", Map.of("id", id, "channel", "web"));

        var renderedWith = ChatMessageKeys.render(withDevice);
        var renderedWithout = ChatMessageKeys.render(withoutDevice);

        assertAll(
                () -> assertEquals("control.chat.line.decision_answered", renderedWith.key()),
                () -> assertEquals(withDevice.args(), renderedWith.args()),
                () -> assertEquals(NO_DEVICE, renderedWithout.key()),
                () -> assertEquals(id + " answered in the web app",
                        MessageCatalogue.format("en", renderedWithout.key(), renderedWithout.args())),
                () -> assertEquals(id + " answered in the web app (" + device + ")",
                        MessageCatalogue.format("en", renderedWith.key(), renderedWith.args())));
    }

    @Test
    @DisplayName("an entry of what the role knows renders as control.chat.knows.item with its item")
    void knowsItem() {
        var count = String.valueOf(Generators.integers(2, 99).next());
        var rendered = ChatMessageKeys.render(new MessageText("chat.knows.tasks", Map.of("count", count)));

        assertAll(
                () -> assertEquals("control.chat.knows.item", rendered.key()),
                () -> assertEquals(Map.of("count", count, ChatMessageKeys.ARG_ITEM, "tasks"), rendered.args()),
                () -> assertEquals(count + " tasks", MessageCatalogue.format("en", rendered.key(), rendered.args())),
                () -> assertEquals(count + " Tasks", MessageCatalogue.format("de", rendered.key(), rendered.args())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("knowsItems")
    @DisplayName("every item the select of control.chat.knows.item names is mapped and never renders as an archive")
    void everyKnowsItemIsMapped(String item) {
        var key = "chat.knows." + item;
        var value = name();
        var args = Map.<String, Object>of("count", value, "phase", value, "version", value, "id", value);
        var rendered = ChatMessageKeys.render(new MessageText(key, args));

        assertAll(key,
                () -> assertTrue(ChatMessageKeys.maps(key)),
                () -> assertEquals("control.chat.knows.item", rendered.key()),
                () -> assertEquals(item, rendered.args().get(ChatMessageKeys.ARG_ITEM)),
                () -> assertFalse(MessageCatalogue.format("en", rendered.key(), rendered.args())
                        .startsWith("its archived record")));
    }

    @Test
    @DisplayName("the archived record of ended work is the select's other branch with its outcome")
    void knowsArchivedRecord() {
        var outcome = name();
        var rendered = ChatMessageKeys.render(new MessageText("chat.knows.other", Map.of("outcome", outcome)));

        assertAll(
                () -> assertEquals("control.chat.knows.item", rendered.key()),
                () -> assertEquals("its archived record, " + outcome,
                        MessageCatalogue.format("en", rendered.key(), rendered.args())),
                () -> assertEquals("the description",
                        MessageCatalogue.format("en", "control.chat.knows.item",
                                Map.of(ChatMessageKeys.ARG_ITEM, "description"))));
    }

    @Test
    @DisplayName("a knows item the select does not name, a typo among them, is refused")
    void unknownKnowsItemIsRefused() {
        var key = "chat.knows.zz_" + name();
        var text = MessageText.of(key);

        assertAll(
                () -> assertFalse(ChatMessageKeys.maps(key)),
                () -> assertThrows(IllegalArgumentException.class, () -> ChatMessageKeys.render(text)));
    }

    @Test
    @DisplayName("the point a revision waits for renders as control.chat.revision_queued with applies")
    void appliesPoint() {
        var taskId = "TASK-" + Generators.integers(1, 99).next();
        var afterTask = ChatMessageKeys.render(new MessageText("chat.applies.after_task", Map.of("task_id", taskId)));
        var onStart = ChatMessageKeys.render(MessageText.of("chat.applies.on_start"));

        assertAll(
                () -> assertEquals("control.chat.revision_queued", afterTask.key()),
                () -> assertEquals(Map.of("task_id", taskId, ChatMessageKeys.ARG_APPLIES, "after_task"),
                        afterTask.args()),
                () -> assertEquals("Revision from the chat queued · applies after " + taskId,
                        MessageCatalogue.format("en", afterTask.key(), afterTask.args())),
                () -> assertEquals("control.chat.revision_queued", onStart.key()),
                () -> assertEquals(Map.of(ChatMessageKeys.ARG_APPLIES, "on_start"), onStart.args()));
    }

    @Test
    @DisplayName("a step, a reason and a finding render through the keys the design names")
    void stepReasonFinding() {
        assertAll(
                () -> assertEquals("control.chat.step.read",
                        ChatMessageKeys.render(MessageText.of("chat.step.read")).key()),
                () -> assertEquals("control.chat.lost", ChatMessageKeys.render(MessageText.of("chat.reason.lost")).key()),
                () -> assertEquals("control.chat.too_long_reply",
                        ChatMessageKeys.render(MessageText.of("chat.reason.too_long")).key()),
                () -> assertEquals("control.chat.infeasible",
                        ChatMessageKeys.render(MessageText.of("chat.reason.infeasible")).key()),
                () -> assertEquals("control.chat.already_queued",
                        ChatMessageKeys.render(MessageText.of("chat.finding.already_queued")).key()));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"control.chat.line.task_ended", "chat.knows", "chat.knows.plan", "chat.knows.archive",
            "chat.line.task_ended.extra",
            "chat.step.reading", "chat.reason.too_long_reply", "chat.finding.refused", "chat.applies.never",
            "chat.proposal.refused", "plan.chat.line"})
    @DisplayName("a key outside the mapped families or without a rendering key is refused")
    void unmappedKeyIsRefused(String key) {
        var text = MessageText.of(key);

        assertAll(key,
                () -> assertFalse(ChatMessageKeys.maps(key)),
                () -> assertThrows(IllegalArgumentException.class, () -> ChatMessageKeys.render(text)));
    }

    @Test
    @DisplayName("a line name the pack does not hold is refused")
    void unknownLineIsRefused() {
        var key = "chat.line.zz_" + name();
        var text = MessageText.of(key);

        assertAll(
                () -> assertFalse(ChatMessageKeys.maps(key)),
                () -> assertThrows(IllegalArgumentException.class, () -> ChatMessageKeys.render(text)));
    }

    @Nested
    @DisplayName("Each family of names agrees with the en control pack")
    class CatalogueAgreement {

        @Test
        @DisplayName("the lines are exactly the narrative lines of the pack")
        void lines() {
            var pack = packLines().map(key -> key.substring(LINE_PREFIX.length())).collect(Collectors.toSet());

            assertEquals(ChatMessageKeys.LINES, pack);
        }

        @Test
        @DisplayName("the steps are exactly the steps of the pack")
        void steps() {
            var pack = controlPack().keySet().stream().filter(key -> key.startsWith(STEP_PREFIX))
                    .map(key -> key.substring(STEP_PREFIX.length())).collect(Collectors.toSet());

            assertEquals(ChatMessageKeys.STEPS, pack);
        }

        @Test
        @DisplayName("the knows items are exactly the branches of the select of control.chat.knows.item")
        void knowsItemsAreTheSelectBranches() {
            var branches = selectBranches(controlPack().get(KNOWS_ITEM), ChatMessageKeys.ARG_ITEM);

            assertEquals(ChatMessageKeys.KNOWS_ITEMS, Set.copyOf(branches));
        }

        @Test
        @DisplayName("every named point of control.chat.revision_queued is mapped, and exactly one point takes"
                + " its other branch")
        void appliesPoints() {
            var branches = selectBranches(controlPack().get(REVISION_QUEUED), ChatMessageKeys.ARG_APPLIES);
            var named = branches.stream().filter(branch -> !OTHER.equals(branch)).collect(Collectors.toSet());
            var byOther = new HashSet<>(ChatMessageKeys.APPLIES_POINTS);
            byOther.removeAll(named);

            assertAll(REVISION_QUEUED,
                    () -> assertTrue(branches.contains(OTHER), "the select has its other branch"),
                    () -> assertTrue(ChatMessageKeys.APPLIES_POINTS.containsAll(named), named.toString()),
                    () -> assertEquals(1, byOther.size(), "one point renders through the other branch: " + byOther));
        }

        @Test
        @DisplayName("every reason renders through a key of the en and de control packs")
        void reasons() {
            assertAll("reasons", ChatMessageKeys.REASONS.values().stream()
                    .map(key -> (Executable) () -> assertTrue(inBothPacks(key), key)));
        }

        @Test
        @DisplayName("every finding renders through control.chat.<name> of the en and de control packs")
        void findings() {
            assertAll("findings", ChatMessageKeys.FINDINGS.stream()
                    .map(name -> (Executable) () -> assertTrue(inBothPacks(RENDERING_PREFIX + name), name)));
        }
    }

    /**
     * The top-level branch names of the select of an argument in a message template: nested braces, a
     * branch text such as {@code {{count} tasks}} among them, are skipped.
     */
    private static List<String> selectBranches(String template, String argument) {
        var head = "{" + argument + ", select, ";
        var start = template.indexOf(head);
        assertTrue(start >= 0, "a select of " + argument + " in " + template);
        var branches = new ArrayList<String>();
        var at = start + head.length();
        while (at < template.length() && template.charAt(at) != '}') {
            if (Character.isWhitespace(template.charAt(at))) {
                at++;
                continue;
            }
            var open = template.indexOf('{', at);
            branches.add(template.substring(at, open).strip());
            var depth = 0;
            do {
                var c = template.charAt(open);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
                open++;
            } while (depth > 0);
            at = open;
        }
        return branches;
    }
}
