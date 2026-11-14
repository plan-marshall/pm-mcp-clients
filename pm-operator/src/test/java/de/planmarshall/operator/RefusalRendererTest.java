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

import de.cuioss.test.generator.Generators;
import de.cuioss.test.generator.junit.EnableGeneratorController;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import de.planmarshall.api.resource.ApiErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RefusalRenderer renders refusals into the three-line pattern")
@EnableGeneratorController
class RefusalRendererTest {

    @Test
    @DisplayName("renders known code in English without HTTP status, JSON, or exception class")
    void knownCodeEnglish() {
        var lines = RefusalRenderer.renderLines("en", ApiErrorCode.SCOPE_NOT_FOUND);
        assertEquals(3, lines.size());
        assertEquals("✗ This entry does not exist", lines.get(0));
        assertEquals("No plan, epic, decision, report or entry carries this id.", lines.get(1));
        assertEquals("→ Check the id or pick the entry from the list again.", lines.get(2));
        assertNoLeaks(lines);
    }

    @Test
    @DisplayName("renders known code in German without HTTP status, JSON, or exception class")
    void knownCodeGerman() {
        var lines = RefusalRenderer.renderLines("de", ApiErrorCode.SCOPE_NOT_FOUND);
        assertEquals(3, lines.size());
        assertEquals("✗ Diesen Eintrag gibt es nicht", lines.get(0));
        assertEquals("Kein Plan, kein Epic, keine Entscheidung, kein Bericht und kein Eintrag trägt diese Kennung.", lines.get(1));
        assertEquals("→ Prüfe die Kennung oder wähle den Eintrag erneut aus der Liste.", lines.get(2));
        assertNoLeaks(lines);
    }

    @Test
    @DisplayName("renders unknown code without server message in English")
    void unknownCodeWithoutMessageEnglish() {
        var lines = RefusalRenderer.renderLines("en", null);
        assertEquals(3, lines.size());
        assertEquals("✗ The runtime refused the request", lines.get(0));
        assertEquals("The refusal carries a code this client does not know.", lines.get(1));
        assertEquals("→ Update the client and try again; if the refusal stays, look at the details.", lines.get(2));
        assertNoLeaks(lines);
    }

    @Test
    @DisplayName("renders unknown code without server message in German")
    void unknownCodeWithoutMessageGerman() {
        var lines = RefusalRenderer.renderLines("de", null);
        assertEquals(3, lines.size());
        assertEquals("✗ Die Runtime hat die Anfrage abgelehnt", lines.get(0));
        assertEquals("Die Ablehnung trägt einen Code, den dieser Client nicht kennt.", lines.get(1));
        assertEquals("→ Aktualisiere den Client und versuche es erneut; bleibt die Ablehnung, sieh dir die Details an.", lines.get(2));
        assertNoLeaks(lines);
    }

    @Test
    @DisplayName("renders unknown code with server message in English")
    void unknownCodeWithMessageEnglish() {
        var msg = Generators.letterStrings(5, 15).next();
        var lines = RefusalRenderer.renderLines("en", null, msg);
        assertEquals(3, lines.size());
        assertEquals("✗ The runtime refused: " + msg, lines.get(0));
        assertEquals("The refusal carries a code this client does not know.", lines.get(1));
        assertEquals("→ Update the client and try again; if the refusal stays, look at the details.", lines.get(2));
        assertNoLeaks(lines);
    }

    @Test
    @DisplayName("renders unknown code with server message in German")
    void unknownCodeWithMessageGerman() {
        var msg = Generators.letterStrings(5, 15).next();
        var lines = RefusalRenderer.renderLines("de", null, msg);
        assertEquals(3, lines.size());
        assertEquals("✗ Die Runtime hat die Anfrage abgelehnt: " + msg, lines.get(0));
        assertEquals("Die Ablehnung trägt einen Code, den dieser Client nicht kennt.", lines.get(1));
        assertEquals("→ Aktualisiere den Client und versuche es erneut; bleibt die Ablehnung, sieh dir die Details an.", lines.get(2));
        assertNoLeaks(lines);
    }

    @Test
    @DisplayName("sanitizes server message with ANSI control sequences")
    void sanitizesServerMessage() {
        var rawMsg = "\u001B[31mcolor\u001B[0m\r\nmessage";
        var lines = RefusalRenderer.renderLines("en", null, rawMsg);
        assertEquals(3, lines.size());
        assertEquals("✗ The runtime refused: color message", lines.get(0));
    }

    @Test
    @DisplayName("formats arguments in fix line for codes that require them")
    void formatsFixArguments() {
        var project = Generators.letterStrings(4, 10).next();
        var lines = RefusalRenderer.renderLines("en", ApiErrorCode.ENROLMENT_TAMPERED, null, Map.of("project_id", project));
        assertEquals(3, lines.size());
        assertTrue(lines.get(2).contains(project), "Fix line should contain project name: " + lines.get(2));
        assertNoLeaks(lines);
    }

    @Test
    @DisplayName("render(PrintWriter) writes all three lines")
    void renderToPrintWriter() {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        RefusalRenderer.render(pw, "en", ApiErrorCode.SCOPE_NOT_FOUND);
        var output = sw.toString();
        assertTrue(output.contains("✗ This entry does not exist\n") || output.contains("✗ This entry does not exist\r\n"));
        assertTrue(output.contains("No plan, epic, decision, report or entry carries this id."));
        assertTrue(output.contains("→ Check the id or pick the entry from the list again."));
    }

    @Test
    @DisplayName("render(PrintWriter, String, OperatorRefusedException) renders exception")
    void renderOperatorRefusedException() {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        var ore = new OperatorRefusedException(ApiErrorCode.SCOPE_NOT_FOUND);
        RefusalRenderer.render(pw, "de", ore);
        var output = sw.toString();
        assertTrue(output.contains("✗ Diesen Eintrag gibt es nicht"));
        assertTrue(output.contains("Kein Plan, kein Epic, keine Entscheidung"));
    }

    private static void assertNoLeaks(List<String> lines) {
        for (var line : lines) {
            assertFalse(line.contains("HTTP "), "Should not leak HTTP status: " + line);
            assertFalse(line.contains("404"), "Should not leak 404: " + line);
            assertFalse(line.contains("500"), "Should not leak 500: " + line);
            assertFalse(line.contains("{"), "Should not leak JSON: " + line);
            assertFalse(line.contains("}"), "Should not leak JSON: " + line);
            assertFalse(line.contains("OperatorRefusedException"), "Should not leak exception class: " + line);
            assertFalse(line.contains("Exception"), "Should not leak exception name: " + line);
        }
    }
}
