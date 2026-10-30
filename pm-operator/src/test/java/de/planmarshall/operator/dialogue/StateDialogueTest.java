/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.dialogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.planmarshall.operator.StartPrompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The one state dialogue ({@code CLI-UC-S-06}): a fake runtime reports three states, and the
 * dialogue renders each frame, reads the choice, sends the link and stops at the final state.
 */
@DisplayName("StateDialogue")
class StateDialogueTest {

    private static final String MODE = "onboard.mode";
    private static final String LANGUAGE = "language.choose";
    private static final String COMPLETED = "onboard.completed";

    /**
     * A runtime that replays states and records the links it was sent.
     */
    private static final class FakeSource implements StateDialogue.Source {

        private final List<StateDialogue.Frame> frames;
        private final List<String> sent = new ArrayList<>();
        private int index;

        FakeSource(StateDialogue.Frame... frames) {
            this.frames = List.of(frames);
        }

        @Override
        public StateDialogue.Frame fetch() {
            return frames.get(Math.min(index, frames.size() - 1));
        }

        @Override
        public StateDialogue.Frame choose(String linkId) {
            sent.add(linkId);
            index++;
            return fetch();
        }
    }

    private static StateDialogue.Frame mode() {
        return new StateDialogue.Frame(MODE, "operator_wait", 0, 7,
                            "Set up this machine for plan-marshall", Map.of(),
                            List.of(new StateDialogue.Link("auto", "alt", false),
                                                new StateDialogue.Link("begin", "next", true)));
    }

    private static StateDialogue.Frame language() {
        return new StateDialogue.Frame(LANGUAGE, "operator_wait", 1, 7, "Choose your languages",
                            Map.of("interface", "de", "model", "same"),
                            List.of(new StateDialogue.Link("set", "submit", true)));
    }

    private static StateDialogue.Frame completed() {
        return new StateDialogue.Frame(COMPLETED, "completed", 7, 7, "This machine is ready",
                            Map.of(), List.of());
    }

    private static StateDialogue dialogue(StateDialogue.Source source, String input,
                        StringWriter writer) {
        var out = new PrintWriter(writer, true);
        var reader = new BufferedReader(new StringReader(input));
        return new StateDialogue(source, out, "en",
                            stateId -> new ChoicePrompt(reader, out, "en", stateId));
    }

    @Test
    @DisplayName("drives three states, renders each frame and sends the chosen links")
    void drivesThreeStates() throws Exception {
        var source = new FakeSource(mode(), language(), completed());
        var writer = new StringWriter();

        var last = dialogue(source, "\n\n", writer).run();

        var output = writer.toString();
        assertEquals(COMPLETED, last.stateId());
        assertEquals(List.of("begin", "set"), source.sent);
        assertTrue(output.contains("Set up this machine for plan-marshall"), output);
        assertTrue(output.contains("1) Decide everything for me\u2026"), output);
        assertTrue(output.contains("2) Begin with step 1"), output);
        assertTrue(output.contains("[1/7] Language \u00b7 Choose your languages"), output);
        assertTrue(output.contains("Interface language: de"), output);
        assertTrue(output.contains("Model language: same"), output);
        assertTrue(output.contains("This machine is ready"), output);
        assertFalse(output.contains(MODE), "a raw state id reached the screen: " + output);
        assertFalse(output.contains(LANGUAGE), "a raw state id reached the screen: " + output);
    }

    @Test
    @DisplayName("q leaves the dialogue without sending anything")
    void leavingSendsNothing() {
        var source = new FakeSource(mode(), language(), completed());
        var writer = new StringWriter();

        var dialogue = dialogue(source, "q\n", writer);

        assertThrows(StartPrompt.StartLeftException.class, dialogue::run);
        assertTrue(source.sent.isEmpty(), source.sent.toString());
        assertTrue(writer.toString().contains("Nothing was sent."), writer.toString());
    }

    @Test
    @DisplayName("balances only the facts whose field the catalogue labels")
    void rendersOnlyLabelledFacts() {
        var frame = new StateDialogue.Frame("harness.consent", "operator_wait", 2, 7, null,
                            Map.of("looks_for", "claude, opencode", "unlabelled_field", "raw"),
                            List.of());
        var writer = new StringWriter();
        var dialogue = dialogue(new FakeSource(frame), "", writer);

        var content = dialogue.content(frame);

        assertEquals(List.of("Looks for: claude, opencode"), content);
    }
}
