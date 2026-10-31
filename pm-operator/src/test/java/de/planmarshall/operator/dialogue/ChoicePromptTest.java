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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import de.planmarshall.operator.StartPrompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ChoicePrompt} covering every rule of CLI-UC-P-02 / PLAN-10:
 * <ul>
 *   <li>Choice selection by key, number, and range</li>
 *   <li>Enter takes preselection only when declared</li>
 *   <li>Enter without preselection re-asks and never falls through</li>
 *   <li>{@code ?} prints the dialogue explanation and {@code ?n} the item's explanation, both only
 *       where an explanation is declared</li>
 *   <li>{@code q} (and German {@code v}) leaves with "Left &lt;state&gt;. Nothing was sent."</li>
 *   <li>Unknown input re-asks with offered keys</li>
 *   <li>The chosen answer is echoed</li>
 *   <li>Consent and irreversible acts have no Enter default</li>
 *   <li>Options with consequences render in aligned two-column layout with {@code ←}</li>
 * </ul>
 */
@DisplayName("ChoicePrompt strict choice dialogue rules")
class ChoicePromptTest {

    private static final String STATE = "control.pause";

    private ChoicePrompt createPrompt(String input, StringWriter out, String locale) {
        var reader = new BufferedReader(new StringReader(input));
        var writer = new PrintWriter(out);
        return new ChoicePrompt(reader, writer, locale, STATE);
    }

    @Test
    @DisplayName("rule 1: picks option by key")
    void picksByKey() {
        var out = new StringWriter();
        var prompt = createPrompt("now\n", out, "en");
        prompt.addChoice("now", "now", "stop now", "immediately", false);
        prompt.addChoice("task", "after the current task", "after task", "after current task finishes", true);

        var result = prompt.ask();
        assertNotNull(result);
        assertEquals("now", result.key());
    }

    @Test
    @DisplayName("rule 2: picks option by 1-based number")
    void picksByNumber() {
        var out = new StringWriter();
        var prompt = createPrompt("2\n", out, "en");
        prompt.addChoice("now", "now", "stop now", null, false);
        prompt.addChoice("task", "after the current task", "finish task", null, false);
        prompt.addChoice("phase", "after the current phase", "finish phase", null, false);

        var result = prompt.ask();
        assertNotNull(result);
        assertEquals("task", result.key());
    }

    @Test
    @DisplayName("rule 2: the chosen answer is echoed")
    void echoesTheAnswer() {
        var out = new StringWriter();
        var prompt = createPrompt("\n", out, "en");
        prompt.addChoice("now", "now", null, null, false);
        prompt.addChoice("task", "after the current task", null, null, true);

        prompt.ask();
        assertTrue(out.toString().contains("Chosen: after the current task"), out.toString());
    }

    @Test
    @DisplayName("rule 7: the leave message names the human label, never the state id")
    void leaveNamesTheLabel() {
        var out = new StringWriter();
        var prompt = createPrompt("q\n", out, "en").leaveLabel("the pause");
        prompt.addChoice("now", "now");

        assertThrows(StartPrompt.StartLeftException.class, prompt::ask);
        assertTrue(out.toString().contains("Left the pause. Nothing was sent."), out.toString());
        assertFalse(out.toString().contains(STATE), out.toString());
    }

    @Test
    @DisplayName("rule 3: picks multi-selection ranges and toggles")
    void picksMultiSelectionRanges() {
        var out = new StringWriter();
        var prompt = createPrompt("1-2\nEnter\n", out, "en");
        prompt.addChoice("opt1", "Option 1");
        prompt.addChoice("opt2", "Option 2");
        prompt.addChoice("opt3", "Option 3");

        var results = prompt.askMulti();
        assertEquals(2, results.size());
        assertEquals("opt1", results.get(0).key());
        assertEquals("opt2", results.get(1).key());
    }

    @Test
    @DisplayName("rule 4: Enter takes declared preselection")
    void enterTakesDeclaredPreselection() {
        var out = new StringWriter();
        var prompt = createPrompt("\n", out, "en");
        prompt.addChoice("now", "now", "stop now", null, false);
        prompt.addChoice("task", "after the current task", "finish task", null, true);
        prompt.addChoice("phase", "after the current phase", "finish phase", null, false);

        var result = prompt.ask();
        assertNotNull(result);
        assertEquals("task", result.key());
        assertTrue(result.preselected());
    }

    @Test
    @DisplayName("rule 5: Enter without preselection is rejected and re-asks")
    void enterWithoutPreselectionReAsks() {
        var out = new StringWriter();
        // First Enter (rejected), then valid choice "1"
        var prompt = createPrompt("\n1\n", out, "en");
        prompt.addChoice("opt1", "Option 1");
        prompt.addChoice("opt2", "Option 2");

        var result = prompt.ask();
        assertEquals("opt1", result.key());
        var output = out.toString();
        assertTrue(output.contains("Enter does nothing here; type one of the keys."), output);
    }

    @Test
    @DisplayName("rule 6: ? prints dialogue explanation and re-asks without changing state")
    void questionMarkExplainsDialogue() {
        var out = new StringWriter();
        // First ?, then valid choice "1"
        var prompt = createPrompt("?\n1\n", out, "en");
        prompt.explanation("Pausing lets you inspect work safely before resuming.");
        prompt.addChoice("now", "now");

        var result = prompt.ask();
        assertEquals("now", result.key());
        var output = out.toString();
        assertTrue(output.contains("Pausing lets you inspect work safely before resuming."), output);
    }

    @Test
    @DisplayName("rule 7: ?n prints item n explanation and re-asks without changing state")
    void questionMarkNumberExplainsItem() {
        var out = new StringWriter();
        // First ?2, then valid choice "2"
        var prompt = createPrompt("?2\n2\n", out, "en");
        prompt.addChoice("now", "now", "immediate stop", "Withdraws running jobs immediately", false);
        prompt.addChoice("task", "after the current task", "clean boundary", "Lets active task finish its build", true);

        var result = prompt.ask();
        assertEquals("task", result.key());
        var output = out.toString();
        assertTrue(output.contains("Lets active task finish its build"), output);
    }

    @Test
    @DisplayName("rule 8a: q leaves with 'Left <state>. Nothing was sent.' in English")
    void qLeavesEnglish() {
        var out = new StringWriter();
        var prompt = createPrompt("q\n", out, "en");
        prompt.addChoice("1", "first");

        assertThrows(StartPrompt.StartLeftException.class, prompt::ask);
        var output = out.toString();
        assertTrue(output.contains("Left control.pause. Nothing was sent."), output);
    }

    @Test
    @DisplayName("rule 8b: v leaves with '<state> verlassen. Es wurde nichts gesendet.' in German")
    void vLeavesGerman() {
        var out = new StringWriter();
        var prompt = createPrompt("v\n", out, "de");
        prompt.addChoice("1", "erstes");

        assertThrows(StartPrompt.StartLeftException.class, prompt::ask);
        var output = out.toString();
        assertTrue(output.contains("Verlassen: control.pause. Es wurde nichts gesendet."), output);
    }

    @Test
    @DisplayName("rule 8c: the German leave message names the human label")
    void vLeavesGermanWithLabel() {
        var out = new StringWriter();
        var prompt = createPrompt("v\n", out, "de").leaveLabel("Das Abbrechen");
        prompt.addChoice("1", "erstes");

        assertThrows(StartPrompt.StartLeftException.class, prompt::ask);
        assertTrue(out.toString().contains("Verlassen: Das Abbrechen. Es wurde nichts gesendet."), out.toString());
    }

    @Test
    @DisplayName("rule 12b: a long consequence wraps into its column and never exceeds 80 columns")
    void wrapsLongConsequenceIntoItsColumn() {
        var out = new StringWriter();
        var prompt = createPrompt("1\n", out, "en");
        prompt.addChoice("now", "now",
                            "laufende Arbeit wird sofort zurueckgezogen; sie laeuft beim Start wieder", null, false);
        prompt.addChoice("task", "after the current task", "der laufende Task schliesst ab, dann haelt der Plan an",
                            null, true);

        prompt.ask();
        for (var line : out.toString().split("\n")) {
            assertTrue(line.length() <= 80, line.length() + ": " + line);
        }
        assertTrue(out.toString().contains("laeuft beim Start wieder"), out.toString());
    }

    @Test
    @DisplayName("rule 9: unknown input prints '{input} is not offered here: {offered}' and re-asks")
    void unknownInputReAsksWithOffered() {
        var out = new StringWriter();
        // Type 9 (unknown), then 1 (valid)
        var prompt = createPrompt("9\n1\n", out, "en");
        prompt.addChoice("now", "now");
        prompt.addChoice("task", "after the current task");

        var result = prompt.ask();
        assertEquals("now", result.key());
        var output = out.toString();
        assertTrue(output.contains("9 is not offered here: 1-2, q"), output);
    }

    @Test
    @DisplayName("rule 9b: ? is only offered where something is explained, and never promises what it cannot tell")
    void questionMarkOfferedOnlyWithExplanation() {
        var without = new StringWriter();
        var plain = createPrompt("?\n1\n", without, "en");
        plain.addChoice("now", "now");
        assertEquals("now", plain.ask().key());
        assertFalse(without.toString().contains("q, ?"), without.toString());
        assertTrue(without.toString().contains("Nothing more is explained here"), without.toString());

        var with = new StringWriter();
        var explained = createPrompt("9\n\n?\n1\n", with, "en");
        explained.explanation("The pause keeps the work ready to resume.");
        explained.addChoice("now", "now");
        assertEquals("now", explained.ask().key());
        assertTrue(with.toString().contains("9 is not offered here: 1, q, ?"), with.toString());
        assertTrue(with.toString().contains("Enter does nothing here"), with.toString());
        assertTrue(with.toString().contains("The pause keeps the work ready to resume."), with.toString());
    }

    @Test
    @DisplayName("rule 10a: consent has no Enter default and prints 'Type y or n.' when Enter is typed")
    void consentEnterPromptsTypeYn() {
        var out = new StringWriter();
        // First Enter (rejected), then y
        var prompt = createPrompt("\ny\n", out, "en");

        assertTrue(prompt.askConsent());
        var output = out.toString();
        assertTrue(output.contains("Type y or n."), output);
    }

    @Test
    @DisplayName("rule 10b: a consent without explanation offers no ?")
    void consentWithoutExplanationOffersNoQuestionMark() {
        var out = new StringWriter();
        var prompt = createPrompt("\ny\n", out, "en");

        assertTrue(prompt.askConsent());
        assertTrue(out.toString().contains("y) yes   n) no   q) leave   \u203a "), out.toString());
    }

    @Test
    @DisplayName("rule 10c: consent Enter in German prints 'Gib j oder n ein.'")
    void consentGermanEnter() {
        var out = new StringWriter();
        // First Enter (rejected), then j
        var prompt = createPrompt("\nj\n", out, "de");

        assertTrue(prompt.askConsent());
        var output = out.toString();
        assertTrue(output.contains("Gib j oder n ein."), output);
    }

    @Test
    @DisplayName("rule 9c: a single choice offers '1', and an unknown item explanation says there is nothing")
    void singleChoiceOffersOneAndExplainsNothing() {
        var out = new StringWriter();
        var prompt = createPrompt("?9\n9\n1\n", out, "en");
        prompt.addChoice("only", "Only one");

        assertEquals("only", prompt.ask().key());
        assertTrue(out.toString().contains("Nothing more is explained here"), out.toString());
        assertTrue(out.toString().contains("9 is not offered here: 1, q"), out.toString());
    }

    @Test
    @DisplayName("rule 11: consent accepts y, j and n, and nothing else")
    void consentAcceptedKeys() {
        var out1 = new StringWriter();
        var prompt1 = createPrompt("y\n", out1, "en");
        assertTrue(prompt1.askConsent());

        var out2 = new StringWriter();
        var prompt2 = createPrompt("j\n", out2, "de");
        assertTrue(prompt2.askConsent());

        var out3 = new StringWriter();
        var prompt3 = createPrompt("n\n", out3, "de");
        assertFalse(prompt3.askConsent());

        var out4 = new StringWriter();
        var prompt4 = createPrompt("r\nn\n", out4, "en");
        assertFalse(prompt4.askConsent());
        assertTrue(out4.toString().contains("r is not offered here: y, n, q"), out4.toString());
    }

    @Test
    @DisplayName("rule 11b: a consent with an explanation offers ? and explains it")
    void consentWithExplanationOffersQuestionMark() {
        var out = new StringWriter();
        var prompt = createPrompt("?\ny\n", out, "en");
        prompt.explanation("Saying yes ends the work now.");

        assertTrue(prompt.askConsent());
        assertTrue(out.toString().contains("y) yes   n) no   q) leave   ?   \u203a "), out.toString());
        assertTrue(out.toString().contains("Saying yes ends the work now."), out.toString());
    }

    @Test
    @DisplayName("rule 11c: q leaves a consent, and an unknown German input offers the German keys")
    void consentLeavesAndOffersGermanKeys() {
        var leaving = new StringWriter();
        var quit = createPrompt("q\n", leaving, "en");
        assertThrows(StartPrompt.StartLeftException.class, quit::askConsent);
        assertTrue(leaving.toString().contains("Left control.pause. Nothing was sent."), leaving.toString());

        var out = new StringWriter();
        var prompt = createPrompt("x\nj\n", out, "de");
        assertTrue(prompt.askConsent());
        assertTrue(out.toString().contains("x wird hier nicht angeboten: j, n, v"), out.toString());
    }

    @Test
    @DisplayName("rule 11d: an empty stream leaves the consent instead of blocking")
    void consentLeavesOnEndOfStream() {
        var out = new StringWriter();
        var prompt = createPrompt("", out, "en");

        assertThrows(StartPrompt.StartLeftException.class, prompt::askConsent);
        assertTrue(out.toString().contains("Nothing was sent."), out.toString());
    }

    @Test
    @DisplayName("rule 3b: an empty stream leaves the choice prompt")
    void leavesOnEndOfStream() {
        var out = new StringWriter();
        var prompt = createPrompt("", out, "en").leaveLabel("the pause");
        prompt.addChoice("now", "now");

        assertThrows(StartPrompt.StartLeftException.class, prompt::ask);
        assertTrue(out.toString().contains("Left the pause. Nothing was sent."), out.toString());
    }

    @Test
    @DisplayName("rule 3c: a read failure is reported, never swallowed")
    void readFailureIsReported() {
        var reader = new BufferedReader(new StringReader("")) {
            @Override
            public String readLine() throws IOException {
                throw new IOException("terminal gone");
            }
        };
        var prompt = new ChoicePrompt(reader, new PrintWriter(new StringWriter()), "en", STATE);
        prompt.addChoice("now", "now");

        var failure = assertThrows(IllegalStateException.class, prompt::ask);
        assertTrue(failure.getMessage().contains("terminal gone"), failure.getMessage());
    }

    @Test
    @DisplayName("rule 13: multi-selection toggles keys, ranges and numbers, and re-asks an unknown input")
    void multiSelectionTogglesAndReAsks() {
        var out = new StringWriter();
        var prompt = createPrompt("opt1\n9\n2 2\n3-3\na-b\nEnter\n", out, "en");
        prompt.addChoice("opt1", "Option 1");
        prompt.addChoice("opt2", "Option 2");
        prompt.addChoice("opt3", "Option 3");

        var results = prompt.askMulti();
        assertEquals(2, results.size());
        assertEquals("opt1", results.get(0).key());
        assertEquals("opt3", results.get(1).key());
        assertTrue(out.toString().contains("9 is not offered here: 1-3, q"), out.toString());
        assertTrue(out.toString().contains("a-b is not offered here: 1-3, q"), out.toString());
    }

    @Test
    @DisplayName("rule 13b: multi-selection starts from its preselection, explains, and leaves on q")
    void multiSelectionPreselectionExplanationAndLeave() {
        var out = new StringWriter();
        var prompt = createPrompt("?\nq\n", out, "en").leaveLabel("the pause");
        prompt.explanation("Pick the plans to keep.");
        prompt.addChoice("now", "now", null, null, true);
        prompt.addChoice("task", "after the current task");

        assertThrows(StartPrompt.StartLeftException.class, prompt::askMulti);
        assertTrue(out.toString().contains("[\u2713] now"), out.toString());
        assertTrue(out.toString().contains("Pick the plans to keep."), out.toString());
        assertTrue(out.toString().contains("Left the pause. Nothing was sent."), out.toString());
    }

    @Test
    @DisplayName("rule 12: rendering options with consequences in aligned two-column layout with preselection arrow")
    void rendersTwoColumnOptionsWithConsequences() {
        var out = new StringWriter();
        var prompt = createPrompt("1\n", out, "en");
        prompt.title("Pause fix-relay?");
        prompt.addChoice("now", "now", "2 tasks withdrawn, 1 job stopped; they run again on start", null, false);
        prompt.addChoice("task", "after the current task", "implement · 4 min, build · 2 min finish", null, true);
        prompt.addChoice("phase", "after the current phase", "execute runs to its end: 3 of 7 tasks left", null, false);

        prompt.ask();
        var output = out.toString();
        assertTrue(output.contains("1) now"), output);
        assertTrue(output.contains("2) after the current task \u2190"), output);
        assertTrue(output.contains("2 tasks withdrawn, 1 job stopped;"), output);
        assertTrue(output.contains("on start"), output);
        assertTrue(output.contains("implement \u00b7 4 min, build \u00b7 2 min finish"), output);
    }

    @Test
    @DisplayName("rule 14: a declared keep key renders below the choices and leaves without printing")
    void keepKeyRendersAndLeavesSilently() {
        var out = new StringWriter();
        var prompt = createPrompt("k\n", out, "en").keepKey("k) keep it running");
        prompt.addChoice("drain", "Stop when done", null, null, true);
        prompt.addChoice("now", "Stop now", null, null, false);

        assertThrows(ChoicePrompt.KeepException.class, prompt::ask);
        var output = out.toString();
        assertTrue(output.contains("k) keep it running"), output);
        assertFalse(output.contains("Nothing was sent."), output);
    }

    @Test
    @DisplayName("rule 14: the keep key is l in German and is offered with the other keys")
    void keepKeyIsGermanAndOffered() {
        var out = new StringWriter();
        var prompt = createPrompt("9\nl\n", out, "de").keepKey("l) lass sie laufen");
        prompt.addChoice("drain", "Wenn fertig", null, null, true);
        prompt.addChoice("now", "Jetzt", null, null, false);

        assertThrows(ChoicePrompt.KeepException.class, prompt::ask);
        var output = out.toString();
        assertTrue(output.contains("l) lass sie laufen"), output);
        assertTrue(output.contains("1-2, v, l"), output);
    }
}
