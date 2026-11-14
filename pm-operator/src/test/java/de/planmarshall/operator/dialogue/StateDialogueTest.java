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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.onboarding.OnboardingAspect;
import de.planmarshall.operator.StartPrompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The one state dialogue ({@code CLI-UC-S-06}): a fake runtime reports three states, and the
 * dialogue renders each frame, reads the choice, sends the link and stops at the final state.
 */
@EnableGeneratorController
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
        assertTrue(output.contains("Interface language: Deutsch"), output);
        assertTrue(output.contains("Model language: same as above"), output);
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
    @DisplayName("renders only the facts whose field the catalogue labels, and a list's items")
    void rendersOnlyLabelledFacts() {
        var item = new LinkedHashMap<String, Object>();
        item.put("name", "Claude Code");
        item.put("version", "2.4.1");
        item.put("default", true);
        var facts = new LinkedHashMap<String, Object>();
        facts.put("looks_for", "claude, opencode");
        facts.put("unlabelled_field", "raw");
        facts.put("harnesses", List.of(item));
        var frame = new StateDialogue.Frame("harness.consent", "operator_wait", 2, 7, null,
                            facts, List.of());
        var writer = new StringWriter();
        var dialogue = dialogue(new FakeSource(frame), "", writer);

        var content = dialogue.content(frame);

        assertEquals(List.of("Looks for: claude, opencode", "Found harnesses",
                            "  - Claude Code \u00b7 2.4.1 \u00b7 yes"), content);
    }

    @Test
    @DisplayName("leaves a wire token out unless the catalogue names it")
    void hidesAnUnnamedWireToken() {
        var facts = new LinkedHashMap<String, Object>();
        facts.put("landing", "landing.main");
        facts.put("posture", "posture.unknown");
        var frame = new StateDialogue.Frame("defaults.review", "operator_wait", 7, 7, null, facts,
                            List.of());
        var writer = new StringWriter();
        var dialogue = dialogue(new FakeSource(frame), "", writer);

        assertEquals(List.of("Landing: Main branch"), dialogue.content(frame));
        assertEquals("Main branch", StateDialogue.rendered("en", "landing.main"));
        assertEquals("Main-Branch", StateDialogue.rendered("de", "landing.main"));
        assertNull(StateDialogue.rendered("en", "no.such.unit"));
        assertNull(StateDialogue.rendered("de", "no.such.unit"));
    }

    @Test
    @DisplayName("reads an operator envelope: kind, step, summary, facts and the preselection")
    void readsAnOperatorEnvelope() throws Exception {
        var frame = StateDialogue.fromJson("""
                            {"state":"harness.consent","completed_steps":1,
                             "operator":{"state":{"state_id":"harness.consent","kind":"operator_wait"},
                               "facts":{"summary":"Look for harnesses on this machine?","looks_for":"claude"},
                               "links":[{"id":"no","method":"POST"},{"id":"yes","method":"POST","preselected":true}]}}
                            """, 7, false);

        assertAll(
                            () -> assertEquals("harness.consent", frame.stateId()),
                            () -> assertEquals("operator_wait", frame.kind()),
                            () -> assertEquals(2, frame.step()),
                            () -> assertEquals("Look for harnesses on this machine?", frame.summary()),
                            () -> assertEquals("claude", frame.facts().get("looks_for")),
                            () -> assertEquals("yes", frame.primary().id()),
                            () -> assertEquals(2, frame.links().size()));
    }

    @Test
    @DisplayName("reads a flat state and marks its last link as the primary one")
    void readsAFlatState() throws Exception {
        var frame = StateDialogue.fromJson("""
                            {"state":"onboard.mode","summary":"Set up this machine",
                             "links":{"auto":{"href":"/auto"},"begin":{"href":"/language"}}}
                            """, 7, false);

        assertAll(
                            () -> assertEquals(0, frame.step()),
                            () -> assertEquals("begin", frame.primary().id()));
    }

    @Test
    @DisplayName("refuses a body without a state")
    void refusesABodyWithoutState() {
        assertAll(
                            () -> assertThrows(IOException.class,
                                                () -> StateDialogue.fromJson("[]", 7, false)),
                            () -> assertThrows(IOException.class,
                                                () -> StateDialogue.fromJson("{}", 7, false)));
    }

    @Test
    @DisplayName("names step lines, titles and labels from the catalogue")
    void namesTheFrameParts() throws Exception {
        var frame = StateDialogue.fromJson("""
                            {"state":"harness.consent","completed_steps":1,
                             "operator":{"state":{"state_id":"harness.consent","kind":"operator_wait"},
                               "facts":{"summary":"Look for harnesses on this machine?"},
                               "links":[{"id":"yes","method":"POST","preselected":true}]}}
                            """, 7, false);
        var writer = new StringWriter();
        var dialogue = dialogue(new FakeSource(frame), "", writer);

        assertAll(
                            () -> assertEquals("[2/7] Harnesses \u00b7 Look for harnesses on this machine?",
                                                dialogue.stepLine(frame)),
                            () -> assertEquals("Look for harnesses on this machine?", dialogue.title(frame)),
                            () -> assertEquals("Yes", dialogue.label(frame, "yes")),
                            () -> assertEquals("Continue", dialogue.label(frame, "unmapped")),
                            () -> assertEquals("Harnesses", dialogue.stepName(frame)),
                            () -> assertEquals("Checkout", dialogue.stepName(new StateDialogue.Frame(
                                                "project.consent", "operator_wait", 1, 10, null,
                                                Map.of(), List.of()))),
                            () -> assertEquals("Checkout", dialogue.stepName(new StateDialogue.Frame(
                                                "project.checkout", "operator_wait", 1, 10, null,
                                                Map.of(), List.of()))),
                            () -> assertEquals("Done", dialogue.stepName(new StateDialogue.Frame(
                                                "project.unknown", "operator_wait", 1, 10, null,
                                                Map.of(), List.of()))));
    }

    private static final Map<String, String> EXPECTED_PROJECT_STEP_BY_STATE = Map.ofEntries(
                        Map.entry("consent", "checkout"),
                        Map.entry("detect", "checkout"),
                        Map.entry("run-consent", "checkout"),
                        Map.entry("probe", "checkout"),
                        Map.entry("checkout", "checkout"),
                        Map.entry("enrol", "enrolment"),
                        Map.entry("enrolment", "enrolment"),
                        Map.entry("operator", "own"),
                        Map.entry("own", "own"),
                        Map.entry("identity", "identity"),
                        Map.entry("team", "team"),
                        Map.entry("architecture", "architecture"),
                        Map.entry("landing", "landing"),
                        Map.entry("roles", "roles"),
                        Map.entry("descriptions", "descriptions"),
                        Map.entry("commit", "commit"));

    @Test
    @DisplayName("every project setup state maps to a step the catalogue defines")
    void projectStatesNameCatalogueSteps() {
        assertEquals(EXPECTED_PROJECT_STEP_BY_STATE, StateDialogue.PROJECT_STEP_BY_STATE,
                            "the production mapping must match the catalogue contract");
        for (var entry : EXPECTED_PROJECT_STEP_BY_STATE.entrySet()) {
            var state = "project." + entry.getKey();
            var suffix = StateDialogue.projectStepOf(state);
            assertEquals(entry.getValue(), suffix, state + " -> " + suffix);
            assertTrue(MessageCatalogue.hasKey("en", "project.step." + suffix), state + " -> " + suffix);
        }
        assertEquals("done", StateDialogue.projectStepOf("project.completed"));
    }

    @Test
    @DisplayName("a being-added run reporting a different total_steps prints that total")
    void beingAddedRunWithCustomTotalStepsPrintsThatTotal() throws Exception {
        var frame = StateDialogue.fromJson("""
                            {"state":"identity","step":3,"total_steps":12,
                             "operator":{"state":{"state_id":"project.identity","kind":"operator_wait"},
                               "facts":{"summary":"Choose identity"},
                               "links":[{"id":"next","method":"POST","preselected":true}]}}
                            """, 10, true);

        assertEquals(12, frame.totalSteps());
        assertEquals("[3/12] Identity \u00b7 How should the project be named?",
                            dialogue(new FakeSource(frame), "", new StringWriter()).stepLine(frame));
    }

    @ParameterizedTest(name = "total_steps {0} falls back to {1}")
    @MethodSource("totalStepsVariants")
    @DisplayName("validates StateDialogue.fromJson total_steps variants: missing, zero, negative, fractional, over-range, non-numeric, and custom valid")
    void stateDialogueTotalStepsVariants(String totalSteps, int expected) throws Exception {
        var frame = StateDialogue.fromJson(
                            "{\"state\":\"project.identity\",\"step\":1" + totalSteps + "}", 10, true);

        assertEquals(expected, frame.totalSteps(), "total_steps " + totalSteps);
    }

    private static Stream<Arguments> totalStepsVariants() {
        var valid = Generators.integers(1, 50).next();
        return Stream.of(
                            Arguments.of("", 10),
                            Arguments.of(",\"total_steps\":0", 10),
                            Arguments.of(",\"total_steps\":-5", 10),
                            Arguments.of(",\"total_steps\":3.9", 10),
                            Arguments.of(",\"total_steps\":4294967297", 10),
                            Arguments.of(",\"total_steps\":\"invalid\"", 10),
                            Arguments.of(",\"total_steps\":\"" + valid + "\"", 10),
                            Arguments.of(",\"total_steps\":" + valid, valid));
    }

    @Test
    @DisplayName("a project payload without total_steps renders the step line with default total 10")
    void projectPayloadWithoutTotalStepsRendersStepLine() throws Exception {
        var frame = StateDialogue.fromJson("""
                            {"state":"identity","step":1,
                             "operator":{"state":{"state_id":"project.identity","kind":"operator_wait"},
                               "facts":{"summary":"Choose identity"},
                               "links":[{"id":"next","method":"POST","preselected":true}]}}
                            """, 10, true);

        assertEquals(10, frame.totalSteps());
        assertEquals("[1/10] Identity \u00b7 How should the project be named?",
                            dialogue(new FakeSource(frame), "", new StringWriter()).stepLine(frame));
    }

    @Test
    @DisplayName("a dialogue over a two-aspect list shows 2 in intro and names exactly those two aspects in overview")
    void dialogueFollowsInjectedAspectList() {
        var aspects = List.of(OnboardingAspect.LANGUAGE, OnboardingAspect.HARNESSES);
        var frame = new StateDialogue.Frame(MODE, "operator_wait", 0, 2, null, Map.of(),
                            List.of(new StateDialogue.Link("begin", "next", true)));
        var dialogue = new StateDialogue(new FakeSource(frame), new PrintWriter(new StringWriter()), "en",
                            stateId -> new ChoicePrompt(new BufferedReader(new StringReader("")),
                                                new PrintWriter(new StringWriter()), "en", stateId), aspects);

        var content = dialogue.content(frame);
        assertAll(
                            () -> assertTrue(content.getFirst().startsWith("2 steps."), "intro says 2 steps"),
                            () -> assertEquals(3, content.size(), "intro plus exactly two aspect rows"),
                            () -> assertTrue(content.get(1).contains(
                                                MessageCatalogue.format("en", OnboardingAspect.LANGUAGE.stepNameKey(),
                                                                    Map.of())), content.toString()),
                            () -> assertTrue(content.get(2).contains(
                                                MessageCatalogue.format("en", OnboardingAspect.HARNESSES.stepNameKey(),
                                                                    Map.of())), content.toString()));
    }

    @Test
    @DisplayName("a state of an aspect the run does not inject names no step, never Done")
    void excludedAspectNamesNoStep() {
        var aspects = List.of(OnboardingAspect.LANGUAGE, OnboardingAspect.HARNESSES);
        var dialogue = new StateDialogue(new FakeSource(mode()), new PrintWriter(new StringWriter()), "en",
                            stateId -> new ChoicePrompt(new BufferedReader(new StringReader("")),
                                                new PrintWriter(new StringWriter()), "en", stateId), aspects);
        var rolesFrame = new StateDialogue.Frame("roles.review", "operator_wait", 3, 2, null, Map.of(), List.of());

        assertAll(
                            () -> assertEquals("", dialogue.stepName(rolesFrame)),
                            () -> assertEquals("[3/2] Give every role a harness and a model",
                                                dialogue.stepLine(rolesFrame)),
                            () -> assertFalse(dialogue.stepLine(rolesFrame).contains("Done"),
                                                dialogue.stepLine(rolesFrame)),
                            () -> assertEquals("Language", dialogue.stepName(new StateDialogue.Frame(
                                                "language.choose", "operator_wait", 1, 2, null, Map.of(), List.of()))),
                            () -> assertEquals("Done", dialogue.stepName(new StateDialogue.Frame(
                                                COMPLETED, "completed", 2, 2, null, Map.of(), List.of()))));
    }

    @Test
    @DisplayName("the intro line says the number of steps the run reports")
    void introFollowsTheRunStepCount() {
        var frame = new StateDialogue.Frame(MODE, "operator_wait", 0, 8, null, Map.of(),
                            List.of(new StateDialogue.Link("begin", "next", true)));

        var content = dialogue(new FakeSource(mode()), "", new StringWriter()).content(frame);

        assertTrue(content.getFirst().startsWith("8 steps."), content.getFirst());
        assertFalse(content.getFirst().contains("Seven"), content.getFirst());
    }

    @Test
    @DisplayName("wraps a content line at its width without breaking a word")
    void wrapsContent() {
        assertEquals(List.of("one two", "three"), StateDialogue.wrap("one two three", 8));
        assertEquals(List.of("unbreakable"), StateDialogue.wrap("unbreakable", 5));
    }

    @Test
    @DisplayName("re-reads a linkless working state until the runtime moves on")
    void reReadsAWorkingState() throws Exception {
        var working = new StateDialogue.Frame("harness.probe", "working", 2, 7,
                            "Checking 2 harnesses", Map.of(), List.of());
        var source = new WorkingSource(List.of(working, working, mode(), language(), completed()));
        var writer = new StringWriter();

        var last = dialogue(source, "\n\n", writer).run();

        assertEquals(COMPLETED, last.stateId());
        assertEquals(List.of("begin", "set"), source.sent);
    }

    @Test
    @DisplayName("gives up on a working state that stays without a link past the working timeout")
    void givesUpOnAWorkingStateAfterTheWorkingTimeout() throws Exception {
        var working = new StateDialogue.Frame("harness.probe", "working", 2, 7,
                            "Checking 2 harnesses", Map.of(), List.of());
        var writer = new StringWriter();
        var out = new PrintWriter(writer, true);
        var timeout = Duration.ofMillis(300);
        var dialogue = new StateDialogue(new FakeSource(working), out, "en",
                            stateId -> new ChoicePrompt(new BufferedReader(new StringReader("")), out, "en",
                                                stateId),
                            timeout);

        var started = System.nanoTime();
        var last = dialogue.run();
        var elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertAll(
                            () -> assertEquals(working, last),
                            () -> assertFalse(last.isFinal()),
                            () -> assertTrue(writer.toString().contains(
                                                                    MessageCatalogue.format("en", "onboard.state.waits", Map.of())),
                                                writer.toString()),
                            () -> assertTrue(elapsed.compareTo(timeout) >= 0,
                                                "gave up before the working timeout: " + elapsed),
                            () -> assertTrue(elapsed.compareTo(Duration.ofSeconds(10)) < 0,
                                                "waited past the working timeout: " + elapsed));
    }

    @Test
    @DisplayName("refuses a working timeout that is zero or negative")
    void refusesANonPositiveWorkingTimeout() {
        var source = new FakeSource(completed());
        var out = new PrintWriter(new StringWriter());
        Function<String, ChoicePrompt> prompts = stateId -> new ChoicePrompt(
                            new BufferedReader(new StringReader("")), out, "en", stateId);
        var negative = Duration.ofMillis(-1);

        assertAll(
                            () -> assertThrows(IllegalArgumentException.class,
                                                () -> new StateDialogue(source, out, "en", prompts, Duration.ZERO)),
                            () -> assertThrows(IllegalArgumentException.class,
                                                () -> new StateDialogue(source, out, "en", prompts, negative)));
    }

    @Test
    @DisplayName("the production working timeout stays 60 s")
    void theProductionWorkingTimeoutStaysSixtySeconds() {
        assertEquals(StateDialogue.WORKING_TIMEOUT, Duration.ofSeconds(60));
    }

    /**
     * A runtime whose linkless state advances on the next read, as a working state does.
     */
    private static final class WorkingSource implements StateDialogue.Source {

        private final List<StateDialogue.Frame> frames;
        private final List<String> sent = new ArrayList<>();
        private int index;

        WorkingSource(List<StateDialogue.Frame> frames) {
            this.frames = frames;
        }

        @Override
        public StateDialogue.Frame fetch() {
            var frame = frames.get(index);
            if (frame.links().isEmpty() && index < frames.size() - 1) {
                index++;
            }
            return frame;
        }

        @Override
        public StateDialogue.Frame choose(String linkId) {
            sent.add(linkId);
            index++;
            return frames.get(index);
        }
    }
}
