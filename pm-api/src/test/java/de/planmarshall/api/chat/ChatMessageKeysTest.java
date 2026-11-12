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

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    /** The runtime keys the goldens and test values of the chat package send on the wire. */
    static Stream<String> wireKeys() {
        return Stream.of("chat.knows.decision", "chat.knows.phase", "chat.step.read", "chat.step.check",
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
    @ValueSource(strings = {"control.chat.line.task_ended", "chat.knows", "chat.line.task_ended.extra",
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

        assertAll(
                () -> assertFalse(ChatMessageKeys.maps(key)),
                () -> assertThrows(IllegalArgumentException.class, () -> ChatMessageKeys.render(MessageText.of(key))));
    }
}
